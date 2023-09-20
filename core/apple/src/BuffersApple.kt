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

@OptIn(ExperimentalForeignApi::class)
internal fun Buffer.write(source: CPointer<uint8_tVar>, maxLength: Int) {
    require(maxLength >= 0) { "maxLength ($maxLength) must not be negative" }

    var currentOffset = 0
    while (currentOffset < maxLength) {
        val tail = writableSegment(1)

        val toCopy = minOf(maxLength - currentOffset, Segment.SIZE - tail.limit)
        tail.data.usePinned {
            memcpy(it.addressOf(tail.limit), source + currentOffset, toCopy.convert())
        }

        currentOffset += toCopy
        tail.limit += toCopy
    }
    size += maxLength
}

internal fun Buffer.readAtMostTo(sink: CPointer<uint8_tVar>, maxLength: Int): Int {
    require(maxLength >= 0) { "maxLength ($maxLength) must not be negative" }

    val s = head ?: return 0
    val toCopy = minOf(maxLength, s.limit - s.pos)
    s.data.usePinned {
        memcpy(sink, it.addressOf(s.pos), toCopy.convert())
    }

    s.pos += toCopy
    size -= toCopy.toLong()

    if (s.pos == s.limit) {
        head = s.pop()
        SegmentPool.recycle(s)
    }

    return toCopy
}

@OptIn(BetaInteropApi::class)
internal fun Buffer.snapshotAsNSData(): NSData {
    if (size == 0L) return NSData.data()

    check(size.toULong() <= NSUIntegerMax) { "Buffer is too long ($size) to be converted into NSData." }

    val bytes = malloc(size.convert())?.reinterpret<uint8_tVar>()
        ?: throw Error("malloc failed: ${strerror(errno)?.toKString()}")
    var curr = head
    var index = 0
    do {
        check(curr != null) { "Current segment is null" }
        val pos = curr.pos
        val length = curr.limit - pos
        curr.data.usePinned {
            memcpy(bytes + index, it.addressOf(pos), length.convert())
        }
        curr = curr.next
        index += length
    } while (curr !== null)
    return NSData.create(bytesNoCopy = bytes, length = size.convert())
}
