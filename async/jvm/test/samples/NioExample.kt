/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async.samples

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.atomicfu.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.io.*
import kotlinx.io.async.AsyncRawSink
import kotlinx.io.async.AsyncRawSource
import kotlinx.io.async.AwaitPredicate
import kotlinx.io.async.buffered
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.*
import kotlin.concurrent.Volatile
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class SimpleNioReactor {
    val selector: Selector = Selector.open()

    @Volatile
    private var continueExecution = true
    suspend fun start(ctx: CoroutineContext = Dispatchers.IO) = withContext(ctx) {
        while (continueExecution) {
            if (selector.select(1000L) == 0) continue
            val keys = selector.selectedKeys().iterator()

            while (keys.hasNext()) {
                val key = keys.next()
                keys.remove()
                try {
                    val attachment = key.attachment() as Selectable
                    attachment.select(key.readyOps())
                } catch (ex: IOException) {
                    key.cancel()
                }
            }
        }
    }

    fun stop() {
        continueExecution = false
    }
}

interface Selectable {
    fun select(ops: Int)
}

class AsyncServerSocket(
    host: String, port: Int,
    private val reactor: SimpleNioReactor,
    private val onAccept: (AsyncRawSource, AsyncRawSink) -> Unit
) : Selectable {
    private val socket: ServerSocketChannel

    init {
        val socket_ = ServerSocketChannel.open()
        try {
            socket_.bind(InetSocketAddress(host, port))
        } catch (t: IOException) {
            socket_.close()
            throw t
        }
        socket = socket_
        socket.configureBlocking(false)
        socket.register(reactor.selector, SelectionKey.OP_ACCEPT, this)

    }

    override fun select(ops: Int) {
        require(ops and SelectionKey.OP_ACCEPT != 0)
        val client = socket.accept()
        client.configureBlocking(false)
        val input = SelectableChannelSource(client)
        val output = SelectableChannelSink(client)
        client.register(reactor.selector, SelectionKey.OP_READ, input)
        client.register(reactor.selector, SelectionKey.OP_WRITE, output)
        onAccept(input, output)
    }
}

abstract class Suspendable {
    private val continuation = atomic<Continuation<Unit>?>(null)

    suspend fun suspend() {
        suspendCoroutine<Unit> {
            continuation.update { it }
        }
    }

    fun resume() {
        val cont = continuation.getAndUpdate { null }
        cont?.resume(Unit)
    }
}

class SelectableChannelSink<T>(private val sink: T) : Suspendable(), AsyncRawSink, Selectable
        where T : WritableByteChannel, T : SelectableChannel {
    private val internalBuffer = ByteBuffer.allocate(8192)

    init {
        require(sink.validOps() and SelectionKey.OP_WRITE != 0) {
            "Sink channel ($sink) does support OP_WRITE operation."
        }
    }

    override suspend fun write(source: Buffer, byteCount: Long) {
        var remaining = byteCount
        while (remaining > 0) {
            internalBuffer.clear()
            internalBuffer.limit(minOf(internalBuffer.capacity().toLong(), remaining).toInt())
            source.readAtMostTo(internalBuffer)
            internalBuffer.flip()
            while (internalBuffer.hasRemaining()) {
                val bytesWritten = sink.write(internalBuffer)
                remaining -= bytesWritten
                if (internalBuffer.hasRemaining()) {
                    suspend()
                }
            }
        }
    }

    override suspend fun flush() = Unit

    override suspend fun close() = withContext(Dispatchers.IO) { sink.close() }

    override fun select(ops: Int) {
        require(ops and SelectionKey.OP_WRITE != 0)
        resume()
    }
}

class SelectableChannelSource<T>(private val source: T) : Suspendable(), AsyncRawSource, Selectable
        where T : ReadableByteChannel, T : SelectableChannel {
    private val internalBuffer = ByteBuffer.allocate(8192)

    init {
        require(source.validOps() and SelectionKey.OP_READ != 0) {
            "Source channel ($source) does support OP_READ operation."
        }
    }

    override suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        internalBuffer.clear()
        internalBuffer.limit(min(byteCount, internalBuffer.capacity().toLong()).toInt())
        var bytesRead = source.read(internalBuffer)
        while (bytesRead == 0) {
            suspend()
            bytesRead = source.read(internalBuffer)
        }
        if (bytesRead == -1) return -1L
        internalBuffer.flip()
        return sink.write(internalBuffer).toLong()
    }

    override fun close() = source.close()

    override fun select(ops: Int) {
        require(ops and SelectionKey.OP_READ != 0)
        resume()
    }
}

class ReactorTest {
    @Test
    fun echoServer() = runTest(timeout = 2L.minutes) {
        val reactor = SimpleNioReactor()
        val reactorJob = launch(Dispatchers.IO) {
            reactor.start(this.coroutineContext)
        }

        val awaitServerReceived = Mutex(locked = true)
        val awaitClientReceived = Mutex(locked = true)
        val awaitStart = Mutex(locked = true)

        val message = "Hello, server!"

        fun onServerAccept(input: AsyncRawSource, output: AsyncRawSink) {
            launch(Dispatchers.IO) {
                awaitStart.unlock()
                val src = input.buffered()
                src.await(AwaitPredicate.newLineFound())
                val line = src.buffer.readLineStrict()
                assertEquals(message, line)
                output.buffered().apply {
                    buffer.writeString(line)
                    buffer.writeString("\n")
                    flush()
                }
                src.close()
                awaitServerReceived.unlock()
            }
        }

        var port: Int
        while (true) {
            ensureActive()
            port = Random.nextInt(8000, 10000)
            try {
                AsyncServerSocket("localhost", port, reactor) { i, o -> onServerAccept(i, o) }
            } catch (e: IOException) {
                continue
            }
            break
        }

        launch(Dispatchers.IO) {
            val socket = Socket("localhost", port)
            awaitStart.withLock { /* latch */ }
            socket.getOutputStream().asSink().buffered().apply {
                writeString(message)
                writeString("\n")
                flush()
            }
            val line = socket.getInputStream().asSource().buffered().readLineStrict()
            assertEquals(message, line)
            awaitClientReceived.unlock()
            socket.close()
        }

        awaitServerReceived.withLock { /* latch */ }
        awaitClientReceived.withLock { /* latch */ }

        reactor.stop()
        reactorJob.join()
    }
}
