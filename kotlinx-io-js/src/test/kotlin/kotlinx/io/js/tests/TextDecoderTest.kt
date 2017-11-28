package kotlinx.io.js.tests

import kotlinx.io.core.*
import kotlinx.io.js.*
import kotlin.test.*

class TextDecoderTest {
    private val pool = BufferView.NoPool

    @Test
    fun testReadText() {
        val packet = buildPacket {
            writeByte(0xc6.toByte())
            writeByte(0x86.toByte())
        }

        assertEquals("\u0186", packet.readText(encoding = "UTF-8"))
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

        assertEquals("\u0186", packet.readText(encoding = "UTF-8", max = 1))
        assertEquals(2, packet.remaining)
        packet.release()
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

        assertEquals("\u0186", packet.readText(encoding = "UTF-8"))
        assertTrue { packet.isEmpty }
    }
}