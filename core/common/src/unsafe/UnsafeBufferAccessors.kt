/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.*

@UnsafeIoApi
public object UnsafeBufferOperations {
    /**
     * Maximum value that is safe to pass to [writeToTail].
     */
    public const val maxSafeWriteCapacity: Int = 8192

    /**
     * Moves [bytes] to the end of the [buffer].
     *
     * Only the region of the [bytes] array spanning from [startIndex] until [endIndex] is considered readable.
     *
     * The array is wrapped into the buffer without any copied, if possible.
     *
     * Attempts to write data into [bytes] array once it was moved may lead to data corruption
     * and should be considered as an error.
     *
     * @param buffer a buffer to which data will be added
     * @param bytes an array that needs to be added to the buffer
     * @param startIndex an index of the first byte readable from the array, `0` by default.
     * @param endIndex an index of the byte past the last readable array byte, `bytes.size` byte default.
     *
     * @throws IllegalArgumentException when [startIndex] or [endIndex] are not within [bytes] bounds
     * @throws IllegalArgumentException when `startIndex > endIndex`
     */
    public fun moveToTail(buffer: Buffer, bytes: ByteArray, startIndex: Int = 0, endIndex: Int = bytes.size) {
        val segment = Segment(bytes, startIndex, endIndex, shared = true /* to prevent recycling */, owner = true)
        if (buffer.tail == null) {
            buffer.head = segment
            buffer.tail = segment
        } else {
            val oldTail = buffer.tail!!
            oldTail.next = segment
            segment.prev = oldTail
            buffer.tail = segment
        }
        buffer.sizeField += endIndex - startIndex
    }

    /**
     * Provides read-only access to the data from the head of a [buffer] by calling the [readAction] on head's data and
     * optionally consumes the data at the end of the action.
     *
     * The [readAction] receives the byte array with buffer head's data and a pair of indices, startIndex and endIndex,
     * denoting the subarray containing meaningful data.
     * It's considered an error to read data outside of that range.
     * The data array is provided for read-only purposes, updating it may affect buffer's data
     * and may lead to undefined behavior when performed outside the provided range.
     *
     * The [readAction] should return the number of bytes consumed, the buffer's size will be decreased by that value,
     * and data from the consumed prefix will be no longer available for read.
     * If the operation does not consume anything, the action should return `0`.
     * It's considered an error to return a negative value or a value exceeding the size of a readable range.
     *
     * If the buffer is empty, [IllegalArgumentException] will be thrown.
     *
     * The data is passed to the [readAction] directly from the buffer's internal storage without copying on
     * the best effort basis, meaning that there are no strong zero-copy guarantees
     * and the copy will be created if it could not be omitted.
     *
     * @throws IllegalStateException when [readAction] returns negative value or a values exceeding
     * the `endIndex - startIndex` value.
     * @throws IllegalArgumentException when the [buffer] is empty.
     */
    public inline fun readFromHead(buffer: Buffer, readAction: (ByteArray, Int, Int) -> Int) {
        require(!buffer.exhausted()) { "Buffer is empty" }
        val head = buffer.head!!
        val bytesRead = readAction(head.dataAsByteArray(), head.pos, head.limit)
        if (bytesRead < 0) throw IllegalStateException("Returned negative read bytes count")
        if (bytesRead == 0) return
        if (bytesRead > head.size) throw IllegalStateException("Returned too many bytes")
        buffer.skip(bytesRead.toLong())
    }

    /**
     * Provides read-only access to the data from the head of a [buffer] by calling the [readAction] on head's data and
     * optionally consumes the data at the end of the action.
     *
     * The [readAction] receives the buffer head data and an instance of [SegmentReadContext] allowing to read data from
     * the segment.
     * The head segment is provided for read-only purposes, updating it may affect buffer's data
     * and may lead to undefined behavior when performed outside the provided range.
     *
     * The [readAction] should return the number of bytes consumed, the buffer's size will be decreased by that value,
     * and data from the consumed prefix will be no longer available for read.
     * If the operation does not consume anything, the action should return `0`.
     * It's considered an error to return a negative value or a value exceeding the [Segment.size].
     *
     * If the buffer is empty, [IllegalArgumentException] will be thrown.
     *
     * @throws IllegalStateException when [readAction] returns negative value or a values exceeding
     * the [Segment.size] value.
     * @throws IllegalArgumentException when the [buffer] is empty.
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readUleb128
     */
    public inline fun readFromHead(buffer: Buffer, readAction: (SegmentReadContext, Segment) -> Int) {
        require(!buffer.exhausted()) { "Buffer is empty" }
        val head = buffer.head!!
        val bytesRead = readAction(SegmentReadContextImpl, head)
        if (bytesRead < 0) throw IllegalStateException("Returned negative read bytes count")
        if (bytesRead == 0) return
        if (bytesRead > head.size) throw IllegalStateException("Returned too many bytes")
        buffer.skip(bytesRead.toLong())
    }

    /**
     * Provides write access to the buffer, allowing to write data
     * into a not yet committed portion of the buffer's tail using a [writeAction].
     *
     * The [writeAction] receives the byte array and the pair of indices, startIndex and endIndex,
     * denoting the range of indices available for writing. It's considered an error to write data outside that range.
     * Writing outside the range may corrupt buffer's data.
     *
     * It's guaranteed that the size of the range is at least [minimumCapacity],
     * but if the [minimumCapacity] bytes could not be provided for writing,
     * the method will throw [IllegalStateException].
     * It is safe to use any [minimumCapacity] value below [maxSafeWriteCapacity], but unless exact minimum number of
     * available bytes is required, it's recommended to use `1` as [minimumCapacity] value.
     *
     * The value returned by the [writeAction] denotes the number of bytes successfully written to the buffer.
     * If no data was written, `0` should be returned.
     * It's an error to return a negative value or a value exceeding the size of the provided writeable range.
     *
     * The data array is passed to the [writeAction] directly from the buffer's internal storage without copying
     * on the best-effort basis, meaning that there are no strong zero-copy guarantees
     * and the copy will be created if it could not be omitted.
     *
     * @throws IllegalStateException when [minimumCapacity] is too large and could not be fulfilled.
     * @throws IllegalStateException when [writeAction] returns a negative value or a value exceeding
     * the `endIndex - startIndex` value.
     */
    public inline fun writeToTail(buffer: Buffer, minimumCapacity: Int, writeAction: (ByteArray, Int, Int) -> Int) {
        val tail = buffer.writableSegment(minimumCapacity)
        val bytesWritten = writeAction(tail.dataAsByteArray(), tail.limit, tail.dataAsByteArray().size)

        // fast path
        if (bytesWritten == minimumCapacity) {
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

    /**
     * Provides write access to the buffer, allowing to write data
     * into a not yet committed portion of the buffer's tail using a [writeAction].
     *
     * The [writeAction] receives the segment to write data into and an instance of [SegmentWriteContext] allowing to
     * write data.
     *
     * It's guaranteed that the [Segment.remainingCapacity] for the provided segment is at least [minimumCapacity],
     * but if the [minimumCapacity] bytes could not be provided for writing,
     * the method will throw [IllegalStateException].
     * It is safe to use any [minimumCapacity] value below [maxSafeWriteCapacity], but unless exact minimum number of
     * available bytes is required, it's recommended to use `1` as [minimumCapacity] value.
     *
     * The value returned by the [writeAction] denotes the number of bytes successfully written to the buffer.
     * If no data was written, `0` should be returned.
     * It's an error to return a negative value or a value exceeding the [Segment.remainingCapacity].
     *
     * @throws IllegalStateException when [minimumCapacity] is too large and could not be fulfilled.
     * @throws IllegalStateException when [writeAction] returns a negative value or a value exceeding
     * the [Segment.remainingCapacity] value for the provided segment.
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeUleb128
     */
    public inline fun writeToTail(buffer: Buffer, minimumCapacity: Int, writeAction: (SegmentWriteContext, Segment) -> Int) {
        val tail = buffer.writableSegment(minimumCapacity)
        val bytesWritten = writeAction(SegmentWriteContextImpl, tail)

        // fast path
        if (bytesWritten == minimumCapacity) {
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

    /**
     * Provides access to [buffer] segments starting from the head.
     *
     * [block] is invoked with a reference to [buffer]'s head segment, which could be null in case of an empty buffer,
     * and an instance of [BufferIterationContext] allowing to iterate over [buffer]'s segments.
     *
     * It's considered an error to use a [BufferIterationContext] or a [Segment] instances outside the scope of
     * the [block].
     *
     * @param buffer a buffer to iterate over
     * @param block a callback to invoke with the head reference and an iteration context instance
     *
     * @sample kotlinx.io.samples.Crc32Sample.crc32Unsafe
     */
    public inline fun iterate(buffer: Buffer, block: (BufferIterationContext, Segment?) -> Unit) {
        block(BufferIterationContextImpl, buffer.head)
    }
    /**
     * Provides access to [buffer] segments starting from the tail.
     *
     * [block] is invoked with a reference to [buffer]'s tail segment, which could be null in case of an empty buffer,
     * and an instance of [BufferIterationContext] allowing to iterate over [buffer]'s segments.
     *
     * It's considered an error to use a [BufferIterationContext] or a [Segment] instances outside the scope of
     * the [block].
     *
     * @param buffer a buffer to iterate over
     * @param block a callback to invoke with the head reference and an iteration context instance
     */
    //public inline fun tail(buffer: Buffer, block: (BufferIterationContext, Segment?) -> Unit) {
    //    block(BufferIterationContextImpl, buffer.tail)
    //}
    /**
     * Provides access to [buffer] segments starting from a segment spanning over a specified [offset].
     *
     * [block] is invoked with a reference to a [buffer]'s segment, which could be null in case of an empty buffer or
     * it's size is less than required [offset], an offset corresponding to the beginning of the segment,
     * and an instance of [BufferIterationContext] allowing to iterate over [buffer]'s segments.
     *
     * To locate [buffer]'s [offset]'th byte within the supplied segment, one has to subtract [offset] from the supplied
     * offset value.
     *
     * @param buffer a buffer to iterate over
     * @param block a callback to invoke with the head reference, an offset corresponding to the segment's beginning,
     * and an iteration context instance
     *
     * @sample kotlinx.io.samples.CRC32Source
     */
    public inline fun iterate(buffer: Buffer, offset: Long, block: (BufferIterationContext, Segment?, Long) -> Unit) {
        buffer.seek(offset) { s, o ->
            block(BufferIterationContextImpl, s, o)
        }
    }
}

//public fun UnsafeBufferOperations.readFromHead(buffer: Buffer, block: (ByteBuffer) -> Unit): Unit
//public fun UnsafeBufferOperations.writeToTail(buffer: Buffer, block: (ByteBuffer) -> Unit): Unit
//public fun UnsafeBufferOperations.readBulk(buffer: Buffer, iovec: Array<ByteArray?>? = null, block: (Array<ByteArray?>, Int) -> Long): Unit
//public fun UnsafeBufferOperations.writeBulk(buffer: Buffer, minCapacity: Long, iovec: Array<ByteArray?>? = null, block: (Array<ByteArray?>, Int) -> Long): Unit

/**
 * Provides read access to [Segment]'s data.
 */
@UnsafeIoApi
public interface SegmentReadContext {
    /**
     * Reads [offset]'s byte from [segment].
     *
     * This operation does not perform any checks, and it's caller's responsibility to ensure
     * that [offset] is between `0` and [Segment.size].
     *
     * @param segment a segment to read from
     * @param offset an offset into segment data
     *
     * @sample kotlinx.io.samples.crc32UsingGetUnchecked
     *
     */
    public fun getUnchecked(segment: Segment, offset: Int): Byte
}

/**
 * Provides read-only access to [segment]'s data by passing it to [readAction] along with two indices, startIndex
 * and endIndex, denoting a readable portion of the array.
 *
 * It's considered an error to read data outside of that range.
 * The data array is provided for read-only purposes, updating it may affect segment's data
 * and may lead to undefined behavior when performed outside the provided range.
 *
 * The data is passed to the [readAction] directly from the segment's internal storage without copying on
 * the best effort basis, meaning that there are no strong zero-copy guarantees
 * and the copy will be created if it could not be omitted.
 *
 * @param segment a segment to access data from
 * @param readAction an action to invoke on segment's data
 *
 * @sample kotlinx.io.samples.Crc32Sample.crc32Unsafe
 */
@UnsafeIoApi
public inline fun SegmentReadContext.withData(segment: Segment, readAction: (ByteArray, Int, Int) -> Unit) {
    readAction(segment.dataAsByteArray(), segment.pos, segment.limit)
}

/**
 * Provides write access to [Segment]'s data.
 */
@UnsafeIoApi
public interface SegmentWriteContext {
    /**
     * Writes [value] to an uncommitted portion of the [segment] at [offset].
     *
     * This operation does not perform any checks, and it's caller's responsibility to ensure
     * that [offset] is between `0` and [Segment.remainingCapacity].
     *
     * Writing outside that range is considered an error and may lead to data corruption.
     *
     * @param segment a segment to write into
     * @param offset an offset inside [segment]'s uncommitted area to write to
     * @param value a value to be written
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeUleb128
     */
    public fun setUnchecked(segment: Segment, offset: Int, value: Byte)
    /**
     * Writes two bytes, [b0] and [b1] to an uncommitted portion of the [segment] starting at [offset].
     *
     * The [b0] is always written at `offset` and [b1] is written at `offset + 1`.
     *
     * This operation does not perform any checks, and it's caller's responsibility to ensure
     * that [offset] is between `0` and [Segment.remainingCapacity] - 1.
     *
     * Writing outside that range is considered an error and may lead to data corruption.
     *
     * @param segment a segment to write into
     * @param offset an offset inside [segment]'s uncommitted area to write to
     * @param b0 a first byte to be written
     * @param b1 a second byte to be written
     */
    public fun setUnchecked(segment: Segment, offset: Int, b0: Byte, b1: Byte)
    /**
     * Writes three bytes, [b0], [b1] and [b2] to an uncommitted portion of the [segment] starting at [offset].
     *
     * The [b0] is always written at `offset`, [b1] is written at `offset + 1` and [b2] is written at `offset + 2`.
     *
     * This operation does not perform any checks, and it's caller's responsibility to ensure
     * that [offset] is between `0` and [Segment.remainingCapacity] - 2.
     *
     * Writing outside that range is considered an error and may lead to data corruption.
     *
     * @param segment a segment to write into
     * @param offset an offset inside [segment]'s uncommitted area to write to
     * @param b0 a first byte to be written
     * @param b1 a second byte to be written
     * @param b2 a third byte to be written
     */
    public fun setUnchecked(segment: Segment, offset: Int, b0: Byte, b1: Byte, b2: Byte)
    /**
     * Writes three bytes, [b0], [b1], [b2] and [b3] to an uncommitted portion of the [segment] starting at [offset].
     *
     * The [b0] is always written at `offset`, [b1] is written at `offset + 1`,
     * [b2] is written at `offset + 2`, and [b3] is written at `offset + 3`.
     *
     * This operation does not perform any checks, and it's caller's responsibility to ensure
     * that [offset] is between `0` and [Segment.remainingCapacity] - 3.
     *
     * Writing outside that range is considered an error and may lead to data corruption.
     *
     * @param segment a segment to write into
     * @param offset an offset inside [segment]'s uncommitted area to write to
     * @param b0 a first byte to be written
     * @param b1 a second byte to be written
     * @param b2 a third byte to be written
     * @param b3 a fourth byte to be written
     *
     */
    public fun setUnchecked(segment: Segment, offset: Int, b0: Byte, b1: Byte, b2: Byte, b3: Byte)
}

/**
 * Allows iterating over [Buffer]'s segments.
 */
@UnsafeIoApi
public interface BufferIterationContext : SegmentReadContext {
    /**
     * Return a segment succeeding [segment] in the buffer, or `null, if there is no such segment (meaning that the
     * [segment] is [Buffer]'s tail).
     *
     * @param segment a segment for which a successor needs to be found
     *
     * @sample kotlinx.io.samples.Crc32Sample.crc32Unsafe
     */
    public fun next(segment: Segment): Segment?
    /**
     * Return a segment preceding [segment] in the buffer, or `null, if there is no such segment (meaning that the
     * [segment] is [Buffer]'s head).
     *
     * @param segment a segment for which a predecessor needs to be found
     */
    // public fun prev(segment: Segment): Segment?
}

/**
 * Allows reading data from the [segment] by invoking a [readAction] with an instance of [SegmentReadContext].
 *
 * @param segment a segment to read from
 * @param readAction an action supplied with a segment and the [SegmentReadContext] instance.
 *
 * @sample kotlinx.io.samples.crc32
 */
//@UnsafeIoApi
//public inline fun BufferIterationContext.read(segment: Segment, readAction: (SegmentReadContext, Segment) -> Unit) {
//    readAction(SegmentReadContextImpl, segment)
//}

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

    // override fun prev(segment: Segment): Segment? = segment.prev
    override fun getUnchecked(segment: Segment, offset: Int): Byte = SegmentReadContextImpl.getUnchecked(segment, offset)
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
