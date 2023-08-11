/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    fun testInterruptAwait() = runTest(timeout = 1L.minutes) {
        val source = AsyncSource(InfiniteSource())
        val m = Mutex()
        val job = launch(Dispatchers.Default) {
            m.withLock {
                source.await(AwaitPredicate.exhausted())
            }
            fail("unreachable")
        }
        while (m.tryLock()) {
            m.unlock()
        }
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
    }

    @Test
    fun testInterruptTryAwait() = runTest(timeout = 1L.minutes) {
        val source = AsyncSource(InfiniteSource())

        val m = Mutex()
        val job = launch(Dispatchers.Default) {
            m.withLock {
                source.tryAwait(AwaitPredicate.exhausted())
            }
            fail("unreachable")
        }
        while (m.tryLock()) {
            m.unlock()
        }
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
    }

    @Test
    fun testTryAwait() = runTest {
        val source = AsyncSource(StringSource("data"))

        assertTrue(source.tryAwait(AwaitPredicate.byteFound('d'.code.toByte())))
        assertFalse(source.tryAwait(AwaitPredicate.byteFound('!'.code.toByte())))
    }

    @Test
    fun testAwait() = runTest {
        val source = AsyncSource(StringSource("data"))

        source.await(AwaitPredicate.byteFound('d'.code.toByte()))
        assertFails {
            source.await(AwaitPredicate.byteFound('!'.code.toByte()))
        }
    }

    @Test
    fun testAllDataTransferred() = runTest {
        val source = AsyncSource(StringSource("the message"))
        source.await(AwaitPredicate.exhausted())

        assertEquals("the message", source.buffer.readString())
    }

    @Test
    fun testClose() = runTest {
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
    }

    @Test
    fun testReadAtMostTo() = runTest {
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
