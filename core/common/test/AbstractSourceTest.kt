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
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import kotlin.test.*

private const val SEGMENT_SIZE = Segment.SIZE

class BufferSourceTest : AbstractBufferedSourceTest(SourceFactory.BUFFER)
class RealBufferedSourceTest : AbstractBufferedSourceTest(SourceFactory.REAL_BUFFERED_SOURCE)
class OneByteAtATimeBufferedSourceTest : AbstractBufferedSourceTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE)
class OneByteAtATimeBufferTest : AbstractBufferedSourceTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFER)
class PeekBufferTest : AbstractBufferedSourceTest(SourceFactory.PEEK_BUFFER)
class PeekBufferedSourceTest : AbstractBufferedSourceTest(SourceFactory.PEEK_BUFFERED_SOURCE)

abstract class AbstractBufferedSourceTest internal constructor(
    private val factory: SourceFactory
) {
    private val sink: Sink
    private val source: Source

    init {
        val pipe = factory.pipe()
        sink = pipe.sink
        source = pipe.source
    }

    @Test
    fun exhausted() {
        assertTrue(source.exhausted())
    }

    @Test
    fun readBytes() {
        sink.write(byteArrayOf(0xab.toByte(), 0xcd.toByte()))
        sink.emit()
        assertEquals(0xab, (source.readByte() and 0xff).toLong())
        assertEquals(0xcd, (source.readByte() and 0xff).toLong())
        assertTrue(source.exhausted())
    }

    @Test
    fun readByteTooShortThrows() {
        assertFailsWith<EOFException> {
            source.readByte()
        }
    }

    @Test
    fun readShort() {
        sink.write(byteArrayOf(0xab.toByte(), 0xcd.toByte(), 0xef.toByte(), 0x01.toByte()))
        sink.emit()
        assertEquals(0xabcd.toShort().toLong(), source.readShort().toLong())
        assertEquals(0xef01.toShort().toLong(), source.readShort().toLong())
        assertTrue(source.exhausted())
    }

    @Test
    fun readShortLe() {
        sink.write(byteArrayOf(0xab.toByte(), 0xcd.toByte(), 0xef.toByte(), 0x10.toByte()))
        sink.emit()
        assertEquals(0xcdab.toShort().toLong(), source.readShortLe().toLong())
        assertEquals(0x10ef.toShort().toLong(), source.readShortLe().toLong())
        assertTrue(source.exhausted())
    }

    @Test
    fun readShortSplitAcrossMultipleSegments() {
        sink.writeString("a".repeat(Segment.SIZE - 1))
        sink.write(byteArrayOf(0xab.toByte(), 0xcd.toByte()))
        sink.emit()
        source.skip((Segment.SIZE - 1).toLong())
        assertEquals(0xabcd.toShort().toLong(), source.readShort().toLong())
        assertTrue(source.exhausted())
    }

    @Test
    fun readShortTooShortThrows() {
        sink.writeShort(Short.MAX_VALUE)
        sink.emit()
        source.skip(1)
        assertFailsWith<EOFException> {
            source.readShort()
        }
        assertEquals(1, source.readByteArray().size)
    }

    @Test
    fun readShortLeTooShortThrows() {
        sink.writeShortLe(Short.MAX_VALUE)
        sink.emit()
        source.skip(1)
        assertFailsWith<EOFException> {
            source.readShortLe()
        }
        assertEquals(1, source.readByteArray().size)
    }

    @Test
    fun readInt() {
        sink.write(
            byteArrayOf(
                0xab.toByte(),
                0xcd.toByte(),
                0xef.toByte(),
                0x01.toByte(),
                0x87.toByte(),
                0x65.toByte(),
                0x43.toByte(),
                0x21.toByte()
            )
        )
        sink.emit()
        assertEquals(-0x543210ff, source.readInt().toLong())
        assertEquals(-0x789abcdf, source.readInt().toLong())
        assertTrue(source.exhausted())
    }

    @Test
    fun readIntLe() {
        sink.write(
            byteArrayOf(
                0xab.toByte(),
                0xcd.toByte(),
                0xef.toByte(),
                0x10.toByte(),
                0x87.toByte(),
                0x65.toByte(),
                0x43.toByte(),
                0x21.toByte()
            )
        )
        sink.emit()
        assertEquals(0x10efcdab, source.readIntLe().toLong())
        assertEquals(0x21436587, source.readIntLe().toLong())
        assertTrue(source.exhausted())
    }

    @Test
    fun readIntSplitAcrossMultipleSegments() {
        sink.writeString("a".repeat(Segment.SIZE - 3))
        sink.write(byteArrayOf(0xab.toByte(), 0xcd.toByte(), 0xef.toByte(), 0x01.toByte()))
        sink.emit()
        source.skip((Segment.SIZE - 3).toLong())
        assertEquals(-0x543210ff, source.readInt().toLong())
        assertTrue(source.exhausted())
    }

    @Test
    fun readIntTooShortThrows() {
        sink.writeInt(Int.MAX_VALUE)
        sink.emit()
        source.skip(1)
        assertFailsWith<EOFException> {
            source.readInt()
        }
        assertEquals(3, source.readByteArray().size)
    }

    @Test
    fun readIntLeTooShortThrows() {
        sink.writeIntLe(Int.MAX_VALUE)
        sink.emit()
        source.skip(1)
        assertFailsWith<EOFException> {
            source.readIntLe()
        }
        assertEquals(3, source.readByteArray().size)
    }

    @Test
    fun readLong() {
        sink.write(
            byteArrayOf(
                0xab.toByte(),
                0xcd.toByte(),
                0xef.toByte(),
                0x10.toByte(),
                0x87.toByte(),
                0x65.toByte(),
                0x43.toByte(),
                0x21.toByte(),
                0x36.toByte(),
                0x47.toByte(),
                0x58.toByte(),
                0x69.toByte(),
                0x12.toByte(),
                0x23.toByte(),
                0x34.toByte(),
                0x45.toByte()
            )
        )
        sink.emit()
        assertEquals(-0x543210ef789abcdfL, source.readLong())
        assertEquals(0x3647586912233445L, source.readLong())
        assertTrue(source.exhausted())
    }

    @Test
    fun readLongLe() {
        sink.write(
            byteArrayOf(
                0xab.toByte(),
                0xcd.toByte(),
                0xef.toByte(),
                0x10.toByte(),
                0x87.toByte(),
                0x65.toByte(),
                0x43.toByte(),
                0x21.toByte(),
                0x36.toByte(),
                0x47.toByte(),
                0x58.toByte(),
                0x69.toByte(),
                0x12.toByte(),
                0x23.toByte(),
                0x34.toByte(),
                0x45.toByte()
            )
        )
        sink.emit()
        assertEquals(0x2143658710efcdabL, source.readLongLe())
        assertEquals(0x4534231269584736L, source.readLongLe())
        assertTrue(source.exhausted())
    }

    @Test
    fun readLongSplitAcrossMultipleSegments() {
        sink.writeString("a".repeat(Segment.SIZE - 7))
        sink.write(
            byteArrayOf(
                0xab.toByte(),
                0xcd.toByte(),
                0xef.toByte(),
                0x01.toByte(),
                0x87.toByte(),
                0x65.toByte(),
                0x43.toByte(),
                0x21.toByte()
            )
        )
        sink.emit()
        source.skip((Segment.SIZE - 7).toLong())
        assertEquals(-0x543210fe789abcdfL, source.readLong())
        assertTrue(source.exhausted())
    }

    @Test
    fun readLongTooShortThrows() {
        sink.writeLong(Long.MAX_VALUE)
        sink.emit()
        source.skip(1)
        assertFailsWith<EOFException> {
            source.readLong()
        }
        assertEquals(7, source.readByteArray().size)
    }

    @Test
    fun readLongLeTooShortThrows() {
        sink.writeLongLe(Long.MAX_VALUE)
        sink.emit()
        source.skip(1)
        assertFailsWith<EOFException> {
            source.readLongLe()
        }
        assertEquals(7, source.readByteArray().size)
    }

    @OptIn(InternalIoApi::class)
    @Test
    fun transferTo() {
        source.buffer.writeString("abc")
        sink.writeString("def")
        sink.emit()

        val sink = Buffer()
        assertEquals(6, source.transferTo(sink))
        assertEquals("abcdef", sink.readString())
        assertTrue(source.exhausted())
    }

    @Test
    fun transferToExhausted() {
        val mockSink = MockSink()
        assertEquals(0, source.transferTo(mockSink))
        assertTrue(source.exhausted())
        mockSink.assertLog()
    }

    @Test
    fun readExhaustedSource() {
        val sink = Buffer()
        sink.writeString("a".repeat(10))
        assertEquals(-1, source.readAtMostTo(sink, 10))
        assertEquals(10, sink.size)
        assertTrue(source.exhausted())
    }

    @Test
    fun readZeroBytesFromSource() {
        val sink = Buffer()
        sink.writeString("a".repeat(10))

        // Either 0 or -1 is reasonable here. For consistency with Android's
        // ByteArrayInputStream we return 0.
        assertEquals(-1, source.readAtMostTo(sink, 0))
        assertEquals(10, sink.size)
        assertTrue(source.exhausted())
    }

    @Test
    fun readNegativeBytesFromSource() {
        assertFailsWith<IllegalArgumentException> {
            source.readAtMostTo(Buffer(), -1L)
        }
    }

    @Test
    fun readFromClosedSource() {
        if (source is Buffer) {
            return
        }

        source.close()
        assertFailsWith<IllegalStateException> {
            source.readAtMostTo(Buffer(), 1L)
        }
    }

    @Test
    fun readAtMostToBufferFromSourceWithFilledBuffer() {
        sink.writeByte(42)
        sink.flush()

        assertTrue(source.request(1))
        assertEquals(1, source.readAtMostTo(Buffer(), 128))
    }

    @Test
    fun readAtMostToNonEmptyBufferFromSourceWithFilledBuffer() {
        if (factory.isOneByteAtATime) {
            return
        }

        val expectedReadSize = 123

        sink.write(ByteArray(expectedReadSize))
        sink.flush()

        assertTrue(source.request(1))
        val buffer = Buffer().also { it.write(ByteArray(SEGMENT_SIZE - expectedReadSize)) }
        assertEquals(expectedReadSize.toLong(), source.readAtMostTo(buffer, SEGMENT_SIZE.toLong()))

        assertTrue(source.exhausted())
        sink.write(ByteArray(expectedReadSize))
        sink.flush()

        assertTrue(source.request(1))
        buffer.clear()
        assertEquals(42L, source.readAtMostTo(buffer, 42L))
    }

    @Test
    fun readAtMostToByteArrayFromSourceWithFilledBuffer() {
        sink.writeByte(42)
        sink.flush()

        assertTrue(source.request(1))
        assertEquals(1, source.readAtMostTo(ByteArray(128)))
    }

    @Test
    fun readToSink() {
        sink.writeString("a".repeat(10000))
        sink.emit()
        val sink = Buffer()
        source.readTo(sink, 9999)
        assertEquals("a".repeat(9999), sink.readString())
        assertEquals("a", source.readString())
    }

    @Test
    fun readToSinkTooShortThrows() {
        sink.writeString("Hi")
        sink.emit()
        val sink = Buffer()
        assertFailsWith<EOFException> {
            source.readTo(sink, 5)
        }

        // Verify we read all that we could from the source.
        assertEquals("Hi", sink.readString())
        assertTrue(source.exhausted())
    }

    @Test
    fun readToSinkWithNegativeByteCount() {
        val sink = Buffer()
        assertFailsWith<IllegalArgumentException> {
            source.readTo(sink, -1)
        }
    }

    @Test
    fun readToSinkZeroBytes() {
        sink.writeString("test")
        sink.flush()
        val sink = Buffer()
        source.readTo(sink, 0)
        assertEquals(0, sink.size)
        assertEquals("test", source.readString())
    }

    @Test
    fun readToByteArray() {
        val data = Buffer()
        data.writeString("Hello")
        data.writeString("e".repeat(Segment.SIZE))

        val expected = data.copy().readByteArray()
        sink.write(data, data.size)
        sink.emit()

        val sink = ByteArray(Segment.SIZE + 5)
        source.readTo(sink)
        assertArrayEquals(expected, sink)
    }

    @Test
    fun readToByteArraySubrange() {
        val buffer = Buffer()
        val source: Source = buffer

        val sink = ByteArray(8)

        buffer.writeString("hello")
        source.readTo(sink, 0, 3)
        assertContentEquals(byteArrayOf('h'.code.toByte(), 'e'.code.toByte(), 'l'.code.toByte(), 0, 0, 0, 0, 0), sink)
        assertEquals("lo", source.readString())

        sink.fill(0)
        buffer.writeString("hello")
        source.readTo(sink, 3)
        assertContentEquals(
            byteArrayOf(
                0, 0, 0, 'h'.code.toByte(), 'e'.code.toByte(), 'l'.code.toByte(), 'l'.code.toByte(),
                'o'.code.toByte()
            ), sink
        )
        assertTrue(source.exhausted())

        sink.fill(0)
        buffer.writeString("hello")
        source.readTo(sink, 3, 4)
        assertContentEquals(byteArrayOf(0, 0, 0, 'h'.code.toByte(), 0, 0, 0, 0), sink)
        assertEquals("ello", source.readString())
    }

    @Test
    fun readToByteArrayInvalidArguments() {
        val source: Source = Buffer()
        val sink = ByteArray(32)

        assertFailsWith<IllegalArgumentException> { source.readTo(sink, 2, 0) }
        assertFailsWith<IndexOutOfBoundsException> { source.readTo(sink, -1) }
        assertFailsWith<IndexOutOfBoundsException> { source.readTo(sink, 33, endIndex = 34) }
        assertFailsWith<IndexOutOfBoundsException> { source.readTo(sink, endIndex = 33) }
    }

    @Test
    fun readToByteArrayTooShortThrows() {
        sink.writeString("Hello")
        sink.emit()

        val array = ByteArray(6)
        assertFailsWith<EOFException> {
            source.readTo(array)
        }

        // Verify we read all that we could from the source.
        assertArrayEquals(
            byteArrayOf(
                'H'.code.toByte(),
                'e'.code.toByte(),
                'l'.code.toByte(),
                'l'.code.toByte(),
                'o'.code.toByte(),
                0
            ),
            array
        )
    }

    @Test
    fun readAtMostToByteArray() {
        sink.writeString("abcd")
        sink.emit()

        val sink = ByteArray(3)
        val read = source.readAtMostTo(sink)
        if (factory.isOneByteAtATime) {
            assertEquals(1, read.toLong())
            val expected = byteArrayOf('a'.code.toByte(), 0, 0)
            assertArrayEquals(expected, sink)
        } else {
            assertEquals(3, read.toLong())
            val expected = byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte())
            assertArrayEquals(expected, sink)
        }
    }

    @Test
    fun readAtMostToByteArrayNotEnough() {
        sink.writeString("abcd")
        sink.emit()

        val sink = ByteArray(5)
        val read = source.readAtMostTo(sink)
        if (factory.isOneByteAtATime) {
            assertEquals(1, read.toLong())
            val expected = byteArrayOf('a'.code.toByte(), 0, 0, 0, 0)
            assertArrayEquals(expected, sink)
        } else {
            assertEquals(4, read.toLong())
            val expected =
                byteArrayOf('a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte(), 'd'.code.toByte(), 0)
            assertArrayEquals(expected, sink)
        }
    }

    @Test
    fun readAtMostToByteArrayOffsetAndCount() {
        sink.writeString("abcd")
        sink.emit()

        val sink = ByteArray(7)
        val bytesToRead = 3
        val read = source.readAtMostTo(sink, startIndex = 2, endIndex = 2 + bytesToRead)
        if (factory.isOneByteAtATime) {
            assertEquals(1, read.toLong())
            val expected = byteArrayOf(0, 0, 'a'.code.toByte(), 0, 0, 0, 0)
            assertArrayEquals(expected, sink)
        } else {
            assertEquals(3, read.toLong())
            val expected =
                byteArrayOf(0, 0, 'a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte(), 0, 0)
            assertArrayEquals(expected, sink)
        }
    }

    @Test
    fun readAtMostToByteArrayFromOffset() {
        sink.writeString("abcd")
        sink.emit()

        val sink = ByteArray(7)
        val read = source.readAtMostTo(sink, 4)
        if (factory.isOneByteAtATime) {
            assertEquals(1, read.toLong())
            val expected = byteArrayOf(0, 0, 0, 0, 'a'.code.toByte(), 0, 0)
            assertArrayEquals(expected, sink)
        } else {
            assertEquals(3, read.toLong())
            val expected =
                byteArrayOf(0, 0, 0, 0, 'a'.code.toByte(), 'b'.code.toByte(), 'c'.code.toByte())
            assertArrayEquals(expected, sink)
        }
    }

    @Test
    fun readAtMostToByteArrayWithInvalidArguments() {
        sink.write(ByteArray(10))
        sink.emit()

        val sink = ByteArray(4)

        assertFailsWith<IllegalArgumentException> {
            source.readAtMostTo(sink, 4, 1)
        }

        assertFailsWith<IndexOutOfBoundsException> {
            source.readAtMostTo(sink, 1, 5)
        }

        assertFailsWith<IndexOutOfBoundsException> {
            source.readAtMostTo(sink, -1, 2)
        }
    }

    @Test
    fun readByteArray() {
        val string = "abcd" + "e".repeat(Segment.SIZE)
        sink.writeString(string)
        sink.emit()
        assertArrayEquals(string.commonAsUtf8ToByteArray(), source.readByteArray())
    }

    @Test
    fun readByteArrayPartial() {
        sink.writeString("abcd")
        sink.emit()
        assertEquals("[97, 98, 99]", source.readByteArray(3).contentToString())
        assertEquals("d", source.readString(1))
    }

    @Test
    fun readByteArrayTooShortThrows() {
        sink.writeString("abc")
        sink.emit()
        assertFailsWith<EOFException> {
            source.readByteArray(4)
        }

        assertEquals("abc", source.readString()) // The read shouldn't consume any data.
    }

    @Test
    fun readByteArrayWithNegativeSizeThrows() {
        assertFailsWith<IllegalArgumentException> { source.readByteArray(-20) }
    }

    @Test
    fun readUtf8SpansSegments() {
        sink.writeString("a".repeat(Segment.SIZE * 2))
        sink.emit()
        source.skip((Segment.SIZE - 1).toLong())
        assertEquals("aa", source.readString(2))
    }

    @Test
    fun readUtf8Segment() {
        sink.writeString("a".repeat(Segment.SIZE))
        sink.emit()
        assertEquals("a".repeat(Segment.SIZE), source.readString(Segment.SIZE.toLong()))
    }

    @Test
    fun readUtf8PartialBuffer() {
        sink.writeString("a".repeat(Segment.SIZE + 20))
        sink.emit()
        assertEquals("a".repeat(Segment.SIZE + 10), source.readString((Segment.SIZE + 10).toLong()))
    }

    @Test
    fun readUtf8EntireBuffer() {
        sink.writeString("a".repeat(Segment.SIZE * 2))
        sink.emit()
        assertEquals("a".repeat(Segment.SIZE * 2), source.readString())
    }

    @Test
    fun readUtf8TooShortThrows() {
        sink.writeString("abc")
        sink.emit()
        assertFailsWith<EOFException> {
            source.readString(4L)
        }

        assertEquals("abc", source.readString()) // The read shouldn't consume any data.
    }

    @Test
    fun skip() {
        sink.writeString("a")
        sink.writeString("b".repeat(Segment.SIZE))
        sink.writeString("c")
        sink.emit()
        source.skip(1)
        assertEquals('b'.code.toLong(), (source.readByte() and 0xff).toLong())
        source.skip((Segment.SIZE - 2).toLong())
        assertEquals('b'.code.toLong(), (source.readByte() and 0xff).toLong())
        source.skip(1)
        assertTrue(source.exhausted())
    }

    @Test
    fun skipInsufficientData() {
        sink.writeString("a")
        sink.emit()
        assertFailsWith<EOFException> {
            source.skip(2)
        }
    }

    @Test
    fun skipNegativeNumberOfBytes() {
        assertFailsWith<IllegalArgumentException> { source.skip(-1) }
    }

    @Test
    fun indexOf() {
        // The segment is empty.
        assertEquals(-1, source.indexOf('a'.code.toByte()))

        // The segment has one value.
        sink.writeString("a") // a
        sink.emit()
        assertEquals(0, source.indexOf('a'.code.toByte()))
        assertEquals(-1, source.indexOf('b'.code.toByte()))

        // The segment has lots of data.
        sink.writeString("b".repeat(Segment.SIZE - 2)) // ab...b
        sink.emit()
        assertEquals(0, source.indexOf('a'.code.toByte()))
        assertEquals(1, source.indexOf('b'.code.toByte()))
        assertEquals(-1, source.indexOf('c'.code.toByte()))

        // The segment doesn't start at 0, it starts at 2.
        source.skip(2) // b...b
        assertEquals(-1, source.indexOf('a'.code.toByte()))
        assertEquals(0, source.indexOf('b'.code.toByte()))
        assertEquals(-1, source.indexOf('c'.code.toByte()))

        // The segment is full.
        sink.writeString("c") // b...bc
        sink.emit()
        assertEquals(-1, source.indexOf('a'.code.toByte()))
        assertEquals(0, source.indexOf('b'.code.toByte()))
        assertEquals((Segment.SIZE - 3).toLong(), source.indexOf('c'.code.toByte()))

        // The segment doesn't start at 2, it starts at 4.
        source.skip(2) // b...bc
        assertEquals(-1, source.indexOf('a'.code.toByte()))
        assertEquals(0, source.indexOf('b'.code.toByte()))
        assertEquals((Segment.SIZE - 5).toLong(), source.indexOf('c'.code.toByte()))

        // Two segments.
        sink.writeString("d") // b...bcd, d is in the 2nd segment.
        sink.emit()
        assertEquals((Segment.SIZE - 4).toLong(), source.indexOf('d'.code.toByte()))
        assertEquals(-1, source.indexOf('e'.code.toByte()))
    }

    @Test
    fun indexOfByteWithStartOffset() {
        with(sink) {
            writeString("a")
            writeString("b".repeat(Segment.SIZE))
            writeString("c")
            emit()
        }
        assertEquals(-1, source.indexOf('a'.code.toByte(), 1))
        assertEquals(15, source.indexOf('b'.code.toByte(), 15))
    }

    @Test
    fun indexOfByteWithIndices() {
        if (factory.isOneByteAtATime) {
            // When run on CI this causes out-of-memory errors.
            return
        }
        val a = 'a'.code.toByte()
        val c = 'c'.code.toByte()

        val size = Segment.SIZE * 5
        val bytes = ByteArray(size) { a }

        // These are tricky places where the buffer
        // starts, ends, or segments come together.
        val points = intArrayOf(
            0,
            1,
            2,
            Segment.SIZE - 1,
            Segment.SIZE,
            Segment.SIZE + 1,
            size / 2 - 1,
            size / 2,
            size / 2 + 1,
            size - Segment.SIZE - 1,
            size - Segment.SIZE,
            size - Segment.SIZE + 1,
            size - 3,
            size - 2,
            size - 1
        )

        // In each iteration, we write c to the known point and then search for it using different
        // windows. Some of the windows don't overlap with c's position, and therefore a match shouldn't
        // be found.
        for (p in points) {
            bytes[p] = c
            sink.write(bytes)
            sink.emit()

            assertEquals(p.toLong(), source.indexOf(c, 0, size.toLong()))
            assertEquals(p.toLong(), source.indexOf(c, 0, (p + 1).toLong()))
            assertEquals(p.toLong(), source.indexOf(c, p.toLong(), size.toLong()))
            assertEquals(p.toLong(), source.indexOf(c, p.toLong(), (p + 1).toLong()))
            assertEquals(p.toLong(), source.indexOf(c, (p / 2).toLong(), (p * 2 + 1).toLong()))
            assertEquals(-1, source.indexOf(c, 0, (p / 2).toLong()))
            assertEquals(-1, source.indexOf(c, 0, p.toLong()))
            assertEquals(-1, source.indexOf(c, 0, 0))
            assertEquals(-1, source.indexOf(c, p.toLong(), p.toLong()))

            // Reset.
            source.transferTo(discardingSink())
            bytes[p] = a
        }
    }

    @Test
    fun indexOfByteInvalidBoundsThrows() {
        sink.writeString("abc")
        sink.emit()
        assertFailsWith<IllegalArgumentException>("Expected failure: fromIndex < 0") {
            source.indexOf('a'.code.toByte(), -1)
        }
        assertFailsWith<IllegalArgumentException>("Expected failure: fromIndex > toIndex") {
            source.indexOf('a'.code.toByte(), 10, 0)
        }
    }

    @Test
    fun indexOfByteWithFromIndex() {
        sink.writeString("aaa")
        sink.emit()
        assertEquals(0, source.indexOf('a'.code.toByte()))
        assertEquals(0, source.indexOf('a'.code.toByte(), 0))
        assertEquals(1, source.indexOf('a'.code.toByte(), 1))
        assertEquals(2, source.indexOf('a'.code.toByte(), 2))
    }

    @Test
    fun request() {
        with(sink) {
            writeString("a")
            writeString("b".repeat(Segment.SIZE))
            writeString("c")
            emit()
        }
        assertTrue(source.request((Segment.SIZE + 2).toLong()))
        assertFalse(source.request((Segment.SIZE + 3).toLong()))
    }

    @Test
    fun requestZeroBytes() {
        assertTrue(source.request(0))
    }

    @Test
    fun requestNegativeNumberOfBytes() {
        assertFailsWith<IllegalArgumentException> { source.request(-1) }
    }

    @Test
    fun require() {
        with(sink) {
            writeString("a")
            writeString("b".repeat(Segment.SIZE))
            writeString("c")
            emit()
        }
        source.require((Segment.SIZE + 2).toLong())
        assertFailsWith<EOFException> {
            source.require((Segment.SIZE + 3).toLong())
        }
    }

    @Test
    fun requireZeroBytes() {
        source.require(0L) // should not throw
    }

    @Test
    fun requireNegativeNumberOfBytes() {
        assertFailsWith<IllegalArgumentException> { source.require(-1) }
    }

    @Test
    fun longHexString() {
        assertLongHexString("8000000000000000", Long.MIN_VALUE)
        assertLongHexString("fffffffffffffffe", -0x2L)
        assertLongHexString("FFFFFFFFFFFFFFFe", -0x2L)
        assertLongHexString("ffffffffffffffff", -0x1L)
        assertLongHexString("FFFFFFFFFFFFFFFF", -0x1L)
        assertLongHexString("0000000000000000", 0x0L)
        assertLongHexString("0000000000000001", 0x1L)
        assertLongHexString("7999999999999999", 0x7999999999999999L)

        assertLongHexString("FF", 0xFF)
        assertLongHexString("0000000000000001", 0x1)
    }

    @Test
    fun hexStringWithManyLeadingZeros() {
        assertLongHexString("00000000000000001", 0x1)
        assertLongHexString("0000000000000000ffffffffffffffff", -0x1L)
        assertLongHexString("00000000000000007fffffffffffffff", 0x7fffffffffffffffL)
        assertLongHexString("0".repeat(Segment.SIZE + 1) + "1", 0x1)
    }

    private fun assertLongHexString(s: String, expected: Long) {
        sink.writeString(s)
        sink.emit()
        val actual = source.readHexadecimalUnsignedLong()
        assertEquals(expected, actual, "$s --> $expected")
    }

    @Test
    fun longHexStringAcrossSegment() {
        with(sink) {
            writeString("a".repeat(Segment.SIZE - 8))
            writeString("FFFFFFFFFFFFFFFF")
            emit()
        }
        source.skip((Segment.SIZE - 8).toLong())
        assertEquals(-1, source.readHexadecimalUnsignedLong())
    }

    @Test
    fun longHexTerminatedByNonDigit() {
        sink.writeString("abcd,")
        sink.emit()
        assertEquals(0xabcdL, source.readHexadecimalUnsignedLong())
    }

    @Test
    fun longHexAlphabet() {
        sink.writeString("7896543210abcdef")
        sink.emit()
        assertEquals(0x7896543210abcdefL, source.readHexadecimalUnsignedLong())
        sink.writeString("ABCDEF")
        sink.emit()
        assertEquals(0xabcdefL, source.readHexadecimalUnsignedLong())
    }

    @Test
    fun longHexStringTooLongThrows() {
        val value = "fffffffffffffffff"
        sink.writeString(value)
        sink.emit()

        val e = assertFailsWith<NumberFormatException> {
            source.readHexadecimalUnsignedLong()
        }
        assertEquals("Number too large: fffffffffffffffff", e.message)
        assertEquals(value, source.readString())
    }

    @Test
    fun longHexStringTooShortThrows() {
        sink.writeString(" ")
        sink.emit()

        val e = assertFailsWith<NumberFormatException> {
            source.readHexadecimalUnsignedLong()
        }
        assertEquals("Expected leading [0-9a-fA-F] character but was 0x20", e.message)
        assertEquals(" ", source.readString())
    }

    @Test
    fun longHexEmptySourceThrows() {
        sink.writeString("")
        sink.emit()
        assertFailsWith<EOFException> { source.readHexadecimalUnsignedLong() }
    }

    @Test
    fun longDecimalString() {
        assertLongDecimalString("-9223372036854775808", Long.MIN_VALUE)
        assertLongDecimalString("-1", -1L)
        assertLongDecimalString("0", 0L)
        assertLongDecimalString("1", 1L)
        assertLongDecimalString("9223372036854775807", Long.MAX_VALUE)

        assertLongDecimalString("00000001", 1L)
        assertLongDecimalString("-000001", -1L)
    }

    private fun assertLongDecimalString(s: String, expected: Long) {
        sink.writeString(s)
        sink.writeString("zzz")
        sink.emit()
        val actual = source.readDecimalLong()
        assertEquals(expected, actual, "$s --> $expected")
        assertEquals("zzz", source.readString())
    }

    @Test
    fun longDecimalStringAcrossSegment() {
        with(sink) {
            writeString("a".repeat(Segment.SIZE - 8))
            writeString("1234567890123456")
            writeString("zzz")
            emit()
        }
        source.skip((Segment.SIZE - 8).toLong())
        assertEquals(1234567890123456L, source.readDecimalLong())
        assertEquals("zzz", source.readString())
    }

    @Test
    fun longDecimalStringTooLongThrows() {
        val value = "12345678901234567890"
        sink.writeString(value) // Too many digits.
        sink.emit()

        val e = assertFailsWith<NumberFormatException> {
            source.readDecimalLong()
        }
        assertEquals("Number too large: 12345678901234567890", e.message)
        assertEquals(value, source.readString())
    }

    @Test
    fun longDecimalStringTooHighThrows() {
        val value = "9223372036854775808"
        sink.writeString(value) // Right size but cannot fit.
        sink.emit()

        val e = assertFailsWith<NumberFormatException> {
            source.readDecimalLong()
        }
        assertEquals("Number too large: 9223372036854775808", e.message)
        assertEquals(value, source.readString())
    }

    @Test
    fun longDecimalStringTooLowThrows() {
        val value = "-9223372036854775809"
        sink.writeString(value) // Right size but cannot fit.
        sink.emit()

        val e = assertFailsWith<NumberFormatException> {
            source.readDecimalLong()
        }
        assertEquals("Number too large: -9223372036854775809", e.message)
        assertEquals(value, source.readString())
    }

    @Test
    fun longDecimalStringTooShortThrows() {
        sink.writeString(" ")
        sink.emit()

        val e = assertFailsWith<NumberFormatException> {
            source.readDecimalLong()
        }
        assertEquals("Expected a digit or '-' but was 0x20", e.message)
        assertEquals(" ", source.readString())
    }

    @Test
    fun longDecimalEmptyThrows() {
        sink.writeString("")
        sink.emit()
        assertFailsWith<EOFException> {
            source.readDecimalLong()
        }
    }

    @Test
    fun longDecimalLoneDashThrows() {
        sink.writeString("-")
        sink.emit()
        assertFailsWith<EOFException> {
            source.readDecimalLong()
        }
        assertEquals("-", source.readString())
    }

    @Test
    fun longDecimalDashFollowedByNonDigitThrows() {
        sink.writeString("- ")
        sink.emit()
        assertFailsWith<NumberFormatException> {
            source.readDecimalLong()
        }
        assertEquals("- ", source.readString())
    }

    @Test
    fun codePoints() {
        with(sink) {
            writeByte(0x7f)
            emit()
            assertEquals(0x7f, source.readCodePointValue().toLong())

            writeByte(0xdf.toByte())
            writeByte(0xbf.toByte())
            emit()
            assertEquals(0x07ff, source.readCodePointValue().toLong())

            writeByte(0xef.toByte())
            writeByte(0xbf.toByte())
            writeByte(0xbf.toByte())
            emit()
            assertEquals(0xffff, source.readCodePointValue().toLong())

            writeByte(0xf4.toByte())
            writeByte(0x8f.toByte())
            writeByte(0xbf.toByte())
            writeByte(0xbf.toByte())
            emit()
            assertEquals(0x10ffff, source.readCodePointValue().toLong())
        }
    }

    @Test
    fun codePointsFromExhaustedSource() {
        with(sink) {
            writeByte(0xdf.toByte()) // a second byte is missing
            emit()
            assertFailsWith<EOFException> { source.readCodePointValue() }
            assertEquals(1, source.readByteArray().size)

            writeByte(0xe2.toByte())
            writeByte(0x98.toByte()) // a third byte is missing
            emit()
            assertFailsWith<EOFException> { source.readCodePointValue() }
            assertEquals(2, source.readByteArray().size)

            writeByte(0xf0.toByte())
            writeByte(0x9f.toByte())
            writeByte(0x92.toByte()) // a forth byte is missing
            emit()
            assertFailsWith<EOFException> { source.readCodePointValue() }
            assertEquals(3, source.readByteArray().size)
        }
    }

    @Test
    fun decimalStringWithManyLeadingZeros() {
        assertLongDecimalString("00000000000000001", 1)
        assertLongDecimalString("00000000000000009223372036854775807", Long.MAX_VALUE)
        assertLongDecimalString("-00000000000000009223372036854775808", Long.MIN_VALUE)
        assertLongDecimalString("0".repeat(Segment.SIZE + 1) + "1", 1)
    }

    @Test
    fun peek() {
        sink.writeString("abcdefghi")
        sink.emit()

        assertEquals("abc", source.readString(3))

        val peek = source.peek()
        assertEquals("def", peek.readString(3))
        assertEquals("ghi", peek.readString(3))
        assertFalse(peek.request(1))

        assertEquals("def", source.readString(3))
    }

    @Test
    fun peekMultiple() {
        sink.writeString("abcdefghi")
        sink.emit()

        assertEquals("abc", source.readString(3))

        val peek1 = source.peek()
        val peek2 = source.peek()

        assertEquals("def", peek1.readString(3))

        assertEquals("def", peek2.readString(3))
        assertEquals("ghi", peek2.readString(3))
        assertFalse(peek2.request(1))

        assertEquals("ghi", peek1.readString(3))
        assertFalse(peek1.request(1))

        assertEquals("def", source.readString(3))
    }

    @Test
    fun peekLarge() {
        if (factory.isOneByteAtATime) {
            // When run on CI this causes out-of-memory errors.
            return
        }
        sink.writeString("abcdef")
        sink.writeString("g".repeat(2 * Segment.SIZE))
        sink.writeString("hij")
        sink.emit()

        assertEquals("abc", source.readString(3))

        val peek = source.peek()
        assertEquals("def", peek.readString(3))
        peek.skip((2 * Segment.SIZE).toLong())
        assertEquals("hij", peek.readString(3))
        assertFalse(peek.request(1))

        assertEquals("def", source.readString(3))
        source.skip((2 * Segment.SIZE).toLong())
        assertEquals("hij", source.readString(3))
    }

    @Test
    fun peekInvalid() {
        sink.writeString("abcdefghi")
        sink.emit()

        assertEquals("abc", source.readString(3))

        val peek = source.peek()
        assertEquals("def", peek.readString(3))
        assertEquals("ghi", peek.readString(3))
        assertFalse(peek.request(1))

        assertEquals("def", source.readString(3))

        val e = assertFailsWith<IllegalStateException> {
            peek.readString()
        }
        assertEquals("Peek source is invalid because upstream source was used", e.message)
    }

    @OptIn(InternalIoApi::class)
    @Test
    fun peekSegmentThenInvalid() {
        sink.writeString("abc")
        sink.writeString("d".repeat(2 * Segment.SIZE))
        sink.emit()

        assertEquals("abc", source.readString(3))

        // Peek a little data and skip the rest of the upstream source
        val peek = source.peek()
        assertEquals("ddd", peek.readString(3))
        source.transferTo(discardingSink())

        // Skip the rest of the buffered data
        peek.skip(peek.buffer.size)

        val e = assertFailsWith<IllegalStateException> {
            peek.readByte()
        }
        assertEquals("Peek source is invalid because upstream source was used", e.message)
    }

    @OptIn(InternalIoApi::class)
    @Test
    fun peekDoesntReadTooMuch() {
        // 6 bytes in source's buffer plus 3 bytes upstream.
        sink.writeString("abcdef")
        sink.emit()
        source.require(6L)
        sink.writeString("ghi")
        sink.emit()

        val peek = source.peek()

        // Read 3 bytes. This reads some of the buffered data.
        assertTrue(peek.request(3))
        if (source !is Buffer) {
            assertEquals(6, source.buffer.size)
            assertEquals(6, peek.buffer.size)
        }
        assertEquals("abc", peek.readString(3L))

        // Read 3 more bytes. This exhausts the buffered data.
        assertTrue(peek.request(3))
        if (source !is Buffer) {
            assertEquals(6, source.buffer.size)
            assertEquals(3, peek.buffer.size)
        }
        assertEquals("def", peek.readString(3L))

        // Read 3 more bytes. This draws new bytes.
        assertTrue(peek.request(3))
        assertEquals(9, source.buffer.size)
        assertEquals(3, peek.buffer.size)
        assertEquals("ghi", peek.readString(3L))
    }

    @OptIn(InternalIoApi::class)
    @Test
    fun factorySegmentSizes() {
        sink.writeString("abc")
        sink.emit()
        source.require(3)
        if (factory.isOneByteAtATime) {
            assertEquals(listOf(1, 1, 1), segmentSizes(source.buffer))
        } else {
            assertEquals(listOf(3), segmentSizes(source.buffer))
        }
    }

    @Test
    fun readUtf8Line() {
        sink.writeString("first line\nsecond line\n")
        sink.flush()
        assertEquals("first line", source.readLine())
        assertEquals("second line\n", source.readString())
        assertEquals(null, source.readLine())

        sink.writeString("\nnext line\n")
        sink.flush()
        assertEquals("", source.readLine())
        assertEquals("next line", source.readLine())

        sink.writeString("There is no newline!")
        sink.flush()
        assertEquals("There is no newline!", source.readLine())

        sink.writeString("Wot do u call it?\r\nWindows")
        sink.flush()
        assertEquals("Wot do u call it?", source.readLine())
        source.transferTo(discardingSink())

        sink.writeString("reo\rde\red\n")
        sink.flush()
        assertEquals("reo\rde\red", source.readLine())
    }

    @Test
    fun readUtf8LineStrict() {
        sink.writeString("first line\nsecond line\n")
        sink.flush()
        assertEquals("first line", source.readLineStrict())
        assertEquals("second line\n", source.readString())
        assertFailsWith<EOFException> { source.readLineStrict() }

        sink.writeString("\nnext line\n")
        sink.flush()
        assertEquals("", source.readLineStrict())
        assertEquals("next line", source.readLineStrict())

        sink.writeString("There is no newline!")
        sink.flush()
        assertFailsWith<EOFException> { source.readLineStrict() }
        assertEquals("There is no newline!", source.readString())

        sink.writeString("Wot do u call it?\r\nWindows")
        sink.flush()
        assertEquals("Wot do u call it?", source.readLineStrict())
        source.transferTo(discardingSink())

        sink.writeString("reo\rde\red\n")
        sink.flush()
        assertEquals("reo\rde\red", source.readLineStrict())

        sink.writeString("line\n")
        sink.flush()
        assertFailsWith<EOFException> { source.readLineStrict(3) }
        assertEquals("line", source.readLineStrict(4))
        assertTrue(source.exhausted())

        sink.writeString("line\r\n")
        sink.flush()
        assertFailsWith<EOFException> { source.readLineStrict(3) }
        assertEquals("line", source.readLineStrict(4))
        assertTrue(source.exhausted())

        sink.writeString("line\n")
        sink.flush()
        assertEquals("line", source.readLineStrict(5))
        assertTrue(source.exhausted())
    }

    @Test
    fun readUnsignedByte() {
        with(sink) {
            writeByte(0)
            writeByte(-1)
            writeByte(-128)
            writeByte(127)
            flush()
        }

        assertEquals(0u, source.readUByte())
        assertEquals(255u, source.readUByte())
        assertEquals(128u, source.readUByte())
        assertEquals(127u, source.readUByte())
        assertTrue(source.exhausted())
    }

    @Test
    fun readTooShortUnsignedByteThrows() {
        assertFailsWith<EOFException> { source.readUByte() }
    }

    @Test
    fun readUnsignedShort() {
        with(sink) {
            writeShort(0)
            writeShort(-1)
            writeShort(-32768)
            writeShort(32767)
            flush()
        }

        assertEquals(0u, source.readUShort())
        assertEquals(65535u, source.readUShort())
        assertEquals(32768u, source.readUShort())
        assertEquals(32767u, source.readUShort())
        assertTrue(source.exhausted())
    }

    @Test
    fun readUnsignedShortLe() {
        sink.write(byteArrayOf(0x12, 0x34))
        sink.flush()
        assertEquals(0x3412u, source.readUShortLe())
    }

    @Test
    fun readTooShortUnsignedShortThrows() {
        assertFailsWith<EOFException> { source.readUShort() }
        sink.writeByte(0)
        sink.flush()
        assertFailsWith<EOFException> { source.readUShort() }
        assertTrue(source.request(1))
    }

    @Test
    fun readTooShortUnsignedShortLeThrows() {
        assertFailsWith<EOFException> { source.readUShortLe() }
        sink.writeByte(0)
        sink.flush()
        assertFailsWith<EOFException> { source.readUShortLe() }
        assertTrue(source.request(1))
    }

    @Test
    fun readUnsignedInt() {
        with(sink) {
            writeInt(0)
            writeInt(-1)
            writeInt(Int.MIN_VALUE)
            writeInt(Int.MAX_VALUE)
            flush()
        }

        assertEquals(0u, source.readUInt())
        assertEquals(UInt.MAX_VALUE, source.readUInt())
        assertEquals(2147483648u, source.readUInt())
        assertEquals(Int.MAX_VALUE.toUInt(), source.readUInt())
        assertTrue(source.exhausted())
    }

    @Test
    fun readUnsignedIntLe() {
        sink.write(byteArrayOf(0x12, 0x34, 0x56, 0x78))
        sink.flush()
        assertEquals(0x78563412u, source.readUIntLe())
    }

    @Test
    fun readFloat() {
        sink.write(byteArrayOf(70, 64, -26, -74))
        sink.flush()
        assertEquals(12345.678F.toBits(), source.readFloat().toBits())
    }

    @Test
    fun readDouble() {
        sink.write(byteArrayOf(64, -2, 36, 12, -97, -56, -13, 35))
        sink.flush()
        assertEquals(123456.78901, source.readDouble())
    }

    @Test
    fun readFloatLe() {
        sink.write(byteArrayOf(-74, -26, 64, 70))
        sink.flush()
        assertEquals(12345.678F.toBits(), source.readFloatLe().toBits())
    }

    @Test
    fun readDoubleLe() {
        sink.write(byteArrayOf(35, -13, -56, -97, 12, 36, -2, 64))
        sink.flush()
        assertEquals(123456.78901, source.readDoubleLe())
    }

    @Test
    fun readTooShortFloatThrows() {
        assertFailsWith<EOFException> { source.readFloat() }
        sink.writeByte(0)
        sink.flush()
        assertFailsWith<EOFException> { source.readFloat() }
        assertTrue(source.request(1))
    }

    @Test
    fun readTooShortDoubleThrows() {
        assertFailsWith<EOFException> { source.readDouble() }
        sink.writeByte(0)
        sink.flush()
        assertFailsWith<EOFException> { source.readDouble() }
        assertTrue(source.request(1))
    }

    @Test
    fun readTooShortFloatLeThrows() {
        assertFailsWith<EOFException> { source.readFloatLe() }
        sink.writeByte(0)
        sink.flush()
        assertFailsWith<EOFException> { source.readFloatLe() }
        assertTrue(source.request(1))
    }

    @Test
    fun readTooShortDoubleLeThrows() {
        assertFailsWith<EOFException> { source.readDoubleLe() }
        sink.writeByte(0)
        sink.flush()
        assertFailsWith<EOFException> { source.readDoubleLe() }
        assertTrue(source.request(1))
    }

    @Test
    fun readTooShortUnsignedIntThrows() {
        assertFailsWith<EOFException> { source.readUInt() }
        sink.writeByte(0)
        sink.flush()
        assertFailsWith<EOFException> { source.readUInt() }
        sink.writeByte(0)
        sink.flush()
        assertFailsWith<EOFException> { source.readUInt() }
        sink.writeByte(0)
        sink.flush()
        assertFailsWith<EOFException> { source.readUInt() }
        assertTrue(source.request(3))
    }

    @Test
    fun readTooShortUnsignedIntLeThrows() {
        assertFailsWith<EOFException> { source.readUIntLe() }
        sink.writeByte(0)
        sink.flush()
        assertFailsWith<EOFException> { source.readUIntLe() }
        sink.writeByte(0)
        sink.flush()
        assertFailsWith<EOFException> { source.readUIntLe() }
        sink.writeByte(0)
        sink.flush()
        assertFailsWith<EOFException> { source.readUIntLe() }
        assertTrue(source.request(3))
    }

    @Test
    fun readUnsignedLong() {
        with(sink) {
            writeLong(0)
            writeLong(-1)
            writeLong(Long.MIN_VALUE)
            writeLong(Long.MAX_VALUE)
            flush()
        }

        assertEquals(0u, source.readULong())
        assertEquals(ULong.MAX_VALUE, source.readULong())
        assertEquals(9223372036854775808u, source.readULong())
        assertEquals(Long.MAX_VALUE.toULong(), source.readULong())
        assertTrue(source.exhausted())
    }

    @Test
    fun readUnsignedLongLe() {
        sink.write(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0xff.toByte()))
        sink.flush()
        assertEquals(0xff07060504030201u, source.readULongLe())
    }

    @Test
    fun readTooShortUnsignedLongThrows() {
        assertFailsWith<EOFException> { source.readULong() }
        for (i in 0 until 7) {
            sink.writeByte(0)
            sink.flush()
            assertFailsWith<EOFException> { source.readULong() }
        }
        assertTrue(source.request(7))
    }

    @Test
    fun readTooShortUnsignedLongLeThrows() {
        assertFailsWith<EOFException> { source.readULongLe() }
        for (i in 0 until 7) {
            sink.writeByte(0)
            sink.flush()
            assertFailsWith<EOFException> { source.readULongLe() }
        }
        assertTrue(source.request(7))
    }

    @Test
    fun readByteString() {
        with(sink) {
            writeString("abcd")
            writeString("e".repeat(Segment.SIZE))
            emit()
        }
        assertEquals("abcd" + "e".repeat(Segment.SIZE), source.readByteString().decodeToString())
    }

    @Test
    fun readByteStringPartial() {
        with(sink) {
            writeString("abcd")
            writeString("e".repeat(Segment.SIZE))
            emit()
        }
        assertEquals("abc", source.readByteString(3).decodeToString())
        assertEquals("d", source.readString(1))
    }

    @Test
    fun readByteStringTooShortThrows() {
        sink.writeString("abc")
        sink.emit()
        assertFailsWith<EOFException> { source.readByteString(4) }

        assertEquals("abc", source.readString()) // The read shouldn't consume any data.
    }

    @Test
    fun indexOfByteString() {
        assertEquals(-1, source.indexOf("flop".encodeToByteString()))

        sink.writeString("flip flop")
        sink.emit()
        assertEquals(5, source.indexOf("flop".encodeToByteString()))
        source.transferTo(discardingSink()) // Clear stream.

        // Make sure we backtrack and resume searching after partial match.
        sink.writeString("hi hi hi hey")
        sink.emit()
        assertEquals(3, source.indexOf("hi hi hey".encodeToByteString()))
    }

    @Test
    fun indexOfByteStringAtSegmentBoundary() {
        sink.writeString("a".repeat(Segment.SIZE - 1))
        sink.writeString("bcd")
        sink.emit()
        assertEquals(
            (Segment.SIZE - 3).toLong(),
            source.indexOf("aabc".encodeToByteString(), (Segment.SIZE - 4).toLong()),
        )
        assertEquals(
            (Segment.SIZE - 3).toLong(),
            source.indexOf("aabc".encodeToByteString(), (Segment.SIZE - 3).toLong()),
        )
        assertEquals(
            (Segment.SIZE - 2).toLong(),
            source.indexOf("abcd".encodeToByteString(), (Segment.SIZE - 2).toLong()),
        )
        assertEquals(
            (Segment.SIZE - 2).toLong(),
            source.indexOf("abc".encodeToByteString(), (Segment.SIZE - 2).toLong()),
        )
        assertEquals(
            (Segment.SIZE - 2).toLong(),
            source.indexOf("abc".encodeToByteString(), (Segment.SIZE - 2).toLong()),
        )
        assertEquals(
            (Segment.SIZE - 2).toLong(),
            source.indexOf("ab".encodeToByteString(), (Segment.SIZE - 2).toLong()),
        )
        assertEquals(
            (Segment.SIZE - 2).toLong(),
            source.indexOf("a".encodeToByteString(), (Segment.SIZE - 2).toLong()),
        )
        assertEquals(
            (Segment.SIZE - 1).toLong(),
            source.indexOf("bc".encodeToByteString(), (Segment.SIZE - 2).toLong()),
        )
        assertEquals(
            (Segment.SIZE - 1).toLong(),
            source.indexOf("b".encodeToByteString(), (Segment.SIZE - 2).toLong()),
        )
        assertEquals(
            Segment.SIZE.toLong(),
            source.indexOf("c".encodeToByteString(), (Segment.SIZE - 2).toLong()),
        )
        assertEquals(
            Segment.SIZE.toLong(),
            source.indexOf("c".encodeToByteString(), Segment.SIZE.toLong()),
        )
        assertEquals(
            (Segment.SIZE + 1).toLong(),
            source.indexOf("d".encodeToByteString(), (Segment.SIZE - 2).toLong()),
        )
        assertEquals(
            (Segment.SIZE + 1).toLong(),
            source.indexOf("d".encodeToByteString(), (Segment.SIZE + 1).toLong()),
        )
    }

    @Test
    fun indexOfDoesNotWrapAround() {
        sink.writeString("a".repeat(Segment.SIZE - 1))
        sink.writeString("bcd")
        sink.emit()
        assertEquals(-1, source.indexOf("abcda".encodeToByteString(), (Segment.SIZE - 3).toLong()))
    }

    @Test
    fun indexOfByteStringWithOffset() {
        assertEquals(-1, source.indexOf("flop".encodeToByteString(), 1))

        sink.writeString("flop flip flop")
        sink.emit()
        assertEquals(10, source.indexOf("flop".encodeToByteString(), 1))
        assertEquals(0, source.indexOf("flop".encodeToByteString(), -1))
        source.transferTo(discardingSink()) // Clear stream

        // Make sure we backtrack and resume searching after the partial match.
        sink.writeString("hi hi hi hi hey")
        sink.emit()
        assertEquals(6, source.indexOf("hi hi hey".encodeToByteString(), 1))

        assertEquals(-1, source.indexOf("ho ho ho".encodeToByteString(), 9001))
    }

    @Test
    fun indexOfEmptyByteString() {
        assertEquals(0, source.indexOf(ByteString()))

        sink.writeString("blablabla")
        sink.emit()
        assertEquals(0, source.indexOf(ByteString()))
        assertEquals(0, source.indexOf(ByteString(), -1))
        assertEquals(9, source.indexOf(ByteString(), 100000))
    }

    /**
     * With [BufferedSourceFactory.ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE], this code was extremely slow.
     * https://github.com/square/okio/issues/171
     */
    @Test
    fun indexOfByteStringAcrossSegmentBoundaries() {
        sink.writeString("a".repeat(Segment.SIZE * 2 - 3))
        sink.writeString("bcdefg")
        sink.emit()
        assertEquals((Segment.SIZE * 2 - 4).toLong(), source.indexOf("ab".encodeToByteString()))
        assertEquals((Segment.SIZE * 2 - 4).toLong(), source.indexOf("abc".encodeToByteString()))
        assertEquals((Segment.SIZE * 2 - 4).toLong(), source.indexOf("abcd".encodeToByteString()))
        assertEquals((Segment.SIZE * 2 - 4).toLong(), source.indexOf("abcde".encodeToByteString()))
        assertEquals((Segment.SIZE * 2 - 4).toLong(), source.indexOf("abcdef".encodeToByteString()))
        assertEquals((Segment.SIZE * 2 - 4).toLong(), source.indexOf("abcdefg".encodeToByteString()))
        assertEquals((Segment.SIZE * 2 - 3).toLong(), source.indexOf("bcdefg".encodeToByteString()))
        assertEquals((Segment.SIZE * 2 - 2).toLong(), source.indexOf("cdefg".encodeToByteString()))
        assertEquals((Segment.SIZE * 2 - 1).toLong(), source.indexOf("defg".encodeToByteString()))
        assertEquals((Segment.SIZE * 2).toLong(), source.indexOf("efg".encodeToByteString()))
        assertEquals((Segment.SIZE * 2 + 1).toLong(), source.indexOf("fg".encodeToByteString()))
        assertEquals((Segment.SIZE * 2 + 2).toLong(), source.indexOf("g".encodeToByteString()))
    }

    @Test
    fun indexOfByteStringSpanningAcrossMultipleSegments() {
        sink.writeString("a".repeat(SEGMENT_SIZE))
        sink.emit()
        sink.writeString("bbbb")
        sink.emit()
        sink.write(Buffer().also { it.writeString("c".repeat(SEGMENT_SIZE)) }, SEGMENT_SIZE.toLong())
        sink.emit()

        source.skip(SEGMENT_SIZE - 10L)
        assertEquals(9, source.indexOf("abbbbc".encodeToByteString()))
    }
}
