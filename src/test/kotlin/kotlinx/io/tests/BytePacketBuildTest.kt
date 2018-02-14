package kotlinx.io.tests

import kotlinx.io.core.*
import kotlin.test.Test
import kotlin.test.*

open class BytePacketBuildTest {
    open val pool: VerifyingObjectPool<BufferView> = VerifyingObjectPool(BufferView.NoPool)

    @AfterTest
    fun verifyPool() {
        pool.assertEmpty()
    }

    @Test
    fun smokeSingleBufferTest() {
        val p = buildPacket {
            val ba = ByteArray(2)
            ba[0] = 0x11
            ba[1] = 0x22
            writeFully(ba)

            writeByte(0x12)
            writeShort(0x1234)
            writeInt(0x12345678)
            writeDouble(1.25)
            writeFloat(1.25f)
            writeLong(0x123456789abcdef0)
            writeLong(0x123456789abcdef0)

            writeStringUtf8("OK\n")
            listOf(1, 2, 3).joinTo(this, separator = "|")
        }

//        assertEquals(2 + 1 + 2 + 4 + 8 + 4 + 8 + 3 + 5, p.remaining)
        val ba = ByteArray(2)
        p.readFully(ba)

        assertEquals(0x11, ba[0])
        assertEquals(0x22, ba[1])

        assertEquals(0x12, p.readByte())
        assertEquals(0x1234, p.readShort())
        assertEquals(0x12345678, p.readInt())
        assertEquals(1.25, p.readDouble())
        assertEquals(1.25f, p.readFloat())

        val ll = (1..8).map { p.readByte().toInt() and 0xff }.joinToString()
        assertEquals("18, 52, 86, 120, 154, 188, 222, 240", ll)
        assertEquals(0x123456789abcdef0, p.readLong())

        assertEquals("OK", p.readUTF8Line())
        assertEquals("1|2|3", p.readUTF8Line())

        assertTrue { p.isEmpty }
    }

    @Test
    fun smokeMultiBufferTest() {
        val p = buildPacket {
            writeFully(ByteArray(9999))
            writeByte(0x12)
            writeShort(0x1234)
            writeInt(0x12345678)
            writeDouble(1.25)
            writeFloat(1.25f)
            writeLong(0x123456789abcdef0)

            writeStringUtf8("OK\n")
            kotlin.collections.listOf(1, 2, 3).joinTo(this, separator = "|")
        }

        assertEquals(9999 + 1 + 2 + 4 + 8 + 4 + 8 + 3 + 5, p.remaining)

        p.readFully(ByteArray(9999))
        assertEquals(0x12, p.readByte())
        assertEquals(0x1234, p.readShort())
        assertEquals(0x12345678, p.readInt())
        assertEquals(1.25, p.readDouble())
        assertEquals(1.25f, p.readFloat())
        assertEquals(0x123456789abcdef0, p.readLong())

        assertEquals("OK", p.readUTF8Line())
        assertEquals("1|2|3", p.readUTF8Line())
    }

    @Test
    fun testSingleBufferSkipTooMuch() {
        val p = buildPacket {
            writeFully(kotlin.ByteArray(9999))
        }

        assertEquals(9999, p.discard(10000))
    }

    @Test
    fun testSingleBufferSkip() {
        val p = buildPacket {
            writeFully("ABC123".toByteArray0())
        }

        assertEquals(3, p.discard(3))
        assertEquals("123", p.readUTF8Line())
    }

    @Test
    fun testSingleBufferSkipExact() {
        val p = buildPacket {
            writeFully("ABC123".toByteArray0())
        }

        p.discardExact(3)
        assertEquals("123", p.readUTF8Line())
    }

    @Test
    fun testSingleBufferSkipExactTooMuch() {
        val p = buildPacket {
            writeFully("ABC123".toByteArray0())
        }

        assertFailsWith<EOFException> {
            p.discardExact(1000)
        }
    }

    @Test
    fun testMultiBufferSkipTooMuch() {
        val p = buildPacket {
            writeFully(kotlin.ByteArray(99999))
        }

        assertEquals(99999, p.discard(1000000))
    }

    @Test
    fun testMultiBufferSkip() {
        val p = buildPacket {
            writeFully(kotlin.ByteArray(99999))
            writeFully("ABC123".toByteArray0())
        }

        assertEquals(99999 + 3, p.discard(99999 + 3))
        assertEquals("123", p.readUTF8Line())
    }

    @Test
    fun testNextBufferBytesStealing() {
        val p = buildPacket {
            kotlin.repeat(PACKET_BUFFER_SIZE + 3) {
                writeByte(1)
            }
        }

        assertEquals(PACKET_BUFFER_SIZE + 3, p.remaining)
        p.readFully(ByteArray(PACKET_BUFFER_SIZE - 1))
        assertEquals(0x01010101, p.readInt())
    }

    @Test
    fun testNextBufferBytesStealingFailed() {
        val p = buildPacket {
            kotlin.repeat(PACKET_BUFFER_SIZE + 1) {
                writeByte(1)
            }
        }

        p.readFully(ByteArray(PACKET_BUFFER_SIZE - 1))

        try {
            p.readInt()
            fail()
        } catch (expected: EOFException) {
        } finally {
            p.release()
        }
    }

    @Test
    fun testPreview() {
        val p = buildPacket {
            writeInt(777)

            val i = preview { tmp ->
                tmp.readInt()
            }

            assertEquals(777, i)
        }

        assertEquals(777, p.readInt())
    }

    private inline fun buildPacket(block: BytePacketBuilder.() -> Unit): ByteReadPacket {
        val builder = BytePacketBuilder(0, pool)
        try {
            block(builder)
            return builder.build()
        } catch (t: Throwable) {
            builder.release()
            throw t
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
        val PACKET_BUFFER_SIZE = 4096
    }
}
