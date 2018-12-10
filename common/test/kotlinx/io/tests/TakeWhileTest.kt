package kotlinx.io.tests

import kotlinx.io.core.*
import kotlin.test.*

class TakeWhileTest {
    private val pool = VerifyingObjectPool(IoBuffer.NoPool)
    private val chunk1 = pool.borrow()
    private val chunk2 = pool.borrow()

    private val chunks = ArrayList<IoBuffer>()
    private val packets = ArrayList<ByteReadPacket>()

    @BeforeTest
    fun prepare() {
        chunk1.resetForWrite()
        chunk2.resetForWrite()

        chunk1.reserveEndGap(8)
        chunk2.reserveEndGap(8)

        chunk1.next = chunk2

        chunks.add(chunk1)
        chunks.add(chunk2)
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
        chunk1.writeFully(cycle(chunk1.writeRemaining))
        chunk2.writeFully(cycle(3))

        val pkt = ByteReadPacket(chunk1, pool)
        packets.add(pkt)
        chunks.remove(chunk1)
        chunks.remove(chunk2)

        pkt.discardExact(chunk1.readRemaining - 1)

        assertEquals(4, pkt.remaining)
        assertEquals(1, chunk1.readRemaining)
        assertEquals(3, chunk2.readRemaining)

        pkt.takeWhileSize(4) { 0 }
        assertEquals(4, pkt.remaining)
    }

    @Test
    fun takeWhileSizeAtEdgeNotConsumed2() {
        chunk1.writeFully(cycle(chunk1.writeRemaining))
        chunk2.writeFully(cycle(10))

        val pkt = ByteReadPacket(chunk1, pool)
        packets.add(pkt)
        chunks.remove(chunk1)
        chunks.remove(chunk2)

        pkt.discardExact(chunk1.readRemaining - 1)

        assertEquals(11, pkt.remaining)
        assertEquals(1, chunk1.readRemaining)
        assertEquals(10, chunk2.readRemaining)

        pkt.takeWhileSize(8) { it.discard(7); 0 }

        assertNotNull(chunk1.next)
        assertEquals(4, pkt.remaining)

        pkt.takeWhileSize(4) { 0 }

        assertEquals(4, pkt.remaining)
    }

    @Test
    fun testTakeWhileFromByteArrayPacket1() {
        val content = byteArrayOf(0x31, 0x32, 0x33, 0x00)
        val packet = ByteReadPacket(content)

        packet.takeWhileSize { it.discardExact(3); 0 }
        assertEquals(1, packet.remaining)
        packet.discardExact(1)
    }

    @Test
    fun testTakeWhileFromByteArrayPacket2() {
        val content = byteArrayOf(0x31, 0x32, 0x33, 0x00)
        val packet = ByteReadPacket(content)

        packet.takeWhileSize { it.discardExact(4); 0 }
        assertEquals(0, packet.remaining)
    }

    @Test
    fun testTakeWhileFromByteArrayPacket3() {
        val content = byteArrayOf(0x31, 0x32, 0x33, 0x00)
        val packet = ByteReadPacket(content)

        packet.takeWhileSize { 0 }
        assertEquals(4, packet.remaining)
        packet.discardExact(4)
    }

    @Test
    @Ignore
    fun testLong() {
        val goldenCopy = cycle(8192)
        for (discardBefore in 0..8192) {
            buildPacket {
                writeFully(goldenCopy)
            }.use { pkt ->
                pkt.discardExact(discardBefore)
                val expected = goldenCopy.copyOfRange(discardBefore, goldenCopy.size)

                for (initialSize in 1..8) {
                    for (consume in 0..initialSize) {
                        val actual = buildPacket {
                            pkt.copy().use { copy ->
                                copy.takeWhileSize(initialSize) { buffer ->
                                    val consumed = buffer.readBytes(consume)
                                    writeFully(consumed)
                                    if (consume == 0) 0 else initialSize
                                }

                                val remainingBytes = copy.readBytes()
                                writeFully(remainingBytes)
                            }
                        }.readBytes()

                        val equals = actual.contentEquals(expected)
                        assertTrue(equals)
                    }
                }
            }
        }
    }

    private fun cycle(size: Int) = ByteArray(size) { (it and 0xff).toByte() }
}
