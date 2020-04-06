@file:Suppress("FORBIDDEN_IDENTITY_EQUALS")

package kotlinx.io

import kotlinx.io.buffer.Buffer
import kotlinx.io.buffer.bufferOf
import kotlinx.io.bytes.buildInput
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OutputTest : LeakDetector() {
    @Test
    fun testBuildBytes() {
        val input = buildInput {
            writeLong(0x0001020304050607)
            writeLong(0x08090A0B0C0D0E0F)
            writeInt(0x08090A0B)
            writeInt(0x00010203)
        }

        input.apply {
            assertFalse(exhausted())
            assertReadLong(0x0001020304050607)
            assertReadLong(0x08090A0B0C0D0E0F)
            assertReadInt(0x08090A0B)
            assertReadInt(0x00010203)
            assertTrue(exhausted())
        }
    }

    @Test
    fun testBuildBytesChunked() {
        val input = buildInput(2) {
            writeByte(0xFF.toByte())
            writeInt(0x08090A0B)
            writeInt(0x00010203)
            writeInt(0xAB023F3)
            writeInt(0xDEAD) // by writing unit tests
        }

        input.apply {
            assertFalse(exhausted())
            assertReadByte(0xFF.toByte())
            assertReadInt(0x08090A0B)
            assertReadInt(0x00010203)
            assertReadInt(0xAB023F3)
            assertReadInt(0xDEAD)
            assertTrue(exhausted())
        }
    }

    @Test
    fun testWriteBufferDirect() {
        val origin = bufferOf(ByteArray(10))
        val output = LambdaOutput(pool) { buffer, _, _ ->
            assertTrue { buffer === origin }
            true
        }

        output.writeBuffer(origin)
        output.close()
    }
}

private class TestInput : Input() {

    override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
        buffer.storeByteAt(startIndex, 42)
        return 1
    }

    override fun closeSource() {
    }
}
