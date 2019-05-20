package kotlinx.io.tests.buffer

import kotlinx.io.buffer.*
import kotlin.test.*

// TODO: Unify IndexOutOfBoundsException and JS RangeError
class BufferTest {
    @Test
    fun emptyBufferSizeIsZero() {
        assertEquals(Buffer.Empty.size, 0)
    }

    @Test
    fun emptyBufferThrowsOnLoad() {
        assertFailsWith<Throwable> {
            Buffer.Empty[0]
        }
        assertFailsWith<Throwable> {
            Buffer.Empty.loadDoubleAt(0)
        }
    }

    @Test
    fun emptyBufferThrowsOnStore() {
        assertFailsWith<Throwable> {
            Buffer.Empty[0] = 1
        }
        assertFailsWith<Throwable> {
            Buffer.Empty.storeDoubleAt(0, 0.0)
        }
    }

    @Test
    fun allocateBufferOfSpecificSize() {
        val buffer = PlatformBufferAllocator.allocate(12)
        assertEquals(buffer.size, 12)
    }

    @Test
    fun storeAndLoadPrimitives() {
        val buffer = PlatformBufferAllocator.allocate(8)
        assertFailsWith<Throwable> {
            buffer.storeLongAt(8, 123456789L)
        }
        // TODO: test all primitives
        assertEquals(123451234567890L, 123451234567890L.also { buffer.storeLongAt(0, it) })
        assertEquals(1234567890.also { buffer.storeIntAt(0, it) }, buffer.loadIntAt(0))
        assertEquals(234.toByte().also { buffer.storeByteAt(0, it) }, buffer.loadByteAt(0))
        assertEquals(1234567890.987.also { buffer.storeDoubleAt(0, it) }, buffer.loadDoubleAt(0))
    }

    // TODO: test all primitive arrays

    @Test
    fun storeCopyAndLoadPrimitives() {
        val buffer1 = PlatformBufferAllocator.allocate(8)
        val buffer2 = PlatformBufferAllocator.allocate(8)
        val value = 123451234567890L.also { buffer1.storeLongAt(0, it) }
        buffer1.copyTo(buffer2, 0, buffer1.size, 0)
        assertEquals(value, buffer2.loadLongAt(0))
    }
}