package kotlinx.io.tests.buffer

import kotlinx.io.buffer.*
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
}
