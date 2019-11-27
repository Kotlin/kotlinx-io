package kotlinx.io

import kotlinx.io.*
import kotlinx.io.buffer.*
import kotlin.test.*

class ByteArrayOutputTest {

    @Test
    fun testEmptyOutput() {
        testEmptyOutput(1)
        testEmptyOutput(DEFAULT_BUFFER_SIZE * 3)
    }

    private fun testEmptyOutput(capacity: Int) {
        val input = ByteArrayOutput(capacity)
        assertEquals(0, input.toByteArray().size)
    }

    @Test
    fun testOutput() {
        testOutput(ByteArray(1) { it.toByte() })
        testOutput(ByteArray(239) { it.toByte() })
        testOutput(ByteArray(DEFAULT_BUFFER_SIZE * 3 + 1) { it.toByte() })
    }

    private fun testOutput(arr: ByteArray) {
       val output = ByteArrayOutput()
        output.writeArray(arr)
        output.flush()
        assertTrue(arr.contentEquals(output.toByteArray()))
    }

    @Test
    fun testIllegalCapacity() {
        assertFailsWith<IllegalArgumentException> { ByteArrayOutput(-1) }
        assertFailsWith<IllegalArgumentException> { ByteArrayOutput(0) }
        assertFailsWith<IllegalArgumentException> { ByteArrayOutput(-42) }
    }
}
