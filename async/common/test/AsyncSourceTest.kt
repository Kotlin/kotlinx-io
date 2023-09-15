/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes

private class StringSource(private val data: String) : AsyncRawSource {
    override suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        return if (sink.exhausted()) {
            sink.writeString(data)
            sink.size
        } else {
            -1L
        }
    }

    override suspend fun close() = Unit

    override fun closeAbruptly() = Unit
}

private class InfiniteSource : AsyncRawSource {
    override suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long = 0L
    override suspend fun close() = Unit
    override fun closeAbruptly() = Unit
}

private class BufferBackedSource(private val buffer: Buffer) : AsyncRawSource {
    override suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        return this.buffer.readAtMostTo(sink, byteCount)
    }

    override suspend fun close() = Unit

    override fun closeAbruptly() = Unit
}

class AsyncSourceTest {
    @Test
    fun interruptAwaitOrThrow() = runTest(timeout = 1L.minutes) {
        val source = AsyncSource(InfiniteSource())
        val q = Channel<Unit>(1)
        val job = launch(Dispatchers.Default) {
            q.send(Unit)
            source.awaitOrThrow(AwaitPredicate.exhausted())
            fail("unreachable")
        }
        q.receive()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
    }

    @Test
    fun interruptAwait() = runTest(timeout = 1L.minutes) {
        val source = AsyncSource(InfiniteSource())
        val q = Channel<Unit>(1)

        val job = launch(Dispatchers.Default) {
            q.send(Unit)
            source.await(AwaitPredicate.exhausted()).getOrThrow()
            fail("unreachable")
        }
        q.receive()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
    }

    @Test
    fun await() = runTest {
        val source = AsyncSource(StringSource("data"))

        assertTrue(source.await(AwaitPredicate.contains('d'.code.toByte())).getOrThrow())
        assertFalse(source.await(AwaitPredicate.contains('!'.code.toByte())).getOrThrow())
    }

    @Test
    fun awaitOrThrow() = runTest {
        val source = AsyncSource(StringSource("data"))

        source.awaitOrThrow(AwaitPredicate.contains('d'.code.toByte()))
        assertFails {
            source.awaitOrThrow(AwaitPredicate.contains('!'.code.toByte()))
        }
    }

    @Test
    fun checkAllDataTransferred() = runTest {
        val source = AsyncSource(StringSource("the message"))
        source.awaitOrThrow(AwaitPredicate.exhausted())

        assertEquals("the message", source.buffer.readString())
    }

    @Test
    fun closeSource() = runTest {
        var closed = false
        val source = AsyncSource(object : AsyncRawSource {
            override suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long = -1L
            override suspend fun close() {
                closed = true
            }

            override fun closeAbruptly() = Unit
        })
        source.close()
        assertTrue(closed)
        assertFailsWith<IllegalStateException> { source.awaitOrThrow(AwaitPredicate.exhausted()) }
    }

    @Test
    fun closeSourceAbruptly() = runTest {
        var closed = false
        val source = AsyncSource(object : AsyncRawSource {
            override suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long = -1L
            override suspend fun close() = Unit
            override fun closeAbruptly() {
                closed = true
            }
        })
        source.closeAbruptly()
        assertTrue(closed)
        assertFailsWith<IllegalStateException> { source.awaitOrThrow(AwaitPredicate.exhausted()) }
    }

    @Test
    fun readAtMostTo() = runTest {
        val buffer = Buffer().apply { write(ByteArray(64)) }
        val source = AsyncSource(BufferBackedSource(buffer))

        source.buffer.write(ByteArray(36))

        val outputBuffer = Buffer()
        assertEquals(32, source.readAtMostTo(outputBuffer, 32))
        assertEquals(32, outputBuffer.size)

        assertEquals(4, source.readAtMostTo(outputBuffer, 9000))
        assertEquals(36, outputBuffer.size)

        assertEquals(10, source.readAtMostTo(outputBuffer, 10))
        assertEquals(46, outputBuffer.size)

        assertEquals(0, source.buffer.size)
        assertEquals(54, buffer.size)
    }
}
