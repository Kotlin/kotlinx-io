package kotlinx.io

import kotlinx.cinterop.IntVar
import kotlinx.cinterop.pointed
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.Foundation.*
import kotlin.test.Test
import kotlin.test.assertEquals

class NSOutputStreamSinkTest {
    @Test
    fun nsOutputStreamSink() {
        val out = NSOutputStream.outputStreamToMemory()
        val sink = out.asSink()
        val buffer = Buffer().apply {
            writeString("a")
        }
        sink.write(buffer, 1L)
        val data = out.propertyForKey(NSStreamDataWrittenToMemoryStreamKey) as NSData
        val byte = data.bytes?.reinterpret<IntVar>()?.pointed?.value
        assertEquals(0x61, byte)
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
