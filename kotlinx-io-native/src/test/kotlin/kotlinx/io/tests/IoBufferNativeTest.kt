package kotlinx.coroutines.io

import kotlinx.io.core.*
import kotlinx.io.pool.*
import kotlinx.cinterop.*
import kotlin.test.*

class IoBufferNativeTest {
    private val buffer = IoBuffer.Pool.borrow()

    @AfterTest
    fun destroy() {
        buffer.release(IoBuffer.Pool)
    }

    @Test
    fun testReadDirectOnEmpty() {
        var invoked = false
        buffer.readDirect { ptr ->
            invoked = true
            0
        }
        assertTrue(invoked)
    }

    @Test
    fun testReadDirectNegativeResult() {
        var invoked = false
        assertFails {
            buffer.readDirect { ptr ->
                -1
            }
        }
    }

    @Test
    fun testReadDirectTooManyBytesResult() {
        var invoked = false
        assertFails {
            buffer.readDirect { ptr ->
                1
            }
        }
    }

    @Test
    fun testReadDirect() {
        var result = 0
        buffer.writeByte(7)
        buffer.writeByte(8)
        buffer.readDirect { ptr ->
            result = ptr[0].toInt()
            1
        }
        assertEquals(7, result)
        assertEquals(8, buffer.readByte().toInt())
    }

    @Test
    fun testReadDirectAtEnd() {
        while (buffer.writeRemaining > 0) {
            buffer.writeByte(1)
        }

        buffer.readDirect { ptr ->
            buffer.readRemaining
        }

        assertEquals(0, buffer.readRemaining)
        buffer.readDirect { 0 }
    }

    @Test
    fun testWriteDirect() {
        buffer.writeDirect { ptr ->
            ptr[0] = 1.toByte()
            ptr[1] = 2.toByte()
            2
        }

        assertEquals(2, buffer.readRemaining)
        assertEquals(1, buffer.readByte().toInt())
        assertEquals(2, buffer.readByte().toInt())
    }

    @Test
    fun testWriteDirectOnFull() {
        val size = buffer.capacity
        buffer.writeDirect { size }
        assertEquals(size, buffer.readRemaining)
        assertEquals(0, buffer.writeRemaining)
        buffer.writeDirect { 0 }
    }
}
