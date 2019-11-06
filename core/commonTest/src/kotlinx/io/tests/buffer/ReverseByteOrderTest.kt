package kotlinx.io.tests

import kotlinx.io.buffer.*
import kotlin.test.*
@UseExperimental(ExperimentalStdlibApi::class)
class ReverseByteOrderTest {

    @Test
    fun testReverseLong() {
        testReverseLong(0L, 0L)
        testReverseLong(0x1234_5566_7788_9922, 0x2299_8877_6655_3412)
        for (i in 0..7) {
            val value = 1L shl i
            val expected = 1L shl (56 + i)
            testReverseLong(value, expected)
        }
    }

    private fun testReverseLong(value: Long, reversed: Long) {
        val buffer = PlatformBufferAllocator.allocate(8)
        try {
            buffer.storeLongAt(0, value, ByteOrder.BIG_ENDIAN)
            assertEquals(value, buffer.loadLongAt(0, ByteOrder.BIG_ENDIAN))
            assertEquals(reversed, buffer.loadLongAt(0, ByteOrder.LITTLE_ENDIAN))
            buffer.storeLongAt(0, value, ByteOrder.LITTLE_ENDIAN)
            assertEquals(reversed, buffer.loadLongAt(0, ByteOrder.BIG_ENDIAN))
            assertEquals(value, buffer.loadLongAt(0, ByteOrder.LITTLE_ENDIAN))
        } finally {
            PlatformBufferAllocator.free(buffer)
        }
    }
}
