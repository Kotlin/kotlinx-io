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
        require(d < 16)
        require(d >= 0)

        if (d < 10) append('0' + d)
        else append('a' + (d - 10))
    }
}