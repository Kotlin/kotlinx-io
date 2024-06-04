/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.*

@UnsafeIoApi
public object UnsafeBufferOperations {
    /**
     * Maximum value that is safe to pass to [writeToTail].
     */
    public const val maxSafeWriteCapacity: Int = Segment.SIZE

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
            bytes, startIndex, endIndex, shared = true /* to prevent recycling */,
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
     *
     * If [readAction] ends execution by throwing an exception, no data will be consumed from the buffer.
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
     *
     * @sample kotlinx.io.samples.unsafe.UnsafeBufferOperationsSamples.readByteArrayFromHead
     */
    public inline fun readFromHead(buffer: Buffer, readAction: (ByteArray, Int, Int) -> Int) {
        require(!buffer.exhausted()) { "Buffer is empty" }
        val head = buffer.head!!
        val bytesRead = readAction(head.dataAsByteArray(true), head.pos, head.limit)
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
     * If [writeAction] ends execution by throwing an exception, no data will be written to the buffer.
     *
     * The data array is passed to the [writeAction] directly from the buffer's internal storage without copying
     * on the best-effort basis, meaning that there are no strong zero-copy guarantees
     * and the copy will be created if it could not be omitted.
     *
     * @throws IllegalStateException when [minimumCapacity] is too large and could not be fulfilled.
     * @throws IllegalStateException when [writeAction] returns a negative value or a value exceeding
     * the `endIndex - startIndex` value.
     *
     * @sample kotlinx.io.samples.unsafe.UnsafeBufferOperationsSamples.writeByteArrayToTail
     */
    public inline fun writeToTail(buffer: Buffer, minimumCapacity: Int, writeAction: (ByteArray, Int, Int) -> Int) {
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
            return
        }

        check(bytesWritten in 0..tail.remainingCapacity) {
            "Invalid number of bytes written: $bytesWritten. Should be in 0..${tail.remainingCapacity}"
        }
        if (bytesWritten != 0) {
            tail.writeBackData(data, bytesWritten)
            tail.limit += bytesWritten
            buffer.sizeMut += bytesWritten
            return
        }
        if (tail.isEmpty()) {
            buffer.recycleTail()
        }
    }
}
