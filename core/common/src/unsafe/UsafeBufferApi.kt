/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.Buffer
import kotlinx.io.SnapshotApi
import kotlinx.io.UnsafeIoApi

@SnapshotApi
@UnsafeIoApi
public object UnsafeBufferAccessors {
    /**
     * Maximum value that is safe to pass to [writeToTail].
     */
    public const val maxSafeWriteCapacity: Int = 8192

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
     *
     * @sample kotlinx.io.samples.unsafe.ReadWriteUnsafe.bulkConsumption
     * @sample kotlinx.io.samples.unsafe.ReadWriteUnsafe.basicReadProperties
     */
    public inline fun readFromHead(
        buffer: Buffer,
        readAction: (data: ByteArray, startIndex: Int, endIndex: Int) -> Int
    ) {
        val head = buffer.head ?: throw IllegalArgumentException("Buffer is empty")
        val data = head.data
        val pos = head.pos
        val limit = head.limit
        val bytesRead = readAction(data, pos, limit)

        if (bytesRead == 0) return
        val maxReadSize = limit - pos
        if (bytesRead < 0 || bytesRead > maxReadSize) {
            throw IllegalStateException(
                "readAction should return value in range [0, $maxReadSize], but returned $bytesRead"
            )
        }
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
     * It's an error to return a negative value or a value exceeding the size of provided writeable range.
     *
     * The data array is passed to the [writeAction] directly from the buffer's internal storage without copying
     * on the best-effort basis, meaning that there are no strong zero-copy guarantees
     * and the copy will be created if it could not be omitted.
     *
     * @throws IllegalStateException when [minimumCapacity] is too large and could not be fulfilled.
     * @throws IllegalStateException when [writeAction] returns a negative value or a value exceeding
     * the `endIndex - startIndex` value.
     *
     * @sample kotlinx.io.samples.unsafe.ReadWriteUnsafe.basicWriteProperties
     */
    public inline fun writeToTail(
        buffer: Buffer, minimumCapacity: Int,
        writeAction: (data: ByteArray, startIndex: Int, endIndex: Int) -> Int
    ) {
        val tail = buffer.writableSegment(minimumCapacity)
        val data = tail.data
        val startIndex = tail.limit
        val endIndex = data.size
        val bytesWritten = writeAction(data, startIndex, endIndex)

        if (bytesWritten == 0) {
            buffer.tryRecycleTail()
            return
        }
        val maxWriteSize = endIndex - startIndex
        if (bytesWritten < 0 || bytesWritten > maxWriteSize) {
            throw IllegalStateException(
                "writeAction should return value in range [0, $maxWriteSize], but returned $bytesWritten"
            )
        }
        tail.limit += bytesWritten
        buffer.incrementSize(bytesWritten)
    }
}
