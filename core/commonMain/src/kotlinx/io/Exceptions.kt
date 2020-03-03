package kotlinx.io

import kotlinx.io.buffer.*

expect open class IOException(message: String, cause: Throwable?) : Exception {
    constructor(message: String)
}

expect open class EOFException(message: String) : IOException

internal fun checkBufferAndIndexes(buffer: Buffer, startIndex: Int, endIndex: Int) {
    require(startIndex >= 0) { "Start index ($startIndex) should be positive." }
    require(endIndex >= startIndex) { "End index ($endIndex) should be greater than start index ($startIndex)." }
    require(endIndex <= buffer.size) {
        "End index ($endIndex) cannot be greater than buffer size (${buffer.size})."
    }
}

internal fun checkArrayStartAndLength(array: ByteArray, startIndex: Int, length: Int) {
    require(startIndex >= 0) { "Start index ($startIndex) should be positive." }
    require(length >= 0) { "Length ($length) should be positive." }
    require(startIndex + length <= array.size) {
        "(start index + length) (${startIndex} + ${length}) cannot be greater than array size (${array.size})."
    }
}

internal fun checkSize(size: Int) {
    require(size >= 0) { "Size ($size) cannot be negative." }
}

internal fun checkCount(count: Int) {
    require(count >= 0) { "Count ($count) should be positive." }
}

internal fun unexpectedEOF(text: String): Nothing = throw EOFException(text)