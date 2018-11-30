package kotlinx.io.tests

import kotlinx.io.core.*
import kotlin.test.*

class TakeWhileTest {
    private val pool = VerifyingObjectPool(IoBuffer.NoPool)
    private val ch1 = pool.borrow()
    private val ch2 = pool.borrow()

    private val chunks = ArrayList<IoBuffer>()
    private val packets = ArrayList<ByteReadPacket>()

    @BeforeTest
    fun prepare() {
        ch1.resetForWrite()
        ch2.resetForWrite()

        ch1.reserveEndGap(8)
        ch2.reserveEndGap(8)

        ch1.next = ch2

        chunks.add(ch1)
        chunks.add(ch2)
    }

    @AfterTest
    fun release() {
        chunks.forEach {
            it.release(pool)
        }
        packets.forEach {
            it.release()
        }

        pool.assertEmpty()
    }

    @Test
    fun takeWhileSizeAtEdgeNotConsumed() {
        ch1.writeFully(cycle(ch1.writeRemaining))
        ch2.writeFully(cycle(3))

        val pkt = ByteReadPacket(ch1, pool)
        packets.add(pkt)
        chunks.remove(ch1)
        chunks.remove(ch2)

        pkt.discardExact(ch1.readRemaining - 1)

        assertEquals(4, pkt.remaining)
        assertEquals(1, ch1.readRemaining)
        assertEquals(3, ch2.readRemaining)

        pkt.takeWhileSize(4) { 0 }
        assertEquals(4, pkt.remaining)
    }

    @Test
    fun takeWhileSizeAtEdgeNotConsumed2() {
        ch1.writeFully(cycle(ch1.writeRemaining))
        ch2.writeFully(cycle(10))

        val pkt = ByteReadPacket(ch1, pool)
        packets.add(pkt)
        chunks.remove(ch1)
        chunks.remove(ch2)

        pkt.discardExact(ch1.readRemaining - 1)

        assertEquals(11, pkt.remaining)
        assertEquals(1, ch1.readRemaining)
        assertEquals(10, ch2.readRemaining)

        pkt.takeWhileSize(8) { it.discard(7); 0 }

        assertNotNull(ch1.next)
        assertEquals(4, pkt.remaining)

        pkt.takeWhileSize(4) { 0 }

        assertEquals(4, pkt.remaining)
    }

    private fun cycle(size: Int) = ByteArray(size) { (it and 0xff).toByte() }
}
