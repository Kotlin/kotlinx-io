@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.buffer

import kotlinx.io.internal.*
import java.nio.*

/**
 * Copies shorts integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Buffer.loadShortArray(
    offset: Int,
    destination: ShortArray,
    destinationOffset: Int,
    count: Int
) {
    buffer.withOffset(offset).asShortBuffer().get(destination, destinationOffset, count)
}

/**
 * Copies regular integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Buffer.loadIntArray(
    offset: Int,
    destination: IntArray,
    destinationOffset: Int,
    count: Int
) {
    buffer.withOffset(offset).asIntBuffer().get(destination, destinationOffset, count)
}

/**
 * Copies regular integers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Buffer.loadLongArray(
    offset: Int,
    destination: LongArray,
    destinationOffset: Int,
    count: Int
) {
    buffer.withOffset(offset).asLongBuffer().get(destination, destinationOffset, count)
}

/**
 * Copies floating point numbers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Buffer.loadFloatArray(
    offset: Int,
    destination: FloatArray,
    destinationOffset: Int,
    count: Int
) {
    buffer.withOffset(offset).asFloatBuffer().get(destination, destinationOffset, count)
}

/**
 * Copies floating point numbers from this buffer range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Buffer.loadDoubleArray(
    offset: Int,
    destination: DoubleArray,
    destinationOffset: Int,
    count: Int
) {
    buffer.withOffset(offset).asDoubleBuffer().get(destination, destinationOffset, count)
}

/**
 * Copies shorts integers from from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
actual fun Buffer.storeShortArray(
    offset: Int,
    source: ShortArray,
    sourceOffset: Int,
    count: Int
) {
    buffer.withOffset(offset).asShortBuffer().put(source, sourceOffset, count)
}

/**
 * Copies regular integers from from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
actual fun Buffer.storeIntArray(
    offset: Int,
    source: IntArray,
    sourceOffset: Int,
    count: Int
) {
    buffer.withOffset(offset).asIntBuffer().put(source, sourceOffset, count)
}

/**
 * Copies regular integers from from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
actual fun Buffer.storeLongArray(
    offset: Int,
    source: LongArray,
    sourceOffset: Int,
    count: Int
) {
    buffer.withOffset(offset).asLongBuffer().put(source, sourceOffset, count)
}

/**
 * Copies floating point numbers from from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
actual fun Buffer.storeFloatArray(
    offset: Int,
    source: FloatArray,
    sourceOffset: Int,
    count: Int
) {
    buffer.withOffset(offset).asFloatBuffer().put(source, sourceOffset, count)
}

/**
 * Copies floating point numbers from from the [source] array at [sourceOffset] to this buffer at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
actual fun Buffer.storeDoubleArray(
    offset: Int,
    source: DoubleArray,
    sourceOffset: Int,
    count: Int
) {
    buffer.withOffset(offset).asDoubleBuffer().put(source, sourceOffset, count)
}


private inline fun ByteBuffer.withOffset(offset: Int): ByteBuffer = duplicate()!!.apply { position(offset) }
