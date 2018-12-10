package kotlinx.io.tests

import kotlinx.io.core.*
import kotlin.test.*

class PeekCharTest {
    @Test
    fun testPeekEOF() {
        assertFailsWith<EOFException> {
            ByteReadPacket.Empty.peekCharUtf8()
        }
    }

    @Test
    fun testPeekAsciiSingle() {
        buildPacket {
            writeByte(0x37)
        }.use {
            assertEquals('7', it.peekCharUtf8())
            assertEquals(1, it.remaining)
        }
    }

    @Test
    fun testPeekAsciiSeveral() {
        buildPacket {
            writeByte(0x37)
            writeByte(0x38)
            writeByte(0x39)
        }.use {
            assertEquals('7', it.peekCharUtf8())
            it.discardExact(1)
            assertEquals('8', it.peekCharUtf8())
            it.discardExact(1)
            assertEquals('9', it.peekCharUtf8())
            it.discardExact(1)
            assertEquals(0, it.remaining)
        }
    }

    @Test
    fun testPeekUtf8() {
        buildPacket {
            append('\u0422')
        }.use {
            assertEquals('\u0422', it.peekCharUtf8())
        }
    }

    @Test
    fun testPeekUtf8of3bytes() {
        val bopomofoChar = '\u310f'

        buildPacket {
            append(bopomofoChar)
        }.use {
            assertEquals(3, it.remaining)
            assertEquals(bopomofoChar, it.peekCharUtf8())
        }
    }

    @Test
    fun testPeekUtf8Edge() {
        val oSlash = '\u00f8'

        val chunk1 = IoBuffer.Pool.borrow()
        val chunk2 = IoBuffer.Pool.borrow()
        chunk1.reserveEndGap(8)
        chunk1.next = chunk2

        chunk1.writeByte(0xc3.toByte())
        chunk2.writeByte(0xb8.toByte())

        ByteReadPacket(chunk1, IoBuffer.Pool).use {
            assertEquals(oSlash, it.peekCharUtf8())
        }
    }

    @Test
    fun testPeekUtf8EdgeFor3BytesCharacter() {
        val bopomofoChar = '\u310f'

        val chunk1 = IoBuffer.Pool.borrow()
        val chunk2 = IoBuffer.Pool.borrow()
        chunk1.reserveEndGap(8)
        chunk1.next = chunk2

        chunk1.writeByte(0xe3.toByte())
        chunk2.writeByte(0x84.toByte())
        chunk2.writeByte(0x8f.toByte())

        ByteReadPacket(chunk1, IoBuffer.Pool).use {
            assertEquals(bopomofoChar, it.peekCharUtf8())
        }
    }

    @Test
    fun testPeekUtf8EdgeFor3BytesCharacter2() {
        val bopomofoChar = '\u310f'

        val chunk1 = IoBuffer.Pool.borrow()
        val chunk2 = IoBuffer.Pool.borrow()
        chunk1.reserveEndGap(8)
        chunk1.next = chunk2

        chunk1.writeByte(0xe3.toByte())
        chunk1.writeByte(0x84.toByte())
        chunk2.writeByte(0x8f.toByte())

        ByteReadPacket(chunk1, IoBuffer.Pool).use {
            assertEquals(bopomofoChar, it.peekCharUtf8())
        }
    }

    @Test
    fun testPeekUtf8EdgeReservedFor3BytesCharacter() {
        val bopomofoChar = '\u310f'

        val chunk1 = IoBuffer.Pool.borrow()
        val chunk2 = IoBuffer.Pool.borrow()
        chunk1.reserveEndGap(8)
        chunk1.next = chunk2

        chunk1.writeFully(ByteArray(4087))
        chunk1.writeByte(0xe3.toByte())
        chunk2.writeByte(0x84.toByte())
        chunk2.writeByte(0x8f.toByte())
        chunk2.writeByte(0x30)

        ByteReadPacket(chunk1, IoBuffer.Pool).use {
            it.discardExact(4087)
            assertEquals(bopomofoChar, it.peekCharUtf8())
            it.discardExact(3)
            assertEquals('0', it.peekCharUtf8())
        }
    }

    @Test
    fun testPeekUtf8EdgeFor3BytesCharacterFromAbstractInput() {
        val bopomofoChar = '\u310f'
        var count = 0

        val myInput = object : AbstractInput() {
            override fun fill(): IoBuffer? = when (count++) {
                0 -> pool.borrow().apply { writeByte(0xe3.toByte()) }
                1 -> pool.borrow().apply { writeByte(0x84.toByte()) }
                2 -> pool.borrow().apply { writeByte(0x8f.toByte()) }
                else -> null
            }

            override fun closeSource() {
            }
        }

        myInput.use {
            assertEquals(bopomofoChar, it.peekCharUtf8())
        }
    }
}
