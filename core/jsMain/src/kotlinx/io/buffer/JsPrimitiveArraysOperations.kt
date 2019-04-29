package kotlinx.io.buffer

import org.khronos.webgl.*


private val isLittleEndianPlatform = ByteOrder.native === ByteOrder.LITTLE_ENDIAN

/**
 * Copies shorts integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Buffer.loadShortArray(
    offset: Int,
    destination: ShortArray,
    destinationOffset: Int,
    count: Int
) {
    val typed = Int16Array(view.buffer, view.byteOffset + offset, count)

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
 * Copies regular integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Buffer.loadIntArray(
    offset: Int,
    destination: IntArray,
    destinationOffset: Int,
    count: Int
) {
    val typed = Int32Array(view.buffer, view.byteOffset + offset, count)

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
actual fun Buffer.loadLongArray(
    offset: Int,
    destination: LongArray,
    destinationOffset: Int,
    count: Int
) {
    val typed = Int32Array(view.buffer, view.byteOffset + offset, count * 2)

    if (isLittleEndianPlatform) {
        for (index in 0 until count * 2 step 2) {
            destination[index / 2 + destinationOffset] =
                (typed[index + 1].reverseByteOrder().toLong() and 0xffffffffL) or
                        (typed[index].reverseByteOrder().toLong() shl 32)
        }
    } else {
        for (index in 0 until count * 2 step 2) {
            destination[index / 2 + destinationOffset] = (typed[index].toLong() and 0xffffffffL) or
                    (typed[index + 1].toLong() shl 32)
        }
    }
}


/**
 * Copies floating point numbers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Buffer.loadFloatArray(
    offset: Int,
    destination: FloatArray,
    destinationOffset: Int,
    count: Int
) {
    val typed = Float32Array(view.buffer, view.byteOffset + offset, count)

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
actual fun Buffer.loadDoubleArray(
    offset: Int,
    destination: DoubleArray,
    destinationOffset: Int,
    count: Int
) {
    val typed = Float64Array(view.buffer, view.byteOffset + offset, count)

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
 * Copies shorts integers from from the [source] array at [sourceOffset] to this memory at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
actual fun Buffer.storeShortArray(
    offset: Int,
    source: ShortArray,
    sourceOffset: Int,
    count: Int
) {
    val typed = Int16Array(view.buffer, view.byteOffset + offset, count)

    if (isLittleEndianPlatform) {
        // TODO investigate this implementation vs DataView.getInt16(...)
        repeat(count) { index ->
            typed[index] = source[index + sourceOffset].reverseByteOrder()
        }
    } else {
        repeat(count) { index ->
            typed[index] = source[index + sourceOffset]
        }
    }
}


/**
 * Copies regular integers from from the [source] array at [sourceOffset] to this memory at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
actual fun Buffer.storeIntArray(
    offset: Int,
    source: IntArray,
    sourceOffset: Int,
    count: Int
) {
    val typed = Int32Array(view.buffer, view.byteOffset + offset, count)

    if (isLittleEndianPlatform) {
        repeat(count) { index ->
            typed[index] = source[index + sourceOffset].reverseByteOrder()
        }
    } else {
        repeat(count) { index ->
            typed[index] = source[index + sourceOffset]
        }
    }
}


/**
 * Copies regular integers from from the [source] array at [sourceOffset] to this memory at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
actual fun Buffer.storeLongArray(
    offset: Int,
    source: LongArray,
    sourceOffset: Int,
    count: Int
) {
    val typed = Int32Array(view.buffer, view.byteOffset + offset, count * 2)

    if (isLittleEndianPlatform) {
        for (index in 0 until count * 2 step 2) {
            val sourceIndex = index / 2 + sourceOffset
            val sourceValue = source[sourceIndex]
            typed[index] = (sourceValue ushr 32).toInt().reverseByteOrder()
            typed[index + 1] = (sourceValue and 0xffffffffL).toInt().reverseByteOrder()
        }
    } else {
        for (index in 0 until count * 2 step 2) {
            val sourceIndex = index / 2 + sourceOffset
            val sourceValue = source[sourceIndex]
            typed[index] = (sourceValue ushr 32).toInt()
            typed[index + 1] = (sourceValue and 0xffffffffL).toInt()
        }
    }
}


/**
 * Copies floating point numbers from from the [source] array at [sourceOffset] to this memory at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
actual fun Buffer.storeFloatArray(
    offset: Int,
    source: FloatArray,
    sourceOffset: Int,
    count: Int
) {
    val typed = Float32Array(view.buffer, view.byteOffset + offset, count)

    if (isLittleEndianPlatform) {
        repeat(count) { index ->
            typed[index] = source[index + sourceOffset].reverseByteOrder()
        }
    } else {
        repeat(count) { index ->
            typed[index] = source[index + sourceOffset]
        }
    }
}


/**
 * Copies floating point numbers from from the [source] array at [sourceOffset] to this memory at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
actual fun Buffer.storeDoubleArray(
    offset: Int,
    source: DoubleArray,
    sourceOffset: Int,
    count: Int
) {
    val typed = Float64Array(view.buffer, view.byteOffset + offset, count)

    if (isLittleEndianPlatform) {
        repeat(count) { index ->
            typed[index] = source[index + sourceOffset].reverseByteOrder()
        }
    } else {
        repeat(count) { index ->
            typed[index] = source[index + sourceOffset]
        }
    }
}
