package kotlinx.io.tests

import kotlinx.io.core.*
import kotlinx.io.streams.*
import org.junit.Test
import org.junit.Rule
import java.util.*
import kotlin.test.*

class BytePacketReaderWriterTest {
    @get:Rule
    internal val pool = VerifyingObjectPool(BufferView.Pool)

    @Test
    fun testReaderEmpty() {
        val packet = buildPacket {
        }

        assertEquals(-1, packet.readerUTF8().read())
    }

    @Test
    fun testReaderFew() {
        val packet = buildPacket {
            append("ABC")
        }

        assertEquals("ABC", packet.readerUTF8().readText())
    }

    @Test
    fun testReaderMultiple() {
        val s = buildString {
            repeat(100000) {
                this.append("e")
            }
        }

        val packet = buildPacket {
            append(s)
        }

        assertEquals(s, packet.readerUTF8().readText())
    }

    @Test
    fun testReaderFewUtf() {
        val s = "\u0447"
        val packet = buildPacket {
            append(s)
        }

        assertEquals(s, packet.readerUTF8().readText())
    }

    @Test
    fun testReaderFewUtf3bytes() {
        val s = "\u0BF5"
        val packet = buildPacket {
            append(s)
        }

        assertEquals(s, packet.readerUTF8().readText())
    }

    @Test
    fun testReaderMultipleUtf() {
        val s = buildString {
            repeat(100000) {
                append("\u0447")
            }
        }

        val packet = buildPacket {
            append(s)
        }

        assertEquals(s, packet.readerUTF8().readText())
    }

    @Test
    fun testReaderMultipleUtf3bytes() {
        val s = buildString {
            repeat(100000) {
                append("\u0BF5")
            }
        }

        val packet = buildPacket {
            append(s)
        }

        assertEquals(s, packet.readerUTF8().readText())
    }

    @Test
    fun testWriterSingleBufferSingleWrite() {
        val s = buildString {
            append("ABC")
        }

        val packet = buildPacket {
            writerUTF8().write(s)
        }

        assertEquals(s, packet.inputStream().readBytes().toString(Charsets.UTF_8))
    }

    @Test
    fun testWriterSingleBufferSingleWriteUtf() {
        val s = buildString {
            append("A\u0447C")
        }

        val packet = buildPacket {
            writerUTF8().write(s)
        }

        assertEquals(s, packet.inputStream().readBytes().toString(Charsets.UTF_8))
    }

    @Test
    fun testWriterSingleBufferMultipleWrite() {
        val s = buildString {
            append("ABC")
        }

        val packet = buildPacket {
            writerUTF8().apply {
                write(s.substring(0, 1))
                write(s.substring(1))
            }
        }

        assertEquals(s, packet.inputStream().readBytes().toString(Charsets.UTF_8))
    }

    @Test
    fun testWriterSingleBufferMultipleWriteUtf() {
        val s = buildString {
            append("\u0447BC")
            append("A\u0447C")
            append("AB\u0447")
            append("\u0447")
        }

        val packet = buildPacket {
            writerUTF8().let { w ->
                w.write("\u0447BC")
                w.write("A\u0447C")
                w.write("AB\u0447")
                w.write("\u0447")
            }
        }

        assertEquals(s, packet.inputStream().readBytes().toString(Charsets.UTF_8))
    }

    @Test
    fun testWriterMultiBufferSingleWrite() {
        val s = buildString {
            repeat(100000) {
                append("x")
            }
        }

        val packet = buildPacket {
            writerUTF8().write(s)
        }

        assertEquals(s, packet.inputStream().readBytes().toString(Charsets.UTF_8))
    }

    @Test
    fun testWriterMultiBufferSingleWriteUtf() {
        val s = buildString {
            repeat(100000) {
                append("A\u0447")
            }
        }

        val packet = buildPacket {
            writerUTF8().write(s)
        }

        assertEquals(s, packet.inputStream().readBytes().toString(Charsets.UTF_8))
    }

    @Test
    fun testWriterMultiBufferSingleWriteUtf3bytes() {
        val s = buildString {
            repeat(100000) {
                append("\u0BF5")
            }
        }

        val packet = buildPacket {
            writerUTF8().write(s)
        }

        assertEquals(s, packet.inputStream().readBytes().toString(Charsets.UTF_8))
    }



    @Test
    fun testSingleBufferReadAll() {
        val bb = ByteArray(100)
        Random().nextBytes(bb)

        val p = buildPacket {
            writeFully(bb)
        }

        assertTrue { bb.contentEquals(p.readBytes()) }
    }

    @Test
    fun testMultiBufferReadAll() {
        val bb = ByteArray(100000)
        Random().nextBytes(bb)

        val p = buildPacket {
            writeFully(bb)
        }

        assertTrue { bb.contentEquals(p.readBytes()) }
    }

    @Test
    fun testCopySingleBufferPacket() {
        val bb = ByteArray(100)
        Random().nextBytes(bb)

        val p = buildPacket {
            writeFully(bb)
        }

        val copy = p.copy()
        assertEquals(p.remaining, p.remaining)
        assertTrue { p.readBytes().contentEquals(copy.readBytes()) }
    }

    @Test
    fun testCopyMultipleBufferPacket() {
        val bb = ByteArray(1000000)
        Random().nextBytes(bb)

        val p = buildPacket {
            writeFully(bb)
        }

        val copy = p.copy()
        assertEquals(p.remaining, p.remaining)
        val bytes = p.readBytes()
        val copied = copy.readBytes()

        assertTrue { bytes.contentEquals(copied) }
    }

    @Test
    fun testWritePacketSingle() {
        val inner = buildPacket {
            append("ABC")
            assertEquals(3, size)
        }

        val outer = buildPacket {
            append("123")
            kotlin.test.assertEquals(3, size)
            writePacket(inner)
            kotlin.test.assertEquals(6, size)
            append(".")
        }

        assertEquals("123ABC.", outer.readText())
        assertEquals(0, inner.remaining)
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
    fun testWriteDirect() {
        val packet = buildPacket {
            writeDirect(8) { bb ->
                bb.putLong(0x1234567812345678L)
            }
        }

        assertEquals(0x1234567812345678L, packet.readLong())
    }

    @Test
    fun testReadText() {
        val packet = buildPacket {
            writeByte(0xc6.toByte())
            writeByte(0x86.toByte())
        }

        assertEquals("\u0186", packet.readText(decoder = Charsets.UTF_8.newDecoder()))
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

        assertEquals("\u0186", packet.readText(decoder = Charsets.UTF_8.newDecoder(), max = 1))
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

        assertEquals("\u0186", packet.readText(decoder = Charsets.UTF_8.newDecoder()))
        assertTrue { packet.isEmpty }
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

    private inline fun buildString(block: StringBuilder.() -> Unit) = StringBuilder().apply(block).toString()

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            BytePacketReaderWriterTest().testWriteDirect()
        }
    }
}
