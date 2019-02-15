@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.bits

import kotlinx.cinterop.*
import kotlin.experimental.*

actual inline fun Memory.loadShortAt(offset: Int): Short {
    assertIndex(offset, 2)
    return pointer.plus(offset)!!.reinterpret<ShortVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadShortAt(offset: Long): Short {
    assertIndex(offset, 2)
    return pointer.plus(offset)!!.reinterpret<ShortVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadIntAt(offset: Int): Int {
    assertIndex(offset, 4)
    return pointer.plus(offset)!!.reinterpret<IntVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadIntAt(offset: Long): Int {
    assertIndex(offset, 4)
    return pointer.plus(offset)!!.reinterpret<IntVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadLongAt(offset: Int): Long {
    assertIndex(offset, 8)
    return pointer.plus(offset)!!.reinterpret<LongVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadLongAt(offset: Long): Long {
    assertIndex(offset, 8)
    return pointer.plus(offset)!!.reinterpret<LongVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadFloatAt(offset: Int): Float {
    assertIndex(offset, 4)
    return pointer.plus(offset)!!.reinterpret<FloatVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadFloatAt(offset: Long): Float {
    assertIndex(offset, 4)
    return pointer.plus(offset)!!.reinterpret<FloatVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadDoubleAt(offset: Int): Double {
    assertIndex(offset, 8)
    return pointer.plus(offset)!!.reinterpret<DoubleVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadDoubleAt(offset: Long): Double {
    assertIndex(offset, 8)
    return pointer.plus(offset)!!.reinterpret<DoubleVar>().pointed.value.toBigEndian()
}

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeIntAt(offset: Int, value: Int) {
    assertIndex(offset, 4)
    pointer.plus(offset)!!.reinterpret<IntVar>().pointed.value = value.toBigEndian()
}

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeIntAt(offset: Long, value: Int) {
    assertIndex(offset, 4)
    pointer.plus(offset)!!.reinterpret<IntVar>().pointed.value = value.toBigEndian()
}

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeShortAt(offset: Int, value: Short) {
    assertIndex(offset, 2)
    pointer.plus(offset)!!.reinterpret<ShortVar>().pointed.value = value.toBigEndian()
}

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeShortAt(offset: Long, value: Short) {
    assertIndex(offset, 2)
    pointer.plus(offset)!!.reinterpret<ShortVar>().pointed.value = value.toBigEndian()
}

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeLongAt(offset: Int, value: Long) {
    assertIndex(offset, 8)
    pointer.plus(offset)!!.reinterpret<LongVar>().pointed.value = value.toBigEndian()
}

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeLongAt(offset: Long, value: Long) {
    assertIndex(offset, 8)
    pointer.plus(offset)!!.reinterpret<LongVar>().pointed.value = value.toBigEndian()
}

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Memory.storeFloatAt(offset: Int, value: Float) {
    assertIndex(offset, 4)
    pointer.plus(offset)!!.reinterpret<FloatVar>().pointed.value = value.toBigEndian()
}

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Memory.storeFloatAt(offset: Long, value: Float) {
    assertIndex(offset, 4)
    pointer.plus(offset)!!.reinterpret<FloatVar>().pointed.value = value.toBigEndian()
}

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Memory.storeDoubleAt(offset: Int, value: Double) {
    assertIndex(offset, 8)
    pointer.plus(offset)!!.reinterpret<DoubleVar>().pointed.value = value.toBigEndian()
}

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Memory.storeDoubleAt(offset: Long, value: Double) {
    assertIndex(offset, 8)
    pointer.plus(offset)!!.reinterpret<DoubleVar>().pointed.value = value.toBigEndian()
}

internal inline fun storeShortSlowAt(pointer: CPointer<ByteVar>, value: Short) {
    pointer[0] = (value.toInt() ushr 8).toByte()
    pointer[1] = (value and 0xff).toByte()
}

internal inline fun storeIntSlowAt(pointer: CPointer<ByteVar>, value: Int) {
    pointer[0] = (value ushr 24).toByte()
    pointer[1] = (value ushr 16).toByte()
    pointer[2] = (value ushr 8).toByte()
    pointer[3] = (value and 0xff).toByte()
}

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

internal inline fun storeFloatSlowAt(pointer: CPointer<ByteVar>, value: Float) {
    storeIntSlowAt(pointer, value.toRawBits())
}

internal inline fun storeDoubleSlowAt(pointer: CPointer<ByteVar>, value: Double) {
    storeLongSlowAt(pointer, value.toRawBits())
}
