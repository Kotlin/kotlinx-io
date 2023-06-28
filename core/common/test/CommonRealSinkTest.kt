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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests solely for the behavior of RealBufferedSink's implementation. For generic
 * BufferedSink behavior use BufferedSinkTest.
 */
@OptIn(InternalIoApi::class)
class CommonRealSinkTest {
    @Test
    fun bufferedSinkEmitsTailWhenItIsComplete() {
        val sink = Buffer()
        val bufferedSink = (sink as RawSink).buffered()
        bufferedSink.writeString("a".repeat(Segment.SIZE - 1))
        assertEquals(0, sink.size)
        bufferedSink.writeByte(0)
        assertEquals(Segment.SIZE.toLong(), sink.size)
        assertEquals(0, bufferedSink.buffer.size)
    }

    @Test
    fun bufferedSinkEmitMultipleSegments() {
        val sink = Buffer()
        val bufferedSink = (sink as RawSink).buffered()
        bufferedSink.writeString("a".repeat(Segment.SIZE * 4 - 1))
        assertEquals(Segment.SIZE.toLong() * 3L, sink.size)
        assertEquals(Segment.SIZE.toLong() - 1L, bufferedSink.buffer.size)
    }

    @Test
    fun bufferedSinkFlush() {
        val sink = Buffer()
        val bufferedSink = (sink as RawSink).buffered()
        bufferedSink.writeByte('a'.code.toByte())
        assertEquals(0, sink.size)
        bufferedSink.flush()
        assertEquals(0, bufferedSink.buffer.size)
        assertEquals(1, sink.size)
    }

    @Test
    fun bytesEmittedToSinkWithFlush() {
        val sink = Buffer()
        val bufferedSink = (sink as RawSink).buffered()
        bufferedSink.writeString("abc")
        bufferedSink.flush()
        assertEquals(3, sink.size)
    }

    @Test
    fun bytesNotEmittedToSinkWithoutFlush() {
        val sink = Buffer()
        val bufferedSink = (sink as RawSink).buffered()
        bufferedSink.writeString("abc")
        assertEquals(0, sink.size)
    }

    @Test
    fun bytesEmittedToSinkWithEmit() {
        val sink = Buffer()
        val bufferedSink = (sink as RawSink).buffered()
        bufferedSink.writeString("abc")
        bufferedSink.emit()
        assertEquals(3, sink.size)
    }

    @Test
    fun completeSegmentsEmitted() {
        val sink = Buffer()
        val bufferedSink = (sink as RawSink).buffered()
        bufferedSink.writeString("a".repeat(Segment.SIZE * 3))
        assertEquals(Segment.SIZE.toLong() * 3L, sink.size)
    }

    @Test
    fun incompleteSegmentsNotEmitted() {
        val sink = Buffer()
        val bufferedSink = (sink as RawSink).buffered()
        bufferedSink.writeString("a".repeat(Segment.SIZE * 3 - 1))
        assertEquals(Segment.SIZE.toLong() * 2L, sink.size)
    }

    @Test
    fun closeWithExceptionWhenWriting() {
        val mockSink = MockSink()
        mockSink.scheduleThrow(0, IOException())
        val bufferedSink = mockSink.buffered()
        bufferedSink.writeByte('a'.code.toByte())
        assertFailsWith<IOException> {
            bufferedSink.close()
        }

        mockSink.assertLog("write(Buffer(size=1 hex=61), 1)", "close()")
    }

    @Test
    fun closeWithExceptionWhenClosing() {
        val mockSink = MockSink()
        mockSink.scheduleThrow(1, IOException())
        val bufferedSink = mockSink.buffered()
        bufferedSink.writeByte('a'.code.toByte())
        assertFailsWith<IOException> {
            bufferedSink.close()
        }

        mockSink.assertLog("write(Buffer(size=1 hex=61), 1)", "close()")
    }

    @Test
    fun closeWithExceptionWhenWritingAndClosing() {
        val mockSink = MockSink()
        mockSink.scheduleThrow(0, IOException("first"))
        mockSink.scheduleThrow(1, IOException("second"))
        val bufferedSink = mockSink.buffered()
        bufferedSink.writeByte('a'.code.toByte())
        assertFailsWith<IOException>("first.*") {
            bufferedSink.close()
        }

        mockSink.assertLog("write(Buffer(size=1 hex=61), 1)", "close()")
    }

    @Test
    fun operationsAfterClose() {
        val mockSink = MockSink()
        val bufferedSink = mockSink.buffered()
        bufferedSink.writeByte('a'.code.toByte())
        bufferedSink.close()

        // Test a sample set of methods.
        assertFailsWith<IllegalStateException> { bufferedSink.writeByte('a'.code.toByte()) }
        assertFailsWith<IllegalStateException> { bufferedSink.write(ByteArray(10)) }
        assertFailsWith<IllegalStateException> { bufferedSink.hintEmit() }
        assertFailsWith<IllegalStateException> { bufferedSink.emit() }
        assertFailsWith<IllegalStateException> { bufferedSink.flush() }
    }

    @Test
    fun writeAll() {
        val mockSink = MockSink()
        val bufferedSink = mockSink.buffered()

        bufferedSink.buffer.writeString("abc")
        assertEquals(3, bufferedSink.transferFrom(Buffer().also { it.writeString("def") }))

        assertEquals(6, bufferedSink.buffer.size)
        assertEquals("abcdef", bufferedSink.buffer.readString(6))
        mockSink.assertLog() // No writes.
    }

    @Test
    fun writeAllExhausted() {
        val mockSink = MockSink()
        val bufferedSink = mockSink.buffered()

        assertEquals(0, bufferedSink.transferFrom(Buffer()))
        assertEquals(0, bufferedSink.buffer.size)
        mockSink.assertLog() // No writes.
    }

    @Test
    fun writeAllWritesOneSegmentAtATime() {
        val write1 = Buffer().also { it.writeString("a".repeat(Segment.SIZE)) }
        val write2 = Buffer().also { it.writeString("b".repeat(Segment.SIZE)) }
        val write3 = Buffer().also { it.writeString("c".repeat(Segment.SIZE)) }

        val source = Buffer()
        source.writeString(
            "${"a".repeat(Segment.SIZE)}${"b".repeat(Segment.SIZE)}${"c".repeat(Segment.SIZE)}"
        )

        val mockSink = MockSink()
        val bufferedSink = mockSink.buffered()
        assertEquals(Segment.SIZE.toLong() * 3L, bufferedSink.transferFrom(source))

        mockSink.assertLog(
            "write($write1, ${write1.size})",
            "write($write2, ${write2.size})",
            "write($write3, ${write3.size})"
        )
    }

    @Test
    fun closeMultipleTimes() {
        var closeCalls = 0
        val rawSink: RawSink = object : RawSink {
            override fun write(source: Buffer, byteCount: Long) = Unit
            override fun flush() = Unit
            override fun close() {
                closeCalls++
            }
        }
        val sink = rawSink.buffered()

        sink.close()
        assertFailsWith<IllegalStateException> { sink.writeByte(0) }
        sink.close() // should do nothing
        assertEquals(1, closeCalls)
    }
}
