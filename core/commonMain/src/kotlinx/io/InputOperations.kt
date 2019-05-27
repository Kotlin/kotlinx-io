package kotlinx.io

import kotlinx.io.buffer.*

fun Input.readArray(array: ByteArray, startIndex: Int = 0, length: Int = array.size - startIndex) {
    var remaining = length
    var consumed = 0
    while (remaining > 0) {
        readBufferLength { buffer, offset, size ->
            val consume = minOf(size, remaining)
            buffer.loadByteArray(offset + consumed, array, startIndex + consumed, consume)
            consumed += consume
            remaining -= consume
            consume
        }
    }
}