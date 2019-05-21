@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.buffer

import kotlinx.io.internal.*


/**
 * Returns byte at [index] position.
 */
inline fun Buffer.loadByteAt(index: Long): Byte = loadByteAt(index.toIntOrFail { "index" })

/**
 * Returns unsigned byte at [index] position.
 */
inline fun Buffer.loadUByteAt(index: Int): UByte = loadByteAt(index).toUByte()

/**
 * Returns unsigned byte at [index] position.
 */
inline fun Buffer.loadUByteAt(index: Long): UByte = loadByteAt(index).toUByte()

/**
 * Write [value] at the specified [index].
 */
inline fun Buffer.storeByteAt(index: Long, value: Byte) = storeByteAt(index.toIntOrFail { "index" }, value)

/**
 * Write [value] at the specified [index].
 */
inline fun Buffer.storeUByteAt(index: Int, value: UByte) = storeByteAt(index, value.toByte())

/**
 * Write [value] at the specified [index].
 */
inline fun Buffer.storeUByteAt(index: Long, value: UByte) = storeByteAt(index.toIntOrFail { "index" }, value.toByte())

/**
 * Read short signed 16bit integer in the network byte order (Big Endian)
 */
expect inline fun Buffer.loadShortAt(offset: Int): Short

/**
 * Read short signed 16bit integer in the network byte order (Big Endian)
 */
expect inline fun Buffer.loadShortAt(offset: Long): Short

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
expect inline fun Buffer.storeShortAt(offset: Int, value: Short)

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
expect inline fun Buffer.storeShortAt(offset: Long, value: Short)

/**
 * Read short unsigned 16bit integer in the network byte order (Big Endian)
 */
inline fun Buffer.loadUShortAt(offset: Int): UShort = loadShortAt(offset).toUShort()

/**
 * Read short unsigned 16bit integer in the network byte order (Big Endian)
 */
inline fun Buffer.loadUShortAt(offset: Long): UShort = loadShortAt(offset).toUShort()

/**
 * Write short unsigned 16bit integer in the network byte order (Big Endian)
 */
inline fun Buffer.storeUShortAt(offset: Int, value: UShort): Unit = storeShortAt(offset, value.toShort())

/**
 * Write short unsigned 16bit integer in the network byte order (Big Endian)
 */
inline fun Buffer.storeUShortAt(offset: Long, value: UShort): Unit = storeShortAt(offset, value.toShort())

/**
 * Read regular signed 32bit integer in the network byte order (Big Endian)
 */
expect inline fun Buffer.loadIntAt(offset: Int): Int

/**
 * Read regular signed 32bit integer in the network byte order (Big Endian)
 */
expect inline fun Buffer.loadIntAt(offset: Long): Int

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
expect inline fun Buffer.storeIntAt(offset: Int, value: Int)

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
expect inline fun Buffer.storeIntAt(offset: Long, value: Int)

/**
 * Read regular unsigned 32bit integer in the network byte order (Big Endian)
 */
inline fun Buffer.loadUIntAt(offset: Int): UInt = loadIntAt(offset).toUInt()

/**
 * Read regular unsigned 32bit integer in the network byte order (Big Endian)
 */
inline fun Buffer.loadUIntAt(offset: Long): UInt = loadIntAt(offset).toUInt()

/**
 * Write regular unsigned 32bit integer in the network byte order (Big Endian)
 */
inline fun Buffer.storeUIntAt(offset: Int, value: UInt): Unit = storeIntAt(offset, value.toInt())

/**
 * Write regular unsigned 32bit integer in the network byte order (Big Endian)
 */
inline fun Buffer.storeUIntAt(offset: Long, value: UInt): Unit = storeIntAt(offset, value.toInt())

/**
 * Read short signed 64bit integer in the network byte order (Big Endian)
 */
expect inline fun Buffer.loadLongAt(offset: Int): Long

/**
 * Read short signed 64bit integer in the network byte order (Big Endian)
 */
expect inline fun Buffer.loadLongAt(offset: Long): Long

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
expect inline fun Buffer.storeLongAt(offset: Int, value: Long)

/**
 * write short signed 64bit integer in the network byte order (Big Endian)
 */
expect inline fun Buffer.storeLongAt(offset: Long, value: Long)

/**
 * Read short signed 64bit integer in the network byte order (Big Endian)
 */
inline fun Buffer.loadULongAt(offset: Int): ULong = loadLongAt(offset).toULong()

/**
 * Read short signed 64bit integer in the network byte order (Big Endian)
 */
inline fun Buffer.loadULongAt(offset: Long): ULong = loadLongAt(offset).toULong()

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
inline fun Buffer.storeULongAt(offset: Int, value: ULong): Unit = storeLongAt(offset, value.toLong())

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
inline fun Buffer.storeULongAt(offset: Long, value: ULong): Unit = storeLongAt(offset, value.toLong())

/**
 * Read short signed 32bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Buffer.loadFloatAt(offset: Int): Float

/**
 * Read short signed 32bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Buffer.loadFloatAt(offset: Long): Float

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Buffer.storeFloatAt(offset: Int, value: Float)

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Buffer.storeFloatAt(offset: Long, value: Float)

/**
 * Read short signed 64bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Buffer.loadDoubleAt(offset: Int): Double

/**
 * Read short signed 64bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Buffer.loadDoubleAt(offset: Long): Double

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Buffer.storeDoubleAt(offset: Int, value: Double)

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Buffer.storeDoubleAt(offset: Long, value: Double)