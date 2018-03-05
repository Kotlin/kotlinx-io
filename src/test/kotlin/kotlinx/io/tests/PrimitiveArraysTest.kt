package kotlinx.io.tests

import kotlinx.io.core.*
import kotlin.test.*

class PrimitiveArraysTest {
    private val view = BufferView.NoPool.borrow()
    private val i8 = byteArrayOf(-15, 0, 1, -1, 127)
    private val i16 = shortArrayOf(-15, 0, 1, 0xff, 0xffff.toShort(), 0xceff.toShort())
    private val i32 = intArrayOf(-15, 0, 1, 0xff, 0xffff, 0xffffffff.toInt(), 0xceffffff.toInt())
    private val i64 = longArrayOf(-15, 0, 1, 0xff, 0xffff, 0xffffffff, 0xceffffff, -1L)
    private val f32 = floatArrayOf(1.0f, 0.5f, -1.0f)
    private val f64 = doubleArrayOf(1.0, 0.5, -1.0)

    @Test
    fun testWriteByteArray() {
        try {
            val array = i8
            val size = 1

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeFully(array, 0, array.size)

            assertEquals(array.size * size, view.readRemaining)
            assertEquals("f10001ff7f", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteByteArrayRange() {
        try {
            val array = i8

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeFully(array, 1, array.size - 2)

            assertEquals("0001ff", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadByteArray() {
        try {
            val array = i8
            val size = 1

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeHex("f10001ff7f")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = ByteArray(array.size)
            view.readFully(tmp, 0, tmp.size)
            assertTrue { tmp.contentEquals(array) }
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadByteArrayRange() {
        try {
            val array = i8
            val size = 1

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeHex("f10001ff7f")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = ByteArray(array.size + 2)
            fill(tmp)
            view.readFully(tmp, 1, tmp.size - 2)
            compareSubRange(tmp)
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteShortArrayBE() {
        try {
            val array = i16
            val size = 2

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeFully(array, 0, array.size)

            assertEquals(array.size * size, view.readRemaining)
            assertEquals("fff10000000100ffffffceff", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteShortArrayBERange() {
        try {
            val array = i16

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeFully(array, 1, array.size - 2)

            assertEquals("0000000100ffffff", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadShortArrayBE() {
        try {
            val array = i16
            val size = 2

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeHex("fff10000000100ffffffceff")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = ShortArray(array.size)
            view.readFully(tmp, 0, tmp.size)
            assertTrue { tmp.contentEquals(array) }
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadShortArrayRangeBE() {
        try {
            val array = i16
            val size = 2

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeHex("fff10000000100ffffffceff")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = ShortArray(array.size + 2)
            fill(tmp)
            view.readFully(tmp, 1, tmp.size - 2)
            compareSubRange(tmp)
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteShortArrayLE() {
        try {
            val array = i16
            val size = 2

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeFully(array, 0, array.size)

            assertEquals(array.size * size, view.readRemaining)
            assertEquals("f1ff00000100ff00ffffffce", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteShortArrayLERange() {
        try {
            val array = i16

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeFully(array, 1, array.size - 2)

            assertEquals("00000100ff00ffff", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadShortArrayLE() {
        try {
            val array = i16
            val size = 2

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeHex("f1ff00000100ff00ffffffce")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = ShortArray(array.size)
            view.readFully(tmp, 0, tmp.size)
            assertTrue { tmp.contentEquals(array) }
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadShortArrayRangeLE() {
        try {
            val array = i16
            val size = 2

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeHex("f1ff00000100ff00ffffffce")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = ShortArray(array.size + 2)
            fill(tmp)
            view.readFully(tmp, 1, tmp.size - 2)
            compareSubRange(tmp)
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteIntArrayBE() {
        try {
            val array = i32
            val size = 4

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeFully(array, 0, array.size)

            assertEquals(array.size * size, view.readRemaining)
            assertEquals("fffffff10000000000000001000000ff0000ffffffffffffceffffff", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteIntArrayBERange() {
        try {
            val array = i32

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeFully(array, 1, array.size - 2)

            assertEquals("0000000000000001000000ff0000ffffffffffff", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadIntArrayBE() {
        try {
            val array = i32
            val size = 4

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeHex("fffffff10000000000000001000000ff0000ffffffffffffceffffff")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = IntArray(array.size)
            view.readFully(tmp, 0, tmp.size)
            assertTrue { tmp.contentEquals(array) }
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadIntArrayRangeBE() {
        try {
            val array = i32
            val size = 4

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeHex("fffffff10000000000000001000000ff0000ffffffffffffceffffff")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = IntArray(array.size + 2)
            fill(tmp)
            view.readFully(tmp, 1, tmp.size - 2)
            compareSubRange(tmp)
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteIntArrayLE() {
        try {
            val array = i32
            val size = 4

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeFully(array, 0, array.size)

            assertEquals(array.size * size, view.readRemaining)
            assertEquals("f1ffffff0000000001000000ff000000ffff0000ffffffffffffffce", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteIntArrayLERange() {
        try {
            val array = i32

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeFully(array, 1, array.size - 2)

            assertEquals("0000000001000000ff000000ffff0000ffffffff", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadIntArrayLE() {
        try {
            val array = i32
            val size = 4

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeHex("f1ffffff0000000001000000ff000000ffff0000ffffffffffffffce")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = IntArray(array.size)
            view.readFully(tmp, 0, tmp.size)
            assertTrue { tmp.contentEquals(array) }
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadIntArrayRangeLE() {
        try {
            val array = i32
            val size = 4

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeHex("f1ffffff0000000001000000ff000000ffff0000ffffffffffffffce")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = IntArray(array.size + 2)
            fill(tmp)
            view.readFully(tmp, 1, tmp.size - 2)
            compareSubRange(tmp)
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteLongArrayBE() {
        try {
            val array = i64
            val size = 8

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeFully(array, 0, array.size)

            assertEquals(array.size * size, view.readRemaining)
            assertEquals("fffffffffffffff10000000000000000000000000000000100000000000000ff000000000000ffff00000000ffffffff00000000ceffffffffffffffffffffff", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteLongArrayBERange() {
        try {
            val array = i64

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeFully(array, 1, array.size - 2)

            assertEquals("0000000000000000000000000000000100000000000000ff000000000000ffff00000000ffffffff00000000ceffffff", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testLongIntArrayBE() {
        try {
            val array = i64
            val size = 8

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeHex("fffffffffffffff10000000000000000000000000000000100000000000000ff000000000000ffff00000000ffffffff00000000ceffffffffffffffffffffff")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = LongArray(array.size)
            view.readFully(tmp, 0, tmp.size)
            assertTrue { tmp.contentEquals(array) }
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadLongArrayRangeBE() {
        try {
            val array = i64
            val size = 8

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeHex("fffffffffffffff10000000000000000000000000000000100000000000000ff000000000000ffff00000000ffffffff00000000ceffffffffffffffffffffff")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = LongArray(array.size + 2)
            fill(tmp)
            view.readFully(tmp, 1, tmp.size - 2)
            compareSubRange(tmp)
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteLongArrayLE() {
        try {
            val array = i64
            val size = 8

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeFully(array, 0, array.size)

            assertEquals(array.size * size, view.readRemaining)
            assertEquals("f1ffffffffffffff00000000000000000100000000000000ff00000000000000ffff000000000000ffffffff00000000ffffffce00000000ffffffffffffffff", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteLongArrayLERange() {
        try {
            val array = i64

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeFully(array, 1, array.size - 2)

            assertEquals("00000000000000000100000000000000ff00000000000000ffff000000000000ffffffff00000000ffffffce00000000", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadLongArrayLE() {
        try {
            val array = i64
            val size = 8

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeHex("f1ffffffffffffff00000000000000000100000000000000ff00000000000000ffff000000000000ffffffff00000000ffffffce00000000ffffffffffffffff")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = LongArray(array.size)
            view.readFully(tmp, 0, tmp.size)
            assertTrue { tmp.contentEquals(array) }
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadLongArrayRangeLE() {
        try {
            val array = i64
            val size = 8

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeHex("f1ffffffffffffff00000000000000000100000000000000ff00000000000000ffff000000000000ffffffff00000000ffffffce00000000ffffffffffffffff")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = LongArray(array.size + 2)
            fill(tmp)
            view.readFully(tmp, 1, tmp.size - 2)
            compareSubRange(tmp)
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteFloatArrayBE() {
        try {
            val array = f32
            val size = 4

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeFully(array, 0, array.size)

            assertEquals(array.size * size, view.readRemaining)
            assertEquals("3f8000003f000000bf800000", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteFloatArrayBERange() {
        try {
            val array = f32

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeFully(array, 1, array.size - 2)

            assertEquals("3f000000", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadFloatArrayBE() {
        try {
            val array = f32
            val size = 4

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeHex("3f8000003f000000bf800000")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = FloatArray(array.size)
            view.readFully(tmp, 0, tmp.size)
            assertTrue { tmp.contentEquals(array) }
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadFloatArrayRangeBE() {
        try {
            val array = f32
            val size = 4

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeHex("3f8000003f000000bf800000")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = FloatArray(array.size + 2)
            fill(tmp)
            view.readFully(tmp, 1, tmp.size - 2)
            compareSubRange(tmp)
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteFloatArrayLE() {
        try {
            val array = f32
            val size = 4

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeFully(array, 0, array.size)

            assertEquals(array.size * size, view.readRemaining)
            assertEquals("0000803f0000003f000080bf", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteFloatArrayLERange() {
        try {
            val array = f32

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeFully(array, 1, array.size - 2)

            assertEquals("0000003f", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadFloatArrayLE() {
        try {
            val array = f32
            val size = 4

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeHex("0000803f0000003f000080bf")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = FloatArray(array.size)
            view.readFully(tmp, 0, tmp.size)
            assertTrue { tmp.contentEquals(array) }
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadFloatArrayRangeLE() {
        try {
            val array = f32
            val size = 4

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeHex("0000803f0000003f000080bf")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = FloatArray(array.size + 2)
            fill(tmp)
            view.readFully(tmp, 1, tmp.size - 2)
            compareSubRange(tmp)
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteDoubleArrayBE() {
        try {
            val array = f64
            val size = 8

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeFully(array, 0, array.size)

            assertEquals(array.size * size, view.readRemaining)
            assertEquals("3ff00000000000003fe0000000000000bff0000000000000", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteDoubleArrayBERange() {
        try {
            val array = f64

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeFully(array, 1, array.size - 2)

            assertEquals("3fe0000000000000", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadDoubleArrayBE() {
        try {
            val array = f64
            val size = 8

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeHex("3ff00000000000003fe0000000000000bff0000000000000")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = DoubleArray(array.size)
            view.readFully(tmp, 0, tmp.size)
            assertTrue { tmp.contentEquals(array) }
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadDoubleArrayRangeBE() {
        try {
            val array = f64
            val size = 8

            view.byteOrder = ByteOrder.BIG_ENDIAN
            view.writeHex("3ff00000000000003fe0000000000000bff0000000000000")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = DoubleArray(array.size + 2)
            fill(tmp)
            view.readFully(tmp, 1, tmp.size - 2)
            compareSubRange(tmp)
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteDoubleArrayLE() {
        try {
            val array = f64
            val size = 8

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeFully(array, 0, array.size)

            assertEquals(array.size * size, view.readRemaining)
            assertEquals("000000000000f03f000000000000e03f000000000000f0bf", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testWriteDoubleArrayLERange() {
        try {
            val array = f64

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeFully(array, 1, array.size - 2)

            assertEquals("000000000000e03f", view.readHex())
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadDoubleArrayLE() {
        try {
            val array = f64
            val size = 8

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeHex("000000000000f03f000000000000e03f000000000000f0bf")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = DoubleArray(array.size)
            view.readFully(tmp, 0, tmp.size)
            assertTrue { tmp.contentEquals(array) }
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    @Test
    fun testReadDoubleArrayRangeLE() {
        try {
            val array = f64
            val size = 8

            view.byteOrder = ByteOrder.LITTLE_ENDIAN
            view.writeHex("000000000000f03f000000000000e03f000000000000f0bf")

            assertEquals(array.size * size, view.readRemaining)
            val tmp = DoubleArray(array.size + 2)
            fill(tmp)
            view.readFully(tmp, 1, tmp.size - 2)
            compareSubRange(tmp)
        } finally {
            view.release(BufferView.NoPool)
        }
    }

    private fun BufferView.readHex() = buildString(readRemaining * 2) {
        repeat(readRemaining) {
            val i = readByte().toInt() and 0xff
            val l = i shr 4
            val r = i and 0x0f

            appendDigit(l)
            appendDigit(r)
        }
    }

    private fun StringBuilder.appendDigit(d: Int) {
        require(d < 16) { "digit $d should be in [0..15]" }
        require(d >= 0) { "digit $d should be in [0..15]" }

        if (d < 10) append('0' + d)
        else append('a' + (d - 10))
    }

    private fun BufferView.writeHex(hex: CharSequence) {
        for (idx in 0 .. hex.length - 2 step 2) {
            val l = unhex(hex[idx])
            val r = unhex(hex[idx + 1])

            writeByte((l shl 4 or r).toByte())
        }
    }

    private fun fill(array: ByteArray) {
        for (i in array.indices) {
            array[i] = 0xee.toByte()
        }
    }

    private fun fill(array: ShortArray) {
        for (i in array.indices) {
            array[i] = 0xeeee.toShort()
        }
    }

    private fun fill(array: IntArray) {
        for (i in array.indices) {
            array[i] = 0xeeeeeeee.toInt()
        }
    }

    private fun fill(array: LongArray) {
        for (i in array.indices) {
            array[i] = 0x0eeeeeeeeeeeeeeeL
        }
    }

    private fun fill(array: FloatArray) {
        for (i in array.indices) {
            array[i] = Float.fromBits(0xeeeeeeee.toInt())
        }
    }

    private fun fill(array: DoubleArray) {
        for (i in array.indices) {
            array[i] = Double.fromBits(0x0eeeeeeeeeeeeeeeL)
        }
    }

    private fun compareSubRange(readBuffer: ByteArray) {
        assertEquals(0xee, readBuffer[0].toInt() and 0xff)
        assertEquals(0xee, readBuffer[readBuffer.lastIndex].toInt() and 0xff)

        assertTrue { readBuffer.copyOfRange(1, readBuffer.size - 1).contentEquals(i8) }
    }

    private fun compareSubRange(readBuffer: ShortArray) {
        assertEquals(0xeeee, readBuffer[0].toInt() and 0xffff)
        assertEquals(0xeeee, readBuffer[readBuffer.lastIndex].toInt() and 0xffff)

        assertTrue { readBuffer.copyOfRange(1, readBuffer.size - 1).contentEquals(i16) }
    }

    private fun compareSubRange(readBuffer: IntArray) {
        assertEquals(0xeeeeeeee, readBuffer[0].toLong() and 0xffffffff)
        assertEquals(0xeeeeeeee, readBuffer[readBuffer.lastIndex].toLong() and 0xffffffff)

        assertTrue { readBuffer.copyOfRange(1, readBuffer.size - 1).contentEquals(i32) }
    }

    private fun compareSubRange(readBuffer: LongArray) {
        assertEquals(0x0eeeeeeeeeeeeeeeL, readBuffer[0])
        assertEquals(0x0eeeeeeeeeeeeeeeL, readBuffer[readBuffer.lastIndex])

        assertTrue { readBuffer.copyOfRange(1, readBuffer.size - 1).contentEquals(i64) }
    }

    private fun compareSubRange(readBuffer: FloatArray) {
        assertEquals(0xeeeeeeee, readBuffer[0].toRawBits().toLong() and 0xffffffff)
        assertEquals(0xeeeeeeee, readBuffer[readBuffer.lastIndex].toRawBits().toLong() and 0xffffffff)

        assertTrue { readBuffer.copyOfRange(1, readBuffer.size - 1).contentEquals(f32) }
    }

    private fun compareSubRange(readBuffer: DoubleArray) {
        assertEquals(0x0eeeeeeeeeeeeeeeL, readBuffer[0].toRawBits())
        assertEquals(0x0eeeeeeeeeeeeeeeL, readBuffer[readBuffer.lastIndex].toRawBits())

        assertTrue { readBuffer.copyOfRange(1, readBuffer.size - 1).contentEquals(f64) }
    }

    private fun unhex(h: Char): Int = if (h in '0'..'9') h - '0' else if (h in 'a' .. 'f') h - 'a' + 10 else fail()
}