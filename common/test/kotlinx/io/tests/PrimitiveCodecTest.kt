package kotlinx.io.tests

import kotlinx.io.core.*
import kotlin.math.*
import kotlin.test.*

class PrimitiveCodecTest {
    val pool = VerifyingObjectPool(IoBuffer.Pool)
    val builder = BytePacketBuilder(0, pool)

    @AfterTest
    fun tearDown() {
        try {
            pool.assertEmpty()
        } finally {
            pool.dispose()
        }
    }

    @Test
    fun testSingleByte() {
        builder.writeByte(7)
        assertEquals(1, builder.size)
        val packet = builder.build()
        assertEquals(1, packet.remaining)
        assertEquals(7, packet.readByte())
        assertEquals(0, packet.remaining)
        assertTrue { packet.isEmpty }
    }

    @Test
    fun testWriteShortLE() {
        builder.byteOrder = ByteOrder.LITTLE_ENDIAN
        builder.writeShort(0x1100)
        assertEquals(2, builder.size)
        val p = builder.build()

        assertEquals(2, p.remaining)
        assertTrue { p.isNotEmpty }
        assertEquals(0, p.readByte())
        assertEquals(0x11, p.readByte())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testWriteShortBE() {
        builder.byteOrder = ByteOrder.BIG_ENDIAN
        builder.writeShort(0x1100)
        assertEquals(2, builder.size)
        val p = builder.build()

        assertEquals(2, p.remaining)
        assertTrue { p.isNotEmpty }
        assertEquals(0x11, p.readByte())
        assertEquals(0x00, p.readByte())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testReadShortLE() {
        builder.byteOrder = ByteOrder.LITTLE_ENDIAN
        builder.writeByte(0x11)
        builder.writeByte(0x22)
        assertEquals(2, builder.size)
        val p = builder.build()
        assertEquals(2, p.remaining)
        assertTrue { p.isNotEmpty }

        p.byteOrder = ByteOrder.LITTLE_ENDIAN
        assertEquals(0x2211, p.readShort())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testReadShortBE() {
        builder.byteOrder = ByteOrder.BIG_ENDIAN
        builder.writeByte(0x11)
        builder.writeByte(0x22)
        assertEquals(2, builder.size)
        val p = builder.build()
        assertEquals(2, p.remaining)
        assertTrue { p.isNotEmpty }

        assertEquals(0x1122, p.readShort())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testWriteIntLE() {
        builder.byteOrder = ByteOrder.LITTLE_ENDIAN
        builder.writeInt(0x11223344)
        assertEquals(4, builder.size)
        val p = builder.build()
        assertEquals(4, p.remaining)
        assertTrue { p.isNotEmpty }

        assertEquals(0x44, p.readByte())
        assertEquals(0x33, p.readByte())
        assertEquals(0x22, p.readByte())
        assertEquals(0x11, p.readByte())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testWriteIntBE() {
        builder.byteOrder = ByteOrder.BIG_ENDIAN
        builder.writeInt(0x11223344)
        assertEquals(4, builder.size)
        val p = builder.build()
        assertEquals(4, p.remaining)
        assertTrue { p.isNotEmpty }

        assertEquals(0x11, p.readByte())
        assertEquals(0x22, p.readByte())
        assertEquals(0x33, p.readByte())
        assertEquals(0x44, p.readByte())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testReadIntLE() {
        builder.byteOrder = ByteOrder.LITTLE_ENDIAN
        builder.writeByte(0x11)
        builder.writeByte(0x22)
        builder.writeByte(0x33)
        builder.writeByte(0x44)
        assertEquals(4, builder.size)
        val p = builder.build()
        assertEquals(4, p.remaining)
        assertTrue { p.isNotEmpty }

        p.byteOrder = ByteOrder.LITTLE_ENDIAN
        assertEquals(0x44332211, p.readInt())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testReadIntBE() {
        builder.byteOrder = ByteOrder.BIG_ENDIAN
        builder.writeByte(0x11)
        builder.writeByte(0x22)
        builder.writeByte(0x33)
        builder.writeByte(0x44)
        assertEquals(4, builder.size)
        val p = builder.build()
        assertEquals(4, p.remaining)
        assertTrue { p.isNotEmpty }

        assertEquals(0x11223344, p.readInt())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testWriteLongLE() {
        builder.byteOrder = ByteOrder.LITTLE_ENDIAN
        builder.writeLong(0x112233440a0b0c0dL)
        assertEquals(8, builder.size)
        val p = builder.build()
        assertEquals(8, p.remaining)
        assertTrue { p.isNotEmpty }

        assertEquals(0x0d, p.readByte())
        assertEquals(0x0c, p.readByte())
        assertEquals(0x0b, p.readByte())
        assertEquals(0x0a, p.readByte())
        assertEquals(0x44, p.readByte())
        assertEquals(0x33, p.readByte())
        assertEquals(0x22, p.readByte())
        assertEquals(0x11, p.readByte())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testWriteLongBE() {
        builder.byteOrder = ByteOrder.BIG_ENDIAN
        builder.writeLong(0x112233440a0b0c0dL)
        assertEquals(8, builder.size)
        val p = builder.build()
        assertEquals(8, p.remaining)
        assertTrue { p.isNotEmpty }

        assertEquals(0x11, p.readByte())
        assertEquals(0x22, p.readByte())
        assertEquals(0x33, p.readByte())
        assertEquals(0x44, p.readByte())
        assertEquals(0x0a, p.readByte())
        assertEquals(0x0b, p.readByte())
        assertEquals(0x0c, p.readByte())
        assertEquals(0x0d, p.readByte())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testReadLongLE() {
        builder.byteOrder = ByteOrder.LITTLE_ENDIAN
        builder.writeByte(0x11)
        builder.writeByte(0x22)
        builder.writeByte(0x33)
        builder.writeByte(0x44)
        builder.writeByte(0x0a)
        builder.writeByte(0x0b)
        builder.writeByte(0x0c)
        builder.writeByte(0x0d)
        assertEquals(8, builder.size)
        val p = builder.build()
        assertEquals(8, p.remaining)
        assertTrue { p.isNotEmpty }

        p.byteOrder = ByteOrder.LITTLE_ENDIAN
        assertEquals(0x0d0c0b0a44332211L, p.readLong())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testReadLongBE() {
        builder.byteOrder = ByteOrder.BIG_ENDIAN
        builder.writeByte(0x11)
        builder.writeByte(0x22)
        builder.writeByte(0x33)
        builder.writeByte(0x44)
        builder.writeByte(0x0a)
        builder.writeByte(0x0b)
        builder.writeByte(0x0c)
        builder.writeByte(0x0d)
        assertEquals(8, builder.size)
        val p = builder.build()
        assertEquals(8, p.remaining)
        assertTrue { p.isNotEmpty }

        assertEquals(0x112233440a0b0c0dL, p.readLong())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testWriteFloatLE() {
        builder.byteOrder = ByteOrder.LITTLE_ENDIAN
        builder.writeFloat(0.05f)
        assertEquals(4, builder.size)
        val p = builder.build()
        assertEquals(4, p.remaining)
        assertTrue { p.isNotEmpty }

        assertEquals(-51, p.readByte())
        assertEquals(-52, p.readByte())
        assertEquals(76, p.readByte())
        assertEquals(61, p.readByte())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testWriteFloatBE() {
        builder.byteOrder = ByteOrder.BIG_ENDIAN
        builder.writeFloat(0.05f)
        assertEquals(4, builder.size)
        val p = builder.build()
        assertEquals(4, p.remaining)
        assertTrue { p.isNotEmpty }

        assertEquals(61, p.readByte())
        assertEquals(76, p.readByte())
        assertEquals(-52, p.readByte())
        assertEquals(-51, p.readByte())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testReadFloatLE() {
        builder.byteOrder = ByteOrder.LITTLE_ENDIAN
        builder.writeByte(-51)
        builder.writeByte(-52)
        builder.writeByte(76)
        builder.writeByte(61)
        assertEquals(4, builder.size)
        val p = builder.build()
        assertEquals(4, p.remaining)
        assertTrue { p.isNotEmpty }

        p.byteOrder = ByteOrder.LITTLE_ENDIAN
        assertEquals(5, (p.readFloat() * 100.0f).roundToInt())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testReadFloatBE() {
        builder.byteOrder = ByteOrder.BIG_ENDIAN
        builder.writeByte(61)
        builder.writeByte(76)
        builder.writeByte(-52)
        builder.writeByte(-51)
        assertEquals(4, builder.size)
        val p = builder.build()
        assertEquals(4, p.remaining)
        assertTrue { p.isNotEmpty }

        assertEquals(5, (p.readFloat() * 100.0f).roundToInt())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testWriteDoubleLE() {
        builder.byteOrder = ByteOrder.LITTLE_ENDIAN
        builder.writeDouble(0.05)
        assertEquals(8, builder.size)
        val p = builder.build()
        assertEquals(8, p.remaining)
        assertTrue { p.isNotEmpty }

        assertEquals(0x9a.toByte(), p.readByte())
        assertEquals(0x99.toByte(), p.readByte())
        assertEquals(0x99.toByte(), p.readByte())
        assertEquals(0x99.toByte(), p.readByte())
        assertEquals(0x99.toByte(), p.readByte())
        assertEquals(0x99.toByte(), p.readByte())
        assertEquals(0xa9.toByte(), p.readByte())
        assertEquals(0x3f.toByte(), p.readByte())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testWriteDoubleBE() {
        builder.byteOrder = ByteOrder.BIG_ENDIAN
        builder.writeDouble(0.05)
        assertEquals(8, builder.size)
        val p = builder.build()
        assertEquals(8, p.remaining)
        assertTrue { p.isNotEmpty }

        assertEquals(0x3f.toByte(), p.readByte())
        assertEquals(0xa9.toByte(), p.readByte())
        assertEquals(0x99.toByte(), p.readByte())
        assertEquals(0x99.toByte(), p.readByte())
        assertEquals(0x99.toByte(), p.readByte())
        assertEquals(0x99.toByte(), p.readByte())
        assertEquals(0x99.toByte(), p.readByte())
        assertEquals(0x9a.toByte(), p.readByte())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testReadDoubleLE() {
        builder.byteOrder = ByteOrder.LITTLE_ENDIAN
        builder.writeByte(0x9a.toByte())
        builder.writeByte(0x99.toByte())
        builder.writeByte(0x99.toByte())
        builder.writeByte(0x99.toByte())
        builder.writeByte(0x99.toByte())
        builder.writeByte(0x99.toByte())
        builder.writeByte(0xa9.toByte())
        builder.writeByte(0x3f.toByte())
        assertEquals(8, builder.size)
        val p = builder.build()
        assertEquals(8, p.remaining)
        assertTrue { p.isNotEmpty }

        p.byteOrder = ByteOrder.LITTLE_ENDIAN
        assertEquals(0.05, p.readDouble())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }

    @Test
    fun testReadDoubleBE() {
        builder.byteOrder = ByteOrder.BIG_ENDIAN
        builder.writeByte(0x3f.toByte())
        builder.writeByte(0xa9.toByte())
        builder.writeByte(0x99.toByte())
        builder.writeByte(0x99.toByte())
        builder.writeByte(0x99.toByte())
        builder.writeByte(0x99.toByte())
        builder.writeByte(0x99.toByte())
        builder.writeByte(0x9a.toByte())
        assertEquals(8, builder.size)
        val p = builder.build()
        assertEquals(8, p.remaining)
        assertTrue { p.isNotEmpty }

        assertEquals(0.05, p.readDouble())
        assertEquals(0, p.remaining)
        assertTrue { p.isEmpty }
    }
}
