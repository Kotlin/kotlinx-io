package kotlinx.io.tests.memory

import kotlinx.io.memory.*
import kotlin.test.*

class MemoryTest {
    @Test
    fun `Empty memory size is zero`() {
        assertEquals(Memory.Empty.size, 0)
    }

    @Test
    fun `Empty memory throws on read`() {
        assertFailsWith<IndexOutOfBoundsException> {
            Memory.Empty[0]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            Memory.Empty.loadDoubleAt(0)
        }
    }

    @Test
    fun `Empty memory throws on write`() {
        assertFailsWith<IndexOutOfBoundsException> {
            Memory.Empty[0] = 1
        }
        assertFailsWith<IndexOutOfBoundsException> {
            Memory.Empty.storeDoubleAt(0, 0.0)
        }
    }

    @Test
    fun `Allocate memory of the specific size`() {
        val memory = Memory.allocate(12)
        assertEquals(memory.size, 12)
    }

    @Test
    fun `Write and read primitives in the allocated memory`() {
        val memory = Memory.allocate(8)
        assertFailsWith<IndexOutOfBoundsException> {
            memory.storeLongAt(8, 123456789L)
        }
        // TODO: test all primitives
        assertEquals(123451234567890L, 123451234567890L.also { memory.storeLongAt(0, it) })
        assertEquals(1234567890.also { memory.storeIntAt(0, it) }, memory.loadIntAt(0))
        assertEquals(234.toByte().also { memory.storeAt(0, it) }, memory.loadAt(0))
        assertEquals(1234567890.987.also { memory.storeDoubleAt(0, it) }, memory.loadDoubleAt(0))
    }

    // TODO: test all primitive arrays

    @Test
    fun `Write and copy memory and then read from it`() {
        val memory = Memory.allocate(8)
        val memory2 = Memory.allocate(8)
        val value = 123451234567890L.also { memory.storeLongAt(0, it) }
        memory.copyTo(memory2, 0, memory.size, 0)
        assertEquals(value, memory2.loadLongAt(0))
    }
}