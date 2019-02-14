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
    inline fun loadAt(index: Int): Byte

    /**
     * Returns byte at [index] position.
     */
    inline fun loadAt(index: Long): Byte

    /**
     * Write [value] at the specified [index].
     */
    inline fun storeAt(index: Int, value: Byte)

    /**
     * Write [value] at the specified [index]
     */
    inline fun storeAt(index: Long, value: Byte)

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
inline operator fun Memory.get(index: Int): Byte = loadAt(index)

/**
 * Read byte at the specified [index].
 */
inline operator fun Memory.get(index: Long): Byte = loadAt(index)

/**
 * Index write operator to write [value] at the specified [index]
 */
inline operator fun Memory.set(index: Long, value: Byte) = storeAt(index, value)

/**
 * Index write operator to write [value] at the specified [index]
 */
inline operator fun Memory.set(index: Int, value: Byte) = storeAt(index, value)

/**
 * Index write operator to write [value] at the specified [index]
 */
inline fun Memory.storeAt(index: Long, value: UByte) = storeAt(index, value.toByte())

/**
 * Index write operator to write [value] at the specified [index]
 */
inline fun Memory.storeAt(index: Int, value: UByte) = storeAt(index, value.toByte())

/**
 * Fill memory range starting at the specified [offset] with [value] repeated [count] times.
 */
expect fun Memory.fill(offset: Long, count: Long, value: Byte)

/**
 * Fill memory range starting at the specified [offset] with [value] repeated [count] times.
 */
expect fun Memory.fill(offset: Int, count: Int, value: Byte)

/**
 * Read short signed 16bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.loadShortAt(offset: Int): Short

/**
 * Read short signed 16bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.loadShortAt(offset: Long): Short

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.storeShortAt(offset: Int, value: Short)

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.storeShortAt(offset: Long, value: Short)


/**
 * Read short unsigned 16bit integer in the network byte order (Big Endian)
 */
inline fun Memory.loadUShortAt(offset: Int): UShort = loadShortAt(offset).toUShort()

/**
 * Read short unsigned 16bit integer in the network byte order (Big Endian)
 */
inline fun Memory.loadUShortAt(offset: Long): UShort = loadShortAt(offset).toUShort()

/**
 * Write short unsigned 16bit integer in the network byte order (Big Endian)
 */
inline fun Memory.storeUShortAt(offset: Int, value: UShort): Unit = storeShortAt(offset, value.toShort())

/**
 * Write short unsigned 16bit integer in the network byte order (Big Endian)
 */
inline fun Memory.storeUShortAt(offset: Long, value: UShort): Unit = storeShortAt(offset, value.toShort())

/**
 * Read regular signed 32bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.loadIntAt(offset: Int): Int

/**
 * Read regular signed 32bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.loadIntAt(offset: Long): Int

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.storeIntAt(offset: Int, value: Int)

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.storeIntAt(offset: Long, value: Int)

/**
 * Read regular unsigned 32bit integer in the network byte order (Big Endian)
 */
inline fun Memory.loadUIntAt(offset: Int): UInt = loadIntAt(offset).toUInt()

/**
 * Read regular unsigned 32bit integer in the network byte order (Big Endian)
 */
inline fun Memory.loadUIntAt(offset: Long): UInt = loadIntAt(offset).toUInt()

/**
 * Write regular unsigned 32bit integer in the network byte order (Big Endian)
 */
inline fun Memory.storeUIntAt(offset: Int, value: UInt): Unit = storeIntAt(offset, value.toInt())

/**
 * Write regular unsigned 32bit integer in the network byte order (Big Endian)
 */
inline fun Memory.storeUIntAt(offset: Long, value: UInt): Unit = storeIntAt(offset, value.toInt())

/**
 * Read short signed 64bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.loadLongAt(offset: Int): Long

/**
 * Read short signed 64bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.loadLongAt(offset: Long): Long

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.storeLongAt(offset: Int, value: Long)

/**
 * write short signed 64bit integer in the network byte order (Big Endian)
 */
expect inline fun Memory.storeLongAt(offset: Long, value: Long)

/**
 * Read short signed 64bit integer in the network byte order (Big Endian)
 */
inline fun Memory.loadULongAt(offset: Int): ULong = loadLongAt(offset).toULong()

/**
 * Read short signed 64bit integer in the network byte order (Big Endian)
 */
inline fun Memory.loadULongAt(offset: Long): ULong = loadLongAt(offset).toULong()

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
inline fun Memory.storeULongAt(offset: Int, value: ULong): Unit = storeLongAt(offset, value.toLong())

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
inline fun Memory.storeULongAt(offset: Long, value: ULong): Unit = storeLongAt(offset, value.toLong())

/**
 * Read short signed 32bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Memory.loadFloatAt(offset: Int): Float

/**
 * Read short signed 32bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Memory.loadFloatAt(offset: Long): Float

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Memory.storeFloatAt(offset: Int, value: Float)

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Memory.storeFloatAt(offset: Long, value: Float)

/**
 * Read short signed 64bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Memory.loadDoubleAt(offset: Int): Double

/**
 * Read short signed 64bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Memory.loadDoubleAt(offset: Long): Double

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Memory.storeDoubleAt(offset: Int, value: Double)

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
expect inline fun Memory.storeDoubleAt(offset: Long, value: Double)

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
