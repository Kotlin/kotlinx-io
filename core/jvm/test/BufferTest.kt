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

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.text.Charsets.UTF_8

private const val SEGMENT_SIZE = Segment.SIZE

class BufferTest {
    @Test
    fun copyToSpanningSegments() {
        val source = Buffer()
        source.writeUtf8("a".repeat(SEGMENT_SIZE * 2))
        source.writeUtf8("b".repeat(SEGMENT_SIZE * 2))
        val out = ByteArrayOutputStream()
        source.copyTo(out, startIndex = 10L, endIndex = 10L + SEGMENT_SIZE * 3L)
        assertEquals(
            "a".repeat(SEGMENT_SIZE * 2 - 10) + "b".repeat(SEGMENT_SIZE + 10),
            out.toString()
        )
        assertEquals(
            "a".repeat(SEGMENT_SIZE * 2) + "b".repeat(SEGMENT_SIZE * 2),
            source.readUtf8(SEGMENT_SIZE * 4L)
        )
    }

    @Test
    fun copyToSkippingSegments() {
        val source = Buffer()
        source.writeUtf8("a".repeat(SEGMENT_SIZE * 2))
        source.writeUtf8("b".repeat(SEGMENT_SIZE * 2))
        val out = ByteArrayOutputStream()
        source.copyTo(out, startIndex = SEGMENT_SIZE * 2 + 1L, endIndex = SEGMENT_SIZE * 2 + 4L)
        assertEquals("bbb", out.toString())
        assertEquals(
            "a".repeat(SEGMENT_SIZE * 2) + "b".repeat(SEGMENT_SIZE * 2),
            source.readUtf8(SEGMENT_SIZE * 4L)
        )
    }

    @Test
    fun copyToStream() {
        val buffer = Buffer().also { it.writeUtf8("hello, world!") }
        val out = ByteArrayOutputStream()
        buffer.copyTo(out)
        val outString = String(out.toByteArray(), UTF_8)
        assertEquals("hello, world!", outString)
        assertEquals("hello, world!", buffer.readUtf8())
    }

    @Test
    fun writeToSpanningSegments() {
        val buffer = Buffer()
        buffer.writeUtf8("a".repeat(SEGMENT_SIZE * 2))
        buffer.writeUtf8("b".repeat(SEGMENT_SIZE * 2))
        val out = ByteArrayOutputStream()
        buffer.skip(10)
        buffer.readTo(out, SEGMENT_SIZE * 3L)
        assertEquals("a".repeat(SEGMENT_SIZE * 2 - 10) + "b".repeat(SEGMENT_SIZE + 10), out.toString())
        assertEquals("b".repeat(SEGMENT_SIZE - 10), buffer.readUtf8(buffer.size))
    }

    @Test
    fun writeToStream() {
        val buffer = Buffer().also { it.writeUtf8("hello, world!") }
        val out = ByteArrayOutputStream()
        buffer.readTo(out)
        val outString = String(out.toByteArray(), UTF_8)
        assertEquals("hello, world!", outString)
        assertEquals(0, buffer.size)
    }

    @Test
    fun readFromStream() {
        val input: InputStream = ByteArrayInputStream("hello, world!".toByteArray(UTF_8))
        val buffer = Buffer()
        buffer.transferFrom(input)
        val out = buffer.readUtf8()
        assertEquals("hello, world!", out)
    }

    @Test
    fun readFromSpanningSegments() {
        val input: InputStream = ByteArrayInputStream("hello, world!".toByteArray(UTF_8))
        val buffer = Buffer().also { it.writeUtf8("a".repeat(SEGMENT_SIZE - 10)) }
        buffer.transferFrom(input)
        val out = buffer.readUtf8()
        assertEquals("a".repeat(SEGMENT_SIZE - 10) + "hello, world!", out)
    }

    @Test
    fun readFromStreamWithCount() {
        val input: InputStream = ByteArrayInputStream("hello, world!".toByteArray(UTF_8))
        val buffer = Buffer()
        buffer.write(input, 10)
        val out = buffer.readUtf8()
        assertEquals("hello, wor", out)
    }

    @Test
    fun readFromStreamThrowsEOFOnExhaustion() {
        val input = ByteArrayInputStream("hello, world!".toByteArray(UTF_8))
        val buffer = Buffer()
        assertFailsWith<EOFException> {
            buffer.write(input, input.available() + 1L)
        }
    }

    @Test
    fun readFromStreamWithNegativeBytesCount() {
        assertFailsWith<IllegalArgumentException> {
            Buffer().write(ByteArrayInputStream(ByteArray(1)), -1)
        }
    }

    @Test
    fun readFromDoesNotLeaveEmptyTailSegment() {
        val buffer = Buffer()
        buffer.transferFrom(ByteArrayInputStream(ByteArray(SEGMENT_SIZE)))
        assertNoEmptySegments(buffer)
    }

    @Test
    fun bufferInputStreamByteByByte() {
        val source = Buffer()
        source.writeUtf8("abc")
        val input: InputStream = source.asInputStream()
        assertEquals(3, input.available())
        assertEquals('a'.code, input.read())
        assertEquals('b'.code, input.read())
        assertEquals('c'.code, input.read())
        assertEquals(-1, input.read())
        assertEquals(0, input.available())
    }

    @Test
    fun bufferInputStreamBulkReads() {
        val source = Buffer()
        source.writeUtf8("abc")
        val byteArray = ByteArray(4)
        Arrays.fill(byteArray, (-5).toByte())
        val input: InputStream = source.asInputStream()
        assertEquals(3, input.read(byteArray))
        assertEquals("[97, 98, 99, -5]", byteArray.contentToString())
        Arrays.fill(byteArray, (-7).toByte())
        assertEquals(-1, input.read(byteArray))
        assertEquals("[-7, -7, -7, -7]", byteArray.contentToString())
    }

    @Test
    fun copyToOutputStream() {
        val source = Buffer()
        source.writeUtf8("party")

        val target = Buffer()
        source.copyTo(target.asOutputStream())
        assertEquals("party", target.readUtf8())
        assertEquals("party", source.readUtf8())
    }

    @Test
    fun copyToOutputStreamWithStartIndex() {
        val source = Buffer()
        source.writeUtf8("party")

        val target = Buffer()
        source.copyTo(target.asOutputStream(), startIndex = 2)
        assertEquals("rty", target.readUtf8())
        assertEquals("party", source.readUtf8())
    }

    @Test
    fun copyToOutputStreamWithEndIndex() {
        val source = Buffer()
        source.writeUtf8("party")

        val target = Buffer()
        source.copyTo(target.asOutputStream(), endIndex = 3)
        assertEquals("par", target.readUtf8())
        assertEquals("party", source.readUtf8())
    }

    @Test
    fun copyToOutputStreamWithIndices() {
        val source = Buffer()
        source.writeUtf8("party")

        val target = Buffer()
        source.copyTo(target.asOutputStream(), startIndex = 1, endIndex = 4)
        assertEquals("art", target.readUtf8())
        assertEquals("party", source.readUtf8())
    }

    @Test
    fun copyToOutputStreamWithEmptyRange() {
        val source = Buffer()
        source.writeUtf8("hello")

        val target = Buffer()
        source.copyTo(target.asOutputStream(), startIndex = 1, endIndex = 1)
        assertEquals("hello", source.readUtf8())
        assertEquals("", target.readUtf8())
    }

    @Test
    fun readToOutputStream() {
        val source = Buffer()
        source.writeUtf8("party")

        val target = Buffer()
        source.readTo(target.asOutputStream())
        assertEquals("party", target.readUtf8())
        assertEquals("", source.readUtf8())
    }

    @Test
    fun readToOutputStreamWithByteCount() {
        val source = Buffer()
        source.writeUtf8("party")

        val target = Buffer()
        source.readTo(target.asOutputStream(), byteCount = 3)
        assertEquals("par", target.readUtf8())
        assertEquals("ty", source.readUtf8())
    }

    @Test
    fun readEmptyBufferToByteBuffer() {
        val bb = ByteBuffer.allocate(128)
        val buffer = Buffer()

        assertEquals(-1, buffer.readAtMostTo(bb))
    }
}
