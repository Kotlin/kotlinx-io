package kotlinx.io.tests.bytes

import kotlinx.io.bytes.*
import kotlinx.io.tests.*
import kotlin.test.*

private val TEST_DATA = ByteArray(8 * 1024 + 1) { it.toByte() }

class BytesOutputTest {
    @Test
    fun testMultipleInputs() {
        val output = BytesOutput()
        fill(output)

        val outputSize = output.size

        val first = output.createInput()
        val second = output.createInput()

        assertEquals(outputSize, first.remaining)
        assertEquals(outputSize, second.remaining)

        assertTrue(!first.eof())
        assertTrue(!second.eof())

        consumeAndCheck(first)

        assertEquals(0, first.remaining)
        assertEquals(outputSize, second.remaining)
        assertEquals(outputSize, output.size)
        assertTrue(first.eof())
        assertTrue(!second.eof())

        consumeAndCheck(second)

        assertEquals(0, first.remaining)
        assertEquals(0, second.remaining)

        assertTrue(first.eof())
        assertTrue(second.eof())

        assertEquals(outputSize, output.size)
    }

    @Test
    fun testAppendAfterCreate() {
        val output = BytesOutput()
        val first = output.createInput()

        assertTrue(first.eof())
        output.writeByte(0)
        assertTrue(first.eof())
        assertFails { first.readByte() }

        assertEquals(1, output.size)
        val second = output.createInput()
        assertEquals(1, second.remaining)
        assertTrue(first.eof())
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

