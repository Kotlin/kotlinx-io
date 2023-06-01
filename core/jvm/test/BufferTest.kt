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

import kotlin.test.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.*
import kotlin.text.Charsets.UTF_8

private const val SEGMENT_SIZE = Segment.SIZE

class BufferTest {
    @Test
    @Throws(Exception::class)
    fun copyToSpanningSegments() {
        val source = Buffer()
        source.writeUtf8("a".repeat(SEGMENT_SIZE * 2))
        source.writeUtf8("b".repeat( SEGMENT_SIZE * 2))
        val out = ByteArrayOutputStream()
        source.copyTo(out, 10L, SEGMENT_SIZE * 3L)
        assertEquals("a".repeat( SEGMENT_SIZE * 2 - 10) + "b".repeat( SEGMENT_SIZE + 10),
            out.toString())
        assertEquals("a".repeat(SEGMENT_SIZE * 2) + "b".repeat( SEGMENT_SIZE * 2),
            source.readUtf8(SEGMENT_SIZE * 4L))
    }

    @Test
    @Throws(Exception::class)
    fun copyToStream() {
        val buffer = Buffer().writeUtf8("hello, world!")
        val out = ByteArrayOutputStream()
        buffer.copyTo(out)
        val outString = String(out.toByteArray(), UTF_8)
        assertEquals("hello, world!", outString)
        assertEquals("hello, world!", buffer.readUtf8())
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun writeToSpanningSegments() {
        val buffer = Buffer()
        buffer.writeUtf8("a".repeat(SEGMENT_SIZE * 2))
        buffer.writeUtf8("b".repeat(SEGMENT_SIZE * 2))
        val out = ByteArrayOutputStream()
        buffer.skip(10)
        buffer.writeTo(out, SEGMENT_SIZE * 3L)
        assertEquals("a".repeat(SEGMENT_SIZE * 2 - 10) + "b".repeat(SEGMENT_SIZE + 10), out.toString())
        assertEquals("b".repeat(SEGMENT_SIZE - 10), buffer.readUtf8(buffer.size))
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun writeToStream() {
        val buffer = Buffer().writeUtf8("hello, world!")
        val out = ByteArrayOutputStream()
        buffer.writeTo(out)
        val outString = String(out.toByteArray(), UTF_8)
        assertEquals("hello, world!", outString)
        assertEquals(0, buffer.size)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun readFromStream() {
        val `in`: InputStream = ByteArrayInputStream("hello, world!".toByteArray(UTF_8))
        val buffer = Buffer()
        buffer.readFrom(`in`)
        val out = buffer.readUtf8()
        assertEquals("hello, world!", out)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun readFromSpanningSegments() {
        val `in`: InputStream = ByteArrayInputStream("hello, world!".toByteArray(UTF_8))
        val buffer = Buffer().writeUtf8("a".repeat(SEGMENT_SIZE - 10))
        buffer.readFrom(`in`)
        val out = buffer.readUtf8()
        assertEquals("a".repeat(SEGMENT_SIZE - 10) + "hello, world!", out)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun readFromStreamWithCount() {
        val `in`: InputStream = ByteArrayInputStream("hello, world!".toByteArray(UTF_8))
        val buffer = Buffer()
        buffer.readFrom(`in`, 10)
        val out = buffer.readUtf8()
        assertEquals("hello, wor", out)
    }

    @Test
    @Throws(IOException::class)
    fun readFromDoesNotLeaveEmptyTailSegment() {
        val buffer = Buffer()
        buffer.readFrom(ByteArrayInputStream(ByteArray(SEGMENT_SIZE)))
        assertNoEmptySegments(buffer)
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun bufferInputStreamByteByByte() {
        val source = Buffer()
        source.writeUtf8("abc")
        val `in`: InputStream = source.inputStream()
        assertEquals(3, `in`.available())
        assertEquals('a'.code, `in`.read())
        assertEquals('b'.code, `in`.read())
        assertEquals('c'.code, `in`.read())
        assertEquals(-1, `in`.read())
        assertEquals(0, `in`.available())
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun bufferInputStreamBulkReads() {
        val source = Buffer()
        source.writeUtf8("abc")
        val byteArray = ByteArray(4)
        Arrays.fill(byteArray, (-5).toByte())
        val `in`: InputStream = source.inputStream()
        assertEquals(3, `in`.read(byteArray))
        assertEquals("[97, 98, 99, -5]", byteArray.contentToString())
        Arrays.fill(byteArray, (-7).toByte())
        assertEquals(-1, `in`.read(byteArray))
        assertEquals("[-7, -7, -7, -7]", byteArray.contentToString())
    }

    @Test fun copyToOutputStream() {
        val source = Buffer()
        source.writeUtf8("party")

        val target = Buffer()
        source.copyTo(target.outputStream())
        assertEquals("party", target.readUtf8())
        assertEquals("party", source.readUtf8())
    }

    @Test fun copyToOutputStreamWithOffset() {
        val source = Buffer()
        source.writeUtf8("party")

        val target = Buffer()
        source.copyTo(target.outputStream(), offset = 2)
        assertEquals("rty", target.readUtf8())
        assertEquals("party", source.readUtf8())
    }

    @Test fun copyToOutputStreamWithByteCount() {
        val source = Buffer()
        source.writeUtf8("party")

        val target = Buffer()
        source.copyTo(target.outputStream(), byteCount = 3)
        assertEquals("par", target.readUtf8())
        assertEquals("party", source.readUtf8())
    }

    @Test fun copyToOutputStreamWithOffsetAndByteCount() {
        val source = Buffer()
        source.writeUtf8("party")

        val target = Buffer()
        source.copyTo(target.outputStream(), offset = 1, byteCount = 3)
        assertEquals("art", target.readUtf8())
        assertEquals("party", source.readUtf8())
    }

    @Test fun writeToOutputStream() {
        val source = Buffer()
        source.writeUtf8("party")

        val target = Buffer()
        source.writeTo(target.outputStream())
        assertEquals("party", target.readUtf8())
        assertEquals("", source.readUtf8())
    }

    @Test fun writeToOutputStreamWithByteCount() {
        val source = Buffer()
        source.writeUtf8("party")

        val target = Buffer()
        source.writeTo(target.outputStream(), byteCount = 3)
        assertEquals("par", target.readUtf8())
        assertEquals("ty", source.readUtf8())
    }
}