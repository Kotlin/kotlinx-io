package kotlinx.io

import kotlinx.io.internal.*

/**
 * Creates an input from the given byte array, starting from inclusively [startIndex] and until [endIndex] exclusively.
 * The array is not copied, and calling [Input.close] on the resulting input has no effect.
 */
public fun ByteArrayInput(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size): Input {
    require(startIndex in 0..endIndex && endIndex <= source.size) {
        "Invalid range of indices ($startIndex..$endIndex) for array of size ${source.size}"
    }
    return kotlinx.io.internal.ByteArrayInput(source, startIndex, endIndex)
}

/**
 * Wraps an input, limiting the number bytes, which can be read up to the given [limit].
 * The resulting input will be closed as soon as either the original input is exhausted
 * or [limit] bytes is read.
 */
public fun Input.limit(limit: Long): Input {
    require(limit >= 0) { "Limit must not be negative, have $limit" }
    return LimitingInput(this, limit)
}

/**
 * Wraps an input, limiting the number bytes which can be read up to the given [limit].
 * The resulting input will be closed as soon as either the original input is exhausted
 * or [limit] bytes is read.
 */
public fun Input.limit(limit: Int) = limit(limit.toLong())