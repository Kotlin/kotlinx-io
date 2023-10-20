/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)

package kotlinx.io

import kotlinx.cinterop.*
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
        writeUnbound(1) {
            val toCopy = minOf(maxLength - currentOffset, it.remainingCapacity)
            it.withContainedData { data, _, limit ->
                when (data) {
                    is ByteArray -> {
                        data.usePinned {
                            memcpy(it.addressOf(limit), source + currentOffset, toCopy.convert())
                        }
                    }
                    else -> {
                        TODO()
                    }
                }
            }
            currentOffset += toCopy
            toCopy
        }
    }
}

@OptIn(UnsafeIoApi::class)
internal fun Buffer.readAtMostTo(sink: CPointer<uint8_tVar>, maxLength: Int): Int {
    require(maxLength >= 0) { "maxLength ($maxLength) must not be negative" }

    val s = this.head ?: return 0
    val toCopy = minOf(maxLength, s.size)
    s.withContainedData { data, pos, _ ->
        when (data) {
            is ByteArray -> {
                data.usePinned {
                    memcpy(sink, it.addressOf(pos), toCopy.convert())
                }
            }
            else -> TODO()
        }
    }
    skip(toCopy.toLong())

    return toCopy
}

@OptIn(BetaInteropApi::class, UnsafeIoApi::class)
internal fun Buffer.snapshotAsNSData(): NSData {
    if (size == 0L) return NSData.data()

    check(size.toULong() <= NSUIntegerMax) { "Buffer is too long ($size) to be converted into NSData." }

    val bytes = malloc(size.convert())?.reinterpret<uint8_tVar>()
        ?: throw Error("malloc failed: ${strerror(errno)?.toKString()}")
    var curr = this.head
    var index = 0
    do {
        check(curr != null) { "Current segment is null" }
        curr.withContainedData { data, pos, limit ->
            val length = limit - pos
            when (data) {
                is ByteArray -> {
                    data.usePinned {
                        memcpy(bytes + index, it.addressOf(pos), length.convert())
                    }
                }
                else -> TODO()
            }
            index += length
        }
        curr = curr.next
    } while (curr !== null)
    return NSData.create(bytesNoCopy = bytes, length = size.convert())
}
