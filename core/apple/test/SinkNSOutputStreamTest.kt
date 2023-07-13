package kotlinx.io

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.darwin.UInt8Var
import kotlin.test.*

@OptIn(UnsafeNumber::class)
class SinkNSOutputStreamTest {
    @Test
    fun bufferOutputStream() {
        val sink = Buffer()
        testOutputStream(sink)
    }

    @Test
    fun realSinkOutputStream() {
        val sink = RealSink(Buffer())
        testOutputStream(sink)
    }

    @OptIn(InternalIoApi::class)
    private fun testOutputStream(sink: Sink) {
        val out = sink.asNSOutputStream()
        val byteArray = "abc".encodeToByteArray()
        byteArray.usePinned {
            val cPtr = it.addressOf(0).reinterpret<UInt8Var>()

            assertEquals(NSStreamStatusNotOpen, out.streamStatus)
            assertEquals(-1, out.write(cPtr, 3U))
            out.open()
            assertEquals(NSStreamStatusOpen, out.streamStatus)

            assertEquals(3, out.write(cPtr, 3U))
            assertEquals("[97, 98, 99]", sink.buffer.readByteArray().contentToString())

            assertEquals(3, out.write(cPtr, 3U))
            val data = out.propertyForKey(NSStreamDataWrittenToMemoryStreamKey) as NSData
            assertEquals("abc", data.toByteArray().decodeToString())
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
