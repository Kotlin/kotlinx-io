package kotlinx.io.tests

import kotlinx.io.core.*
import kotlin.test.*

class IoBufferTest {
    @Test
    fun smokeTest() {
        assertEquals(0, IoBuffer.Empty.capacity)
        assertEquals(0, IoBuffer.Empty.readRemaining)
        assertEquals(0, IoBuffer.Empty.writeRemaining)
        assertEquals(0, IoBuffer.Empty.startGap)
        assertEquals(0, IoBuffer.Empty.endGap)
        assertFalse(IoBuffer.Empty.canRead())
        assertFalse(IoBuffer.Empty.canWrite())

        val buffer = IoBuffer.Pool.borrow()
        try {
            assertNotEquals(0, buffer.writeRemaining)
            assertEquals(buffer.capacity, buffer.writeRemaining)
            assertTrue(buffer.canWrite())
            buffer.writeInt(0x11223344)
            assertEquals(4, buffer.readRemaining)
            assertEquals(0x11223344, buffer.readInt())
            assertEquals(0, buffer.readRemaining)
        } finally {
            buffer.release(IoBuffer.Pool)
        }
    }

    @Test
    fun testResetForWrite() {
        val buffer = IoBuffer.Pool.borrow()
        try {
            val capacity = buffer.capacity

            buffer.resetForWrite(7)
            assertEquals(7, buffer.writeRemaining)
            assertEquals(0, buffer.readRemaining)

            buffer.resetForWrite()
            assertEquals(capacity, buffer.writeRemaining)
            assertEquals(0, buffer.readRemaining)
        } finally {
            buffer.release(IoBuffer.Pool)
        }
    }

    @Test
    fun testWriteWhenImpossible() {
        val buffer = IoBuffer.Pool.borrow()
        try {
            buffer.resetForRead()
            assertFails {
                buffer.writeInt(1)
            }
        } finally {
            buffer.release(IoBuffer.Pool)
        }
    }
}