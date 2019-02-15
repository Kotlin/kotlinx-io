@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.bits

/**
 * Copies unsigned shorts integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Memory.loadByteArray(
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
inline fun Memory.loadByteArray(
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
inline fun Memory.loadUByteArray(
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
inline fun Memory.loadUByteArray(
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
expect fun Memory.loadShortArray(
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
expect fun Memory.loadShortArray(
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
inline fun Memory.loadUShortArray(
    offset: Int,
    destination: UShortArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadShortArray(offset, destination.asShortArray(), destinationOffset, count)
}

/**
 * Copies unsigned shorts integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Memory.loadUShortArray(
    offset: Long,
    destination: UShortArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadShortArray(offset, destination.asShortArray(), destinationOffset, count)
}

/**
 * Copies regular integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Memory.loadIntArray(
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
expect fun Memory.loadIntArray(
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
inline fun Memory.loadUIntArray(
    offset: Int,
    destination: UIntArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadIntArray(offset, destination.asIntArray(), destinationOffset, count)
}

/**
 * Copies unsigned integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Memory.loadUIntArray(
    offset: Long,
    destination: UIntArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadIntArray(offset, destination.asIntArray(), destinationOffset, count)
}

/**
 * Copies long integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Memory.loadLongArray(
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
expect fun Memory.loadLongArray(
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
inline fun Memory.loadULongArray(
    offset: Int,
    destination: ULongArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadLongArray(offset, destination.asLongArray(), destinationOffset, count)
}

/**
 * Copies unsigned long integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Memory.loadULongArray(
    offset: Long,
    destination: ULongArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadLongArray(offset, destination.asLongArray(), destinationOffset, count)
}

/**
 * Copies floating point numbers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Memory.loadFloatArray(
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
expect fun Memory.loadFloatArray(
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
expect fun Memory.loadDoubleArray(
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
expect fun Memory.loadDoubleArray(
    offset: Long,
    destination: DoubleArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
)
