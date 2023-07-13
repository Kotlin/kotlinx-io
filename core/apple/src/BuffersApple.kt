@file:OptIn(UnsafeNumber::class)

package kotlinx.io

import kotlinx.cinterop.*
import platform.posix.memcpy
import platform.posix.uint8_tVar

internal fun Buffer.write(source: CPointer<uint8_tVar>?, maxLength: Int) {
    var currentOffset = 0
    while (currentOffset < maxLength) {
        val tail = writableSegment(1)

        val toCopy = minOf(maxLength - currentOffset, Segment.SIZE - tail.limit)
        tail.data.usePinned {
            memcpy(it.addressOf(tail.pos), source + currentOffset, toCopy.convert())
        }

        currentOffset += toCopy
        tail.limit += toCopy
    }
    size += maxLength
}

internal fun Buffer.readAtMostTo(sink: CPointer<uint8_tVar>?, maxLength: Int): Int {
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
