/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
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

/**
 * A segment of a buffer.
 *
 * Each segment in a buffer is a circularly-linked list node referencing the following and
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
@PublishedApi
internal class Segment {
    @JvmField
    @PublishedApi
    internal val data: ByteArray

    /** The next byte of application data byte to read in this segment. */
    @JvmField
    @PublishedApi
    internal var pos: Int = 0

    /**
     * The first byte of available data ready to be written to.
     *
     * If the segment is free and linked in the segment pool, the field contains total
     * byte count of this and next segments.
     */
    @JvmField
    @PublishedApi
    internal var limit: Int = 0

    /** True if other segments or byte strings use the same byte array. */
    @JvmField
    internal var shared: Boolean = false

    /** True if this segment owns the byte array and can append to it, extending `limit`. */
    @JvmField
    internal var owner: Boolean = false

    /** Next segment in a linked or circularly-linked list. */
    @JvmField
    @PublishedApi
    internal var next: Segment? = null

    /** Previous segment in a circularly-linked list. */
    @JvmField
    internal var prev: Segment? = null

    internal constructor() {
        this.data = ByteArray(SIZE)
        this.owner = true
        this.shared = false
    }

    internal constructor(data: ByteArray, pos: Int, limit: Int, shared: Boolean, owner: Boolean) {
        this.data = data
        this.pos = pos
        this.limit = limit
        this.shared = shared
        this.owner = owner
    }

    /**
     * Returns a new segment that shares the underlying byte array with this. Adjusting pos and limit
     * are safe but writes are forbidden. This also marks the current segment as shared, which
     * prevents it from being pooled.
     */
    internal fun sharedCopy(): Segment {
        shared = true
        return Segment(data, pos, limit, true, false)
    }

    /** Returns a new segment that its own private copy of the underlying byte array.  */
    internal fun unsharedCopy() = Segment(data.copyOf(), pos, limit, false, true)

    /**
     * Removes this segment of a circularly-linked list and returns its successor.
     * Returns null if the list is now empty.
     */
    internal fun pop(): Segment? {
        val result = if (next !== this) next else null
        prev!!.next = next
        next!!.prev = prev
        next = null
        prev = null
        return result
    }

    /**
     * Appends `segment` after this segment in the circularly-linked list. Returns the pushed segment.
     */
    internal fun push(segment: Segment): Segment {
        segment.prev = this
        segment.next = next
        next!!.prev = segment
        next = segment
        return segment
    }

    /**
     * Splits this head of a circularly-linked list into two segments. The first segment contains the
     * data in `[pos..pos+byteCount)`. The second segment contains the data in
     * `[pos+byteCount..limit)`. This can be useful when moving partial segments from one buffer to
     * another.
     *
     * Returns the new head of the circularly-linked list.
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
        prev!!.push(prefix)
        return prefix
    }

    /**
     * Call this when the tail and its predecessor may both be less than half full. This will copy
     * data so that segments can be recycled.
     */
    internal fun compact() {
        check(prev !== this) { "cannot compact" }
        if (!prev!!.owner) return // Cannot compact: prev isn't writable.
        val byteCount = limit - pos
        val availableByteCount = SIZE - prev!!.limit + if (prev!!.shared) 0 else prev!!.pos
        if (byteCount > availableByteCount) return // Cannot compact: not enough writable space.
        writeTo(prev!!, byteCount)
        pop()
        SegmentPool.recycle(this)
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

    internal val size: Int
        get() = limit - pos

    internal companion object {
        /** The size of all segments in bytes.  */
        internal const val SIZE = 8192

        /** Segments will be shared when doing so avoids `arraycopy()` of this many bytes.  */
        internal const val SHARE_MINIMUM = 1024
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
    // require(startOffset in 0 until size)
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
internal fun Segment.indexOfBytesOutbound(bytes: ByteArray, startOffset: Int, head: Segment?): Int {
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
                val next = seg.next
                if (next === head) return -1
                seg = next!!
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
