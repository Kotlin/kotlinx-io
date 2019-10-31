package kotlinx.io.tests.buffer

import kotlinx.io.buffer.*
import kotlin.test.*

class BufferBasicTest {
    @Test
    fun emptyBufferSizeIsZero() {
        assertEquals(Buffer.EMPTY.size, 0)
    }

    @Test
    fun allocateBufferOfSpecificSize() {
        val buffer = PlatformBufferAllocator.allocate(12)
        assertEquals(buffer.size, 12)
    }
}
