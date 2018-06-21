package kotlinx.io.tests

import kotlinx.io.charsets.*
import kotlinx.io.core.*
import kotlin.test.*

class ReadTextCommonTest {
    private val pool: VerifyingObjectPool<BufferView> = VerifyingObjectPool(BufferView.NoPool)

    @AfterTest
    fun verifyPool() {
        pool.assertEmpty()
    }

    @Test
    fun testWritePacketMultiple() {
        val inner = buildPacket {
            append("o".repeat(100000))
        }

        val outer = buildPacket {
            append("123")
            assertEquals(3, size)
            writePacket(inner)
            assertEquals(100003, size)
            append(".")
        }

        assertEquals("123" + "o".repeat(100000) + ".", outer.readText())
        assertEquals(0, inner.remaining)
    }

    @Test
    fun writePacketWithHintExact() {
        val inner = buildPacket(4) {
            append(".")
        }

        val outer = buildPacket {
            append("1234")
            assertEquals(4, size)
            writePacket(inner)
            assertEquals(5, size)
        }

        assertEquals("1234.", outer.readText())
        assertEquals(0, inner.remaining)
    }

    @Test
    fun writePacketWithHintBigger() {
        val inner = buildPacket(10) {
            append(".")
        }

        val outer = buildPacket {
            append("1234")
            kotlin.test.assertEquals(4, size)
            writePacket(inner)
            kotlin.test.assertEquals(5, size)
        }

        assertEquals("1234.", outer.readText())
        assertEquals(0, inner.remaining)
    }

    @Test
    fun writePacketWithHintFailed() {
        val inner = buildPacket(3) {
            append(".")
        }

        val outer = buildPacket {
            append("1234")
            kotlin.test.assertEquals(4, size)
            writePacket(inner)
            kotlin.test.assertEquals(5, size)
        }

        assertEquals("1234.", outer.readText())
        assertEquals(0, inner.remaining)
    }

    @Test
    fun testWritePacketSingleUnconsumed() {
        val inner = buildPacket {
            append("ABC")
        }

        val outer = buildPacket {
            append("123")
            kotlin.test.assertEquals(3, size)
            writePacket(inner.copy())
            kotlin.test.assertEquals(6, size)
            append(".")
        }

        assertEquals("123ABC.", outer.readText())
        assertEquals(3, inner.remaining)
        inner.release()
    }

    @Test
    fun testWritePacketMultipleUnconsumed() {
        val inner = buildPacket {
            append("o".repeat(100000))
        }

        val outer = buildPacket {
            append("123")
            kotlin.test.assertEquals(3, size)
            writePacket(inner.copy())
            kotlin.test.assertEquals(100003, size)
            append(".")
        }

        assertEquals("123" + "o".repeat(100000) + ".", outer.readText())
        assertEquals(100000, inner.remaining)
        inner.release()
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
        assertEquals(2, packet.remaining)

        assertEquals("\u0186", packet.readText())
        assertTrue { packet.isEmpty }
    }

    @Test
    fun testReadTextChain2() {
        val segment1 = pool.borrow()
        val segment2 = pool.borrow()
        segment1.next = segment2
        segment1.reserveEndGap(8)

        segment1.writeByte(0xe2.toByte())
        segment2.writeByte(0x82.toByte())
        segment2.writeByte(0xac.toByte())

        val packet = ByteReadPacket(segment1, pool)
        assertEquals(3, packet.remaining)

        assertEquals("\u20ac", packet.readText())
        assertTrue { packet.isEmpty }
    }

    @Test
    fun testReadTextChain3() {
        val segment1 = pool.borrow()
        val segment2 = pool.borrow()
        segment1.next = segment2
        segment1.reserveEndGap(8)

        segment1.writeByte(0xc6.toByte())
        segment1.writeByte(0x86.toByte())

        segment1.writeByte(0xe2.toByte())
        segment2.writeByte(0x82.toByte())
        segment2.writeByte(0xac.toByte())

        val packet = ByteReadPacket(segment1, pool)

        assertEquals(5, packet.remaining)

        assertEquals("\u0186\u20ac", packet.readText())
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

        assertEquals("\u0186", packet.readText(charset = Charsets.UTF_8))
        assertTrue { packet.isEmpty }
    }

    @Test
    fun testReadTextChainWithDecoderBadChar() {
        val segment1 = pool.borrow()
        val segment2 = pool.borrow()
        segment1.next = segment2
        segment1.reserveEndGap(8)

        segment1.writeByte(0xc0.toByte()) // overlong illegal utf8 sequence
        segment2.writeByte(0x81.toByte())

        val packet = ByteReadPacket(segment1, pool)

        try {
            packet.readText(charset = Charsets.UTF_8)
            fail("Decode illegal characters should fail")
        } catch (expected: MalformedInputException) {
//            assertEquals(2, packet.remaining) // impossible on comply in JS
        } finally {
            packet.release()
        }
    }

    private inline fun buildPacket(startGap: Int = 0, block: BytePacketBuilder.() -> Unit): ByteReadPacket {
        val builder = BytePacketBuilder(startGap, pool)
        try {
            block(builder)
            return builder.build()
        } finally {
            builder.release()
        }
    }
}
