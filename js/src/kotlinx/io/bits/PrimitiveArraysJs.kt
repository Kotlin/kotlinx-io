@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.bits

import kotlinx.io.core.*
import kotlinx.io.core.internal.*
import org.khronos.webgl.*

private val isLittleEndianPlatform = ByteOrder.nativeOrder() === ByteOrder.LITTLE_ENDIAN

/**
 * Copies shorts integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.getShortArray(
    offset: Int,
    destination: ShortArray,
    destinationOffset: Int,
    count: Int
) {
    val typed = Int16Array(view.buffer, view.buffer.byteLength + offset, count)

    if (isLittleEndianPlatform) {
        // TODO investigate this implementation vs DataView.getInt16(...)
        repeat(count) { index ->
            destination[index + destinationOffset] = typed[index].reverseByteOrder()
        }
    } else {
        repeat(count) { index ->
            destination[index + destinationOffset] = typed[index]
        }
    }
}

/**
 * Copies shorts integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.getShortArray(
    offset: Long,
    destination: ShortArray,
    destinationOffset: Int,
    count: Int
) {
    getShortArray(offset.toIntOrFail("offset"), destination, destinationOffset, count)
}

/**
 * Copies regular integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.getIntArray(
    offset: Int,
    destination: IntArray,
    destinationOffset: Int,
    count: Int
) {
    val typed = Int32Array(view.buffer, view.buffer.byteLength + offset, count)

    if (isLittleEndianPlatform) {
        repeat(count) { index ->
            destination[index + destinationOffset] = typed[index].reverseByteOrder()
        }
    } else {
        repeat(count) { index ->
            destination[index + destinationOffset] = typed[index]
        }
    }
}

/**
 * Copies regular integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.getIntArray(
    offset: Long,
    destination: IntArray,
    destinationOffset: Int,
    count: Int
) {
    getIntArray(offset.toIntOrFail("offset"), destination, destinationOffset, count)
}

/**
 * Copies regular integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.getLongArray(
    offset: Int,
    destination: LongArray,
    destinationOffset: Int,
    count: Int
) {
    val typed = Int32Array(view.buffer, view.buffer.byteLength + offset, count)

    if (isLittleEndianPlatform) {
        for (index in 0 until count step 2) {
            destination[index + destinationOffset] = (typed[index].reverseByteOrder().toLong() and 0xffffffffL) or
                (typed[index + 1].reverseByteOrder().toLong() shl 32)
        }
    } else {
        for (index in 0 until count step 2) {
            destination[index + destinationOffset] = (typed[index].toLong() and 0xffffffffL) or
                (typed[index + 1].toLong() shl 32)
        }
    }
}

/**
 * Copies regular integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.getLongArray(
    offset: Long,
    destination: LongArray,
    destinationOffset: Int,
    count: Int
) {
    getLongArray(offset.toIntOrFail("offset"), destination, destinationOffset, count)
}

/**
 * Copies floating point numbers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.getFloatArray(
    offset: Int,
    destination: FloatArray,
    destinationOffset: Int,
    count: Int
) {
    val typed = Float32Array(view.buffer, view.buffer.byteLength + offset, count)

    if (isLittleEndianPlatform) {
        repeat(count) { index ->
            destination[index + destinationOffset] = typed[index].reverseByteOrder()
        }
    } else {
        repeat(count) { index ->
            destination[index + destinationOffset] = typed[index]
        }
    }
}

/**
 * Copies floating point numbers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.getFloatArray(
    offset: Long,
    destination: FloatArray,
    destinationOffset: Int,
    count: Int
) {
    getFloatArray(offset.toIntOrFail("offset"), destination, destinationOffset, count)
}

/**
 * Copies floating point numbers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.getDoubleArray(
    offset: Int,
    destination: DoubleArray,
    destinationOffset: Int,
    count: Int
) {
    val typed = Float64Array(view.buffer, view.buffer.byteLength + offset, count)

    if (isLittleEndianPlatform) {
        repeat(count) { index ->
            destination[index + destinationOffset] = typed[index].reverseByteOrder()
        }
    } else {
        repeat(count) { index ->
            destination[index + destinationOffset] = typed[index]
        }
    }
}

/**
 * Copies floating point numbers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.getDoubleArray(
    offset: Long,
    destination: DoubleArray,
    destinationOffset: Int,
    count: Int
) {
    getDoubleArray(offset.toIntOrFail("offset"), destination, destinationOffset, count)
}
