@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.bits

/**
 * Represents a linear range of bytes.
 * All operations are guarded by range-checks by default however at some platforms they could be disabled
 * in release builds.
 *
 * Instance of this class has no additional state except the bytes themselves.
 */
expect class Memory {
    /**
     * Size of memory range in bytes.
     */
    val size: Long

    /**
     * Size of memory range in bytes represented as signed 32bit integer
     * @throws IllegalStateException when size doesn't fit into a signed 32bit integer
     */
    val size32: Int

    /**
     * Returns byte at [index] position.
     */
    inline fun getAt(index: Int): Byte

    /**
     * Returns byte at [index] position.
     */
    inline fun getAt(index: Long): Byte

    /**
     * Write [value] at the specified [index].
     */
    inline fun setAt(index: Int, value: Byte)

    /**
     * Write [value] at the specified [index]
     */
    inline fun setAt(index: Long, value: Byte)

    /**
     * Returns memory's subrange. On some platforms it could do range checks but it is not guaranteed to be safe.
     * It also could lead to memory allocations on some platforms.
     */
    fun slice(offset: Int, length: Int): Memory

    /**
     * Returns memory's subrange. On some platforms it could do range checks but it is not guaranteed to be safe.
     * It also could lead to memory allocations on some platforms.
     */
    fun slice(offset: Long, length: Long): Memory

    /**
     * Copies bytes from this memory range from the specified [offset] and [length]
     * to the [destination] at [destinationOffset].
     * Copying bytes from a memory to itself is allowed.
     */
    fun copyTo(destination: Memory, offset: Int, length: Int, destinationOffset: Int)

    /**
     * Copies bytes from this memory range from the specified [offset] and [length]
     * to the [destination] at [destinationOffset].
     * Copying bytes from a memory to itself is allowed.
     */
    fun copyTo(destination: Memory, offset: Long, length: Long, destinationOffset: Long)

    companion object {
        /**
         * Represents an empty memory region
         */
        val Empty: Memory
    }
}

/**
 * Read byte at the specified [index].
 */
inline operator fun Memory.get(index: Int): Byte = getAt(index)

/**
 * Read byte at the specified [index].
 */
inline operator fun Memory.get(index: Long): Byte = getAt(index)

/**
 * Index write operator to write [value] at the specified [index]
 */
inline operator fun Memory.set(index: Long, value: Byte) = setAt(index, value)

/**
 * Index write operator to write [value] at the specified [index]
 */
inline operator fun Memory.set(index: Int, value: Byte) = setAt(index, value)

/**
 * Index write operator to write [value] at the specified [index]
 */
inline fun Memory.setAt(index: Long, value: UByte) = setAt(index, value.toByte())

/**
 * Index write operator to write [value] at the specified [index]
 */
inline fun Memory.setAt(index: Int, value: UByte) = setAt(index, value.toByte())

/**
 * Read short signed 16bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.getShortAt(offset: Int): Short

/**
 * Read short signed 16bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.getShortAt(offset: Long): Short

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.setShortAt(offset: Int, value: Short)

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.setShortAt(offset: Long, value: Short)


/**
 * Read short unsigned 16bit integer in the network byte order (Big Endian)
 */
inline fun Memory.getUShortAt(offset: Int): UShort = getShortAt(offset).toUShort()

/**
 * Read short unsigned 16bit integer in the network byte order (Big Endian)
 */
inline fun Memory.getUShortAt(offset: Long): UShort = getShortAt(offset).toUShort()

/**
 * Write short unsigned 16bit integer in the network byte order (Big Endian)
 */
inline fun Memory.setUShortAt(offset: Int, value: UShort): Unit = setShortAt(offset, value.toShort())

/**
 * Write short unsigned 16bit integer in the network byte order (Big Endian)
 */
inline fun Memory.setUShortAt(offset: Long, value: UShort): Unit = setShortAt(offset, value.toShort())

/**
 * Read regular signed 32bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.getIntAt(offset: Int): Int

/**
 * Read regular signed 32bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.getIntAt(offset: Long): Int

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.setIntAt(offset: Int, value: Int)

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.setIntAt(offset: Long, value: Int)

/**
 * Read regular unsigned 32bit integer in the network byte order (Big Endian)
 */
inline fun Memory.getUIntAt(offset: Int): UInt = getIntAt(offset).toUInt()

/**
 * Read regular unsigned 32bit integer in the network byte order (Big Endian)
 */
inline fun Memory.getUIntAt(offset: Long): UInt = getIntAt(offset).toUInt()

/**
 * Write regular unsigned 32bit integer in the network byte order (Big Endian)
 */
inline fun Memory.setUIntAt(offset: Int, value: UInt): Unit = setIntAt(offset, value.toInt())

/**
 * Write regular unsigned 32bit integer in the network byte order (Big Endian)
 */
inline fun Memory.setUIntAt(offset: Long, value: UInt): Unit = setIntAt(offset, value.toInt())

/**
 * Read short signed 64bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.getLongAt(offset: Int): Long

/**
 * Read short signed 64bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.getLongAt(offset: Long): Long

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.setLongAt(offset: Int, value: Long)

/**
 * write short signed 64bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.setLongAt(offset: Long, value: Long)

/**
 * Read short signed 64bit integer in the network byte order (Big Endian)
 */
inline fun Memory.getULongAt(offset: Int): ULong = getLongAt(offset).toULong()

/**
 * Read short signed 64bit integer in the network byte order (Big Endian)
 */
inline fun Memory.getULongAt(offset: Long): ULong = getLongAt(offset).toULong()

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
inline fun Memory.setULongAt(offset: Int, value: ULong): Unit = setLongAt(offset, value.toLong())

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
inline fun Memory.setULongAt(offset: Long, value: ULong): Unit = setLongAt(offset, value.toLong())

/**
 * Read short signed 32bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Memory.getFloatAt(offset: Int): Float

/**
 * Read short signed 32bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Memory.getFloatAt(offset: Long): Float

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Memory.setFloatAt(offset: Int, value: Float)

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Memory.setFloatAt(offset: Long, value: Float)

/**
 * Read short signed 64bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Memory.getDoubleAt(offset: Int): Double

/**
 * Read short signed 64bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Memory.getDoubleAt(offset: Long): Double

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Memory.setDoubleAt(offset: Int, value: Double)

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Memory.setDoubleAt(offset: Long, value: Double)

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
expect fun Memory.copyTo(destination: ByteArray, offset: Int, length: Int, destinationOffset: Int = 0)

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
expect fun Memory.copyTo(destination: ByteArray, offset: Long, length: Int, destinationOffset: Int = 0)
