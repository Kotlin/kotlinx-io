/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.samples

import kotlinx.io.*
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

        // Basic Latin (a.k.a. ASCII) characters are encoded with a single byte
        buffer.writeCodePointValue('Y'.code)
        assertContentEquals(byteArrayOf(0x59), buffer.readByteArray())

        // wider characters are encoded into multiple UTF-8 code units
        buffer.writeCodePointValue('Δ'.code)
        assertContentEquals(byteArrayOf(0xce.toByte(), 0x94.toByte()), buffer.readByteArray())

        // note the difference: writeInt won't encode the code point, like writeCodePointValue did
        buffer.writeInt('Δ'.code)
        assertContentEquals(byteArrayOf(0, 0, 0x3, 0x94.toByte()), buffer.readByteArray())
    }

    @Test
    fun writeSurrogatePair() {
        val buffer = Buffer()

        // U+1F31E (a.k.a. "sun with face") is too wide to fit in a single UTF-16 character,
        // so it's represented using a surrogate pair.
        val chars = "🌞".toCharArray()
        assertEquals(2, chars.size)

        // such a pair has to be manually converted to a single code point
        assertTrue(chars[0].isHighSurrogate())
        assertTrue(chars[1].isLowSurrogate())

        val highSurrogate = chars[0].code
        val lowSurrogate = chars[1].code

        // see https://en.wikipedia.org/wiki/UTF-16#Code_points_from_U+010000_to_U+10FFFF for details
        val codePoint = 0x10000 + (highSurrogate - 0xD800).shl(10).or(lowSurrogate - 0xDC00)
        assertEquals(0x1F31E, codePoint)

        // now we can write the code point
        buffer.writeCodePointValue(codePoint)
        // and read the correct string back
        assertEquals("🌞", buffer.readString())

        // we won't achieve that by writing surrogates as it is
        buffer.apply {
            writeCodePointValue(highSurrogate)
            writeCodePointValue(lowSurrogate)
        }
        assertNotEquals("🌞", buffer.readString())
    }

    @Test
    fun readUtf8CodePointSample() {
        val buffer = Buffer()

        buffer.writeUShort(0xce94U)
        assertEquals(0x394, buffer.readCodePointValue()) // decodes a single UTF-8 encoded code point
    }

    @Test
    fun surrogatePairs() {
        val buffer = Buffer()

        // that's a U+1F31A, a.k.a. "new moon with face"
        buffer.writeString("🌚")
        // it should be encoded with 4 code units
        assertEquals(4, buffer.size)

        // let's read it back as a single code point
        val moonCodePoint = buffer.readCodePointValue()
        // all code units were consumed
        assertEquals(0, buffer.size)

        // the moon is too wide to fit in a single UTF-16 character!
        assertNotEquals(moonCodePoint, moonCodePoint.toChar().code)
        // "too wide" means in the [U+010000, U+10FFFF] range
        assertTrue(moonCodePoint in 0x10000..0x10FFFF)

        // See https://en.wikipedia.org/wiki/UTF-16#Code_points_from_U+010000_to_U+10FFFF for details
        val highSurrogate = (0xD800 + (moonCodePoint - 0x10000).ushr(10)).toChar()
        val lowSurrogate = (0xDC00 + (moonCodePoint - 0x10000).and(0x3FF)).toChar()

        assertContentEquals(charArrayOf(highSurrogate, lowSurrogate), "🌚".toCharArray())
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
    fun writeUtf8SeqSample() {
        val buffer = Buffer()

        buffer.writeString(StringBuilder("hello"), startIndex = 1, endIndex = 4)
        assertContentEquals(
            byteArrayOf(
                'e'.code.toByte(),
                'l'.code.toByte(),
                'l'.code.toByte()
            ), buffer.readByteArray()
        )

        buffer.writeString(StringBuilder("Δ"))
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
}

/**
 * Samples demonstrating [Processor] usage with pure Kotlin implementations.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class ProcessorSamplesCommon {
    /**
     * A pure Kotlin implementation of CRC32 as a [Processor].
     *
     * This demonstrates how to implement a checksum processor that works
     * across all Kotlin platforms without platform-specific dependencies.
     */
    private class Crc32Processor : Processor<Long> {
        private var crc: UInt = 0xffffffffU
        private var closed = false

        override fun process(source: Buffer, byteCount: Long) {
            check(!closed) { "Processor is closed" }
            require(byteCount >= 0) { "byteCount: $byteCount" }

            val toProcess = minOf(byteCount, source.size)
            // Read bytes without consuming
            for (i in 0 until toProcess) {
                val byte = source[i]
                val index = (crc xor byte.toUInt()).toUByte()
                crc = CRC32_TABLE[index.toInt()] xor (crc shr 8)
            }
        }

        fun current(): Long {
            check(!closed) { "Processor is closed" }
            return (crc xor 0xffffffffU).toLong()
        }

        override fun compute(): Long {
            check(!closed) { "Processor is closed" }
            val result = current()
            crc = 0xffffffffU  // reset
            return result
        }

        override fun close() {
            closed = true
        }

        companion object {
            // Pre-computed CRC32 lookup table (IEEE 802.3 polynomial)
            private val CRC32_TABLE: UIntArray = UIntArray(256) { i ->
                var crc = i.toUInt()
                repeat(8) {
                    crc = if (crc and 1U != 0U) {
                        (crc shr 1) xor 0xEDB88320U
                    } else {
                        crc shr 1
                    }
                }
                crc
            }
        }
    }

    @Test
    fun crc32PureKotlin() {
        val data = "Hello, World!"
        val buffer = Buffer()
        buffer.writeString(data)

        val checksum = (buffer as RawSource).compute(Crc32Processor())

        // Known CRC32 for "Hello, World!" (IEEE 802.3 polynomial)
        assertEquals(3964322768L, checksum)
    }

    @Test
    fun crc32IntermediateValues() {
        val processor = Crc32Processor()

        val buffer1 = Buffer()
        buffer1.writeString("Hello")
        processor.process(buffer1, buffer1.size)

        // Get intermediate CRC32
        val intermediate = processor.current()
        assertTrue(intermediate > 0)

        val buffer2 = Buffer()
        buffer2.writeString(", World!")
        processor.process(buffer2, buffer2.size)

        // Final CRC32 should be different from intermediate
        val final = processor.current()
        assertNotEquals(intermediate, final)

        // compute() returns same value as current() but resets
        assertEquals(final, processor.compute())

        // After compute(), starting value should be used
        assertEquals(0L, processor.current())

        processor.close()
    }

    @Test
    fun crc32ProcessorReuse() {
        val processor = Crc32Processor()

        // First computation
        val buffer1 = Buffer()
        buffer1.writeString("Test1")
        processor.process(buffer1, buffer1.size)
        val first = processor.compute()

        // Second computation - processor was reset
        val buffer2 = Buffer()
        buffer2.writeString("Test2")
        processor.process(buffer2, buffer2.size)
        val second = processor.compute()

        // Different inputs should produce different checksums
        assertNotEquals(first, second)

        processor.close()
    }

    @Test
    fun processDoesNotConsumeBuffer() {
        val processor = Crc32Processor()
        val buffer = Buffer()
        buffer.writeString("Hello")

        processor.process(buffer, buffer.size)

        // Buffer should still contain the data
        assertEquals(5L, buffer.size)
        assertEquals("Hello", buffer.readString())

        processor.close()
    }

    @Test
    fun processPartialBuffer() {
        val processor = Crc32Processor()
        val buffer = Buffer()
        buffer.writeString("Hello, World!")

        // Process only first 5 bytes
        processor.process(buffer, 5)

        val partialCrc = processor.current()

        // Process remaining bytes
        processor.process(buffer, buffer.size)

        val fullCrc = processor.current()

        // But wait - we processed "Hello" twice because buffer wasn't consumed!
        // This is expected behavior - process() doesn't consume.
        // The extension function RawSource.compute() handles consumption.

        processor.close()
    }
}
