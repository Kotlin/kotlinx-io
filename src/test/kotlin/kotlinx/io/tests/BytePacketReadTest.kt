package kotlinx.io.tests

import kotlinx.io.charsets.*
import kotlinx.io.core.*
import kotlin.test.*

class BytePacketReadTest {
    val pool: VerifyingObjectPool<IoBuffer> = VerifyingObjectPool(IoBuffer.Pool)

    @AfterTest
    fun verifyPool() {
        pool.assertEmpty()
    }

    @Test
    fun testReadText() {
        val packet = buildPacket {
            writeByte(0xc6.toByte())
            writeByte(0x86.toByte())
        }

        assertEquals("\u0186", packet.readText(charset = Charsets.UTF_8))
        assertEquals(0, packet.remaining)
    }

    @Test
    fun testReadTextLimited() {
        val packet = buildPacket {
            writeByte(0xc6.toByte())
            writeByte(0x86.toByte())
            writeByte(0xc6.toByte())
            writeByte(0x86.toByte())
        }

        assertEquals("\u0186", packet.readText(charset = Charsets.UTF_8, max = 1))
        assertEquals(2, packet.remaining)
        packet.release()
    }

    @Test
    fun testReadTextChain() {
        val segment1 = pool.borrow()
        val segment2 = pool.borrow()
        segment1.next = segment2
        segment1.reserveEndGap(8)

        segment1.writeByte(0xc6.toByte())
        segment2.writeByte(0x86.toByte())

        val packet = ByteReadPacket(segment1, pool)

        assertEquals("\u0186", packet.readText())
        assertTrue { packet.isEmpty }
    }

    @Test
    fun testReadTextChainThroughReservation() {
        val segment1 = pool.borrow()
        val segment2 = pool.borrow()
        segment1.next = segment2
        segment1.reserveEndGap(8)

        while (segment1.writeRemaining > 1) {
            segment1.writeByte(0)
        }
        segment1.writeByte(0xc6.toByte())
        while (segment1.readRemaining > 1) {
            segment1.readByte()
        }
        segment2.writeByte(0x86.toByte())

        val packet = ByteReadPacket(segment1, pool)

        assertEquals("\u0186", packet.readText())
        assertTrue { packet.isEmpty }
    }

    @Test
    fun testReadTextChainWithDecoder() {
        val segment1 = pool.borrow()
        val segment2 = pool.borrow()
        segment1.next = segment2
        segment1.reserveEndGap(8)

        segment1.writeByte(0xc6.toByte())
        segment2.writeByte(0x86.toByte())

        val packet = ByteReadPacket(segment1, pool)
        assertEquals(2, packet.remaining)

        assertEquals("\u0186", packet.readText(charset = Charsets.UTF_8))
        assertTrue { packet.isEmpty }
    }


    @Test
    fun testReadBytesAll() {
        val pkt = buildPacket {
            byteOrder = ByteOrder.BIG_ENDIAN
            writeInt(0x01020304)
        }

        try {
            assertTrue { byteArrayOf(1, 2, 3, 4).contentEquals(pkt.readBytes()) }
        } finally {
            pkt.release()
        }
    }

    @Test
    fun testReadBytesExact1() {
        val pkt = buildPacket {
            byteOrder = ByteOrder.BIG_ENDIAN
            writeInt(0x01020304)
        }

        try {
            assertTrue { byteArrayOf(1, 2, 3, 4).contentEquals(pkt.readBytes(4)) }
        } finally {
            pkt.release()
        }
    }

    @Test
    fun testReadBytesExact2() {
        val pkt = buildPacket {
            byteOrder = ByteOrder.BIG_ENDIAN
            writeInt(0x01020304)
        }

        try {
            assertTrue { byteArrayOf(1, 2).contentEquals(pkt.readBytes(2)) }
        } finally {
            pkt.release()
        }
    }

    @Test
    fun testReadBytesExact3() {
        val pkt = buildPacket {
            byteOrder = ByteOrder.BIG_ENDIAN
            writeInt(0x01020304)
        }

        try {
            assertTrue { byteArrayOf().contentEquals(pkt.readBytes(0)) }
        } finally {
            pkt.release()
        }
    }

    @Test
    fun testReadBytesExactFails() {
        val pkt = buildPacket {
            byteOrder = ByteOrder.BIG_ENDIAN
            writeInt(0x01020304)
        }

        try {
            assertFails {
                pkt.readBytes(9)
            }
        } finally {
            pkt.release()
        }
    }

    @Test
    fun testReadBytesOf1() {
        val pkt = buildPacket {
            byteOrder = ByteOrder.BIG_ENDIAN
            writeInt(0x01020304)
        }

        try {
            assertTrue { byteArrayOf(1, 2, 3).contentEquals(pkt.readBytesOf(2, 3)) }
        } finally {
            pkt.release()
        }
    }

    @Test
    fun testReadBytesOf2() {
        val pkt = buildPacket {
            byteOrder = ByteOrder.BIG_ENDIAN
            writeInt(0x01020304)
        }

        try {
            assertTrue { byteArrayOf(1, 2, 3, 4).contentEquals(pkt.readBytesOf(2, 9)) }
        } finally {
            pkt.release()
        }
    }

    @Test
    fun testReadBytesOf3() {
        val pkt = buildPacket {
            byteOrder = ByteOrder.BIG_ENDIAN
            writeInt(0x01020304)
        }

        try {
            assertTrue { byteArrayOf().contentEquals(pkt.readBytesOf(0, 0)) }
        } finally {
            pkt.release()
        }
    }


    @Test
    fun testReadBytesOfFails() {
        val pkt = buildPacket {
            byteOrder = ByteOrder.BIG_ENDIAN
            writeInt(0x11223344)
        }

        try {
            assertFails {
                pkt.readBytesOf(9, 13)
            }
        } finally {
            pkt.release()
        }
    }

    @Test
    fun tryPeekTest() {
        buildPacket {
            writeByte(1)
            writeByte(2)
        }.use { pkt ->
            assertEquals(1, pkt.tryPeek())
            assertEquals(2, pkt.tryPeek())
            assertEquals(-1, pkt.tryPeek())
        }

        assertEquals(-1, ByteReadPacket.Empty.tryPeek())

        val segment1 = pool.borrow()
        val segment2 = pool.borrow()
        segment1.resetForWrite()
        segment2.resetForWrite()
        segment1.next = segment2
        segment2.writeByte(1)

        ByteReadPacket(segment1, pool).use { pkt ->
            assertEquals(1, pkt.tryPeek())
            assertEquals(-1, pkt.tryPeek())
        }
    }

    private inline fun buildPacket(startGap: Int = 0, block: BytePacketBuilder.() -> Unit): ByteReadPacket {
        val builder = BytePacketBuilder(startGap, pool)
        try {
            block(builder)
            return builder.build()
        } catch (t: Throwable) {
            builder.release()
            throw t
        }
    }

}