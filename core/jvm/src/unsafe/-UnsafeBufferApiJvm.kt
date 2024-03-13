/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package kotlinx.io.unsafe

import kotlinx.io.Buffer
import kotlinx.io.Segment
import kotlinx.io.SnapshotApi
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferAccessors.maxSafeWriteCapacity
import java.nio.ByteBuffer

/**
 * Provides read-only access to the data from [buffer]'s head by calling [readAction] on it
 * and optionally consumes the data by the end of the action.
 *
 * [ByteBuffer.position] of the buffer supplied to the [readAction] is used as the number of bytes consumed.
 * If the position is non-zero, corresponding number of bytes will be consumed from the buffer.
 *
 * If the buffer is empty, [IllegalArgumentException] will be thrown.
 *
 * This method avoids copying buffer's data when providing the [ByteBuffer] on the best-effort basis,
 * meaning that there are no strong zero-copy guarantees and the copy will be created if it could not be omitted.
 *
 * @throws IllegalArgumentException when [buffer] is empty.
 *
 * @sample kotlinx.io.samples.unsafe.UnsafeReadWriteSamplesJvm.writeToByteChannel
 */
@SnapshotApi
@UnsafeIoApi
public inline fun UnsafeBufferAccessors.readFromHead(buffer: Buffer, readAction: (data: ByteBuffer) -> Unit) {
    UnsafeBufferAccessors.readFromHead(buffer) { rawData, pos, limit ->
        val bb = ByteBuffer.wrap(rawData, pos, limit - pos).slice().asReadOnlyBuffer()
        readAction(bb)
        //val remaining = bb.remaining()
        //val originalSize = limit - pos
        //originalSize - remaining
        bb.position()
    }
}

/**
 * Provides read-only access to all the data from [buffer] by filling the [array] with [ByteBuffer]s
 * backed by [buffer]'s data and calling [readAction] on it,
 * and optionally consumes the data by the end of the action.
 *
 * The [readAction] receives [array] filled with [ByteBuffer]s and a pair of indices, startIndex and endIndex,
 * denoting the subarray containing meaningful data.
 * If the [buffer] has more segments than [array]'s capacity, only the portion that fits into [array] will be
 * passed down the [readAction].
 *
 * The [readAction] should return the number of bytes consumed, the buffer's size will be decreased by that value,
 * and data from the consumed prefix will be no longer available for read.
 * If the operation does not consume anything, the action should return `0`.
 * It's considered an error to return a negative value or a value exceeding the total capacity of [ByteBuffer]s
 * passed via [array].
 *
 * If the buffer or [array] is empty, [IllegalArgumentException] will be thrown.
 *
 * This method avoids copying buffer's data when providing the [ByteBuffer]s on the best-effort basis,
 * meaning that there are no strong zero-copy guarantees and the copy will be created if it could not be omitted.
 *
 * @throws IllegalArgumentException when [buffer] or [array] is empty.
 *
 * @sample kotlinx.io.samples.unsafe.UnsafeReadWriteSamplesJvm.gatheringWrite
 */
@SnapshotApi
@UnsafeIoApi
public inline fun UnsafeBufferAccessors.readFully(
    buffer: Buffer,
    array: Array<ByteBuffer?>,
    readAction: (data: Array<ByteBuffer?>, startIndex: Int, endIndex: Int) -> Long
) {
    val head = buffer.head ?: throw IllegalArgumentException("Buffer is empty.")
    if (array.isEmpty()) throw IllegalArgumentException("Array is empty.")

    var currentSegment: Segment = head
    var idx = 0
    var capacity = 0L
    do {
        val pos = currentSegment.pos
        val limit = currentSegment.limit
        val len = limit - pos
        array[idx++] = ByteBuffer.wrap(currentSegment.data, pos, len)
            .slice()
            .asReadOnlyBuffer()
        currentSegment = currentSegment.next!!
        capacity += len
    } while (idx < array.size && currentSegment !== head)
    val bytesRead = readAction(array, 0, idx)
    if (bytesRead == 0L) return
    if (bytesRead < 0 || bytesRead > capacity) {
        throw IllegalStateException(
            "readAction should return a value in range [0, $capacity], but returned: $bytesRead"
        )
    }
    buffer.skip(bytesRead)
}

/**
 * Provides write access to the [buffer], allowing to write data
 * into a not yet committed portion of the buffer's tail using a [writeAction].
 *
 * [ByteBuffer.position] of the buffer supplied to the [writeAction] is used as the number of bytes written.
 * If the position is non-zero, corresponding number of bytes will be added to the end of the [buffer].
 *
 * It's guaranteed that the size of the range is at least [minimumCapacity],
 * but if the [minimumCapacity] bytes could not be provided for writing,
 * the method will throw [IllegalStateException].
 * It is safe to use any [minimumCapacity] value below [maxSafeWriteCapacity], but unless exact minimum number of
 * available bytes is required, it's recommended to use `1` as [minimumCapacity] value.
 *
 * This method avoids copying buffer's data when providing the [ByteBuffer] on the best-effort basis,
 * meaning that there are no strong zero-copy guarantees and the copy will be created if it could not be omitted.
 *
 * @throws IllegalStateException when [minimumCapacity] is too large and could not be fulfilled.
 *
 * @sample kotlinx.io.samples.unsafe.UnsafeReadWriteSamplesJvm.readFromByteChannel
 */
@SnapshotApi
@UnsafeIoApi
public inline fun UnsafeBufferAccessors.writeToTail(
    buffer: Buffer,
    minimumCapacity: Int,
    writeAction: (data: ByteBuffer) -> Unit
) {
    UnsafeBufferAccessors.writeToTail(buffer, minimumCapacity) { rawData, pos, limit ->
        val bb = ByteBuffer.wrap(rawData, pos, limit - pos).slice()
        writeAction(bb)
        //val remaining = bb.remaining()
        //val originalSize = limit - pos
        //originalSize - remaining
        bb.position()
    }
}
