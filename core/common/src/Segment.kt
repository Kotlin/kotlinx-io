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

import kotlinx.io.unsafe.UnsafeSegmentAccessors

/**
 * Context for writing data into [Segment].
 */
public sealed interface SegmentSetContext {
    /**
     * Writes [value] to [index]-position within this segment.
     * [index] value must be in range `[0, Segment.capacity)`.
     *
     * Unlike [Segment.writeByte] this function does not affect [Segment.size], i.e., the value is not available for
     * reading immediately after calling this method.
     * To publish or commit written data, this method should be used inside [Buffer.writeUnbound].
     *
     * @param value the value to be written.
     * @param index the index of byte to read from the segment.
     *
     * @throws IllegalArgumentException if [index] is negative or greater or equal to [Segment.remainingCapacity].
     */
    public fun Segment.setChecked(index: Int, value: Byte)
}

/**
 * A segment of a buffer.
 *
 * Each segment in a buffer is a list node referencing the following and
 * preceding segments in the buffer.
 * Buffer's head segment refers to null as its preceding segment and
 * the tail segment refers to null as its following segment.
 *
 * Each segment in the pool is a singly-linked list node referencing the rest of segments in the pool.
 *
 * The underlying byte arrays of segments may be shared between buffers and byte strings. When a
 * segment's byte array is shared the segment may not be recycled, nor may its byte data be changed.
 * The lone exception is that the owner segment is allowed to append to the segment, writing data at
 * `limit` and beyond. There is a single owning segment for each byte array. Positions,
 * limits, prev, and next references are not shared.
 */
public expect class Segment {
    @PublishedApi
    internal val rawData: Any
    @PublishedApi
    internal var pos: Int
    @PublishedApi
    internal var limit: Int
    internal var shared: Boolean
    internal var owner: Boolean
    internal var next: Segment?
    internal var prev: Segment?

    internal constructor()
    internal fun sharedCopy(): Segment
    @PublishedApi
    internal fun pop(): Segment?
    internal fun push(segment: Segment): Segment
    internal fun split(byteCount: Int): Segment
    internal fun compact(): Segment
    internal fun writeTo(sink: Segment, byteCount: Int)

    public val size: Int

    public val remainingCapacity: Int

    internal fun writeByte(byte: Byte)
    internal fun writeShort(short: Short)
    internal fun writeInt(int: Int)
    internal fun writeLong(long: Long)
    internal fun readByte(): Byte
    internal fun readShort(): Short
    internal fun readInt(): Int
    internal fun readLong(): Long
    internal fun readTo(dst: ByteArray, dstStartOffset: Int, dstEndOffset: Int)
    internal fun write(src: ByteArray, srcStartOffset: Int, srcEndOffset: Int)
    public fun getChecked(index: Int): Byte
    internal fun getUnchecked(index: Int): Byte
    internal fun setChecked(index: Int, value: Byte)
    @PublishedApi
    internal fun setUnchecked(index: Int, value: Byte)

    @PublishedApi
    internal fun setUnchecked(index: Int, b0: Byte, b1: Byte)

    @PublishedApi
    internal fun setUnchecked(index: Int, b0: Byte, b1: Byte, b2: Byte)

    @PublishedApi
    internal fun setUnchecked(index: Int, b0: Byte, b1: Byte, b2: Byte, b3: Byte)

    internal companion object {
        /** The size of all segments in bytes.  */
        internal val SIZE: Int

        /** Segments will be shared when doing so avoids `arraycopy()` of this many bytes.  */
        internal val SHARE_MINIMUM: Int
    }
}

@OptIn(UnsafeIoApi::class)
internal fun Segment.indexOf(byte: Byte, startOffset: Int, endOffset: Int): Int {
    // TODO: replace with assert
    /*
    require(startOffset in 0 until size) {
        "$startOffset"
    }
    require(endOffset in startOffset..size) {
        "$endOffset"
    }
     */
    for (idx in startOffset until endOffset) {
        if (UnsafeSegmentAccessors.getUnchecked(this, + idx) == byte) {
            return idx
        }
    }
    return -1
}

/**
 * Checks if the segment is empty.
 * Empty segment contains `0` readable bytes (i.e., its [Segment.size] is `0`).
 */
public fun Segment.isEmpty(): Boolean = size == 0

/**
 * Searches for a `bytes` pattern within this segment starting at the offset `startOffset`.
 * `startOffset` is relative and should be within `[0, size)`.
 */
@OptIn(UnsafeIoApi::class)
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
            if (UnsafeSegmentAccessors.getUnchecked(this, + idx + innerIdx) != bytes[innerIdx]) {
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
@OptIn(UnsafeIoApi::class)
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
                val next = seg.next
                if (next === null) return -1
                seg = next
                scanOffset = 0 // we're scanning the next segment right from the beginning
            }
            if (element != UnsafeSegmentAccessors.getUnchecked(seg, scanOffset)) {
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
