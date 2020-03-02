package kotlinx.io.bytes

import kotlinx.io.*
import kotlin.test.*

private val TEST_DATA = ByteArray(8 * 1024 + 1) { it.toByte() }

class BytesOutputTest {
    @Test
    fun testMultipleInputs() {
        val output = BytesOutput()
        fill(output)

        val outputSize = output.size

        assertEquals(8 + 8 * 1024 + 1, outputSize)
        val first = output.createInput()
        val second = output.createInput()

        assertEquals(outputSize, first.remaining)
        assertEquals(outputSize, second.remaining)

        assertTrue(!first.exhausted())
        assertTrue(!second.exhausted())

        consumeAndCheck(first)

        assertEquals(0, first.remaining)
        assertEquals(outputSize, second.remaining)
        assertEquals(outputSize, output.size)
        assertTrue(first.exhausted())
        assertTrue(!second.exhausted())

        consumeAndCheck(second)

        assertEquals(0, first.remaining)
        assertEquals(0, second.remaining)

        assertTrue(first.exhausted())
        assertTrue(second.exhausted())

        assertEquals(outputSize, output.size)
    }

    @Test
    fun testAppendAfterCreate() {
        val output = BytesOutput()
        val first = output.createInput()

        assertTrue(first.exhausted())
        output.writeByte(0)
        assertTrue(first.exhausted())
        assertFails { first.readByte() }

        assertEquals(1, output.size)
        val second = output.createInput()
        assertEquals(1, second.remaining)
        assertTrue(first.exhausted())
        assertFails { first.readByte() }

        second.assertReadByte(0)
    }
}

private fun fill(output: BytesOutput) {
    output.apply {
        writeLong(0)
        writeByteArray(TEST_DATA)
    }
}

private fun consumeAndCheck(input: BytesInput) {
    input.assertReadLong(0)
    repeat(8 * 1024 + 1) {
        input.assertReadByte(it.toByte())
    }
}

