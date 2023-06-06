/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

/*
 * Copyright (C) 2014 Square, Inc.
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

import java.io.InputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kotlin.test.*

private const val SEGMENT_SIZE = Segment.SIZE

class BufferSourceTestJVM : AbstractSourceTestJVM(SourceFactory.BUFFER)
class RealBufferedSourceTestJVM : AbstractSourceTestJVM(SourceFactory.REAL_BUFFERED_SOURCE)
class OneByteAtATimeBufferedSourceTestJVM : AbstractSourceTestJVM(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE)
class OneByteAtATimeBufferTestJVM : AbstractSourceTestJVM(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFER)
class PeekBufferTestJVM : AbstractSourceTestJVM(SourceFactory.PEEK_BUFFER)
class PeekBufferedSourceTestJVM : AbstractSourceTestJVM(SourceFactory.PEEK_BUFFERED_SOURCE)
abstract class AbstractSourceTestJVM(private val factory: SourceFactory) {
    private var sink: Sink
    private var source: Source

    init {
        val pipe: SourceFactory.Pipe = factory.pipe()
        sink = pipe.sink
        source = pipe.source
    }
    @Test
    fun inputStream() {
        sink.writeUtf8("abc")
        sink.emit()
        val input: InputStream = source.inputStream()
        val bytes = byteArrayOf('z'.code.toByte(), 'z'.code.toByte(), 'z'.code.toByte())
        var read: Int = input.read(bytes)
        if (factory.isOneByteAtATime) {
            assertEquals(1, read)
            assertByteArrayEquals("azz", bytes)
            read = input.read(bytes)
            assertEquals(1, read)
            assertByteArrayEquals("bzz", bytes)
            read = input.read(bytes)
            assertEquals(1, read)
            assertByteArrayEquals("czz", bytes)
        } else {
            assertEquals(3, read)
            assertByteArrayEquals("abc", bytes)
        }
        assertEquals(-1, input.read())
    }

    @Test
    fun inputStreamOffsetCount() {
        sink.writeUtf8("abcde")
        sink.emit()
        val input: InputStream = source.inputStream()
        val bytes =
            byteArrayOf('z'.code.toByte(), 'z'.code.toByte(), 'z'.code.toByte(), 'z'.code.toByte(), 'z'.code.toByte())
        val read: Int = input.read(bytes, 1, 3)
        if (factory.isOneByteAtATime) {
            assertEquals(1, read)
            assertByteArrayEquals("zazzz", bytes)
        } else {
            assertEquals(3, read)
            assertByteArrayEquals("zabcz", bytes)
        }
    }

    @Test
    fun inputStreamSkip() {
        sink.writeUtf8("abcde")
        sink.emit()
        val input: InputStream = source.inputStream()
        assertEquals(4, input.skip(4))
        assertEquals('e'.code, input.read())
        sink.writeUtf8("abcde")
        sink.emit()
        assertEquals(5, input.skip(10)) // Try to skip too much.
        assertEquals(0, input.skip(1)) // Try to skip when exhausted.
    }

    @Test
    fun inputStreamCharByChar() {
        sink.writeUtf8("abc")
        sink.emit()
        val input: InputStream = source.inputStream()
        assertEquals('a'.code, input.read())
        assertEquals('b'.code, input.read())
        assertEquals('c'.code, input.read())
        assertEquals(-1, input.read())
    }

    @Test
    fun inputStreamBounds() {
        sink.writeUtf8("a".repeat(100))
        sink.emit()
        val input: InputStream = source.inputStream()
        assertFailsWith<IndexOutOfBoundsException> {
            input.read(ByteArray(100), 50, 51)
        }
    }

    @Test
    fun inputStreamForClosedSource() {
        if (source is kotlinx.io.Buffer) {
            return
        }

        sink.writeByte(0)
        sink.emit()

        val input = source.inputStream()
        source.close()
        assertFailsWith<IOException> { input.read() }
        assertFailsWith<IOException> { input.read(ByteArray(1)) }
        assertFailsWith<IOException> { input.read(ByteArray(10), 0, 1) }
    }

    @Test
    fun inputStreamClosesSource() {
        if (source is kotlinx.io.Buffer) {
            return
        }

        sink.writeByte(0)
        sink.emit()

        val input = source.inputStream()
        input.close()

        assertFailsWith<IllegalStateException> { source.readByte() }
    }

    @Test
    fun inputStreamAvailable() {
        val input = source.inputStream()
        assertEquals(0, input.available())

        sink.writeInt(42)
        sink.emit()
        assertTrue(source.request(4)) // fill the buffer

        assertEquals(4, input.available())

        input.read()
        assertEquals(3, input.available())

        source.readByte()
        assertEquals(2, input.available())

        sink.writeByte(0)
        sink.emit()

        val expectedBytes = if (source is kotlinx.io.Buffer) { 3 } else { 2 }
        assertEquals(expectedBytes, input.available())
    }

    @Test
    fun inputStreamAvailableForClosedSource() {
        if (source is kotlinx.io.Buffer) {
            return
        }

        val input = source.inputStream()
        source.close()

        assertFailsWith<IOException> { input.available() }
    }

    @Test
    fun readNioBuffer() {
        val expected = if (factory.isOneByteAtATime) "a" else "abcdefg"
        sink.writeUtf8("abcdefg")
        sink.emit()
        val nioByteBuffer: ByteBuffer = ByteBuffer.allocate(1024)
        val byteCount: Int = source.read(nioByteBuffer)
        assertEquals(expected.length, byteCount)
        assertEquals(expected.length, nioByteBuffer.position())
        assertEquals(nioByteBuffer.capacity(), nioByteBuffer.limit())
        (nioByteBuffer as Buffer).flip() // Cast necessary for Java 8.
        val data = ByteArray(expected.length)
        nioByteBuffer.get(data)
        assertEquals(expected, String(data))
    }

    /** Note that this test crashes the VM on Android.  */
    @Test
    fun readLargeNioBufferOnlyReadsOneSegment() {
        val expected: String = if (factory.isOneByteAtATime) "a" else "a".repeat(SEGMENT_SIZE)
        sink.writeUtf8("a".repeat(SEGMENT_SIZE * 4))
        sink.emit()
        val nioByteBuffer: ByteBuffer = ByteBuffer.allocate(SEGMENT_SIZE * 3)
        val byteCount: Int = source.read(nioByteBuffer)
        assertEquals(expected.length, byteCount)
        assertEquals(expected.length, nioByteBuffer.position())
        assertEquals(nioByteBuffer.capacity(), nioByteBuffer.limit())
        (nioByteBuffer as Buffer).flip() // Cast necessary for Java 8.
        val data = ByteArray(expected.length)
        nioByteBuffer.get(data)
        assertEquals(expected, String(data))
    }

    @Test
    fun readNioBufferFromEmptySource() {
        assertEquals(-1, source.read(ByteBuffer.allocate(10)))
    }

    @Test
    fun readSpecificCharsetPartial() {
        sink.write(("0000007600000259000002c80000006c000000e40000007300000259000002" +
                "cc000000720000006100000070000000740000025900000072").decodeHex())
        sink.emit()
        assertEquals("vəˈläsə", source.readString(7 * 4, Charset.forName("utf-32")))
    }

    @Test
    fun readSpecificCharset() {
        sink.write(("0000007600000259000002c80000006c000000e40000007300000259000002" +
                "cc000000720000006100000070000000740000025900000072").decodeHex())

        sink.emit()
        assertEquals("vəˈläsəˌraptər", source.readString(Charset.forName("utf-32")))
    }

    @Test
    fun readStringTooShortThrows() {
        sink.writeString("abc", Charsets.US_ASCII)
        sink.emit()
        assertFailsWith<EOFException> {
            source.readString(4, Charsets.US_ASCII)
        }
        assertEquals("abc", source.readUtf8()) // The read shouldn't consume any data.
    }
}