package kotlinx.io.tests

import kotlinx.io.core.*
import kotlinx.io.nio.*
import org.junit.*
import java.io.*
import java.nio.channels.*
import java.util.*
import kotlin.test.*
import kotlin.test.Test

class ChannelsTest {
    @Test
    fun testInput() {
        val content = ByteArrayInputStream(byteArrayOf(0x11, 0x22, 0x33, 0x44))
        val input = Channels.newChannel(content).asInput()

        input.byteOrder = ByteOrder.BIG_ENDIAN
        assertEquals(0x11223344, input.readInt())
    }

    @Test
    fun testInputBig() {
        val array = ByteArray(16384)
        array.fill('a'.toByte())

        val content = ByteArrayInputStream(array)
        val input = Channels.newChannel(content).asInput()

        var iterations = 0
        while (!input.endOfInput) {
            input.peekEquals("erfr")
            input.discard(1)
            iterations++
        }

        assertEquals(array.size, iterations)
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

    private fun Input.peekEquals(text: String): Boolean {
        var equals = false
        takeWhileSize(text.length) { buffer ->
            val remaining = buffer.readRemaining
            val sourceText = buffer.readText(max = text.length)
            equals = sourceText == text
            buffer.pushBack(remaining - buffer.readRemaining)
            0
        }
        return equals
    }
}
