@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.bits

/**
 * Copies unsigned shorts integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Memory.getByteArray(
    offset: Int,
    destination: ByteArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    copyTo(destination, offset, count, destinationOffset)
}

/**
 * Copies unsigned shorts integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Memory.getByteArray(
    offset: Long,
    destination: ByteArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    copyTo(destination, offset, count, destinationOffset)
}

/**
 * Copies unsigned shorts integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Memory.getUByteArray(
    offset: Int,
    destination: UByteArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    copyTo(destination.asByteArray(), offset, count, destinationOffset)
}

/**
 * Copies unsigned shorts integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Memory.getUByteArray(
    offset: Long,
    destination: UByteArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    copyTo(destination.asByteArray(), offset, count, destinationOffset)
}

/**
 * Copies shorts integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Memory.getShortArray(
    offset: Int,
    destination: ShortArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
)

/**
 * Copies shorts integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Memory.getShortArray(
    offset: Long,
    destination: ShortArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
)

/**
 * Copies unsigned shorts integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Memory.getUShortArray(
    offset: Int,
    destination: UShortArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    getShortArray(offset, destination.asShortArray(), destinationOffset, count)
}

/**
 * Copies unsigned shorts integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Memory.getUShortArray(
    offset: Long,
    destination: UShortArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    getShortArray(offset, destination.asShortArray(), destinationOffset, count)
}

/**
 * Copies regular integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Memory.getIntArray(
    offset: Int,
    destination: IntArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
)

/**
 * Copies regular integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Memory.getIntArray(
    offset: Long,
    destination: IntArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
)

/**
 * Copies unsigned integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Memory.getUIntArray(
    offset: Int,
    destination: UIntArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    getIntArray(offset, destination.asIntArray(), destinationOffset, count)
}

/**
 * Copies unsigned integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Memory.getUIntArray(
    offset: Long,
    destination: UIntArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    getIntArray(offset, destination.asIntArray(), destinationOffset, count)
}

/**
 * Copies long integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Memory.getLongArray(
    offset: Int,
    destination: LongArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
)

/**
 * Copies long integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Memory.getLongArray(
    offset: Long,
    destination: LongArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
)

/**
 * Copies unsigned long integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Memory.getULongArray(
    offset: Int,
    destination: ULongArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    getLongArray(offset, destination.asLongArray(), destinationOffset, count)
}

/**
 * Copies unsigned long integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Memory.getULongArray(
    offset: Long,
    destination: ULongArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    getLongArray(offset, destination.asLongArray(), destinationOffset, count)
}

/**
 * Copies floating point numbers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Memory.getFloatArray(
    offset: Int,
    destination: FloatArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
)

/**
 * Copies floating point numbers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Memory.getFloatArray(
    offset: Long,
    destination: FloatArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
)

/**
 * Copies floating point numbers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Memory.getDoubleArray(
    offset: Int,
    destination: DoubleArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
)

/**
 * Copies floating point numbers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Memory.getDoubleArray(
    offset: Long,
    destination: DoubleArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
)
