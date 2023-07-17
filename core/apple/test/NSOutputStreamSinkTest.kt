package kotlinx.io

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import platform.Foundation.*
import kotlin.test.Test
import kotlin.test.assertEquals

class NSOutputStreamSinkTest {
    @Test
    @OptIn(UnsafeNumber::class)
    fun nsOutputStreamSink() {
        val out = NSOutputStream.outputStreamToMemory()
        val sink = out.asSink()
        val buffer = Buffer().apply {
            writeString("a")
        }
        sink.write(buffer, 1L)
        val data = out.propertyForKey(NSStreamDataWrittenToMemoryStreamKey) as NSData
        assertEquals(1U, data.length)
        val bytes = data.bytes!!.reinterpret<ByteVar>()
        assertEquals(0x61, bytes[0])
    }

    @Test
    fun sinkFromOutputStream() {
        val data = Buffer().apply {
            writeString("a")
            writeString("b".repeat(9998))
            writeString("c")
        }
        val out = NSOutputStream.outputStreamToMemory()
        val sink = out.asSink()

        sink.write(data, 3)
        val outData = out.propertyForKey(NSStreamDataWrittenToMemoryStreamKey) as NSData
        val outString = outData.toByteArray().decodeToString()
        assertEquals("abb", outString)

        sink.write(data, data.size)
        val outData2 = out.propertyForKey(NSStreamDataWrittenToMemoryStreamKey) as NSData
        val outString2 = outData2.toByteArray().decodeToString()
        assertEquals("a" + "b".repeat(9998) + "c", outString2)
    }
}
