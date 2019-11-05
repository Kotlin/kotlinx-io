package kotlinx.io.tests

import kotlinx.io.*
import kotlin.test.*

class BytesTest {
    private val bufferSizes = (1..64)

    @Test
    fun smokeSingleBufferTest() = bufferSizes.forEach { size ->
        val bytes = buildBytes(size) {
            val array = ByteArray(2)
            array[0] = 0x11
            array[1] = 0x22
            writeArray(array)

            writeByte(0x12)
            writeUByte(0x82u)
            writeShort(0x3456)
            writeInt(0x789abcde)
            writeDouble(1.25)
            writeFloat(1.25f)
            writeLong(0x123456789abcdef0)
            writeLong(0x123456789abcdef0)

            writeUTF8String("OK\n")
            //listOf(1, 2, 3).joinTo(this, separator = "|")
        }

        assertEquals(2 + 2 + 2 + 4 + 8 + 4 + 8 + 8 + 3/* + 5*/, bytes.size())

        val input = bytes.input()
        val ba = ByteArray(2)
        input.readArray(ba)

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

        assertEquals("OK", input.readUTF8Line())
/*
        assertEquals("1|2|3", input.readUTF8Line())
*/

        //assertTrue { bytes.eof() }
    }

    @Test
    fun smokeMultiBufferTest() {
        buildBytes {
            writeArray(ByteArray(9999))
            writeByte(0x12)
            writeShort(0x1234)
            writeInt(0x12345678)
            writeDouble(1.25)
            writeFloat(1.25f)
            writeLong(0x123456789abcdef0)

            writeUTF8String("OK\n")
            val text = listOf(1, 2, 3).joinToString(separator = "|")
            writeUTF8String("$text\n")
        }.useInput {
            //        assertEquals(9999 + 1 + 2 + 4 + 8 + 4 + 8 + 3 + 5, p.remaining)
            readArray(ByteArray(9999))
            assertEquals(0x12, readByte())
            assertEquals(0x1234, readShort())
            assertEquals(0x12345678, readInt())
            assertEquals(1.25, readDouble())
            assertEquals(1.25f, readFloat())
            assertEquals(0x123456789abcdef0, readLong())

            assertEquals("OK", readUTF8Line())
            assertEquals("1|2|3", readUTF8Line())
            assertTrue { eof() }
        }
    }

    @Test
    fun testSingleBufferSkipTooMuch() {
        buildBytes {
            writeArray(ByteArray(9999))
        }.use { buffer ->
            val input = buffer.input()
//            assertEquals(9999, input.discard(10000))
            input.readArray(ByteArray(9999))
            assertTrue { input.eof() }
        }
    }

    @Test
    fun testSingleBufferSkip() {
        buildBytes {
            writeArray("ABC123\n".toByteArray0())
        }.useInput {
            //            assertEquals(3, it.discard(3))
            readArray(ByteArray(3))
            assertEquals("123", readUTF8Line())
            assertTrue { eof() }
        }
    }

    @Test
    fun testSingleBufferSkipExact() {
        val p = buildBytes {
            writeArray("ABC123".toByteArray0())
        }.useInput {
            readArray(ByteArray(3))
            assertEquals("123", readUTF8String(3))
            assertTrue { eof() }
        }

    }

    @Test
    fun testSingleBufferSkipExactTooMuch() {
        buildBytes {
            writeArray("ABC123".toByteArray0())
        }.useInput {
            assertFailsWith<EOFException> {
                readArray(ByteArray(1000))
            }
            assertTrue { eof() }
        }


    }

    @Test
    @Ignore
    fun testMultiBufferSkipTooMuch() {
        buildBytes {
            writeArray(ByteArray(99999))
        }.useInput {
            //            assertEquals(99999, it.discard(1000000))
            assertTrue { eof() }
        }

    }

    @Test
    fun testMultiBufferSkip() {
        buildBytes {
            writeArray(ByteArray(99999))
            writeArray("ABC123\n".toByteArray0())
        }.useInput {
            //            assertEquals(99999 + 3, it.discard(99999 + 3))
            readArray(ByteArray(99999 + 3))
            assertEquals("123", readUTF8Line())
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
            //            assertEquals(PACKET_BUFFER_SIZE + 3, it.remaining.toInt())
            readArray(ByteArray(PACKET_BUFFER_SIZE - 1))
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
            readArray(ByteArray(PACKET_BUFFER_SIZE - 1))

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


