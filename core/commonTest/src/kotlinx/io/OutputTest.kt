@file:Suppress("FORBIDDEN_IDENTITY_EQUALS")

package kotlinx.io

import kotlinx.io.buffer.*
import kotlin.test.*

class OutputTest {
    @Test
    fun testBuildBytes() {
        val bytes = buildBytes {
            writeLong(0x0001020304050607)
            writeLong(0x08090A0B0C0D0E0F)
            writeInt(0x08090A0B)
            writeInt(0x00010203)
        }
        bytes.input().apply {
            assertFalse(eof())
            assertReadLong(0x0001020304050607)
            assertReadLong(0x08090A0B0C0D0E0F)
            assertReadInt(0x08090A0B)
            assertReadInt(0x00010203)
            assertTrue(eof())
        }
    }

    @Test
    fun testBuildBytesChunked() {
        val bytes = buildBytes(2) {
            writeByte(0xFF.toByte())
            writeInt(0x08090A0B)
            writeInt(0x00010203)
            writeInt(0xAB023F3)
            writeInt(0xDEAD) // by writing unit tests
        }
        bytes.input().apply {
            assertFalse(eof())
            assertReadByte(0xFF.toByte())
            assertReadInt(0x08090A0B)
            assertReadInt(0x00010203)
            assertReadInt(0xAB023F3)
            assertReadInt(0xDEAD)
            assertTrue(eof())
        }
    }

    @Test
    fun testWriteBufferDirect() {
        val origin = bufferOf(ByteArray(10))
        val output = LambdaOutput { buffer, startIndex, endIndex ->
            assertTrue { buffer === origin }
        }

        output.writeBuffer(origin)
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