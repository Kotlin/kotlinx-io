/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)

package kotlinx.io

import kotlinx.cinterop.*
import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlinx.io.unsafe.withData
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.data
import platform.darwin.NSUIntegerMax
import platform.posix.*

@OptIn(ExperimentalForeignApi::class, UnsafeIoApi::class)
internal fun Buffer.write(source: CPointer<uint8_tVar>, maxLength: Int) {
    require(maxLength >= 0) { "maxLength ($maxLength) must not be negative" }

    var currentOffset = 0
    while (currentOffset < maxLength) {
        UnsafeBufferOperations.writeToTail(this, 1) { data, pos, limit ->
            val toCopy = minOf(maxLength - currentOffset, limit - pos)
            data.usePinned {
                memcpy(it.addressOf(pos), source + currentOffset, toCopy.convert())
            }
            currentOffset += toCopy
            toCopy
        }
    }
}

@OptIn(UnsafeIoApi::class)
internal fun Buffer.readAtMostTo(sink: CPointer<uint8_tVar>, maxLength: Int): Int {
    require(maxLength >= 0) { "maxLength ($maxLength) must not be negative" }

    var toCopy = 0
    UnsafeBufferOperations.readFromHead(this) { data, pos, limit ->
        toCopy = minOf(maxLength, limit - pos)
        data.usePinned {
            memcpy(sink, it.addressOf(pos), toCopy.convert())
        }
        toCopy
    }
    return toCopy
}

@OptIn(BetaInteropApi::class, UnsafeIoApi::class)
internal fun Buffer.snapshotAsNSData(): NSData {
    if (size == 0L) return NSData.data()

    check(size.toULong() <= NSUIntegerMax) { "Buffer is too long ($size) to be converted into NSData." }

    val bytes = malloc(size.convert())?.reinterpret<uint8_tVar>()
        ?: throw Error("malloc failed: ${strerror(errno)?.toKString()}")

    UnsafeBufferOperations.iterate(this) { ctx, head ->
        var curr: Segment? = head
        var index = 0
        while (curr != null) {
            val segment: Segment = curr
            //ctx.read(segment) { rctx, seg ->
                ctx.withData(segment) { data, pos, limit ->
                    val length = limit - pos
                    data.usePinned {
                        memcpy(bytes + index, it.addressOf(pos), length.convert())
                    }
                    index += length
                }
            //}
            curr = ctx.next(segment)
        }
    }
    return NSData.create(bytesNoCopy = bytes, length = size.convert())
}
