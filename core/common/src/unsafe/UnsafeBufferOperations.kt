/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlinx.io.*
import kotlin.jvm.JvmSynthetic

@UnsafeIoApi
@OptIn(ExperimentalContracts::class)
public object UnsafeBufferOperations {
    /**
     * Maximum value that is safe to pass to [writeToTail].
     */
    public val maxSafeWriteCapacity: Int get() = Segment.SIZE

    /**
     * Moves [bytes] to the end of the [buffer].
     *
     * Only the region of the [bytes] array spanning from [startIndex] until [endIndex] is considered readable.
     *
     * The array is wrapped into the buffer without any copies, if possible.
     *
     * Attempts to write data into [bytes] array once it was moved may lead to data corruption
     * and should be considered as an error.
     *
     * @param buffer a buffer to which data will be added
     * @param bytes an array that needs to be added to the buffer
     * @param startIndex an index of the first byte readable from the array, `0` by default.
     * @param endIndex an index of the byte past the last readable array byte, `bytes.size` byte default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] are not within [bytes] bounds
     * @throws IllegalArgumentException when `startIndex > endIndex`
     *
     * @sample kotlinx.io.samples.unsafe.UnsafeBufferOperationsSamples.moveToTail
     */
    public fun moveToTail(buffer: Buffer, bytes: ByteArray, startIndex: Int = 0, endIndex: Int = bytes.size) {
        checkBounds(bytes.size, startIndex, endIndex)
        val segment = Segment.new(
            bytes, startIndex, endIndex,
            AlwaysSharedCopyTracker, /* to prevent recycling */
            owner = false /* can't append to it */
        )
        val tail = buffer.tail
        if (tail == null) {
            buffer.head = segment
            buffer.tail = segment
        } else {
            buffer.tail = tail.push(segment)
        }
        buffer.sizeMut += endIndex - startIndex
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
     * This value will also be propagated as the function return value.
     *
     * If [readAction] ends execution by throwing an exception, no data will be consumed from the buffer.
     *
     * If the buffer is empty, [IllegalArgumentException] will be thrown.
     *
     * The data is passed to the [readAction] directly from the buffer's internal storage without copying on
     * the best effort basis, meaning that there are no strong zero-copy guarantees
     * and the copy will be created if it could not be omitted.
     *
     * @return Number of bytes consumed as returned by [readAction].
     *
     * @throws IllegalStateException when [readAction] returns negative value or a values exceeding
     * the `endIndexExclusive - startIndexInclusive` value.
     * @throws IllegalArgumentException when the [buffer] is empty.
     *
     * @sample kotlinx.io.samples.unsafe.UnsafeBufferOperationsSamples.readByteArrayFromHead
     */
    public inline fun readFromHead(
        buffer: Buffer,
        readAction: (bytes: ByteArray, startIndexInclusive: Int, endIndexExclusive: Int) -> Int
    ): Int {
        contract {
            callsInPlace(readAction, EXACTLY_ONCE)
        }

        require(!buffer.exhausted()) { "Buffer is empty" }
        val head = buffer.head!!
        val bytesRead = readAction(head.dataAsByteArray(true), head.pos, head.limit)
        if (bytesRead != 0) {
            if (bytesRead < 0) throw IllegalStateException("Returned negative read bytes count")
            if (bytesRead > head.size) throw IllegalStateException("Returned too many bytes")
            buffer.skip(bytesRead.toLong())
        }
        return bytesRead
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
     * This value will also be propagated as the function return value.
     *
     * Both [readAction] arguments are valid only within [readAction] scope,
     * it's an error to store and reuse it later.
     *
     * If the buffer is empty, [IllegalArgumentException] will be thrown.
     *
     * @return Number of bytes consumed as returned by [readAction].
     *
     * @throws IllegalStateException when [readAction] returns negative value or a values exceeding
     * the [Segment.size] value.
     * @throws IllegalArgumentException when the [buffer] is empty.
     *
     * @sample kotlinx.io.samples.unsafe.UnsafeBufferOperationsSamples.readUleb128
     */
    public inline fun readFromHead(buffer: Buffer, readAction: (SegmentReadContext, Segment) -> Int): Int {
        contract {
            callsInPlace(readAction, EXACTLY_ONCE)
        }

        require(!buffer.exhausted()) { "Buffer is empty" }
        val head = buffer.head!!
        val bytesRead = readAction(SegmentReadContextImpl, head)
        if (bytesRead != 0) {
            if (bytesRead < 0) throw IllegalStateException("Returned negative read bytes count")
            if (bytesRead > head.size) throw IllegalStateException("Returned too many bytes")
            buffer.skip(bytesRead.toLong())
        }
        return bytesRead
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
     * This value will also be propagated as the function return value.
     *
     * If [writeAction] ends execution by throwing an exception, no data will be written to the buffer.
     *
     * The data array is passed to the [writeAction] directly from the buffer's internal storage without copying
     * on the best-effort basis, meaning that there are no strong zero-copy guarantees
     * and the copy will be created if it could not be omitted.
     *
     * @return Number of bytes written as returned by [writeAction].
     *
     * @throws IllegalStateException when [minimumCapacity] is too large and could not be fulfilled.
     * @throws IllegalStateException when [writeAction] returns a negative value or a value exceeding
     * the `endIndexExclusive - startIndexInclusive` value.
     *
     * @sample kotlinx.io.samples.unsafe.UnsafeBufferOperationsSamples.writeByteArrayToTail
     */
    public inline fun writeToTail(
        buffer: Buffer, minimumCapacity: Int,
        writeAction: (bytes: ByteArray, startIndexInclusive: Int, endIndexExclusive: Int) -> Int
    ): Int {
        contract {
            callsInPlace(writeAction, EXACTLY_ONCE)
        }

        val tail = buffer.writableSegment(minimumCapacity)

        val data = tail.dataAsByteArray(false)
        // If writeAction throws an exception, we may end up with an empty segment in tail.
        // That's fine as long as we don't treat the presence of a segment as a sing of a buffer being non-empty.
        val bytesWritten = writeAction(data, tail.limit, data.size)

        // fast path
        if (bytesWritten == minimumCapacity) {
            tail.writeBackData(data, bytesWritten)
            tail.limit += bytesWritten
            buffer.sizeMut += bytesWritten
            return bytesWritten
        }

        check(bytesWritten in 0..tail.remainingCapacity) {
            "Invalid number of bytes written: $bytesWritten. Should be in 0..${tail.remainingCapacity}"
        }
        if (bytesWritten != 0) {
            tail.writeBackData(data, bytesWritten)
            tail.limit += bytesWritten
            buffer.sizeMut += bytesWritten
            return bytesWritten
        }
        if (tail.isEmpty()) {
            buffer.recycleTail()
        }
        return bytesWritten
    }

    /**
     * Provides write access to the [buffer], allowing to write data
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
     * This value will also be propagated as the function return value.
     *
     * Both [writeAction] arguments are valid only within [writeAction] scope,
     * it's an error to store and reuse it later.
     *
     * @return Number of bytes written as returned by [writeAction].
     *
     * @throws IllegalStateException when [minimumCapacity] is too large and could not be fulfilled.
     * @throws IllegalStateException when [writeAction] returns a negative value or a value exceeding
     * the [Segment.remainingCapacity] value for the provided segment.
     *
     * @sample kotlinx.io.samples.unsafe.UnsafeBufferOperationsSamples.writeUleb128
     */
    public inline fun writeToTail(
        buffer: Buffer,
        minimumCapacity: Int,
        writeAction: (SegmentWriteContext, Segment) -> Int
    ): Int {
        contract {
            callsInPlace(writeAction, EXACTLY_ONCE)
        }

        val tail = buffer.writableSegment(minimumCapacity)
        val bytesWritten = writeAction(SegmentWriteContextImpl, tail)

        // fast path
        if (bytesWritten == minimumCapacity) {
            tail.limit += bytesWritten
            buffer.sizeMut += bytesWritten
            return bytesWritten
        }

        check(bytesWritten in 0..tail.remainingCapacity) {
            "Invalid number of bytes written: $bytesWritten. Should be in 0..${tail.remainingCapacity}"
        }
        if (bytesWritten != 0) {
            tail.limit += bytesWritten
            buffer.sizeMut += bytesWritten
            return bytesWritten
        }

        if (tail.isEmpty()) {
            buffer.recycleTail()
        }
        return bytesWritten
    }

    /**
     * Provides access to [buffer] segments starting from the head.
     *
     * [iterationAction] is invoked with an instance of [BufferIterationContext]
     * allowing to iterate over [buffer]'s segments
     * and a reference to [buffer]'s head segment, which could be null in case of an empty buffer.
     *
     * It's considered an error to use a [BufferIterationContext] or a [Segment] instances outside the scope of
     * the [iterationAction].
     *
     * Both [iterationAction] arguments are valid only within [iterationAction] scope,
     * it's an error to store and reuse it later.
     *
     * @param buffer a buffer to iterate over
     * @param iterationAction a callback to invoke with the head reference and an iteration context instance
     *
     * @sample kotlinx.io.samples.unsafe.UnsafeReadWriteSamplesJvm.messageDigest
     * @sample kotlinx.io.samples.unsafe.UnsafeBufferOperationsSamples.crc32Unsafe
     */
    public inline fun iterate(buffer: Buffer, iterationAction: (BufferIterationContext, Segment?) -> Unit) {
        contract {
            callsInPlace(iterationAction, EXACTLY_ONCE)
        }
        iterationAction(BufferIterationContextImpl, buffer.head)
    }

    /**
     * Provides access to [buffer] segments starting from a segment spanning over a specified [offset].
     *
     * [iterationAction] is invoked with an instance of [BufferIterationContext]
     * allowing to iterate over [buffer]'s segments, a reference to a [buffer]'s segment,
     * an offset corresponding to the beginning of the segment.
     *
     * If the [buffer] is empty, [iterationAction] will be invoked with a null segment.
     *
     * To locate [buffer]'s [offset]'th byte within the supplied segment, one has to subtract [offset] from the supplied
     * offset value.
     *
     * All [iterationAction] arguments are valid only within [iterationAction] scope,
     * it's an error to store and reuse it later.
     *
     * @param buffer a buffer to iterate over
     * @param iterationAction a callback to invoke with an iteration context instance, a segment reference and
     * an offset corresponding to the segment's beginning.
     *
     * @throws IllegalArgumentException when [offset] is negative
     * @throws IndexOutOfBoundsException when [offset] is greater or equal to [Buffer.size]
     */
    public inline fun iterate(
        buffer: Buffer, offset: Long,
        iterationAction: (BufferIterationContext, Segment?, Long) -> Unit
    ) {
        contract {
            callsInPlace(iterationAction, EXACTLY_ONCE)
        }

        require(offset >= 0) { "Offset must be non-negative: $offset" }
        if (offset >= buffer.size) {
            throw IndexOutOfBoundsException("Offset should be less than buffer's size (${buffer.size}): $offset")
        }

        buffer.seek(offset) { s, o ->
            iterationAction(BufferIterationContextImpl, s, o)
        }
    }
}

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
     * @sample kotlinx.io.samples.unsafe.UnsafeBufferOperationsSamples.crc32GetUnchecked
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
 * @sample kotlinx.io.samples.unsafe.UnsafeReadWriteSamplesJvm.messageDigest
 * @sample kotlinx.io.samples.unsafe.UnsafeBufferOperationsSamples.crc32Unsafe
 */
@UnsafeIoApi
@JvmSynthetic
@OptIn(ExperimentalContracts::class)
public inline fun SegmentReadContext.withData(segment: Segment, readAction: (ByteArray, Int, Int) -> Unit) {
    contract {
        callsInPlace(readAction, EXACTLY_ONCE)
    }
    readAction(segment.dataAsByteArray(true), segment.pos, segment.limit)
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
     * @sample kotlinx.io.samples.unsafe.UnsafeBufferOperationsSamples.writeUleb128
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
 * Allows iterating over [Buffer]'s segments and reading its data.
 */
@UnsafeIoApi
public interface BufferIterationContext : SegmentReadContext {
    /**
     * Return a segment succeeding [segment] in the buffer, or `null`, if there is no such segment (meaning that the
     * [segment] is [Buffer]'s tail).
     *
     * @param segment a segment for which a successor needs to be found
     *
     * @sample kotlinx.io.samples.unsafe.UnsafeReadWriteSamplesJvm.messageDigest
     * @sample kotlinx.io.samples.unsafe.UnsafeBufferOperationsSamples.crc32Unsafe
     */
    public fun next(segment: Segment): Segment?
}

@UnsafeIoApi
@PublishedApi
@get:JvmSynthetic
internal val SegmentReadContextImpl: SegmentReadContext = object : SegmentReadContext {
    override fun getUnchecked(segment: Segment, offset: Int): Byte = segment.getUnchecked(offset)
}

@UnsafeIoApi
@PublishedApi
@get:JvmSynthetic
internal val SegmentWriteContextImpl: SegmentWriteContext = object : SegmentWriteContext {
    override fun setUnchecked(segment: Segment, offset: Int, value: Byte) {
        segment.setUnchecked(offset, value)
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
@get:JvmSynthetic
internal val BufferIterationContextImpl: BufferIterationContext = object : BufferIterationContext {
    override fun next(segment: Segment): Segment? = segment.next

    override fun getUnchecked(segment: Segment, offset: Int): Byte =
        SegmentReadContextImpl.getUnchecked(segment, offset)
}
