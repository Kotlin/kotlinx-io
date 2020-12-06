package kotlinx.io

import kotlinx.io.buffer.Buffer
import kotlinx.io.text.writeUtf8String
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OutputStreamTest {

    @Test
    fun testOutputAsOutputStream() {
        val output = ByteArrayOutput()

        output.asOutputStream().bufferedWriter().use {
            it.write("a\n")
            it.write("b")
        }

        assertEquals("a\nb", output.toByteArray().decodeToString())
    }

    @Test
    fun testOutputAsOutputStreamBufferBoundary() {
        val baseline = "1".repeat(DEFAULT_BUFFER_SIZE + 1) + "\n" + "2".repeat(DEFAULT_BUFFER_SIZE + 1)
        val output = ByteArrayOutput()
        output.asOutputStream().bufferedWriter().use { it.write(baseline) }
        assertEquals(baseline, output.toByteArray().decodeToString())
    }

    @Test
    fun testOutputAsOutputStreamNegativeValues() {
        val baseline = byteArrayOf(1, -1, -127, 0, -125, 127, -42, -1, 3)
        val output = ByteArrayOutput()
        val stream = output.asOutputStream()
        stream.write(baseline)
        stream.close()
        assertArrayEquals(baseline, output.toByteArray())
    }

    @Test
    fun testOutputAsOutputStreamClose() {
        var closed = false
        val output = object : Output() {
            override fun flush(source: Buffer, startIndex: Int, endIndex: Int) {

            }

            override fun closeSource() {
                closed = true
            }
        }
        val outputStream = output.asOutputStream()
        outputStream.write(1)
        assertFalse(closed)
        outputStream.close()
        assertTrue(closed)
    }

    // IS as I

    @Test
    fun testOutputStreamAsOutput() {
        val outputStream = ByteArrayOutputStream()
        val output = outputStream.asOutput()
        output.writeUtf8String("1\n2")
        output.close()
        assertEquals("1\n2", outputStream.toByteArray().decodeToString())
    }

    @Test
    fun testOutputStreamAsOutputBufferBoundary() {
        val baseline = "1".repeat(DEFAULT_BUFFER_SIZE + 1) + "\n" + "2".repeat(DEFAULT_BUFFER_SIZE + 1)
        val outputStream = ByteArrayOutputStream()
        val output = outputStream.asOutput()
        output.writeUtf8String(baseline)
        output.close()
        assertEquals(baseline, outputStream.toByteArray().decodeToString())
    }

    @Test
    fun testOutputStreamAsOutputNegativeValues() {
        val baseline = byteArrayOf(1, -1, -127, 0, -125, 127, -42, -1, 3)
        val outputStream = ByteArrayOutputStream()
        val output = outputStream.asOutput()
        output.writeByteArray(baseline)
        output.close()
        assertArrayEquals(baseline, outputStream.toByteArray())
    }


    @Test
    fun testOutputStreamAsOutputClose() {
        var closed = false
        val outputStream = object : OutputStream() {
            override fun write(b: Int) {

            }

            override fun close() {
                closed = true
            }
        }
        val output = outputStream.asOutput()
        output.writeByte(1)
        assertFalse(closed)
        output.close()
        assertTrue(closed)
    }
}
