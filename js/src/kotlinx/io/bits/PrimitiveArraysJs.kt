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
actual fun Memory.loadShortArray(
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
actual fun Memory.loadShortArray(
    offset: Long,
    destination: ShortArray,
    destinationOffset: Int,
    count: Int
) {
    loadShortArray(offset.toIntOrFail("offset"), destination, destinationOffset, count)
}

/**
 * Copies regular integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.loadIntArray(
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
actual fun Memory.loadIntArray(
    offset: Long,
    destination: IntArray,
    destinationOffset: Int,
    count: Int
) {
    loadIntArray(offset.toIntOrFail("offset"), destination, destinationOffset, count)
}

/**
 * Copies regular integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.loadLongArray(
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
actual fun Memory.loadLongArray(
    offset: Long,
    destination: LongArray,
    destinationOffset: Int,
    count: Int
) {
    loadLongArray(offset.toIntOrFail("offset"), destination, destinationOffset, count)
}

/**
 * Copies floating point numbers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.loadFloatArray(
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
actual fun Memory.loadFloatArray(
    offset: Long,
    destination: FloatArray,
    destinationOffset: Int,
    count: Int
) {
    loadFloatArray(offset.toIntOrFail("offset"), destination, destinationOffset, count)
}

/**
 * Copies floating point numbers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.loadDoubleArray(
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
actual fun Memory.loadDoubleArray(
    offset: Long,
    destination: DoubleArray,
    destinationOffset: Int,
    count: Int
) {
    loadDoubleArray(offset.toIntOrFail("offset"), destination, destinationOffset, count)
}
