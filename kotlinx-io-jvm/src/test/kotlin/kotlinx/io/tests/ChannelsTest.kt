package kotlinx.io.tests

import kotlinx.io.core.*
import kotlinx.io.nio.*
import java.io.*
import java.nio.channels.*
import kotlin.test.*

class ChannelsTest {
    @Test
    fun testInput() {
        val content = ByteArrayInputStream(byteArrayOf(0x11, 0x22, 0x33, 0x44))
        val input = Channels.newChannel(content).asInput()

        input.byteOrder = ByteOrder.BIG_ENDIAN
        assertEquals(0x11223344, input.readInt())
    }

    @Test
    fun testOutput() {
        val baos = ByteArrayOutputStream()
        val output = Channels.newChannel(baos).asOutput()

        output.byteOrder = ByteOrder.BIG_ENDIAN
        output.writeInt(0x11223344)

        output.flush()

        assertTrue { byteArrayOf(0x11, 0x22, 0x33, 0x44).contentEquals(baos.toByteArray()) }
    }
}