/*
 * Copyright 2010-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.cinterop.*
import kotlinx.io.unsafe.UnsafeBufferOperations
import platform.posix.memcpy

/**
 * Writes exactly [byteCount] bytes from a memory pointed by [ptr] into this [Sink](this).
 *
 * **Note that this function does not verify whether the [ptr] points to a readable memory region.**
 *
 * @param ptr The memory region to read data from.
 * @param byteCount The number of bytes that should be written into this sink from [ptr].
 *
 * @throws IllegalArgumentException when [byteCount] is negative.
 * @throws IOException when some I/O error happens.
 */
@DelicateIoApi
@OptIn(ExperimentalForeignApi::class, UnsafeIoApi::class, InternalIoApi::class, UnsafeNumber::class)
public fun Sink.write(ptr: CPointer<ByteVar>, byteCount: Long) {
    require(byteCount >= 0L) { "byteCount shouldn't be negative: $byteCount" }

    var remaining = byteCount
    var currentOffset = 0L

    while (remaining > 0) {
        UnsafeBufferOperations.writeToTail(buffer, 1) { array, startIndex, endIndex ->
            val toWrite = minOf(endIndex - startIndex, remaining).toInt()
            array.usePinned { pinned ->
                memcpy(pinned.addressOf(startIndex), ptr + currentOffset, toWrite.convert())
            }
            currentOffset += toWrite
            remaining -= toWrite

            toWrite
        }

        hintEmit()
    }
}
