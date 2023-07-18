/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */
package kotlinx.io

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.darwin.NSUInteger
import platform.darwin.UInt8Var
import kotlin.test.*

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
            val cPtr = it.addressOf(0).reinterpret<UInt8Var>()

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
            val cPtr = it.addressOf(0).reinterpret<UInt8Var>()

            assertEquals(-1, out.write(cPtr, 4U))
            assertNotNull(out.streamError)
            assertEquals("Underlying sink is closed.", out.streamError?.localizedDescription)
            assertTrue(sink.buffer.readByteArray().isEmpty())
        }
    }
}
