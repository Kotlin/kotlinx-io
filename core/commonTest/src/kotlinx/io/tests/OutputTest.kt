package kotlinx.io.tests

import kotlinx.io.bytes.*
import kotlin.test.*

class OutputTest {
    @Test
    fun testBuildBytes() {
        val input = buildInput {
            writeLong(0x0001020304050607)
            writeLong(0x08090A0B0C0D0E0F)
            writeInt(0x08090A0B)
            writeInt(0x00010203)
        }

        input.apply {
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
        val input = buildInput(2) {
            writeByte(0xFF.toByte())
            writeInt(0x08090A0B)
            writeInt(0x00010203)
            writeInt(0xAB023F3)
            writeInt(0xDEAD) // by writing unit tests
        }

        input.apply {
            assertFalse(eof())
            assertReadByte(0xFF.toByte())
            assertReadInt(0x08090A0B)
            assertReadInt(0x00010203)
            assertReadInt(0xAB023F3)
            assertReadInt(0xDEAD)
            assertTrue(eof())
        }
    }
}