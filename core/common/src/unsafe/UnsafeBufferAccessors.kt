/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.*

@UnsafeIoApi
public object UnsafeBufferAccessors {
    /**
     * Allocates at least [minimumCapacity] bytes of space for writing and supplies it to [block] in form of [Segment].
     * Actual number of bytes available for writing may exceed [minimumCapacity] and could be checked using
     * [Segment.remainingCapacity].
     *
     * [block] can write into [Segment] using [SegmentWriteContext.setChecked].
     * Data written into [Segment] will not be available for reading from the buffer until [block] returned.
     * A value returned from [block] represent the length of [Segment]'s prefix that should be appended to the buffer.
     * That value may be less or greater than [minimumCapacity], but it should be non-negative and should not exceed
     * [Segment.remainingCapacity].
     *
     * @param buffer the buffer to write into.
     * @param minimumCapacity the minimum number of bytes that could be written into a segment
     * that will be supplied into [block].
     * @param block the block writing data into provided [Segment], should return the number of consecutive bytes
     * that will be appended to the buffer.
     *
     * @throws IllegalArgumentException when [minimumCapacity] is negative or exceeds the maximum size of a segment.
     * @throws IllegalStateException when [block] returns negative value or a value that exceeds capacity of a segment
     * that was supplied to the [block].
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeUleb128
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeUleb128Array
     */
    public inline fun writeUnbound(buffer: Buffer, minimumCapacity: Int, block: (SegmentWriteContext, Segment) -> Int) {
        val segment = buffer.writableSegment(minimumCapacity)
        val bytesWritten = block(SegmentWriteContextImpl, segment)

        // fast path
        if (bytesWritten == minimumCapacity) {
            segment.limit += bytesWritten
            buffer.sizeField += bytesWritten
            return
        }

        check(bytesWritten in 0 .. segment.remainingCapacity)
        if (bytesWritten != 0) {
            segment.limit += bytesWritten
            buffer.sizeField += bytesWritten
            return
        }

        if (segment.isEmpty()) {
            val res = segment.pop()
            if (res == null) {
                buffer.head = null
            }
        }
    }
}
