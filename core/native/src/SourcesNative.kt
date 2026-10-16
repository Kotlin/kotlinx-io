/*
 * Copyright 2010-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.cinterop.*
import kotlinx.io.unsafe.UnsafeBufferOperations
import platform.posix.memcpy

/**
 * Reads at most [byteCount] bytes from this [Source](this), writes them into [ptr] and returns the number of
 * bytes read.
 *
 * **Note that this function does not verify whether the [ptr] points to a writeable memory region.**
 *
 * @param ptr The memory region to write data into.
 * @param byteCount The maximum number of bytes to read from this source.
 *
 * @throws IllegalArgumentException when [byteCount] is negative.
 * @throws IOException when some I/O error happens.
 */
@DelicateIoApi
@OptIn(ExperimentalForeignApi::class, InternalIoApi::class, UnsafeIoApi::class, UnsafeNumber::class)
public fun Source.readAtMostTo(ptr: CPointer<ByteVar>, byteCount: Long): Long {
    require(byteCount >= 0L) { "byteCount shouldn't be negative: $byteCount" }

    if (byteCount == 0L) return 0L

    if (!request(1L)) {
        return if (exhausted()) -1L else 0L
    }

    var consumed = 0L
    UnsafeBufferOperations.readFromHead(buffer) { array, startIndex, endIndex ->
        val toRead = minOf(endIndex - startIndex, byteCount).toInt()

        array.usePinned {
            memcpy(ptr, it.addressOf(startIndex), toRead.convert())
        }

        consumed += toRead
        toRead
    }

    return consumed
}

/**
 * Reads exactly [byteCount] bytes from this [Source](this) and writes them into a memory region pointed by [ptr].
 *
 * **Note that this function does not verify whether the [ptr] points to a writeable memory region.**
 *
 * This function consumes data from the source even if an error occurs.
 *
 * @param ptr The memory region to write data into.
 * @param byteCount The exact number of bytes to read from this source.
 *
 * @throws IllegalArgumentException when [byteCount] is negative.
 * @throws EOFException when the source exhausts before [byteCount] were read.
 * @throws IOException when some I/O error happens.
 */
@DelicateIoApi
@OptIn(ExperimentalForeignApi::class, InternalIoApi::class, UnsafeIoApi::class, UnsafeNumber::class)
public fun Source.readTo(ptr: CPointer<ByteVar>, byteCount: Long) {
    require(byteCount >= 0L) { "byteCount shouldn't be negative: $byteCount" }

    if (byteCount == 0L) return

    var consumed = 0L

    while (consumed < byteCount) {
        if (!request(1L)) {
            if (exhausted()) {
                throw EOFException("The source is exhausted before reading $byteCount bytes " +
                        "(it contained only $consumed bytes)")
            }
        }
        UnsafeBufferOperations.readFromHead(buffer) { array, startIndex, endIndex ->
            val toRead = minOf(endIndex - startIndex, byteCount - consumed).toInt()

            array.usePinned {
                memcpy(ptr + consumed, it.addressOf(startIndex), toRead.convert())
            }

            consumed += toRead
            toRead
        }
    }
}
