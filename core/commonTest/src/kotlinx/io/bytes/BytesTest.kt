package kotlinx.io.bytes

import kotlinx.io.*
import kotlinx.io.text.*
import kotlin.test.*

class BytesTest {
    private val bufferSizes = (1..64)

    @Test
    fun testSmokeSingleBuffer() = bufferSizes.forEach { size ->
        val bytes = buildBytes(size) {
            val array = ByteArray(2)
            array[0] = 0x11
            array[1] = 0x22
            writeByteArray(array)

            writeByte(0x12)
            writeUByte(0x82u)
            writeShort(0x3456)
            writeInt(0x789abcde)
            writeDouble(1.25)
            writeFloat(1.25f)
            writeLong(0x123456789abcdef0)
            writeLong(0x123456789abcdef0)

            writeUtf8String("OK\n")
        }
        val input = bytes.input()

        assertEquals(2 + 2 + 2 + 4 + 8 + 4 + 8 + 8 + 3, bytes.size())

        val ba = ByteArray(2)
        input.readByteArray(ba)

        assertEquals(0x11, ba[0])
        assertEquals(0x22, ba[1])

        assertEquals(0x12, input.readByte())
        assertEquals(0x82u, input.readUByte())
        assertEquals(0x3456, input.readShort())
        assertEquals(0x789abcde, input.readInt())
        assertEquals(1.25, input.readDouble())
        assertEquals(1.25f, input.readFloat())

        val ll = (1..8).map { input.readByte().toInt() and 0xff }.joinToString()
        assertEquals("18, 52, 86, 120, 154, 188, 222, 240", ll)
        assertEquals(0x123456789abcdef0, input.readLong())

        assertEquals("OK", input.readUtf8Line())
    }

    @Test
    fun testSmokeMultiBuffer() {
        buildBytes {
            writeByteArray(ByteArray(9999))
            writeByte(0x12)
            writeShort(0x1234)
            writeInt(0x12345678)
            writeDouble(1.25)
            writeFloat(1.25f)
            writeLong(0x123456789abcdef0)

            writeUtf8String("OK\n")
            val text = listOf(1, 2, 3).joinToString(separator = "|")
            writeUtf8String("$text\n")
        }.useInput {
            readByteArray(ByteArray(9999))
            assertEquals(0x12, readByte())
            assertEquals(0x1234, readShort())
            assertEquals(0x12345678, readInt())
            assertEquals(1.25, readDouble())
            assertEquals(1.25f, readFloat())
            assertEquals(0x123456789abcdef0, readLong())

            assertEquals("OK", readUtf8Line())
            assertEquals("1|2|3", readUtf8Line())
            assertTrue { eof() }
        }
    }

    @Test
    fun testSingleBufferSkipTooMuch() {
        buildBytes {
            writeByteArray(ByteArray(9999))
        }.useInput {
            readByteArray(ByteArray(9999))
            assertTrue { eof() }
        }
    }

    @Test
    fun testSingleBufferSkip() {
        buildBytes {
            writeByteArray("ABC123\n".toByteArray0())
        }.useInput {
            readByteArray(ByteArray(3))
            assertEquals("123", readUtf8Line())
            assertTrue { eof() }
        }
    }

    @Test
    fun testSingleBufferSkipExact() {
        buildBytes {
            writeByteArray("ABC123".toByteArray0())
        }.useInput {
            readByteArray(ByteArray(3))
            assertEquals("123", readUtf8String(3))
            assertTrue { eof() }
        }
    }

    @Test
    fun testSingleBufferSkipExactTooMuch() {
        buildBytes {
            writeByteArray("ABC123".toByteArray0())
        }.useInput {
            assertFailsWith<EOFException> {
                readByteArray(ByteArray(1000))
            }
            assertTrue { eof() }
        }
    }

    @Test
    fun testMultiBufferSkipTooMuch() {
        buildBytes {
            writeByteArray(ByteArray(99999))
        }.useInput {
            discard(99999)
            assertTrue { eof() }
        }

    }

    @Test
    fun testMultiBufferSkip() {
        buildBytes {
            writeByteArray(ByteArray(99999))
            writeByteArray("ABC123\n".toByteArray0())
        }.useInput {
            readByteArray(ByteArray(99999 + 3))
            assertEquals("123", readUtf8Line())
            assertTrue { eof() }
        }
    }

    @Test
    fun testNextBufferBytesStealing() {
        buildBytes {
            repeat(PACKET_BUFFER_SIZE + 3) {
                writeByte(1)
            }
        }.useInput {
            readByteArray(ByteArray(PACKET_BUFFER_SIZE - 1))
            assertEquals(0x01010101, readInt())
            assertTrue { eof() }
        }
    }

    @Test
    fun testNextBufferBytesStealingFailed() {
        buildBytes {
            repeat(PACKET_BUFFER_SIZE + 1) {
                writeByte(1)
            }
        }.useInput {
            readByteArray(ByteArray(PACKET_BUFFER_SIZE - 1))

            try {
                readInt()
                fail()
            } catch (_: EOFException) {
            }
        }
    }

    @Test
    fun testReadByteEmptyPacket() {
        assertFailsWith<EOFException> {
            buildBytes { }.useInput {
                readInt()
            }
        }

        assertFailsWith<EOFException> {
            buildBytes {
                writeInt(1)
            }.useInput {
                readInt()
                readByte()
            }
        }
    }

    private fun String.toByteArray0(): ByteArray {
        val result = ByteArray(length)

        for (i in 0 until length) {
            val v = this[i].toInt() and 0xff
            if (v > 0x7f) fail()
            result[i] = v.toByte()
        }

        return result
    }

    companion object {
        const val PACKET_BUFFER_SIZE: Int = 4096
    }
}


