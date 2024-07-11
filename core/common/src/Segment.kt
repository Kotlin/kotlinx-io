/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kotlinx.io

import kotlin.jvm.JvmField
import kotlin.jvm.JvmSynthetic

/**
 * Tracks shared segment copies.
 *
 * A new [SegmentCopyTracker] instance should be not shared by default (i.e. `shared == false`).
 * Any further [addCopy] calls should move the tracker to a shared state (i.e. `shared == true`).
 * Once a shared segment copy is recycled, [removeCopy] should be called.
 * Depending on implementation, calling [removeCopy] the same number of times as [addCopy] may
 * or may not transition the tracked back to unshared stated.
 *
 * The class is not intended for public use and currently designed to fit the only use case - within JVM SegmentPool
 * implementation.
 */
internal abstract class SegmentCopyTracker {
    /**
     * `true` if a tracker shared by multiple segment copies.
     */
    abstract val shared: Boolean

    /**
     * Track a new copy created by sharing an associated segment.
     */
    abstract fun addCopy()

    /**
     * Records reclamation of a shared segment copy associated with this tracker.
     * If a tracker was in unshared state, this call should not affect an internal state.
     *
     * @return `true` if the segment was not shared *before* this called.
     */
    abstract fun removeCopy(): Boolean
}

/**
 * Simple [SegmentCopyTracker] that always reports shared state.
 */
internal object AlwaysSharedCopyTracker : SegmentCopyTracker() {
    override val shared: Boolean = true
    override fun addCopy() = Unit
    override fun removeCopy(): Boolean = true
}

/**
 * A segment of a buffer.
 *
 * Each segment in a buffer is a doubly-linked list node referencing the following and
 * preceding segments in the buffer.
 *
 * Each segment in the pool is a singly-linked list node referencing the rest of segments in the pool.
 *
 * The underlying byte arrays of segments may be shared between buffers and byte strings. When a
 * segment's byte array is shared the segment may not be recycled, nor may its byte data be changed.
 * The lone exception is that the owner segment is allowed to append to the segment, writing data at
 * `limit` and beyond. There is a single owning segment for each byte array. Positions,
 * limits, prev, and next references are not shared.
 */
public class Segment {
    @JvmField
    internal val data: ByteArray

    /** The next byte of application data byte to read in this segment. */
    @PublishedApi
    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var pos: Int = 0

    /**
     * The first byte of available data ready to be written to.
     *
     * If the segment is free and linked in the segment pool, the field contains total
     * byte count of this and next segments.
     */
    @PublishedApi
    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var limit: Int = 0

    /** True if other segments or byte strings use the same byte array. */
    internal val shared: Boolean
        get() = copyTracker?.shared ?: false

    /**
     * Tracks number shared copies
     *
     * Note that this reference is not `@Volatile` as segments are not thread-safe and it's an error
     * to modify the same segment concurrently.
     * At the same time, an object [copyTracker] refers to could be modified concurrently.
     */
    internal var copyTracker: SegmentCopyTracker? = null

    /** True if this segment owns the byte array and can append to it, extending `limit`. */
    @JvmField
    internal var owner: Boolean = false

    /** Next segment in a list, or null. */
    @PublishedApi
    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var next: Segment? = null

    /** Previous segment in the list, or null. */
    @JvmField
    internal var prev: Segment? = null

    private constructor() {
        this.data = ByteArray(SIZE)
        this.owner = true
        this.copyTracker = null
    }

    private constructor(data: ByteArray, pos: Int, limit: Int, shareToken: SegmentCopyTracker?, owner: Boolean) {
        this.data = data
        this.pos = pos
        this.limit = limit
        this.copyTracker = shareToken
        this.owner = owner
    }

    /**
     * Returns a new segment that shares the underlying byte array with this. Adjusting pos and limit
     * are safe but writes are forbidden. This also marks the current segment as shared, which
     * prevents it from being pooled.
     */
    internal fun sharedCopy(): Segment {
        val t = copyTracker ?: SegmentPool.tracker().also {
            copyTracker = it
        }
        return Segment(data, pos, limit, t.also { it.addCopy() }, false)
    }

    /**
     * Removes this segment of a list and returns its successor.
     * Returns null if the list is now empty.
     */
    internal fun pop(): Segment? {
        val result = this.next
        if (this.prev != null) {
            this.prev!!.next = this.next
        }
        if (this.next != null) {
            this.next!!.prev = this.prev
        }
        this.next = null
        this.prev = null
        return result
    }

    /**
     * Appends `segment` after this segment in the list. Returns the pushed segment.
     */
    internal fun push(segment: Segment): Segment {
        segment.prev = this
        segment.next = this.next
        if (this.next != null) {
            this.next!!.prev = segment
        }
        this.next = segment
        return segment
    }

    /**
     * Splits this head of a list into two segments. The first segment contains the
     * data in `[pos..pos+byteCount)`. The second segment contains the data in
     * `[pos+byteCount..limit)`. This can be useful when moving partial segments from one buffer to
     * another.
     *
     * Returns the new head of the list.
     */
    internal fun split(byteCount: Int): Segment {
        require(byteCount > 0 && byteCount <= limit - pos) { "byteCount out of range" }
        val prefix: Segment

        // We have two competing performance goals:
        //  - Avoid copying data. We accomplish this by sharing segments.
        //  - Avoid short shared segments. These are bad for performance because they are readonly and
        //    may lead to long chains of short segments.
        // To balance these goals we only share segments when the copy will be large.
        if (byteCount >= SHARE_MINIMUM) {
            prefix = sharedCopy()
        } else {
            prefix = SegmentPool.take()
            data.copyInto(prefix.data, startIndex = pos, endIndex = pos + byteCount)
        }

        prefix.limit = prefix.pos + byteCount
        pos += byteCount
        if (this.prev != null) {
            this.prev!!.push(prefix)
        } else {
            prefix.next = this
            this.prev = prefix
        }
        return prefix
    }

    /**
     * Call this when the tail and its predecessor may both be less than half full. This will copy
     * data so that segments can be recycled.
     */
    internal fun compact(): Segment {
        check(this.prev != null) { "cannot compact" }
        if (!this.prev!!.owner) return this // Cannot compact: prev isn't writable.
        val byteCount = limit - pos
        val availableByteCount = SIZE - this.prev!!.limit + if (this.prev!!.shared) 0 else this.prev!!.pos
        if (byteCount > availableByteCount) return this // Cannot compact: not enough writable space.
        val predecessor = this.prev
        writeTo(predecessor!!, byteCount)
        val successor = pop()
        check(successor == null)
        SegmentPool.recycle(this)
        return predecessor
    }

    /** Moves `byteCount` bytes from this segment to `sink`.  */
    internal fun writeTo(sink: Segment, byteCount: Int) {
        check(sink.owner) { "only owner can write" }
        if (sink.limit + byteCount > SIZE) {
            // We can't fit byteCount bytes at the sink's current position. Shift sink first.
            if (sink.shared) throw IllegalArgumentException()
            if (sink.limit + byteCount - sink.pos > SIZE) throw IllegalArgumentException()
            sink.data.copyInto(sink.data, startIndex = sink.pos, endIndex = sink.limit)
            sink.limit -= sink.pos
            sink.pos = 0
        }

        data.copyInto(
            sink.data, destinationOffset = sink.limit, startIndex = pos,
            endIndex = pos + byteCount
        )
        sink.limit += byteCount
        pos += byteCount
    }

    @PublishedApi
    @get:JvmSynthetic
    internal val size: Int
        get() = limit - pos

    @PublishedApi
    @get:JvmSynthetic
    internal val remainingCapacity: Int
        get() = data.size - limit

    /**
     * Return a byte-array view over internal data.
     *
     * Returned array contains data layed out so that a readable slice starts at
     * [Segment.pos] and ends at [Segment.limit], writable slice starts at [Segment.limit]
     * and spans over [Segment.remainingCapacity] bytes.
     *
     * This method exists only to preserve binary compatibility if a segment's internal
     * container eventually changes from ByteArray to something else.
     */
    @PublishedApi
    @JvmSynthetic
    @Suppress("UNUSED_PARAMETER")
    internal fun dataAsByteArray(readOnly: Boolean): ByteArray = data

    /**
     * Write back all modifications that were made to a view returned from [dataAsByteArray].
     *
     * This method exists only to preserve binary compatibility if a segment's internal
     * container eventually changes from ByteArray to something else.
     */
    @PublishedApi
    @JvmSynthetic
    @Suppress("UNUSED_PARAMETER")
    internal fun writeBackData(data: ByteArray, bytesToCommit: Int): Unit = Unit

    internal companion object {
        /** The size of all segments in bytes.  */
        internal const val SIZE = 8192

        /** Segments will be shared when doing so avoids `arraycopy()` of this many bytes.  */
        internal const val SHARE_MINIMUM = 1024

        @JvmSynthetic
        internal fun new(): Segment = Segment()

        @JvmSynthetic
        internal fun new(
            data: ByteArray,
            pos: Int,
            limit: Int,
            copyTracker: SegmentCopyTracker?,
            owner: Boolean
        ): Segment = Segment(data, pos, limit, copyTracker, owner)
    }
}

internal fun Segment.indexOf(byte: Byte, startOffset: Int, endOffset: Int): Int {
    require(startOffset in 0 until size) {
        "$startOffset"
    }
    require(endOffset in startOffset..size) {
        "$endOffset"
    }
    val p = pos
    for (idx in startOffset until endOffset) {
        if (data[p + idx] == byte) {
            return idx
        }
    }
    return -1
}

/**
 * Searches for a `bytes` pattern within this segment starting at the offset `startOffset`.
 * `startOffset` is relative and should be within `[0, size)`.
 */
internal fun Segment.indexOfBytesInbound(bytes: ByteArray, startOffset: Int): Int {
    var offset = startOffset
    val limit = size - bytes.size + 1
    val firstByte = bytes[0]
    while (offset < limit) {
        val idx = indexOf(firstByte, offset, limit)
        if (idx < 0) {
            return -1
        }
        var found = true
        for (innerIdx in 1 until bytes.size) {
            if (data[pos + idx + innerIdx] != bytes[innerIdx]) {
                found = false
                break
            }
        }
        if (found) {
            return idx
        } else {
            offset++
        }
    }
    return -1
}

/**
 * Searches for a `bytes` pattern starting in between offset `startOffset` and `size` within this segment
 * and continued in the following segments.
 * `startOffset` is relative and should be within `[0, size)`.
 */
internal fun Segment.indexOfBytesOutbound(bytes: ByteArray, startOffset: Int): Int {
    var offset = startOffset
    val firstByte = bytes[0]

    while (offset in 0 until size) {
        val idx = indexOf(firstByte, offset, size)
        if (idx < 0) {
            return -1
        }
        // The pattern should start in this segment
        var seg = this
        var scanOffset = offset

        var found = true
        for (element in bytes) {
            // We ran out of bytes in this segment,
            // so let's take the next one and continue the scan there.
            if (scanOffset == seg.size) {
                val next = seg.next ?: return -1
                seg = next
                scanOffset = 0 // we're scanning the next segment right from the beginning
            }
            if (element != seg.data[seg.pos + scanOffset]) {
                found = false
                break
            }
            scanOffset++
        }
        if (found) {
            return offset
        }
        offset++
    }
    return -1
}

@PublishedApi
internal fun Segment.isEmpty(): Boolean = size == 0
