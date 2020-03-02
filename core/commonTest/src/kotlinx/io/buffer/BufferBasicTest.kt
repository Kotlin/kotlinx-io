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
    fun testBufferOfBackedByArray() {
        val array = ByteArray(1024) { it.toByte() }
        val buffer = bufferOf(array)

        assertEquals(0, buffer.loadByteAt(0))
        array[0] = 1
        assertEquals(1, buffer.loadByteAt(0))
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

    @Test
    fun testCompact() {
        val first = ByteArray(1022) { 42 }
        val second = ByteArray(1021) { 16 }

        val buffer = bufferOf(first + second)

        assertTrue {
            val length = buffer.compact(1022, buffer.size)
            val result = ByteArray(length)
            buffer.loadByteArray(0, result)
            second.contentEquals(second)
        }
    }
}
