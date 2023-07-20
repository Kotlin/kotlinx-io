/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import io.ktor.utils.io.core.*
import kotlinx.atomicfu.atomic
import kotlinx.cinterop.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import platform.CoreFoundation.CFRunLoopStop
import platform.Foundation.*
import platform.darwin.NSObject
import platform.posix.uint8_tVar
import platform.posix.usleep
import kotlin.random.Random
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

@OptIn(UnsafeNumber::class)
class SourceNSInputStreamTest {
    @Test
    fun bufferInputStream() {
        val source = Buffer()
        source.writeString("abc")
        testInputStream(source.asNSInputStream())
    }

    @Test
    fun realSourceInputStream() {
        val source = Buffer()
        source.writeString("abc")
        testInputStream(RealSource(source).asNSInputStream())
    }

    private fun testInputStream(input: NSInputStream) {
        val byteArray = ByteArray(4)
        byteArray.usePinned {
            val cPtr = it.addressOf(0).reinterpret<uint8_tVar>()

            assertEquals(NSStreamStatusNotOpen, input.streamStatus)
            assertEquals(-1, input.read(cPtr, 4U))
            input.open()
            assertEquals(NSStreamStatusOpen, input.streamStatus)

            byteArray.fill(-5)
            assertEquals(3, input.read(cPtr, 4U))
            assertEquals("[97, 98, 99, -5]", byteArray.contentToString())

            byteArray.fill(-7)
            assertEquals(0, input.read(cPtr, 4U))
            assertEquals("[-7, -7, -7, -7]", byteArray.contentToString())
        }
    }

    @Test
    fun bufferInputStreamLongData() {
        val source = Buffer()
        source.writeString("a" + "b".repeat(Segment.SIZE * 2) + "c")
        testInputStreamLongData(source.asNSInputStream())
    }

    @Test
    fun realSourceInputStreamLongData() {
        val source = Buffer()
        source.writeString("a" + "b".repeat(Segment.SIZE * 2) + "c")
        testInputStreamLongData(RealSource(source).asNSInputStream())
    }

    private fun testInputStreamLongData(input: NSInputStream) {
        val lengthPlusOne = Segment.SIZE * 2 + 3
        val byteArray = ByteArray(lengthPlusOne)
        byteArray.usePinned {
            val cPtr = it.addressOf(0).reinterpret<uint8_tVar>()

            assertEquals(NSStreamStatusNotOpen, input.streamStatus)
            assertEquals(-1, input.read(cPtr, lengthPlusOne.convert()))
            input.open()
            assertEquals(NSStreamStatusOpen, input.streamStatus)

            byteArray.fill(-5)
            assertEquals(Segment.SIZE.convert(), input.read(cPtr, lengthPlusOne.convert()))
            assertEquals("[97${", 98".repeat(Segment.SIZE - 1)}${", -5".repeat(Segment.SIZE + 3)}]", byteArray.contentToString())

            byteArray.fill(-6)
            assertEquals(Segment.SIZE.convert(), input.read(cPtr, lengthPlusOne.convert()))
            assertEquals("[98${", 98".repeat(Segment.SIZE - 1)}${", -6".repeat(Segment.SIZE + 3)}]", byteArray.contentToString())

            byteArray.fill(-7)
            assertEquals(2, input.read(cPtr, lengthPlusOne.convert()))
            assertEquals("[98, 99${", -7".repeat(Segment.SIZE * 2 + 1)}]", byteArray.contentToString())

            byteArray.fill(-8)
            assertEquals(0, input.read(cPtr, lengthPlusOne.convert()))
            assertEquals("[-8${", -8".repeat(lengthPlusOne - 1)}]", byteArray.contentToString())
        }
    }

    @Test
    fun nsInputStreamClose() {
        val buffer = Buffer()
        buffer.writeString("abc")
        val source = RealSource(buffer)
        assertFalse(source.closed)

        val input = source.asNSInputStream()
        input.open()
        input.close()
        assertTrue(source.closed)
        assertEquals(NSStreamStatusClosed, input.streamStatus)

        val byteArray = ByteArray(4)
        byteArray.usePinned {
            val cPtr = it.addressOf(0).reinterpret<uint8_tVar>()

            byteArray.fill(-5)
            assertEquals(-1, input.read(cPtr, 4U))
            assertNotNull(input.streamError)
            assertEquals("Underlying source is closed.", input.streamError?.localizedDescription)
            assertEquals("[-5, -5, -5, -5]", byteArray.contentToString())
        }
    }

    @Test
    fun delegateTest() {
        val runLoop = startRunLoop()

        fun consumeWithDelegate(input: NSInputStream, data: String) {
            val opened = Mutex(true)
            val read = atomic(0)
            val completed = Mutex(true)

            input.delegate = object : NSObject(), NSStreamDelegateProtocol {
                val sink = ByteArray(data.length)
                override fun stream(aStream: NSStream, handleEvent: NSStreamEvent) {
                    assertEquals("run-loop", NSThread.currentThread.name)
                    when (handleEvent) {
                        NSStreamEventOpenCompleted -> opened.unlock()
                        NSStreamEventHasBytesAvailable -> {
                            sink.usePinned {
                                assertEquals(1, input.read(it.addressOf(read.value).reinterpret(), 1U))
                                read.value++
                            }
                        }
                        NSStreamEventEndEncountered -> {
                            assertEquals(data, sink.decodeToString())
                            input.close()
                            completed.unlock()
                        }
                    }
                }
            }
            input.scheduleInRunLoop(runLoop, NSDefaultRunLoopMode)
            input.open()
            runBlocking {
                withTimeout(5.seconds) {
                    opened.lock()
                    completed.lock()
                }
            }
            assertEquals(data.length, read.value)
        }

        consumeWithDelegate(NSInputStream(data = "default".encodeToByteArray().toNSData()), "default")
        consumeWithDelegate(Buffer().apply { writeString("custom") }.asNSInputStream(), "custom")
        consumeWithDelegate(NSInputStream(data = NSData.data()), "")
        consumeWithDelegate(Buffer().asNSInputStream(), "")
        CFRunLoopStop(runLoop.getCFRunLoop())
    }

    @Test
    fun uploadTest() {
        val port = Random.nextInt(4000, 8000)
        fun tryUpload(stream: NSInputStream) {
            val request = NSMutableURLRequest.requestWithURL(
                NSURL(string = "http://127.0.0.1:$port")
            )
            request.HTTPMethod = "POST"
            request.HTTPBodyStream = stream
            request.setValue("application/octet-stream", "Content-Type")

            val session = NSURLSession.sharedSession
            val task = session.uploadTaskWithStreamedRequest(request)
            task.resume()
            while (stream.streamStatus != NSStreamStatusAtEnd &&
                stream.streamStatus != NSStreamStatusClosed &&
                stream.streamStatus != NSStreamStatusError
            ) {
                usleep(100_000U)
            }
        }
        @Suppress("ExtractKtorModule")
        val server = embeddedServer(CIO, port = port, host = "localhost") {
            routing {
                post {
                    this.context.request.receiveChannel().readAvailable(1) {
                        println(it.readText())
                    }
                }
            }
        }
        server.start(false)
        tryUpload(NSInputStream(data = "default".encodeToByteArray().toNSData()))
        tryUpload(Buffer().apply { writeString("custom") }.asNSInputStream())
        server.stop()
    }

    @Test
    fun testRunLoopPolling() {
        val runLoop = startRunLoop()

        val opened = Mutex(true)
        val read = atomic(0)
        val end = Mutex(true)

        val buffer = Buffer()
        val input = buffer.asNSInputStream()
        val sink = ByteArray(6)
        input.delegate = object : NSObject(), NSStreamDelegateProtocol {
            override fun stream(aStream: NSStream, handleEvent: NSStreamEvent) {
                when (handleEvent) {
                    NSStreamEventOpenCompleted -> opened.unlock()
                    NSStreamEventHasBytesAvailable -> {
                        sink.usePinned {
                            assertEquals(3, input.read(it.addressOf(read.value).reinterpret(), 3U))
                            read.value += 3
                        }
                    }
                    NSStreamEventEndEncountered -> end.unlock()
                }
            }
        }
        input.scheduleInRunLoop(runLoop, NSDefaultRunLoopMode)
        input.open()
        runBlocking {
            withTimeout(5.seconds) {
                opened.lock()
                end.lock()
                buffer.writeString("abc")
                end.lock()
                assertEquals(3, read.value)
                buffer.writeString("123")
                end.lock()
            }
        }
        assertEquals(6, read.value)
        assertEquals("abc123", sink.decodeToString())
        input.close()
        CFRunLoopStop(runLoop.getCFRunLoop())
    }

    @Test
    fun testRunLoopSwitch() {
        val runLoop1 = startRunLoop("run-loop-1")
        val runLoop2 = startRunLoop("run-loop-2")

        fun consumeSwitching(input: NSInputStream, data: String) {
            val opened = Mutex(true)
            val read = atomic(0)
            val completed = Mutex(true)

            input.delegate = object : NSObject(), NSStreamDelegateProtocol {
                val sink = ByteArray(data.length)
                override fun stream(aStream: NSStream, handleEvent: NSStreamEvent) {
                    if (read.value == 0) {
                        assertEquals("run-loop-1", NSThread.currentThread.name)
                    } else {
                        assertEquals("run-loop-2", NSThread.currentThread.name)
                    }
                    when (handleEvent) {
                        NSStreamEventOpenCompleted -> opened.unlock()
                        NSStreamEventHasBytesAvailable -> {
                            if (read.value == 0) {
                                // switch to other run loop
                                input.removeFromRunLoop(runLoop1, NSDefaultRunLoopMode)
                                input.scheduleInRunLoop(runLoop2, NSDefaultRunLoopMode)
                            } else if (read.value >= data.length - 3) {
                                // unsubscribe
                                input.removeFromRunLoop(runLoop2, NSDefaultRunLoopMode)
                            }
                            sink.usePinned {
                                val readBytes = input.read(it.addressOf(read.value).reinterpret(), 3U)
                                read.value += readBytes.toInt()
                            }
                            if (read.value == data.length) {
                                assertEquals(data, sink.decodeToString())
                                completed.unlock()
                            }
                        }
                        NSStreamEventEndEncountered -> fail("$data shouldn't be subscribed")
                    }
                }
            }
            input.scheduleInRunLoop(runLoop1, NSDefaultRunLoopMode)
            input.open()
            runBlocking {
                withTimeout(5.seconds) {
                    opened.lock()
                    completed.lock()
                    // wait a bit to be sure delegate is no longer called
                    delay(200)
                }
            }
            input.close()
        }

        consumeSwitching(NSInputStream(data = "default".encodeToByteArray().toNSData()), "default")
        consumeSwitching(Buffer().apply { writeString("custom") }.asNSInputStream(), "custom")
        CFRunLoopStop(runLoop1.getCFRunLoop())
        CFRunLoopStop(runLoop2.getCFRunLoop())
    }

    @Test
    fun testSubscribeAfterOpen() {
        val runLoop = startRunLoop()

        fun subscribeAfterOpen(input: NSInputStream) {
            val available = Mutex(true)

            input.delegate = object : NSObject(), NSStreamDelegateProtocol {
                override fun stream(aStream: NSStream, handleEvent: NSStreamEvent) {
                    assertEquals("run-loop", NSThread.currentThread.name)
                    when (handleEvent) {
                        NSStreamEventOpenCompleted -> fail("opened before subscribe")
                        NSStreamEventHasBytesAvailable -> available.unlock()
                    }
                }
            }
            input.open()
            input.scheduleInRunLoop(runLoop, NSDefaultRunLoopMode)
            runBlocking {
                withTimeout(5.seconds) {
                    available.lock()
                }
            }
            input.close()
        }

        subscribeAfterOpen(NSInputStream(data = "default".encodeToByteArray().toNSData()))
        subscribeAfterOpen(Buffer().apply { writeString("custom") }.asNSInputStream())
        CFRunLoopStop(runLoop.getCFRunLoop())
    }
}
