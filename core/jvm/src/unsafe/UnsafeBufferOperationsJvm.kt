/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlinx.io.Buffer
import kotlinx.io.Segment
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations.maxSafeWriteCapacity
import java.nio.ByteBuffer

/**
 * Provides read-only access to the data from the head of a [buffer] by calling the [readAction] on head's data and
 * optionally consumes the data at the end of the action.
 *
 * The [readAction] receives a read-only [ByteBuffer] with buffer head's data.
 *
 * After exiting the [readAction], all data consumed from the [ByteBuffer] will be also consumed from the [buffer].
 * Consumed bytes determined as a difference between [ByteBuffer.capacity] and [ByteBuffer.remaining].
 * This value will also be propagated as the function return value.
 *
 * If [readAction] ends execution by throwing an exception, no data will be consumed from the buffer.
 *
 * If the [buffer] is empty, [IllegalArgumentException] will be thrown.
 *
 * The data is passed to the [readAction] directly from the buffer's internal storage without copying on
 * the best effort basis, meaning that there are no strong zero-copy guarantees
 * and the copy will be created if it could not be omitted.
 *
 * @param buffer a buffer to read from
 * @param readAction an action that will be invoked on a [ByteBuffer] containing data from [buffer]'s head
 *
 * @return Number of bytes read by [readAction].
 *
 * @throws IllegalArgumentException when the [buffer] is empty.
 *
 * @sample kotlinx.io.samples.unsafe.UnsafeReadWriteSamplesJvm.writeToByteChannel
 */
@UnsafeIoApi
@OptIn(ExperimentalContracts::class)
public inline fun UnsafeBufferOperations.readFromHead(buffer: Buffer, readAction: (ByteBuffer) -> Unit): Int {
    contract {
        callsInPlace(readAction, EXACTLY_ONCE)
    }
    return readFromHead(buffer) { rawData, pos, limit ->
        val bb = ByteBuffer.wrap(rawData, pos, limit - pos).slice().asReadOnlyBuffer()
        readAction(bb)
        bb.position()
    }
}

/**
 * Provides write access to the buffer, allowing to write data
 * into a not yet committed portion of the buffer's tail using a [writeAction].
 *
 * The [writeAction] receives a [ByteBuffer] representing uncommitted portion of [buffer]'s tail
 *
 * It's guaranteed that the size of the [ByteBuffer] is at least [minimumCapacity],
 * but if the [minimumCapacity] bytes could not be provided for writing,
 * the method will throw [IllegalStateException].
 * It is safe to use any [minimumCapacity] value below [maxSafeWriteCapacity], but unless exact minimum number of
 * available bytes is required, it's recommended to use `1` as [minimumCapacity] value.
 *
 * After exiting [writeAction], bytes written to the [ByteBuffer] will be committed to the buffer.
 * The number of bytes written is determined as a difference between [ByteBuffer.capacity] and [ByteBuffer.remaining].
 * This value will also be propagated as the function return value.
 *
 * If [writeAction] ends execution by throwing an exception, no data will be written to the buffer.
 *
 * The data array is passed to the [writeAction] directly from the buffer's internal storage without copying
 * on the best-effort basis, meaning that there are no strong zero-copy guarantees
 * and the copy will be created if it could not be omitted.
 *
 * @param buffer a buffer to read from
 * @param minimumCapacity the minimum amount of writable space
 * @param writeAction an action that will be invoked on a [ByteBuffer] that will be added to a [buffer] by the end of
 * the call
 *
 * @return Number of bytes written by [writeAction].
 *
 * @throws IllegalStateException when [minimumCapacity] is too large and could not be fulfilled.
 *
 * @sample kotlinx.io.samples.unsafe.UnsafeReadWriteSamplesJvm.readFromByteChannel
 */
@UnsafeIoApi
@OptIn(ExperimentalContracts::class)
public inline fun UnsafeBufferOperations.writeToTail(
    buffer: Buffer,
    minimumCapacity: Int,
    writeAction: (ByteBuffer) -> Unit
): Int {
    contract {
        callsInPlace(writeAction, EXACTLY_ONCE)
    }
    return writeToTail(buffer, minimumCapacity) { rawData, pos, limit ->
        val bb = ByteBuffer.wrap(rawData, pos, limit - pos).slice()
        writeAction(bb)
        bb.position()
    }
}

/**
 * Provides read-only access to [buffer]'s data by filling provided [iovec] array with [ByteBuffer]'s representing
 * [buffer]'s data, supplying it to [readAction] and consuming number of bytes returned by the [readAction].
 *
 * If there's not enough space in [iovec] to fit all byte buffers, only a prefix of [buffer]'s data will be supplied to
 * [readAction].
 * If the number of byte buffers available for read is less than [iovec]'s size,
 * only a prefix of [iovec] will be filled.
 *
 * The second [readAction]'s parameter denotes the number of buffers supplied.
 *
 * The value returned by the [readAction] is interpreted as the number of consumed bytes.
 * The size of the buffer will be reduced by that value,
 * and the corresponding number of bytes from buffer's prefix will be no longer available for read.
 * If data was not consumed, the [readAction] should return `0`.
 * This value will also be propagated as the function return value.
 *
 * If [readAction] ends execution by throwing an exception, no data will be consumed from the buffer.
 *
 * If the [iovec] contains any references, it will be overridden during the call.
 *
 * If the [buffer] is empty, [IllegalArgumentException] will be thrown.
 *
 * The data is passed to the [readAction] directly from the buffer's internal storage without copying on
 * the best effort basis, meaning that there are no strong zero-copy guarantees
 * and the copy will be created if it could not be omitted.
 *
 * @param buffer a buffer to read from
 * @param iovec a temporary array to store [ByteBuffer]s with data from [buffer]'s prefix
 * @param readAction an action that will be invoked on an array filled with [ByteBuffer]s holding data from [buffer]'s
 * prefix
 *
 * @return Number of bytes read as returned by [readAction].
 *
 * @throws IllegalArgumentException when the [buffer] is empty.
 * @throws IllegalArgumentException when the [iovec] is empty.
 *
 * @sample kotlinx.io.samples.unsafe.UnsafeReadWriteSamplesJvm.gatheringWrite
 *
 */
@UnsafeIoApi
@OptIn(ExperimentalContracts::class)
public inline fun UnsafeBufferOperations.readBulk(
    buffer: Buffer,
    iovec: Array<ByteBuffer?>,
    readAction: (iovec: Array<ByteBuffer?>, iovecSize: Int) -> Long
): Long {
    contract {
        callsInPlace(readAction, EXACTLY_ONCE)
    }

    val head = buffer.head ?: throw IllegalArgumentException("buffer is empty.")
    if (iovec.isEmpty()) throw IllegalArgumentException("iovec is empty.")

    var currentSegment: Segment = head
    var idx = 0
    var capacity = 0L
    do {
        val pos = currentSegment.pos
        val limit = currentSegment.limit
        val len = limit - pos
        iovec[idx++] = ByteBuffer.wrap(currentSegment.dataAsByteArray(true), pos, len)
            .slice()
            .asReadOnlyBuffer()
        capacity += len
        currentSegment = currentSegment.next ?: break
    } while (idx < iovec.size)

    val bytesRead = readAction(iovec, idx)
    if (bytesRead != 0L) {
        if (bytesRead < 0 || bytesRead > capacity) {
            throw IllegalStateException(
                "readAction should return a value in range [0, $capacity], but returned: $bytesRead"
            )
        }
        buffer.skip(bytesRead)
    }
    return bytesRead
}
