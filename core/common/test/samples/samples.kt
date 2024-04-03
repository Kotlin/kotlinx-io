/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.samples

import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlin.math.min
import kotlin.test.*

class KotlinxIoCoreCommonSamples {
    @Test
    fun bufferToString() {
        val buffer = Buffer()
        assertEquals("Buffer(size=0)", buffer.toString())

        buffer.writeInt(0x12345678)
        assertEquals("Buffer(size=4 hex=12345678)", buffer.toString())

        buffer.skip(1)
        assertEquals("Buffer(size=3 hex=345678)", buffer.toString())
    }

    @Test
    fun bufferClear() {
        val buffer = Buffer()
        buffer.write(ByteArray(1024))
        assertFalse(buffer.exhausted())
        buffer.clear()
        assertTrue(buffer.exhausted())
    }

    @Test
    fun bufferGetByte() {
        val buffer = Buffer()
        buffer.writeString("Hello World!")

        assertEquals('H'.code, buffer[0].toInt())
        assertEquals('W'.code, buffer[buffer.indexOf('W'.code.toByte())].toInt())
    }

    @Test
    fun bufferCopy() {
        val buffer = Buffer()
        buffer.writeString("some string")

        val copy = Buffer()
        copy.writeString("sub")
        buffer.copyTo(copy, startIndex = 5)

        assertEquals("some string", buffer.readString())
        assertEquals("substring", copy.readString())
    }

    @Test
    fun transferFrom() {
        val src: Source = Buffer().also { it.writeString("Some data to transfer") }
        val dst = Buffer().also { it.writeString("Transferred: ") }

        dst.transferFrom(src)

        assertTrue(src.exhausted())
        assertEquals("Transferred: Some data to transfer", dst.readString())
    }

    @Test
    fun transferTo() {
        val src: Source = Buffer().also { it.writeString("Some data to transfer") }
        val dst = Buffer().also { it.writeString("Transferred: ") }

        src.transferTo(dst)

        assertTrue(src.exhausted())
        assertEquals("Transferred: Some data to transfer", dst.readString())
    }

    @Test
    fun peekSample() {
        val source: Source = Buffer().also { it.writeString("hello world") }

        val peek = source.peek().buffered()
        assertEquals("hello", peek.readString(5))
        peek.skip(1)
        assertEquals("world", peek.readString(5))
        assertTrue(peek.exhausted())

        assertEquals("hello world", source.readString())
    }

    @Test
    fun utf8SizeSample() {
        assertEquals("yes".length, "yes".utf8Size().toInt())

        assertNotEquals("არა".length, "არა".utf8Size().toInt())
        assertEquals(9, "არა".utf8Size().toInt())
        assertEquals("არა".encodeToByteArray().size, "არა".utf8Size().toInt())
    }

    @Test
    fun writeUtf8CodePointSample() {
        val buffer = Buffer()

        buffer.writeInt('Δ'.code) // writes integer value as is
        assertContentEquals(byteArrayOf(0, 0, 0x3, 0x94.toByte()), buffer.readByteArray())

        buffer.writeUtf8CodePoint('Δ'.code) // encodes code point using UTF-8 encoding
        assertContentEquals(byteArrayOf(0xce.toByte(), 0x94.toByte()), buffer.readByteArray())
    }

    @Test
    fun readUtf8CodePointSample() {
        val buffer = Buffer()

        buffer.writeUShort(0xce94U)
        assertEquals(0x394, buffer.readUtf8CodePoint()) // decodes single UTF-8 encoded code point
    }

    @Test
    fun readLinesSample() {
        val buffer = Buffer()
        buffer.writeString("No new line here.")

        assertFailsWith<EOFException> { buffer.readLineStrict() }
        assertEquals("No new line here.", buffer.readLine())

        buffer.writeString("Line1\n\nLine2")
        assertEquals("Line1", buffer.readLineStrict())
        assertEquals("\nLine2", buffer.readString())
    }

    @Test
    fun readShortLe() {
        val buffer = Buffer()
        buffer.writeShort(0x1234)
        assertEquals(0x3412, buffer.readShortLe())
    }

    @Test
    fun readIntLe() {
        val buffer = Buffer()
        buffer.writeInt(0x12345678)
        assertEquals(0x78563412, buffer.readIntLe())
    }

    @Test
    fun readLongLe() {
        val buffer = Buffer()
        buffer.writeLong(0x123456789ABCDEF0)
        assertEquals(0xF0DEBC9A78563412U.toLong(), buffer.readLongLe())
    }

    @Test
    fun writeShortLe() {
        val buffer = Buffer()
        buffer.writeShortLe(0x1234)
        assertEquals(0x3412, buffer.readShort())
    }

    @Test
    fun writeIntLe() {
        val buffer = Buffer()
        buffer.writeIntLe(0x12345678)
        assertEquals(0x78563412, buffer.readInt())
    }

    @Test
    fun writeLongLe() {
        val buffer = Buffer()
        buffer.writeLongLe(0x123456789ABCDEF0)
        assertEquals(0xF0DEBC9A78563412U.toLong(), buffer.readLong())
    }

    @Test
    fun readDecimalLong() {
        val buffer = Buffer()
        buffer.writeString("42 -1 1234567!")

        assertEquals(42L, buffer.readDecimalLong())
        buffer.skip(1) // skip space
        assertEquals(-1L, buffer.readDecimalLong())
        buffer.skip(1) // skip space
        assertEquals(1234567L, buffer.readDecimalLong())
        buffer.skip(1) // skip !
    }

    @Test
    fun writeDecimalLong() {
        val buffer = Buffer()

        buffer.writeDecimalLong(1024)
        buffer.writeString(", ")
        buffer.writeDecimalLong(-24)

        assertEquals("1024, -24", buffer.readString())
    }

    @Test
    fun writeHexLong() {
        val buffer = Buffer()

        buffer.writeHexadecimalUnsignedLong(10)
        assertEquals("a", buffer.readString())

        buffer.writeHexadecimalUnsignedLong(-10)
        assertEquals("fffffffffffffff6", buffer.readString())
    }

    @Test
    fun readHexLong() {
        val buffer = Buffer()

        buffer.writeString("0000a")
        assertEquals(10L, buffer.readHexadecimalUnsignedLong())

        buffer.writeString("fffFffFffFfffff6")
        assertEquals(-10L, buffer.readHexadecimalUnsignedLong())

        buffer.writeString("dear friend!")
        assertEquals(0xdea, buffer.readHexadecimalUnsignedLong())
        assertEquals("r friend!", buffer.readString())
    }

    @Test
    fun startsWithSample() {
        val buffer = Buffer()
        buffer.write(byteArrayOf(1, 2, 3, 4, 5))

        assertTrue(buffer.startsWith(1))
        assertFalse(buffer.startsWith(0))

        buffer.clear()
        assertFalse(buffer.startsWith(1))
    }

    @Test
    fun indexOfByteSample() {
        val buffer = Buffer()

        assertEquals(-1, buffer.indexOf('\n'.code.toByte()))

        buffer.writeString("Hello\nThis is line 2\nAnd this one is third.")
        assertEquals(5, buffer.indexOf('\n'.code.toByte()))
        assertEquals(20, buffer.indexOf('\n'.code.toByte(), startIndex = 6))
        assertEquals(-1, buffer.indexOf('\n'.code.toByte(), startIndex = 21))
    }

    @Test
    fun readToArraySample() {
        val buffer = Buffer()
        buffer.write(byteArrayOf(1, 2, 3, 4, 5, 6, 7))

        val out = ByteArray(10)
        buffer.readTo(out, startIndex = 1, endIndex = 4)
        assertContentEquals(byteArrayOf(0, 1, 2, 3, 0, 0, 0, 0, 0, 0), out)
        assertContentEquals(byteArrayOf(4, 5, 6, 7), buffer.readByteArray())
    }

    @Test
    fun writeUtf8Sample() {
        val buffer = Buffer()

        buffer.writeString("hello", startIndex = 1, endIndex = 4)
        assertContentEquals(
            byteArrayOf(
                'e'.code.toByte(),
                'l'.code.toByte(),
                'l'.code.toByte()
            ), buffer.readByteArray()
        )

        buffer.writeString("Δ")
        assertContentEquals(byteArrayOf(0xce.toByte(), 0x94.toByte()), buffer.readByteArray())
    }

    @Test
    fun readUtf8() {
        val buffer = Buffer()

        buffer.write("hello world".encodeToByteArray())
        assertEquals("hello", buffer.readString(5))
        assertEquals(" world", buffer.readString())

        buffer.write(byteArrayOf(0xce.toByte(), 0x94.toByte()))
        assertEquals("Δ", buffer.readString())
    }

    @Test
    fun readByteArraySample() {
        val buffer = Buffer()
        buffer.writeInt(0x12345678)
        buffer.writeShort(0)

        assertContentEquals(byteArrayOf(0x12, 0x34), buffer.readByteArray(2))
        assertContentEquals(byteArrayOf(0x56, 0x78, 0, 0), buffer.readByteArray())
    }

    @Test
    fun readByte() {
        val buffer = Buffer()
        buffer.write(byteArrayOf(1, 2, 3, 4))

        assertEquals(1, buffer.readByte())
        assertEquals(3, buffer.size)
    }

    @Test
    fun readShort() {
        val buffer = Buffer()
        buffer.write(byteArrayOf(1, 2, 3, 4))

        assertEquals(0x0102, buffer.readShort())
        assertEquals(2, buffer.size)
    }

    @Test
    fun readInt() {
        val buffer = Buffer()
        buffer.write(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))

        assertEquals(0x01020304, buffer.readInt())
        assertEquals(6, buffer.size)
    }

    @Test
    fun readLong() {
        val buffer = Buffer()
        buffer.write(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))

        assertEquals(0x0102030405060708L, buffer.readLong())
        assertEquals(2, buffer.size)
    }

    @Test
    fun skip() {
        val buffer = Buffer()
        buffer.write(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))

        buffer.skip(3)
        assertContentEquals(byteArrayOf(4, 5, 6, 7, 8), buffer.readByteArray())
    }

    @Test
    fun request() {
        val singleByteSource = object : RawSource {
            private var exhausted = false
            override fun readAtMostTo(sink: Buffer, byteCount: Long): Long = when {
                byteCount == 0L -> 0L
                exhausted -> -1L
                else -> {
                    exhausted = true
                    sink.writeByte(42)
                    1L
                }
            }

            override fun close() = Unit
        }

        val source = singleByteSource.buffered()

        assertTrue(source.request(1))
        // The request call already soaked all the data from the source
        assertEquals(-1, singleByteSource.readAtMostTo(Buffer(), 1))
        // There is only one byte, so we can't request more
        assertFalse(source.request(2))
        // But we didn't consume single byte yet, so request(1) will succeed
        assertTrue(source.request(1))
        assertEquals(42, source.readByte())
        assertFalse(source.request(1))
    }

    @Test
    fun require() {
        val singleByteSource = object : RawSource {
            private var exhausted = false
            override fun readAtMostTo(sink: Buffer, byteCount: Long): Long = when {
                byteCount == 0L -> 0L
                exhausted -> -1L
                else -> {
                    exhausted = true
                    sink.writeByte(42)
                    1L
                }
            }

            override fun close() = Unit
        }

        val source = singleByteSource.buffered()

        source.require(1)
        // The require call already soaked all the data from the source
        assertEquals(-1, singleByteSource.readAtMostTo(Buffer(), 1))
        // There is only one byte, so we can't request more
        assertFailsWith<EOFException> { source.require(2) }
        // But we didn't consume single byte yet, so require(1) will succeed
        source.require(1)
        assertEquals(42, source.readByte())
        assertFailsWith<EOFException> { source.require(1) }
    }

    @Test
    fun exhausted() {
        val singleByteSource = object : RawSource {
            private var exhausted = false
            override fun readAtMostTo(sink: Buffer, byteCount: Long): Long = when {
                byteCount == 0L -> 0L
                exhausted -> -1L
                else -> {
                    exhausted = true
                    sink.writeByte(42)
                    1L
                }
            }

            override fun close() = Unit
        }

        val source = singleByteSource.buffered()

        assertFalse(source.exhausted())
        assertContentEquals(byteArrayOf(42), source.readByteArray())
        assertTrue(source.exhausted())
    }

    @Test
    fun writeByte() {
        val buffer = Buffer()

        buffer.writeByte(42)
        assertContentEquals(byteArrayOf(42), buffer.readByteArray())
    }

    @Test
    fun writeShort() {
        val buffer = Buffer()

        buffer.writeShort(42)
        assertContentEquals(byteArrayOf(0, 42), buffer.readByteArray())
    }

    @Test
    fun writeInt() {
        val buffer = Buffer()

        buffer.writeInt(42)
        assertContentEquals(byteArrayOf(0, 0, 0, 42), buffer.readByteArray())
    }


    @Test
    fun writeLong() {
        val buffer = Buffer()

        buffer.writeLong(42)
        assertContentEquals(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 42), buffer.readByteArray())
    }

    @Test
    fun readUByte() {
        val buffer = Buffer()
        buffer.write(byteArrayOf(-1))

        assertEquals(255U, buffer.readUByte())
    }

    @Test
    fun readUShort() {
        val buffer = Buffer()
        buffer.write(byteArrayOf(-1, -1))

        assertEquals(65535U, buffer.readUShort())
    }

    @Test
    fun readUInt() {
        val buffer = Buffer()
        buffer.write(byteArrayOf(-1, -1, -1, -1))

        assertEquals(4294967295U, buffer.readUInt())
    }

    @Test
    fun readULong() {
        val buffer = Buffer()
        buffer.write(byteArrayOf(-1, -1, -1, -1, -1, -1, -1, -1))

        assertEquals(18446744073709551615UL, buffer.readULong())
    }

    @Test
    fun readFloat() {
        val buffer = Buffer()
        buffer.write(byteArrayOf(70, 64, -26, -74))
        assertEquals(12345.678F.toBits(), buffer.readFloat().toBits())
    }

    @Test
    fun readDouble() {
        val buffer = Buffer()
        buffer.write(byteArrayOf(64, -2, 36, 12, -97, -56, -13, 35))

        assertEquals(123456.78901, buffer.readDouble())
    }

    @Test
    fun writeUByte() {
        val buffer = Buffer()
        buffer.writeUByte(255U)

        assertContentEquals(byteArrayOf(-1), buffer.readByteArray())
    }

    @Test
    fun writeUShort() {
        val buffer = Buffer()
        buffer.writeUShort(65535U)

        assertContentEquals(byteArrayOf(-1, -1), buffer.readByteArray())
    }

    @Test
    fun writeUInt() {
        val buffer = Buffer()
        buffer.writeUInt(4294967295U)

        assertContentEquals(byteArrayOf(-1, -1, -1, -1), buffer.readByteArray())
    }

    @Test
    fun writeULong() {
        val buffer = Buffer()
        buffer.writeULong(18446744073709551615UL)

        assertContentEquals(byteArrayOf(-1, -1, -1, -1, -1, -1, -1, -1), buffer.readByteArray())
    }

    @Test
    fun writeFloat() {
        val buffer = Buffer()
        buffer.writeFloat(12345.678F)

        assertContentEquals(byteArrayOf(70, 64, -26, -74), buffer.readByteArray())
    }

    @Test
    fun writeDouble() {
        val buffer = Buffer()
        buffer.writeDouble(123456.78901)

        assertContentEquals(byteArrayOf(64, -2, 36, 12, -97, -56, -13, 35), buffer.readByteArray())
    }

    @Test
    fun flush() {
        val rawSink = object : RawSink {
            val buffer = Buffer()
            var flushed = false
            override fun write(source: Buffer, byteCount: Long) {
                source.readTo(buffer, byteCount)
            }

            override fun flush() {
                flushed = true
            }

            override fun close() = Unit
        }

        val buffered = rawSink.buffered()

        buffered.writeShort(0x1000)
        // Not data were sent to the underlying sink and it was not flushed yet
        assertFalse(rawSink.flushed)
        assertEquals(0, rawSink.buffer.size)

        buffered.flush()
        // The sink was filled with buffered data and then flushed
        assertTrue(rawSink.flushed)
        assertContentEquals(byteArrayOf(0x10, 0), rawSink.buffer.readByteArray())
    }

    @Test
    fun emit() {
        val rawSink = object : RawSink {
            val buffer = Buffer()
            var flushed = false
            override fun write(source: Buffer, byteCount: Long) {
                source.readTo(buffer, byteCount)
            }

            override fun flush() {
                flushed = true
            }

            override fun close() = Unit
        }

        val buffered = rawSink.buffered()

        buffered.writeShort(0x1000)
        // Not data were sent to the underlying sink and it was not flushed yet
        assertFalse(rawSink.flushed)
        assertEquals(0, rawSink.buffer.size)

        buffered.emit()
        // The sink was filled with buffered data, but it was not flushed
        assertFalse(rawSink.flushed)
        assertContentEquals(byteArrayOf(0x10, 0), rawSink.buffer.readByteArray())
    }

    @Test
    fun writeSourceToSink() {
        val sink = Buffer()
        val source = Buffer().also { it.writeInt(0x01020304) }

        sink.write(source, 3)
        assertContentEquals(byteArrayOf(1, 2, 3), sink.readByteArray())
        assertContentEquals(byteArrayOf(4), source.readByteArray())
    }

    @Test
    fun writeByteArrayToSink() {
        val sink = Buffer()

        sink.write(byteArrayOf(1, 2, 3, 4))
        assertContentEquals(byteArrayOf(1, 2, 3, 4), sink.readByteArray())

        sink.write(byteArrayOf(1, 2, 3, 4), startIndex = 1, endIndex = 3)
        assertContentEquals(byteArrayOf(2, 3), sink.readByteArray())
    }

    @Test
    fun readSourceToSink() {
        val sink = Buffer()
        val source = Buffer().also { it.writeInt(0x01020304) }

        source.readTo(sink, 3)
        assertContentEquals(byteArrayOf(1, 2, 3), sink.readByteArray())
        assertContentEquals(byteArrayOf(4), source.readByteArray())
    }

    @Test
    fun readAtMostToByteArray() {
        val source = Buffer().also { it.write(byteArrayOf(1, 2, 3, 4, 5, 6)) }
        val sink = ByteArray(10)

        val bytesRead = source.readAtMostTo(sink) // read at most 10 bytes
        assertEquals(6, bytesRead)
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 0, 0, 0, 0), sink)
    }

    @Test
    fun readAtMostToSink() {
        val source = Buffer().also { it.write(byteArrayOf(1, 2, 3, 4, 5, 6)) }
        val sink = Buffer()

        val bytesRead = source.readAtMostTo(sink, 10) // read at most 10 bytes
        assertEquals(6, bytesRead)
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5, 6), sink.readByteArray())
    }

    @Test
    fun readUShortLe() {
        val buffer = Buffer()
        buffer.writeUShort(0x1234U)
        assertEquals(0x3412U, buffer.readUShortLe())
    }

    @Test
    fun readUIntLe() {
        val buffer = Buffer()
        buffer.writeUInt(0x12345678U)
        assertEquals(0x78563412U, buffer.readUIntLe())
    }

    @Test
    fun readULongLe() {
        val buffer = Buffer()
        buffer.writeULong(0x123456789ABCDEF0U)
        assertEquals(0xF0DEBC9A78563412U, buffer.readULongLe())
    }

    @Test
    fun readFloatLe() {
        val buffer = Buffer()
        buffer.write(byteArrayOf(-74, -26, 64, 70))
        assertEquals(12345.678F.toBits(), buffer.readFloatLe().toBits())
    }

    @Test
    fun readDoubleLe() {
        val buffer = Buffer()
        buffer.write(byteArrayOf(35, -13, -56, -97, 12, 36, -2, 64))
        assertEquals(123456.78901, buffer.readDoubleLe())
    }

    @Test
    fun writeUShortLe() {
        val buffer = Buffer()
        buffer.writeUShortLe(0x1234U)
        assertEquals(0x3412U, buffer.readUShort())
    }

    @Test
    fun writeUIntLe() {
        val buffer = Buffer()
        buffer.writeUIntLe(0x12345678U)
        assertEquals(0x78563412U, buffer.readUInt())
    }

    @Test
    fun writeULongLe() {
        val buffer = Buffer()
        buffer.writeULongLe(0x123456789ABCDEF0U)
        assertEquals(0xF0DEBC9A78563412U, buffer.readULong())
    }

    @Test
    fun writeFloatLe() {
        val buffer = Buffer()
        buffer.writeFloatLe(12345.678F)

        assertContentEquals(byteArrayOf(-74, -26, 64, 70), buffer.readByteArray())
    }

    @Test
    fun writeDoubleLe() {
        val buffer = Buffer()
        buffer.writeDoubleLe(123456.78901)

        assertContentEquals(byteArrayOf(35, -13, -56, -97, 12, 36, -2, 64), buffer.readByteArray())
    }

    @OptIn(UnsafeIoApi::class)
    @Test
    fun writeUleb128() {
        // https://en.wikipedia.org/wiki/LEB128
        fun Buffer.writeULEB128(value: UInt) {
            // update buffer's state after writing all bytes
            UnsafeBufferOperations.writeToTail(this, 5 /* in the worst case, int will be encoded using 5 bytes */) { ctx, seg ->
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

        val buffer = Buffer()
        buffer.writeULEB128(624485u)
        assertEquals(ByteString(0xe5.toByte(), 0x8e.toByte(), 0x26), buffer.readByteString())
    }

    @OptIn(UnsafeIoApi::class)
    private fun Buffer.writeULEB128(value: UInt) {
        // update buffer's state after writing all bytes
        UnsafeBufferOperations.writeToTail(this, 5 /* in the worst case, int will be encoded using 5 bytes */) { ctx, seg ->
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
        fun Buffer.writeULEB128(data: UIntArray) {
            // encode array length
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
}
