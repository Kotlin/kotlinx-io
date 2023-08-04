/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class StateCapturingSink : AsyncRawSink {
    public var closed = false
    public var flushed = false
    public var bytesWritten: Long = 0

    override suspend fun write(source: Buffer, byteCount: Long) {
        source.skip(byteCount)
        bytesWritten += byteCount
    }

    override suspend fun flush() {
        flushed = true
    }

    override suspend fun close() {
        closed = true
    }

}

class AsyncSinkTest {
    @Test
    fun testClose() = runTest{
        val rawSink = StateCapturingSink()
        val sink = AsyncSink(rawSink)

        sink.buffer.write(ByteArray(42))
        assertEquals(0, rawSink.bytesWritten)

        sink.close()
        assertTrue(rawSink.closed)
        assertTrue(rawSink.flushed)
        assertEquals(42, rawSink.bytesWritten)
    }

    @Test
    fun testFlush() = runTest{
        val rawSink = StateCapturingSink()
        val sink = AsyncSink(rawSink)

        sink.buffer.write(ByteArray(42))
        assertEquals(0, rawSink.bytesWritten)

        sink.flush()
        assertFalse(rawSink.closed)
        assertTrue(rawSink.flushed)
        assertEquals(42, rawSink.bytesWritten)
    }

    @Test
    fun testWrite() = runTest {
        val rawSink = StateCapturingSink()
        val sink = AsyncSink(rawSink)
        val buffer = Buffer().apply { write(ByteArray(42)) }

        sink.write(buffer, buffer.size)
        assertEquals(0, rawSink.bytesWritten)
        assertEquals(42, sink.buffer.size)
        assertEquals(0, buffer.size)

        buffer.writeLong(0L)
        sink.write(buffer, 3)
        assertEquals(0, rawSink.bytesWritten)
        assertEquals(45, sink.buffer.size)
        assertEquals(5, buffer.size)
    }
}
