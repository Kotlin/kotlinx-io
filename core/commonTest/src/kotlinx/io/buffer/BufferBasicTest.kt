package kotlinx.io.buffer

import kotlin.test.*

class BufferBasicTest {
    @Test
    fun testEmptyBufferSizeIsZero() {
        assertEquals(0, Buffer.EMPTY.size)
    }

    @Test
    fun testAllocateBufferOfSpecificSize() {
        PlatformBufferAllocator.borrow(12) { buffer ->
            assertEquals(12, buffer.size)
        }
    }

    @Test
    @Ignore
    fun testToByteArray() {
        val array = ByteArray(100) { it.toByte() }
        val slice = bufferOf(array).toByteArray(1, 10)

        assertTrue {
            val expected = ByteArray(10) { (it + 1).toByte() }
            slice.contentEquals(expected)
        }
    }
}
