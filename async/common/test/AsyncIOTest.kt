/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.coroutines.test.runTest
import kotlinx.io.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class TestAsyncSink : AsyncRawSink {
    val buffer = Buffer()
    var closed = false
    var flushed = false
    override suspend fun write(source: Buffer, byteCount: Long) {
        buffer.write(source, byteCount)
    }

    override suspend fun flush() {
        flushed = true
    }

    override suspend fun close() {
       closed = true
    }

    override fun closeAbruptly() {
        closed = true
    }
}

private class TestAsyncSource : AsyncRawSource {
    var closed = false
    override suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long = -1

    override suspend fun close() {
        closed = true
    }

    override fun closeAbruptly() {
        closed = true
    }

}

private class TestSource : RawSource {
    var closed = false
    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long = -1L
    override fun close() {
        closed = true
    }
}

private class TestSink : RawSink {
    var closed = false
    override fun write(source: Buffer, byteCount: Long) = Unit

    override fun flush() = Unit

    override fun close() {
        closed = true
    }
}

class AsyncIOTest {
    @Test
    fun writeWithBuffer() = runTest {
        val sink = TestAsyncSink()
        sink.writeWithBuffer {
            writeString("blablabla")
        }
        assertTrue(sink.flushed)
        assertFalse(sink.closed)
        assertEquals("blablabla", sink.buffer.readString())
    }

    @Test
    fun useSource() = runTest {
        val source = TestAsyncSource()
        source.use {
            assertEquals(-1, it.readAtMostTo(Buffer(), 100500))
        }
        assertTrue(source.closed)
    }

    @Test
    fun useSink() = runTest {
        val sink = TestAsyncSink()
        sink.use {
            it.writeWithBuffer {
                writeString("hello")
            }
        }
        assertTrue(sink.closed)
    }

    @Test
    fun bufferedSource() = runTest {
        val source = TestAsyncSource()
        source.buffered().use {
            it.awaitOrThrow(AwaitPredicate.exhausted())
        }
        assertTrue(source.closed)
    }

    @Test
    fun blockingSourceAsAsync() = runTest {
        val source = Buffer().apply { writeString("test") }
        (source as RawSource).asAsync().buffered().use {
            it.awaitOrThrow(AwaitPredicate.exhausted())
            assertEquals("test", it.buffer.readString())
        }
    }

    @Test
    fun delegateCloseAbruptlyToBlockingSource() = runTest {
        val source = TestSource()
        source.asAsync().closeAbruptly()
        assertTrue(source.closed)
    }

    @Test
    fun blockingSinkAsAsync() = runTest {
        val sink = Buffer()
        (sink as RawSink).asAsync().use {
            it.writeWithBuffer {
                writeString("hello")
            }
        }
        assertEquals("hello", sink.readString())
    }

    @Test
    fun delegateCloseAbruptlyToBlockingSink() = runTest {
        val sink = TestSink()
        sink.asAsync().closeAbruptly()
        assertTrue(sink.closed)
    }
}
