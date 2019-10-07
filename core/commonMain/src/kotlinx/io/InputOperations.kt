package kotlinx.io

import kotlinx.io.buffer.*

/**
 * Read [length] bytes from [Input] to [array] from [startIndex].
 *
 * @throws EOFException if not enough bytes available.
 */
fun Input.readArray(array: ByteArray, startIndex: Int = 0, length: Int = array.size - startIndex) {
    var remaining = length
    var consumed = 0
    while (remaining > 0) {
        readBufferLength { buffer, offset, size ->
            val consume = minOf(size, remaining)
            buffer.loadByteArray(offset, array, startIndex + consumed, consume)
            consumed += consume
            remaining -= consume
            consume
        }
    }
}

/**
 * Read [length] bytes from [Input] to [array] from [startIndex].
 *
 * @throws EOFException if not enough bytes available.
 */
fun Input.readArray(array: UByteArray, startIndex: Int = 0, length: Int = array.size - startIndex) {
    var remaining = length
    var consumed = 0
    while (remaining > 0) {
        readBufferLength { buffer, offset, size ->
            val consume = minOf(size, remaining)
            buffer.loadUByteArray(offset, array, startIndex + consumed, consume)
            consumed += consume
            remaining -= consume
            consume
        }
    }
}