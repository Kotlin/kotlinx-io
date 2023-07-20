/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.atomicfu.atomic
import kotlinx.cinterop.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import platform.CoreFoundation.CFRunLoopStop
import platform.Foundation.*
import platform.darwin.NSObject
import platform.darwin.NSUInteger
import platform.posix.uint8_tVar
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

@OptIn(UnsafeNumber::class)
class SinkNSOutputStreamTest {
    @Test
    fun bufferOutputStream() {
        testOutputStream(Buffer(), "abc")
        testOutputStream(Buffer(), "a" + "b".repeat(Segment.SIZE * 2) + "c")
    }

    @Test
    fun realSinkOutputStream() {
        testOutputStream(RealSink(Buffer()), "abc")
        testOutputStream(RealSink(Buffer()), "a" + "b".repeat(Segment.SIZE * 2) + "c")
    }

    @OptIn(InternalIoApi::class)
    private fun testOutputStream(sink: Sink, input: String) {
        val out = sink.asNSOutputStream()
        val byteArray = input.encodeToByteArray()
        val size: NSUInteger = input.length.convert()
        byteArray.usePinned {
            val cPtr = it.addressOf(0).reinterpret<uint8_tVar>()

            assertEquals(NSStreamStatusNotOpen, out.streamStatus)
            assertEquals(-1, out.write(cPtr, size))
            out.open()
            assertEquals(NSStreamStatusOpen, out.streamStatus)

            assertEquals(size.convert(), out.write(cPtr, size))
            sink.flush()
            when (sink) {
                is Buffer -> {
                    val data = out.propertyForKey(NSStreamDataWrittenToMemoryStreamKey) as NSData
                    assertContentEquals(byteArray, data.toByteArray())
                    assertContentEquals(byteArray, sink.buffer.readByteArray())
                }
                is RealSink -> assertContentEquals(byteArray, (sink.sink as Buffer).readByteArray())
            }
        }
    }

    @Test
    @OptIn(DelicateIoApi::class)
    fun nsOutputStreamClose() {
        val buffer = Buffer()
        val sink = RealSink(buffer)
        assertFalse(sink.closed)

        val out = sink.asNSOutputStream()
        out.open()
        out.close()
        assertTrue(sink.closed)
        assertEquals(NSStreamStatusClosed, out.streamStatus)

        val byteArray = ByteArray(4)
        byteArray.usePinned {
            val cPtr = it.addressOf(0).reinterpret<uint8_tVar>()

            assertEquals(-1, out.write(cPtr, 4U))
            assertNotNull(out.streamError)
            assertEquals("Underlying sink is closed.", out.streamError?.localizedDescription)
            assertTrue(sink.buffer.readByteArray().isEmpty())
        }
    }

    @Test
    fun delegateTest() {
        val runLoop = startRunLoop()

        fun produceWithDelegate(out: NSOutputStream, data: String) {
            val opened = Mutex(true)
            val written = atomic(0)
            val completed = Mutex(true)

            out.delegate = object : NSObject(), NSStreamDelegateProtocol {
                val source = data.encodeToByteArray()
                override fun stream(aStream: NSStream, handleEvent: NSStreamEvent) {
                    assertEquals("run-loop", NSThread.currentThread.name)
                    when (handleEvent) {
                        NSStreamEventOpenCompleted -> opened.unlock()
                        NSStreamEventHasSpaceAvailable -> {
                            if (source.isNotEmpty()) {
                                source.usePinned {
                                    assertEquals(
                                        data.length.convert(),
                                        out.write(it.addressOf(written.value).reinterpret(), data.length.convert())
                                    )
                                    written.value += data.length
                                }
                            }
                            val writtenData = out.propertyForKey(NSStreamDataWrittenToMemoryStreamKey) as NSData
                            assertEquals(data, writtenData.toByteArray().decodeToString())
                            out.close()
                            completed.unlock()
                        }
                    }
                }
            }
            out.scheduleInRunLoop(runLoop, NSDefaultRunLoopMode)
            out.open()
            runBlocking {
                withTimeout(5.seconds) {
                    opened.lock()
                    completed.lock()
                }
            }
            assertEquals(data.length, written.value)
        }

        produceWithDelegate(NSOutputStream.outputStreamToMemory(), "default")
        produceWithDelegate(Buffer().asNSOutputStream(), "custom")
        produceWithDelegate(NSOutputStream.outputStreamToMemory(), "")
        produceWithDelegate(Buffer().asNSOutputStream(), "")
        CFRunLoopStop(runLoop.getCFRunLoop())
    }

    @Test
    fun testSubscribeAfterOpen() {
        val runLoop = startRunLoop()

        fun subscribeAfterOpen(out: NSOutputStream) {
            val available = Mutex(true)

            out.delegate = object : NSObject(), NSStreamDelegateProtocol {
                override fun stream(aStream: NSStream, handleEvent: NSStreamEvent) {
                    assertEquals("run-loop", NSThread.currentThread.name)
                    when (handleEvent) {
                        NSStreamEventOpenCompleted -> fail("opened before subscribe")
                        NSStreamEventHasSpaceAvailable -> available.unlock()
                    }
                }
            }
            out.open()
            out.scheduleInRunLoop(runLoop, NSDefaultRunLoopMode)
            runBlocking {
                withTimeout(5.seconds) {
                    available.lock()
                }
            }
            out.close()
        }

        subscribeAfterOpen(NSOutputStream.outputStreamToMemory())
        subscribeAfterOpen(Buffer().asNSOutputStream())
        CFRunLoopStop(runLoop.getCFRunLoop())
    }
}
