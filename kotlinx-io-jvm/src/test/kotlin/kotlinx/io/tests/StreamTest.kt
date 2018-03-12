package kotlinx.io.tests

import kotlinx.io.core.*
import kotlinx.io.streams.*
import org.junit.Test
import java.io.*
import kotlin.test.*

class StreamTest {
    @Test
    fun testOutput() {
        val baos = ByteArrayOutputStream()
        val output = baos.asOutput()

        output.byteOrder = ByteOrder.BIG_ENDIAN
        output.writeInt(0x11223344)

        output.flush()

        assertTrue { byteArrayOf(0x11, 0x22, 0x33, 0x44).contentEquals(baos.toByteArray()) }
    }

    @Test
    fun testInput() {
        val baos = ByteArrayInputStream(byteArrayOf(0x11, 0x22, 0x33, 0x44))
        val input = baos.asInput()

        input.byteOrder = ByteOrder.BIG_ENDIAN
        assertEquals(0x11223344, input.readInt())
    }

    @Test
    fun testInputParts() {
        val inputStream = PipedInputStream()
        val outputStream = PipedOutputStream()

        inputStream.connect(outputStream)

        val baos = ByteArrayInputStream(byteArrayOf(0x11, 0x22))
        val input = baos.asInput()

        input.byteOrder = ByteOrder.BIG_ENDIAN
        assertEquals(0x11223344, input.readInt())
    }
}
