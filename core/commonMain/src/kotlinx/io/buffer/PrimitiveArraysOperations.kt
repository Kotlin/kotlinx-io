@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.buffer

import kotlinx.io.internal.*

/**
 * Copies bytes from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset].
 */
inline fun Buffer.loadByteArray(
    offset: Int,
    destination: ByteArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    copyTo(destination, offset, count, destinationOffset)
}

/**
 * Copies unsigned shorts integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Buffer.loadByteArray(
    offset: Long,
    destination: ByteArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    copyTo(destination, offset, count, destinationOffset)
}

/**
 * Copies unsigned shorts integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
@ExperimentalUnsignedTypes
inline fun Buffer.loadUByteArray(
    offset: Int,
    destination: UByteArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    copyTo(destination.asByteArray(), offset, count, destinationOffset)
}

/**
 * Copies unsigned shorts integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
@ExperimentalUnsignedTypes
inline fun Buffer.loadUByteArray(
    offset: Long,
    destination: UByteArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    copyTo(destination.asByteArray(), offset, count, destinationOffset)
}

/**
 * Copies shorts integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Buffer.loadShortArray(
    offset: Int,
    destination: ShortArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
)

/**
 * Copies shorts integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Buffer.loadShortArray(
    offset: Long,
    destination: ShortArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadShortArray(offset.toIntOrFail { "offset" }, destination, destinationOffset, count)
}

/**
 * Copies unsigned shorts integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
@ExperimentalUnsignedTypes
inline fun Buffer.loadUShortArray(
    offset: Int,
    destination: UShortArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadShortArray(offset, destination.asShortArray(), destinationOffset, count)
}

/**
 * Copies unsigned shorts integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
@ExperimentalUnsignedTypes
inline fun Buffer.loadUShortArray(
    offset: Long,
    destination: UShortArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadShortArray(offset, destination.asShortArray(), destinationOffset, count)
}

/**
 * Copies regular integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Buffer.loadIntArray(
    offset: Int,
    destination: IntArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
)

/**
 * Copies regular integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
fun Buffer.loadIntArray(
    offset: Long,
    destination: IntArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadIntArray(offset.toIntOrFail { "offset" }, destination, destinationOffset, count)
}

/**
 * Copies unsigned integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
@ExperimentalUnsignedTypes
inline fun Buffer.loadUIntArray(
    offset: Int,
    destination: UIntArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadIntArray(offset, destination.asIntArray(), destinationOffset, count)
}

/**
 * Copies unsigned integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
@ExperimentalUnsignedTypes
inline fun Buffer.loadUIntArray(
    offset: Long,
    destination: UIntArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadIntArray(offset, destination.asIntArray(), destinationOffset, count)
}

/**
 * Copies long integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Buffer.loadLongArray(
    offset: Int,
    destination: LongArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
)

/**
 * Copies long integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Buffer.loadLongArray(
    offset: Long,
    destination: LongArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadLongArray(offset.toIntOrFail { "offset" }, destination, destinationOffset, count)
}

/**
 * Copies unsigned long integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
@ExperimentalUnsignedTypes
inline fun Buffer.loadULongArray(
    offset: Int,
    destination: ULongArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadLongArray(offset, destination.asLongArray(), destinationOffset, count)
}

/**
 * Copies unsigned long integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
@ExperimentalUnsignedTypes
inline fun Buffer.loadULongArray(
    offset: Long,
    destination: ULongArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadLongArray(offset, destination.asLongArray(), destinationOffset, count)
}

/**
 * Copies floating point numbers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Buffer.loadFloatArray(
    offset: Int,
    destination: FloatArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
)

/**
 * Copies floating point numbers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Buffer.loadFloatArray(
    offset: Long,
    destination: FloatArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadFloatArray(offset.toIntOrFail { "offset" }, destination, destinationOffset, count)
}

/**
 * Copies floating point numbers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
expect fun Buffer.loadDoubleArray(
    offset: Int,
    destination: DoubleArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
)

/**
 * Copies floating point numbers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
inline fun Buffer.loadDoubleArray(
    offset: Long,
    destination: DoubleArray,
    destinationOffset: Int = 0,
    count: Int = destination.size - destinationOffset
) {
    loadDoubleArray(offset.toIntOrFail { "offset" }, destination, destinationOffset, count)
}

/**
 * Copies unsigned shorts integers from the [source] array at [sourceOffset] to this buffer at the specified [offset].
 * @param sourceOffset items
 */
inline fun Buffer.storeByteArray(
    offset: Long,
    source: ByteArray,
    sourceOffset: Long = 0,
    count: Long = source.size - sourceOffset
) {
    source.useBuffer(sourceOffset.toIntOrFail { "sourceOffset" }, count.toIntOrFail { "count" }) { sourcebuffer ->
        sourcebuffer.copyTo(this, 0, count, offset)
    }
}

/**
 * Copies unsigned shorts integers from the [source] array at [sourceOffset] to this buffer at the specified [offset].
 * @param sourceOffset items
 */
inline fun Buffer.storeByteArray(
    offset: Int,
    source: ByteArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
) {
    source.useBuffer(sourceOffset, count) { buffer ->
        buffer.copyTo(this, 0, count, offset)
    }
}

/**
 * Copies unsigned shorts integers from the [source] array at [sourceOffset] to this buffer at the specified [offset].
 * @param sourceOffset items
 */
@ExperimentalUnsignedTypes
inline fun Buffer.storeUByteArray(
    offset: Int,
    source: UByteArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
) {
    storeByteArray(offset, source.asByteArray(), sourceOffset, count)
}

/**
 * Copies unsigned shorts integers from the [source] array at [sourceOffset] to this buffer at the specified [offset].
 * @param sourceOffset items
 */
@ExperimentalUnsignedTypes
inline fun Buffer.storeUByteArray(
    offset: Long,
    source: UByteArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
) {
    storeByteArray(offset.toIntOrFail { "offset" }, source.asByteArray(), sourceOffset, count)
}

/**
 * Copies shorts integers from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
expect fun Buffer.storeShortArray(
    offset: Int,
    source: ShortArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
)

/**
 * Copies shorts integers from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
inline fun Buffer.storeShortArray(
    offset: Long,
    source: ShortArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
) {
    storeShortArray(offset.toIntOrFail { "offset" }, source, sourceOffset, count)
}

/**
 * Copies unsigned shorts integers from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
@ExperimentalUnsignedTypes
inline fun Buffer.storeUShortArray(
    offset: Int,
    source: UShortArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
) {
    storeShortArray(offset, source.asShortArray(), sourceOffset, count)
}

/**
 * Copies unsigned shorts integers from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
@ExperimentalUnsignedTypes
inline fun Buffer.storeUShortArray(
    offset: Long,
    source: UShortArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
) {
    storeShortArray(offset, source.asShortArray(), sourceOffset, count)
}

/**
 * Copies regular integers from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
expect fun Buffer.storeIntArray(
    offset: Int,
    source: IntArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
)

/**
 * Copies regular integers from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
inline fun Buffer.storeIntArray(
    offset: Long,
    source: IntArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
) {
    storeIntArray(offset.toIntOrFail { "offset" }, source, sourceOffset, count)
}

/**
 * Copies unsigned integers from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
@ExperimentalUnsignedTypes
inline fun Buffer.storeUIntArray(
    offset: Int,
    source: UIntArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
) {
    storeIntArray(offset, source.asIntArray(), sourceOffset, count)
}

/**
 * Copies unsigned integers from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
@ExperimentalUnsignedTypes
inline fun Buffer.storeUIntArray(
    offset: Long,
    source: UIntArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
) {
    storeIntArray(offset, source.asIntArray(), sourceOffset, count)
}

/**
 * Copies long integers from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
expect fun Buffer.storeLongArray(
    offset: Int,
    source: LongArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
)

/**
 * Copies long integers from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
inline fun Buffer.storeLongArray(
    offset: Long,
    source: LongArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
) {
    storeLongArray(offset.toIntOrFail { "offset" }, source, sourceOffset, count)
}

/**
 * Copies unsigned long integers from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
@ExperimentalUnsignedTypes
inline fun Buffer.storeULongArray(
    offset: Int,
    source: ULongArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
) {
    storeLongArray(offset, source.asLongArray(), sourceOffset, count)
}

/**
 * Copies unsigned long integers from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
@ExperimentalUnsignedTypes
inline fun Buffer.storeULongArray(
    offset: Long,
    source: ULongArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
) {
    storeLongArray(offset, source.asLongArray(), sourceOffset, count)
}

/**
 * Copies floating point numbers from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
expect fun Buffer.storeFloatArray(
    offset: Int,
    source: FloatArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
)

/**
 * Copies floating point numbers from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
inline fun Buffer.storeFloatArray(
    offset: Long,
    source: FloatArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
) {
    storeFloatArray(offset.toIntOrFail { "offset" }, source, sourceOffset, count)
}

/**
 * Copies floating point numbers from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
expect fun Buffer.storeDoubleArray(
    offset: Int,
    source: DoubleArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
)

/**
 * Copies floating point numbers from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
inline fun Buffer.storeDoubleArray(
    offset: Long,
    source: DoubleArray,
    sourceOffset: Int = 0,
    count: Int = source.size - sourceOffset
) {
    storeDoubleArray(offset.toIntOrFail { "offset" }, source, sourceOffset, count)
}

