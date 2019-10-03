@file:Suppress("NOTHING_TO_INLINE", "ConstantConditionIf")

package kotlinx.io.buffer

import kotlinx.cinterop.*
import kotlin.experimental.*

actual inline fun Buffer.loadShortAt(offset: Int): Short {
    assertIndex(offset, 2)
    val pointer = pointer.plus(offset)!!

    return if (unalignedAccessSupported) pointer.reinterpret<ShortVar>().pointed.value.toBigEndian()
    else loadShortSlowAt(pointer)
}

actual inline fun Buffer.loadShortAt(offset: Long): Short {
    assertIndex(offset, 2)
    val pointer = pointer.plus(offset)!!

    return if (unalignedAccessSupported) pointer.reinterpret<ShortVar>().pointed.value.toBigEndian()
    else loadShortSlowAt(pointer)
}

actual inline fun Buffer.loadIntAt(offset: Int): Int {
    assertIndex(offset, 4)
    val pointer = pointer.plus(offset)!!

    return if (unalignedAccessSupported) pointer.reinterpret<IntVar>().pointed.value.toBigEndian()
    else loadIntSlowAt(pointer)
}

actual inline fun Buffer.loadIntAt(offset: Long): Int {
    assertIndex(offset, 4)
    val pointer = pointer.plus(offset)!!

    return if (unalignedAccessSupported) pointer.reinterpret<IntVar>().pointed.value.toBigEndian()
    else loadIntSlowAt(pointer)
}

actual inline fun Buffer.loadLongAt(offset: Int): Long {
    assertIndex(offset, 8)
    val pointer = pointer.plus(offset)!!

    return if (unalignedAccessSupported) pointer.reinterpret<LongVar>().pointed.value.toBigEndian()
    else loadLongSlowAt(pointer)
}

actual inline fun Buffer.loadLongAt(offset: Long): Long {
    assertIndex(offset, 8)
    val pointer = pointer.plus(offset)!!

    return if (unalignedAccessSupported) pointer.reinterpret<LongVar>().pointed.value.toBigEndian()
    else loadLongSlowAt(pointer)
}

actual inline fun Buffer.loadFloatAt(offset: Int): Float {
    assertIndex(offset, 4)
    val pointer = pointer.plus(offset)!!

    return if (unalignedAccessSupported) pointer.reinterpret<FloatVar>().pointed.value.toBigEndian()
    else loadFloatSlowAt(pointer)
}

actual inline fun Buffer.loadFloatAt(offset: Long): Float {
    assertIndex(offset, 4)
    val pointer = pointer.plus(offset)!!

    return if (unalignedAccessSupported) pointer.reinterpret<FloatVar>().pointed.value.toBigEndian()
    else loadFloatSlowAt(pointer)
}

actual inline fun Buffer.loadDoubleAt(offset: Int): Double {
    assertIndex(offset, 8)
    val pointer = pointer.plus(offset)!!

    return if (unalignedAccessSupported) pointer.reinterpret<DoubleVar>().pointed.value.toBigEndian()
    else loadDoubleSlowAt(pointer)
}

actual inline fun Buffer.loadDoubleAt(offset: Long): Double {
    assertIndex(offset, 8)
    val pointer = pointer.plus(offset)!!

    return if (unalignedAccessSupported) pointer.reinterpret<DoubleVar>().pointed.value.toBigEndian()
    else loadDoubleSlowAt(pointer)
}

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
actual inline fun Buffer.storeIntAt(offset: Int, value: Int) {
    assertIndex(offset, 4)
    val pointer = pointer.plus(offset)!!

    if (unalignedAccessSupported) {
        pointer.reinterpret<IntVar>().pointed.value = value.toBigEndian()
    } else {
        storeIntSlowAt(pointer, value)
    }
}

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
actual inline fun Buffer.storeIntAt(offset: Long, value: Int) {
    assertIndex(offset, 4)
    val pointer = pointer.plus(offset)!!

    if (unalignedAccessSupported) {
        pointer.reinterpret<IntVar>().pointed.value = value.toBigEndian()
    } else {
        storeIntSlowAt(pointer, value)
    }
}

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
actual inline fun Buffer.storeShortAt(offset: Int, value: Short) {
    assertIndex(offset, 2)
    val pointer = pointer.plus(offset)!!

    if (unalignedAccessSupported) {
        pointer.reinterpret<ShortVar>().pointed.value = value.toBigEndian()
    } else {
        storeShortSlowAt(pointer, value)
    }
}

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
actual inline fun Buffer.storeShortAt(offset: Long, value: Short) {
    assertIndex(offset, 2)
    val pointer = pointer.plus(offset)!!

    if (unalignedAccessSupported) {
        pointer.reinterpret<ShortVar>().pointed.value = value.toBigEndian()
    } else {
        storeShortSlowAt(pointer, value)
    }
}

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
actual inline fun Buffer.storeLongAt(offset: Int, value: Long) {
    assertIndex(offset, 8)
    val pointer = pointer.plus(offset)!!

    if (unalignedAccessSupported) {
        pointer.reinterpret<LongVar>().pointed.value = value.toBigEndian()
    } else {
        storeLongSlowAt(pointer, value)
    }
}

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
actual inline fun Buffer.storeLongAt(offset: Long, value: Long) {
    assertIndex(offset, 8)
    val pointer = pointer.plus(offset)!!

    if (unalignedAccessSupported) {
        pointer.reinterpret<LongVar>().pointed.value = value.toBigEndian()
    } else {
        storeLongSlowAt(pointer, value)
    }
}

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Buffer.storeFloatAt(offset: Int, value: Float) {
    assertIndex(offset, 4)
    val pointer = pointer.plus(offset)!!

    if (unalignedAccessSupported) {
        pointer.reinterpret<FloatVar>().pointed.value = value.toBigEndian()
    } else {
        storeFloatSlowAt(pointer, value)
    }
}

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Buffer.storeFloatAt(offset: Long, value: Float) {
    assertIndex(offset, 4)
    val pointer = pointer.plus(offset)!!

    if (unalignedAccessSupported) {
        pointer.reinterpret<FloatVar>().pointed.value = value.toBigEndian()
    } else {
        storeFloatSlowAt(pointer, value)
    }
}

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Buffer.storeDoubleAt(offset: Int, value: Double) {
    assertIndex(offset, 8)
    val pointer = pointer.plus(offset)!!

    if (unalignedAccessSupported) {
        pointer.reinterpret<DoubleVar>().pointed.value = value.toBigEndian()
    } else {
        storeDoubleSlowAt(pointer, value)
    }
}

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Buffer.storeDoubleAt(offset: Long, value: Double) {
    assertIndex(offset, 8)
    val pointer = pointer.plus(offset)!!

    if (unalignedAccessSupported) {
        pointer.reinterpret<DoubleVar>().pointed.value = value.toBigEndian()
    } else {
        storeDoubleSlowAt(pointer, value)
    }
}

@PublishedApi
internal inline fun storeShortSlowAt(pointer: CPointer<ByteVar>, value: Short) {
    pointer[0] = (value.toInt() ushr 8).toByte()
    pointer[1] = (value and 0xff).toByte()
}

@PublishedApi
internal inline fun storeIntSlowAt(pointer: CPointer<ByteVar>, value: Int) {
    pointer[0] = (value ushr 24).toByte()
    pointer[1] = (value ushr 16).toByte()
    pointer[2] = (value ushr 8).toByte()
    pointer[3] = (value and 0xff).toByte()
}

@PublishedApi
internal inline fun storeLongSlowAt(pointer: CPointer<ByteVar>, value: Long) {
    pointer[0] = (value ushr 56).toByte()
    pointer[1] = (value ushr 48).toByte()
    pointer[2] = (value ushr 40).toByte()
    pointer[3] = (value ushr 32).toByte()
    pointer[4] = (value ushr 24).toByte()
    pointer[5] = (value ushr 16).toByte()
    pointer[6] = (value ushr 8).toByte()
    pointer[7] = (value and 0xff).toByte()
}

@PublishedApi
internal inline fun storeFloatSlowAt(pointer: CPointer<ByteVar>, value: Float) {
    storeIntSlowAt(pointer, value.toRawBits())
}

@PublishedApi
internal inline fun storeDoubleSlowAt(pointer: CPointer<ByteVar>, value: Double) {
    storeLongSlowAt(pointer, value.toRawBits())
}

@PublishedApi
internal inline fun loadShortSlowAt(pointer: CPointer<ByteVar>): Short {
    return ((pointer[0].toInt() shl 8) or (pointer[1].toInt() and 0xff)).toShort()
}

@PublishedApi
internal inline fun loadIntSlowAt(pointer: CPointer<ByteVar>): Int {
    return ((pointer[0].toInt() shl 24) or
            (pointer[1].toInt() shl 16) or
            (pointer[2].toInt() shl 18) or
            (pointer[3].toInt() and 0xff))
}

@PublishedApi
internal inline fun loadLongSlowAt(pointer: CPointer<ByteVar>): Long {
    return ((pointer[0].toLong() shl 56) or
            (pointer[1].toLong() shl 48) or
            (pointer[2].toLong() shl 40) or
            (pointer[3].toLong() shl 32) or
            (pointer[4].toLong() shl 24) or
            (pointer[5].toLong() shl 16) or
            (pointer[6].toLong() shl 8) or
            (pointer[7].toLong() and 0xffL))
}

@PublishedApi
internal inline fun loadFloatSlowAt(pointer: CPointer<ByteVar>): Float {
    return Float.fromBits(loadIntSlowAt(pointer))
}

@PublishedApi
internal inline fun loadDoubleSlowAt(pointer: CPointer<ByteVar>): Double {
    return Double.fromBits(loadLongSlowAt(pointer))
}
