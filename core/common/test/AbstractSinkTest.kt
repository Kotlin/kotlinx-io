/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.io

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BufferSinkTest : AbstractSinkTest(SinkFactory.BUFFER)
class RealSinkTest : AbstractSinkTest(SinkFactory.REAL_BUFFERED_SINK)

abstract class AbstractSinkTest internal constructor(
    factory: SinkFactory
) {
    private val data: Buffer = Buffer()
    private val sink: Sink = factory.create(data)

    @Test
    fun writeByteArray() {
        val source = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
        sink.write(source)
        sink.flush()
        assertEquals("Buffer(size=10 hex=00010203040506070809)", data.toString())
        data.clear()

        sink.write(source, 3)
        sink.flush()
        assertEquals("Buffer(size=7 hex=03040506070809)", data.toString())
        data.clear()

        sink.write(source, 0, 3)
        sink.flush()
        assertEquals("Buffer(size=3 hex=000102)", data.toString())
        data.clear()

        assertFailsWith<IndexOutOfBoundsException> {
            sink.write(source, startIndex = -1, endIndex = 1)
        }
        assertEquals(0, data.size)

        assertFailsWith<IndexOutOfBoundsException> {
            sink.write(source, startIndex = 1, endIndex = source.size + 1)
        }
        assertEquals(0, data.size)
    }

    @Test
    fun writeNothing() {
        sink.writeString("")
        sink.flush()
        assertEquals(0, data.size)
    }

    @Test
    fun writeByte() {
        sink.writeByte(0xba.toByte())
        sink.flush()
        assertEquals("Buffer(size=1 hex=ba)", data.toString())
    }

    @Test
    fun writeBytes() {
        sink.writeByte(0xab.toByte())
        sink.writeByte(0xcd.toByte())
        sink.flush()
        assertEquals("Buffer(size=2 hex=abcd)", data.toString())
    }

    @Test
    fun writeLastByteInSegment() {
        sink.writeString("a".repeat(Segment.SIZE - 1))
        sink.writeByte(0x20)
        sink.writeByte(0x21)
        sink.flush()
        assertEquals(listOf(Segment.SIZE, 1), segmentSizes(data))
        assertEquals("a".repeat(Segment.SIZE - 1), data.readString(Segment.SIZE - 1L))
        assertEquals("Buffer(size=2 hex=2021)", data.toString())
    }

    @Test
    fun writeShort() {
        sink.writeShort(0xab01.toShort())
        sink.flush()
        assertEquals("Buffer(size=2 hex=ab01)", data.toString())
    }

    @Test
    fun writeShorts() {
        sink.writeShort(0xabcd.toShort())
        sink.writeShort(0x4321)
        sink.flush()
        assertEquals("Buffer(size=4 hex=abcd4321)", data.toString())
    }

    @Test
    fun writeShortLe() {
        sink.writeShortLe(0xcdab.toShort())
        sink.writeShortLe(0x2143)
        sink.flush()
        assertEquals("Buffer(size=4 hex=abcd4321)", data.toString())
    }

    @Test
    fun writeInt() {
        sink.writeInt(0x197760)
        sink.flush()
        assertEquals("Buffer(size=4 hex=00197760)", data.toString())
    }

    @Test
    fun writeInts() {
        sink.writeInt(-0x543210ff)
        sink.writeInt(-0x789abcdf)
        sink.flush()
        assertEquals("Buffer(size=8 hex=abcdef0187654321)", data.toString())
    }

    @Test
    fun writeLastIntegerInSegment() {
        sink.writeString("a".repeat(Segment.SIZE - 4))
        sink.writeInt(-0x543210ff)
        sink.writeInt(-0x789abcdf)
        sink.flush()
        assertEquals(listOf(Segment.SIZE, 4), segmentSizes(data))
        assertEquals("a".repeat(Segment.SIZE - 4), data.readString(Segment.SIZE - 4L))
        assertEquals("Buffer(size=8 hex=abcdef0187654321)", data.toString())
    }

    @Test
    fun writeIntegerDoesNotQuiteFitInSegment() {
        sink.writeString("a".repeat(Segment.SIZE - 3))
        sink.writeInt(-0x543210ff)
        sink.writeInt(-0x789abcdf)
        sink.flush()
        assertEquals(listOf(Segment.SIZE - 3, 8), segmentSizes(data))
        assertEquals("a".repeat(Segment.SIZE - 3), data.readString(Segment.SIZE - 3L))
        assertEquals("Buffer(size=8 hex=abcdef0187654321)", data.toString())
    }

    @Test
    fun writeIntLe() {
        sink.writeIntLe(-0x543210ff)
        sink.writeIntLe(-0x789abcdf)
        sink.flush()
        assertEquals("Buffer(size=8 hex=01efcdab21436587)", data.toString())
    }

    @Test
    fun writeLong() {
        sink.writeLong(0x123456789abcdef0L)
        sink.flush()
        assertEquals("Buffer(size=8 hex=123456789abcdef0)", data.toString())
    }

    @Test
    fun writeLongs() {
        sink.writeLong(-0x543210fe789abcdfL)
        sink.writeLong(-0x350145414f4ea400L)
        sink.flush()
        assertEquals("Buffer(size=16 hex=abcdef0187654321cafebabeb0b15c00)", data.toString())
    }

    @Test
    fun writeLongLe() {
        sink.writeLongLe(-0x543210fe789abcdfL)
        sink.writeLongLe(-0x350145414f4ea400L)
        sink.flush()
        assertEquals("Buffer(size=16 hex=2143658701efcdab005cb1b0bebafeca)", data.toString())
    }

    @Test
    fun writeAll() {
        val source = Buffer()
        source.writeString("abcdef")

        assertEquals(6, sink.transferFrom(source))
        assertEquals(0, source.size)
        sink.flush()
        assertEquals("abcdef", data.readString())
    }

    @Test
    fun writeAllExhausted() {
        val source = Buffer()
        assertEquals(0, sink.transferFrom(source))
        assertEquals(0, source.size)
    }

    @Test
    fun writeSource() {
        val source = Buffer()
        source.writeString("abcdef")

        // Force resolution of the Source method overload.
        sink.write(source as RawSource, 4)
        sink.flush()
        assertEquals("abcd", data.readString())
        assertEquals("ef", source.readString())
    }

    @Test
    fun writeSourceReadsFully() {
        val source = object : RawSource by Buffer() {
            override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
                sink.writeString("abcd")
                return 4
            }
        }

        sink.write(source, 8)
        sink.flush()
        assertEquals("abcdabcd", data.readString())
    }

    @Test
    fun writeSourcePropagatesEof() {
        val source: RawSource = Buffer().also { it.writeString("abcd") }

        assertFailsWith<EOFException> {
            sink.write(source, 8)
        }

        // Ensure that whatever was available was correctly written.
        sink.flush()
        assertEquals("abcd", data.readString())
    }

    @Test
    fun writeBufferThrowsIAE() {
        val source = Buffer()
        source.writeString("abcd")

        assertFailsWith<IllegalArgumentException> {
            sink.write(source, 8)
        }

        sink.flush()
        assertEquals("", data.readString())
    }

    @Test
    fun writeSourceWithNegativeBytesCount() {
        val source: RawSource = Buffer().also { it.writeByte(0) }

        assertFailsWith<IllegalArgumentException> {
            sink.write(source, -1L)
        }
    }

    @Test
    fun writeBufferWithNegativeBytesCount() {
        val source = Buffer().also { it.writeByte(0) }

        assertFailsWith<IllegalArgumentException> {
            sink.write(source, -1L)
        }
    }

    @Test
    fun writeSourceWithZeroIsNoOp() {
        // This test ensures that a zero byte count never calls through to read the source. It may be
        // tied to something like a socket which will potentially block trying to read a segment when
        // ultimately we don't want any data.
        val source = object : RawSource by Buffer() {
            override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
                throw AssertionError()
            }
        }
        sink.write(source, 0)
        assertEquals(0, data.size)
    }

    @Test
    fun closeEmitsBufferedBytes() {
        sink.writeByte('a'.code.toByte())
        sink.close()
        assertEquals('a', data.readByte().toInt().toChar())
    }

    /**
     * This test hard codes the results of Long.toString() because that function rounds large values
     * when using Kotlin/JS IR. https://youtrack.jetbrains.com/issue/KT-39891
     */
    @Test
    fun longDecimalString() {
        assertLongDecimalString("0", 0)
        assertLongDecimalString("-9223372036854775808", Long.MIN_VALUE)
        assertLongDecimalString("9223372036854775807", Long.MAX_VALUE)
        assertLongDecimalString("9", 9L)
        assertLongDecimalString("99", 99L)
        assertLongDecimalString("999", 999L)
        assertLongDecimalString("9999", 9999L)
        assertLongDecimalString("99999", 99999L)
        assertLongDecimalString("999999", 999999L)
        assertLongDecimalString("9999999", 9999999L)
        assertLongDecimalString("99999999", 99999999L)
        assertLongDecimalString("999999999", 999999999L)
        assertLongDecimalString("9999999999", 9999999999L)
        assertLongDecimalString("99999999999", 99999999999L)
        assertLongDecimalString("999999999999", 999999999999L)
        assertLongDecimalString("9999999999999", 9999999999999L)
        assertLongDecimalString("99999999999999", 99999999999999L)
        assertLongDecimalString("999999999999999", 999999999999999L)
        assertLongDecimalString("9999999999999999", 9999999999999999L)
        assertLongDecimalString("99999999999999999", 99999999999999999L)
        assertLongDecimalString("999999999999999999", 999999999999999999L)
        assertLongDecimalString("10", 10L)
        assertLongDecimalString("100", 100L)
        assertLongDecimalString("1000", 1000L)
        assertLongDecimalString("10000", 10000L)
        assertLongDecimalString("100000", 100000L)
        assertLongDecimalString("1000000", 1000000L)
        assertLongDecimalString("10000000", 10000000L)
        assertLongDecimalString("100000000", 100000000L)
        assertLongDecimalString("1000000000", 1000000000L)
        assertLongDecimalString("10000000000", 10000000000L)
        assertLongDecimalString("100000000000", 100000000000L)
        assertLongDecimalString("1000000000000", 1000000000000L)
        assertLongDecimalString("10000000000000", 10000000000000L)
        assertLongDecimalString("100000000000000", 100000000000000L)
        assertLongDecimalString("1000000000000000", 1000000000000000L)
        assertLongDecimalString("10000000000000000", 10000000000000000L)
        assertLongDecimalString("100000000000000000", 100000000000000000L)
        assertLongDecimalString("1000000000000000000", 1000000000000000000L)
    }

    private fun assertLongDecimalString(string: String, value: Long) {
        with(sink) {
            writeDecimalLong(value)
            writeString("zzz")
            flush()
        }
        val expected = "${string}zzz"
        val actual = data.readString()
        assertEquals(expected, actual, "$value expected $expected but was $actual")
    }

    @Test
    fun longHexString() {
        assertLongHexString(0)
        assertLongHexString(Long.MIN_VALUE)
        assertLongHexString(Long.MAX_VALUE)

        for (i in 0..62) {
            assertLongHexString((1L shl i) - 1)
            assertLongHexString(1L shl i)
        }
    }

    private fun assertLongHexString(value: Long) {
        with(sink) {
            writeHexadecimalUnsignedLong(value)
            writeString("zzz")
            flush()
        }
        val expected = "${value.toHexString()}zzz"
        val actual = data.readString()
        assertEquals(expected, actual, "$value expected $expected but was $actual")
    }

    @Test
    fun writeUtf8FromIndex() {
        sink.writeString("12345", 3)
        sink.emit()
        assertEquals("45", data.readString())
    }

    @Test
    fun writeUtf8FromRange() {
        sink.writeString("0123456789", 4, 7)
        sink.emit()
        assertEquals("456", data.readString())
    }

    @Test
    fun writeUtf8WithInvalidIndexes() {
        assertFailsWith<IndexOutOfBoundsException> { sink.writeString("hello", startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { sink.writeString("hello", startIndex = 0, endIndex = 6) }
        assertFailsWith<IllegalArgumentException> { sink.writeString("hello", startIndex = 6) }
    }

    @Test
    fun writeCharSequenceFromIndex() {
        sink.writeString(StringBuilder("12345"), 3)
        sink.emit()
        assertEquals("45", data.readString())
    }

    @Test
    fun writeCharSequenceFromRange() {
        sink.writeString(StringBuilder("0123456789"), 4, 7)
        sink.emit()
        assertEquals("456", data.readString())
    }

    @Test
    fun writeCharSequenceWithInvalidIndexes() {
        assertFailsWith<IndexOutOfBoundsException> { sink.writeString(StringBuilder("hello"), startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { sink.writeString(StringBuilder("hello"), startIndex = 0, endIndex = 6) }
        assertFailsWith<IllegalArgumentException> { sink.writeString(StringBuilder("hello"), startIndex = 6) }
    }

    @Test
    fun writeUByte() {
        sink.writeUByte(0xffu)
        sink.flush()
        assertEquals(-1, data.readByte())
    }

    @Test
    fun writeUShort() {
        sink.writeUShort(0xffffu)
        sink.flush()
        assertEquals(-1, data.readShort())
    }

    @Test
    fun writeUShortLe() {
        sink.writeUShortLe(0x1234u)
        sink.flush()
        assertEquals("Buffer(size=2 hex=3412)", data.toString())
    }

    @Test
    fun writeUInt() {
        sink.writeUInt(0xffffffffu)
        sink.flush()
        assertEquals(-1, data.readInt())
    }

    @Test
    fun writeUIntLe() {
        sink.writeUIntLe(0x12345678u)
        sink.flush()
        assertEquals("Buffer(size=4 hex=78563412)", data.toString())
    }

    @Test
    fun writeULong() {
        sink.writeULong(0xffffffffffffffffu)
        sink.flush()
        assertEquals(-1, data.readLong())
    }

    @Test
    fun writeULongLe() {
        sink.writeULongLe(0x1234567890abcdefu)
        sink.flush()
        assertEquals("Buffer(size=8 hex=efcdab9078563412)", data.toString())
    }

    @Test
    fun writeFloat() {
        sink.writeFloat(12345.678F)
        sink.flush()
        assertEquals(12345.678F.toBits(), data.readInt())
    }

    @Test
    fun writeFloatLe() {
        sink.writeFloatLe(12345.678F)
        sink.flush()
        assertEquals(12345.678F.toBits(), data.readIntLe())
    }

    @Test
    fun writeDouble() {
        sink.writeDouble(123456.78901)
        sink.flush()
        assertEquals(123456.78901.toBits(), data.readLong())
    }

    @Test
    fun writeDoubleLe() {
        sink.writeDoubleLe(123456.78901)
        sink.flush()
        assertEquals(123456.78901.toBits(), data.readLongLe())
    }

    @Test
    fun writeByteString() {
        sink.write("təˈranəˌsôr".encodeToByteString())
        sink.flush()
        assertEquals(ByteString("74c999cb8872616ec999cb8c73c3b472".decodeHex()), data.readByteString())
    }

    @Test
    fun writeByteStringOffset() {
        sink.write("təˈranəˌsôr".encodeToByteString(), 5, 10)
        sink.flush()
        assertEquals(ByteString("72616ec999".decodeHex()), data.readByteString())
    }
}
