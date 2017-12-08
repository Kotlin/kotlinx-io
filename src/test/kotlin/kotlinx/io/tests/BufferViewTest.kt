package kotlinx.io.tests

import kotlinx.io.core.*
import kotlin.test.*

class BufferViewTest {
    @Test
    fun smokeTest() {
        assertEquals(0, BufferView.Empty.capacity)
        assertEquals(0, BufferView.Empty.readRemaining)
        assertEquals(0, BufferView.Empty.writeRemaining)
        assertEquals(0, BufferView.Empty.startGap)
        assertEquals(0, BufferView.Empty.endGap)
        assertFalse(BufferView.Empty.canRead())
        assertFalse(BufferView.Empty.canWrite())

        val buffer = BufferView.Pool.borrow()
        try {
            assertNotEquals(0, buffer.writeRemaining)
            assertEquals(buffer.capacity, buffer.writeRemaining)
            assertTrue(buffer.canWrite())
            buffer.writeInt(0x11223344)
            assertEquals(4, buffer.readRemaining)
            assertEquals(0x11223344, buffer.readInt())
            assertEquals(0, buffer.readRemaining)
        } finally {
            buffer.release(BufferView.Pool)
        }
    }

    @Test
    fun testResetForWrite() {
        val buffer = BufferView.Pool.borrow()
        try {
            val capacity = buffer.capacity

            buffer.resetForWrite(7)
            assertEquals(7, buffer.writeRemaining)
            assertEquals(0, buffer.readRemaining)

            buffer.resetForWrite()
            assertEquals(capacity, buffer.writeRemaining)
            assertEquals(0, buffer.readRemaining)
        } finally {
            buffer.release(BufferView.Pool)
        }
    }
}