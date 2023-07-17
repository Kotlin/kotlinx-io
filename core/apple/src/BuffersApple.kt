@file:OptIn(UnsafeNumber::class)

package kotlinx.io

import kotlinx.cinterop.*
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.buildByteString
import platform.Foundation.*
import platform.darwin.NSUInteger
import platform.darwin.NSUIntegerMax
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

internal fun Buffer.snapshotAsNSData(): NSData {
    if (size == 0L) return NSData.data()

    check(size.toULong() <= NSUIntegerMax) { "Buffer is too long ($size) to be converted into NSData." }

    val data = NSMutableData.create(length = size.convert())!!
    var curr = head
    var index: NSUInteger = 0U
    do {
        check(curr != null) { "Current segment is null" }
        val pos = curr.pos
        val length: NSUInteger = (curr.limit - pos).convert()
        curr.data.usePinned {
            data.replaceBytesInRange(NSMakeRange(index, length), it.addressOf(pos))
        }
        curr = curr.next
        index += length
    } while (curr !== head)
    return data
}
