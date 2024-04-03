/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.*

@UnsafeIoApi
public object UnsafeBufferOperations {
    public inline fun readFromHead(buffer: Buffer, block: (ByteArray, Int, Int) -> Int) {
        require(!buffer.exhausted()) { "Buffer is empty" }
        val head = buffer.head!!
        val bytesRead = block(head.dataAsByteArray(), head.pos, head.limit)
        if (bytesRead < 0) throw IllegalStateException("Returned negative read bytes count")
        if (bytesRead == 0) return
        if (bytesRead > head.size) throw IllegalStateException("Returned too many bytes")
        buffer.skip(bytesRead.toLong())
    }

    public inline fun readFromHead(buffer: Buffer, block: (SegmentReadContext, Segment) -> Int) {
        require(!buffer.exhausted()) { "Buffer is empty" }
        val head = buffer.head!!
        val bytesRead = block(SegmentReadContextImpl, head)
        if (bytesRead < 0) throw IllegalStateException("Returned negative read bytes count")
        if (bytesRead == 0) return
        if (bytesRead > head.size) throw IllegalStateException("Returned too many bytes")
        buffer.skip(bytesRead.toLong())
    }
    public inline fun writeToTail(buffer: Buffer, minCapacity: Int, block: (ByteArray, Int, Int) -> Int) {
        val tail = buffer.writableSegment(minCapacity)
        val bytesWritten = block(tail.dataAsByteArray(), tail.limit, tail.dataAsByteArray().size)

        // fast path
        if (bytesWritten == minCapacity) {
            tail.limit += bytesWritten
            buffer.sizeField += bytesWritten
            return
        }

        check(bytesWritten in 0 .. tail.remainingCapacity)
        if (bytesWritten != 0) {
            tail.limit += bytesWritten
            buffer.sizeField += bytesWritten
            return
        }

        if (tail.isEmpty()) {
            val newTail = tail.prev
            if (newTail != null) {
                buffer.tail = newTail
                newTail.next = null
            } else {
                buffer.head = null
                buffer.tail = null
            }
            // TODO
            // SegmentPool.recycle(tail)
        }
    }
    public inline fun writeToTail(buffer: Buffer, minCapacity: Int, block: (SegmentWriteContext, Segment) -> Int) {
        val tail = buffer.writableSegment(minCapacity)
        val bytesWritten = block(SegmentWriteContextImpl, tail)

        // fast path
        if (bytesWritten == minCapacity) {
            tail.limit += bytesWritten
            buffer.sizeField += bytesWritten
            return
        }

        check(bytesWritten in 0 .. tail.remainingCapacity)
        if (bytesWritten != 0) {
            tail.limit += bytesWritten
            buffer.sizeField += bytesWritten
            return
        }

        if (tail.isEmpty()) {
            val newTail = tail.prev
            if (newTail != null) {
                buffer.tail = newTail
                newTail.next = null
            } else {
                buffer.head = null
                buffer.tail = null
            }
            // TODO
            // SegmentPool.recycle(tail)
        }
    }

    public inline fun head(buffer: Buffer, block: (BufferIterationContext, Segment?) -> Unit) {
        block(BufferIterationContextImpl, buffer.head)
    }
    public inline fun tail(buffer: Buffer, block: (BufferIterationContext, Segment?) -> Unit) {
        block(BufferIterationContextImpl, buffer.tail)
    }
    public inline fun seek(buffer: Buffer, offset: Long, block: (BufferIterationContext, Segment?, Long) -> Unit) {
        buffer.seek(offset) { s, o ->
            block(BufferIterationContextImpl, s, o)
        }
    }
}

//public fun UnsafeBufferOperations.readFromHead(buffer: Buffer, block: (ByteBuffer) -> Unit): Unit
//public fun UnsafeBufferOperations.writeToTail(buffer: Buffer, block: (ByteBuffer) -> Unit): Unit
//public fun UnsafeBufferOperations.readBulk(buffer: Buffer, iovec: Array<ByteArray?>? = null, block: (Array<ByteArray?>, Int) -> Long): Unit
//public fun UnsafeBufferOperations.writeBulk(buffer: Buffer, minCapacity: Long, iovec: Array<ByteArray?>? = null, block: (Array<ByteArray?>, Int) -> Long): Unit

@UnsafeIoApi
public interface SegmentReadContext {
    public fun getUnchecked(segment: Segment, offset: Int): Byte
}

@UnsafeIoApi
public inline fun SegmentReadContext.withData(segment: Segment, block: (ByteArray, Int, Int) -> Unit) {
    block(segment.dataAsByteArray(), segment.pos, segment.limit)
}


@UnsafeIoApi
public interface SegmentWriteContext {
    public fun setUnchecked(segment: Segment, offset: Int, value: Byte)
    public fun setUnchecked(segment: Segment, offset: Int, b0: Byte, b1: Byte)
    public fun setUnchecked(segment: Segment, offset: Int, b0: Byte, b1: Byte, b2: Byte)
    public fun setUnchecked(segment: Segment, offset: Int, b0: Byte, b1: Byte, b2: Byte, b3: Byte)
}

@UnsafeIoApi
public interface BufferIterationContext {
    public fun next(segment: Segment): Segment?
    public fun prev(segment: Segment): Segment?
}

@UnsafeIoApi
public inline fun BufferIterationContext.read(segment: Segment, block: (SegmentReadContext, Segment) -> Unit) {
    block(SegmentReadContextImpl, segment)
}

@UnsafeIoApi
@PublishedApi
internal object SegmentReadContextImpl : SegmentReadContext {
    override fun getUnchecked(segment: Segment, offset: Int): Byte = segment.getUnchecked(offset)
}

@UnsafeIoApi
@PublishedApi
internal object SegmentWriteContextImpl : SegmentWriteContext {
    override fun setUnchecked(segment: Segment, offset: Int, value: Byte) {
        segment.setUnchecked(offset ,value)
    }

    override fun setUnchecked(segment: Segment, offset: Int, b0: Byte, b1: Byte) {
        segment.setUnchecked(offset, b0, b1)
    }

    override fun setUnchecked(segment: Segment, offset: Int, b0: Byte, b1: Byte, b2: Byte) {
        segment.setUnchecked(offset, b0, b1, b2)
    }

    override fun setUnchecked(segment: Segment, offset: Int, b0: Byte, b1: Byte, b2: Byte, b3: Byte) {
        segment.setUnchecked(offset, b0, b1, b2, b3)
    }
}

@UnsafeIoApi
@PublishedApi
internal object BufferIterationContextImpl : BufferIterationContext {
    override fun next(segment: Segment): Segment? = segment.next

    override fun prev(segment: Segment): Segment? = segment.prev
}

//@UnsafeIoApi
//public object UnsafeBufferAccessors {
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
    /*
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
*/
