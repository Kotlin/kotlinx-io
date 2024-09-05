/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.samples.unsafe

import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlinx.io.unsafe.withData
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnsafeBufferOperationsSamples {
    @OptIn(UnsafeIoApi::class)
    @Test
    fun writeByteArrayToTail() {
        fun Buffer.writeRandomBytes(byteCount: Int) {
            require(byteCount > 0) { "byteCount should be positive. Was: $byteCount" }
            var remaining = byteCount
            while (remaining > 0) {
                UnsafeBufferOperations.writeToTail(this, 1) { data, startIndex, endIndex ->
                    // data's slice from startIndex to endIndex is available for writing,
                    // but that slice could be much larger than what remained to write.
                    val correctedEndIndex = min(endIndex, startIndex + remaining)
                    // write random bytes
                    Random.Default.nextBytes(data, startIndex, correctedEndIndex)
                    // number of bytes written
                    val written = correctedEndIndex - startIndex
                    remaining -= written
                    // that many bytes will be committed to the buffer
                    written
                }
            }
        }

        val buffer = Buffer()

        buffer.writeRandomBytes(42)
        assertEquals(42L, buffer.size)

        buffer.writeRandomBytes(10000)
        assertEquals(10042L, buffer.size)
    }

    @OptIn(ExperimentalEncodingApi::class, UnsafeIoApi::class)
    @Test
    fun moveToTail() {
        fun Buffer.writeBase64(data: ByteArray, encoder: Base64 = Base64.Default) {
            UnsafeBufferOperations.moveToTail(this, encoder.encodeToByteArray(data))
        }

        val buffer = Buffer()
        buffer.writeBase64(byteArrayOf(-1, 0, -2, 0))

        assertEquals("/wD+AA==", buffer.readString())
    }

    @OptIn(UnsafeIoApi::class)
    @Test
    fun readByteArrayFromHead() {
        // see https://en.wikipedia.org/wiki/LEB128#Unsigned_LEB128
        fun Buffer.readULeb128(): ULong {
            var shift = 0
            var result = 0L
            var complete = false

            while (!complete) {
                require(1) // check if we still have something to read

                UnsafeBufferOperations.readFromHead(this) { data, startOffset, endOffset ->
                    var offset = startOffset
                    do {
                        val b = data[offset++]
                        result = result.or(0x7fL.and(b.toLong()).shl(shift))
                        shift += 7
                        complete = b >= 0 // we're done if the most significant bit was not set
                    } while (!complete && offset < endOffset)
                    // return the number of consumed bytes
                    offset - startOffset
                }
            }

            return result.toULong()
        }

        val buffer = Buffer().apply {
            write(byteArrayOf(0)) // 0
            write(byteArrayOf(0xed.toByte(), 0x9b.toByte(), 0xb0.toByte(), 0x6f)) // dec0ded
            write(byteArrayOf(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1)) // ffffffffffffffff
        }
        assertEquals(0U, buffer.readULeb128())
        assertEquals(0xDEC0DEDu, buffer.readULeb128())
        assertEquals((-1).toULong(), buffer.readULeb128())
    }

    @OptIn(UnsafeIoApi::class)
    @Test
    fun readUleb128() {
        // Decode unsigned integer encoded using unsigned LEB128 format:
        // https://en.wikipedia.org/wiki/LEB128#Unsigned_LEB128
        fun Buffer.readULEB128UInt(): UInt {
            var result = 0u
            var shift = 0
            var finished = false
            while (!finished) { // Read until a number is fully fetched
                if (exhausted()) throw EOFException()
                // Pick the first segment and read from it either until the segment exhausted,
                // or the number if finished.
                UnsafeBufferOperations.readFromHead(this) { readCtx, segment ->
                    // Iterate over every byte contained in the segment
                    for (offset in 0..< segment.size) {
                        if (shift > 28) throw NumberFormatException("Overflow")
                        // Read the byte at the offset
                        val b = readCtx.getUnchecked(segment, offset)
                        val lsb = b.toUInt() and 0x7fu
                        result = result or (lsb shl shift)
                        shift += 7
                        if (b >= 0) {
                            finished = true
                            // We're done, return how many bytes were consumed from the segment
                            return@readFromHead offset + 1
                        }
                    }
                    // We read all the data from segment, but not finished yet.
                    // Return segment.size to indicate that the head segment was consumed in full.
                    segment.size
                }
            }
            return result
        }

        val buffer = Buffer().also { it.write(ByteString(0xe5.toByte(), 0x8e.toByte(), 0x26)) }
        assertEquals(624485u, buffer.readULEB128UInt())
        assertTrue(buffer.exhausted())

        buffer.write(ByteArray(8191))
        buffer.write(ByteString(0xe5.toByte(), 0x8e.toByte(), 0x26))
        buffer.skip(8191)
        assertEquals(624485u, buffer.readULEB128UInt())
        assertTrue(buffer.exhausted())
    }

    @OptIn(UnsafeIoApi::class)
    @Test
    fun writeUleb128() {
        // Encode an unsigned integer using unsigned LEB128 format:
        // https://en.wikipedia.org/wiki/LEB128#Unsigned_LEB128
        fun Buffer.writeULEB128(value: UInt) {
            // In the worst case, int will be encoded using 5 bytes
            val minCapacity = 5
            // Acquire a segment that can fit at least 5 bytes
            UnsafeBufferOperations.writeToTail(this, minCapacity) { ctx, segment ->
                // Count how many bytes were actually written
                var bytesWritten = 0
                var remainingBits = value
                do {
                    var b = remainingBits and 0x7fu
                    remainingBits = remainingBits shr 7
                    if (remainingBits != 0u) {
                        b = 0x80u or b
                    }
                    // Append a byte to the segment
                    ctx.setUnchecked(segment, bytesWritten++, b.toByte())
                } while (remainingBits != 0u)
                // Return how many bytes were actually written
                bytesWritten
            }
        }

        val buffer = Buffer()
        buffer.writeULEB128(624485u)
        assertEquals(ByteString(0xe5.toByte(), 0x8e.toByte(), 0x26), buffer.readByteString())
    }

    @OptIn(UnsafeIoApi::class)
    private fun Buffer.writeULEB128(value: UInt) {
        // update buffer's state after writing all bytes

        val minCapacity = 5 // in the worst case, int will be encoded using 5 bytes
        UnsafeBufferOperations.writeToTail(this, minCapacity) { ctx, seg ->
            var bytesWritten = 0
            var remainingBits = value
            do {
                var b = remainingBits and 0x7fu
                remainingBits = remainingBits shr 7
                if (remainingBits != 0u) {
                    b = 0x80u or b
                }
                ctx.setUnchecked(seg, bytesWritten++, b.toByte())
            } while (remainingBits != 0u)
            // return how many bytes were actually written
            bytesWritten
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class, UnsafeIoApi::class)
    @Test
    fun writeUleb128Array() {
        // Encode multiple unsigned integers using unsigned LEB128 format:
        // https://en.wikipedia.org/wiki/LEB128#Unsigned_LEB128
        fun Buffer.writeULEB128(data: UIntArray) {
            // Encode array length
            writeULEB128(data.size.toUInt())

            var index = 0
            while (index < data.size) {
                val value = data[index++]
                // optimize small values encoding: anything below 127 will be encoded using a single byte anyway
                if (value < 0x80u) {
                    // we need a space for a single byte, but if there's more - we'll try to fill it
                    UnsafeBufferOperations.writeToTail(this, 1) { ctx, seg ->
                        var bytesWritten = 0
                        ctx.setUnchecked(seg, bytesWritten++, value.toByte())

                        // let's save as much succeeding small values as possible
                        val remainingDataLength = data.size - index
                        val remainingCapacity = seg.remainingCapacity - 1
                        for (i in 0 until min(remainingDataLength, remainingCapacity)) {
                            val b = data[index]
                            if (b >= 0x80u) break
                            ctx.setUnchecked(seg, bytesWritten++, b.toByte())
                            index++
                        }
                        bytesWritten
                    }
                } else {
                    writeULEB128(value)
                }
            }
        }

        val buffer = Buffer()
        val data = UIntArray(10) { it.toUInt() }
        buffer.writeULEB128(data)
        assertEquals(ByteString(10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9), buffer.readByteString())
    }

    @Test
    @OptIn(ExperimentalUnsignedTypes::class)
    fun crc32Unsafe() {
        fun generateCrc32Table(): UIntArray {
            val table = UIntArray(256)
            for (idx in table.indices) {
                table[idx] = idx.toUInt()
                for (bit in 8 downTo 1) {
                    table[idx] = if (table[idx] % 2U == 0U) {
                        table[idx].shr(1)
                    } else {
                        table[idx].shr(1).xor(0xEDB88320U)
                    }
                }
            }
            return table
        }

        val crc32Table = generateCrc32Table()

        // Compute a CRC32 checksum over Buffer's content without consuming it
        @OptIn(UnsafeIoApi::class)
        fun Buffer.crc32(): UInt {
            var crc32 = 0xffffffffU
            // Iterate over segments starting from buffer's head
            UnsafeBufferOperations.forEachSegment(this) { ctx, segment ->
                var currentSegment = segment
                // Get data from a segment
                ctx.withData(currentSegment) { data, startIndex, endIndex ->
                    for (idx in startIndex..<endIndex) {
                        // Update crc32
                        val index = data[idx].xor(crc32.toByte()).toUByte()
                        crc32 = crc32Table[index.toInt()].xor(crc32.shr(8))
                    }
                }
            }
            return crc32.xor(0xffffffffU)
        }

        val buffer = Buffer().also { it.writeString("hello crc32") }
        assertEquals(0x9896d398U, buffer.crc32())
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun crc32GetUnchecked() {
        fun generateCrc32Table(): UIntArray {
            val table = UIntArray(256)
            for (idx in table.indices) {
                table[idx] = idx.toUInt()
                for (bit in 8 downTo 1) {
                    table[idx] = if (table[idx] % 2U == 0U) {
                        table[idx].shr(1)
                    } else {
                        table[idx].shr(1).xor(0xEDB88320U)
                    }
                }
            }
            return table
        }
        val crc32Table = generateCrc32Table()

        @OptIn(UnsafeIoApi::class)
        fun Buffer.crc32UsingGetUnchecked(): UInt {
            var crc32 = 0xffffffffU
            // iterate over all segments
            UnsafeBufferOperations.iterate(this) { ctx, head ->
                var currentSegment = head
                while (currentSegment != null) {
                    // Get data from a segment
                    for (offset in 0..<currentSegment.size) {
                        val index = ctx.getUnchecked(currentSegment, offset).xor(crc32.toByte()).toUByte()
                        crc32 = crc32Table[index.toInt()].xor(crc32.shr(8))
                    }
                    currentSegment = ctx.next(currentSegment)
                }
            }
            return crc32.xor(0xffffffffU)
        }

        val buffer = Buffer().also { it.writeString("hello crc32") }

        assertEquals(0x9896d398U, buffer.crc32UsingGetUnchecked())
    }
}
