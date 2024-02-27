/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package kotlinx.io.unsafe

import kotlinx.io.Buffer
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
