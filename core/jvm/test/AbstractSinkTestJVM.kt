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

import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kotlin.test.*
import kotlin.text.Charsets.UTF_8

private const val SEGMENT_SIZE = Segment.SIZE

class BufferSinkTestJVM : AbstractSinkTestJVM(SinkFactory.BUFFER)
class RealSinkTestJVM : AbstractSinkTestJVM(SinkFactory.REAL_BUFFERED_SINK)

abstract class AbstractSinkTestJVM internal constructor(factory: SinkFactory) {
    private val data: Buffer = Buffer()
    private val sink: Sink = factory.create(data)

    @Test
    @Throws(Exception::class)
    fun outputStream() {
        val out: OutputStream = sink.outputStream()
        out.write('a'.code)
        out.write("b".repeat(9998).toByteArray(UTF_8))
        out.write('c'.code)
        out.flush()
        assertEquals(("a" + "b".repeat(9998)) + "c", data.readUtf8())
    }

    @Test
    @Throws(Exception::class)
    fun outputStreamBounds() {
        val out: OutputStream = sink.outputStream()
        assertFailsWith<IndexOutOfBoundsException> {
            out.write(ByteArray(100), 50, 51)
        }
    }

    @Test
    fun writeToClosedOutputStream() {
        if (sink is Buffer) {
            return
        }
        val out = sink.outputStream()
        sink.close()
        assertFailsWith<IOException> { out.write(0) }
        assertFailsWith<IOException> { out.write(ByteArray(1)) }
        assertFailsWith<IOException> { out.write(ByteArray(42), 0, 1) }
    }

    @Test
    fun outputStreamClosesSink() {
        if (sink is Buffer) {
            return
        }

        val out = sink.outputStream()
        out.close()
        assertFailsWith<IllegalStateException> { sink.writeByte(0) }
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun writeNioBuffer() {
        val expected = "abcdefg"
        val nioByteBuffer: ByteBuffer = ByteBuffer.allocate(1024)
        nioByteBuffer.put("abcdefg".toByteArray(UTF_8))
        nioByteBuffer.flip() // Cast necessary for Java 8.
        val byteCount: Int = sink.write(nioByteBuffer)
        assertEquals(expected.length, byteCount)
        assertEquals(expected.length, nioByteBuffer.position())
        assertEquals(expected.length, nioByteBuffer.limit())
        sink.flush()
        assertEquals(expected, data.readUtf8())
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun writeLargeNioBufferWritesAllData() {
        val expected: String = "a".repeat(SEGMENT_SIZE * 3)
        val nioByteBuffer: ByteBuffer = ByteBuffer.allocate(SEGMENT_SIZE * 4)
        nioByteBuffer.put("a".repeat(SEGMENT_SIZE * 3).toByteArray(UTF_8))
        nioByteBuffer.flip() // Cast necessary for Java 8.
        val byteCount: Int = sink.write(nioByteBuffer)
        assertEquals(expected.length, byteCount)
        assertEquals(expected.length, nioByteBuffer.position())
        assertEquals(expected.length, nioByteBuffer.limit())
        sink.flush()
        assertEquals(expected, data.readUtf8())
    }

    @Test
    fun writeNioBufferToClosedSink() {
        if (sink is Buffer) {
            return
        }
        sink.close()
        assertFailsWith<IllegalStateException> {
            sink.write(ByteBuffer.allocate(10))
        }
    }

    @Test
    @Throws(IOException::class)
    fun writeStringWithCharset() {
        sink.writeString("təˈranəˌsôr", Charset.forName("utf-32be"))
        sink.flush()
        val expected = "0000007400000259000002c800000072000000610000006e00000259000002cc00000073000000f400000072"
        assertArrayEquals(expected.decodeHex(), data.readByteArray())
    }

    @Test
    @Throws(IOException::class)
    fun writeSubstringWithCharset() {
        sink.writeString("təˈranəˌsôr", Charset.forName("utf-32be"), 3, 7)
        sink.flush()
        assertArrayEquals("00000072000000610000006e00000259".decodeHex(), data.readByteArray())
    }

    @Test
    @Throws(IOException::class)
    fun writeUtf8SubstringWithCharset() {
        sink.writeString("təˈranəˌsôr", Charset.forName("utf-8"), 3, 7)
        sink.flush()
        assertArrayEquals("ranə".toByteArray(UTF_8), data.readByteArray())
    }
}