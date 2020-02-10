package kotlinx.io

import kotlinx.io.buffer.*

expect open class IOException(message: String, cause: Throwable?) : Exception {
    constructor(message: String)
}

expect open class EOFException(message: String) : IOException

internal fun checkBufferAndIndexes(buffer: Buffer, startIndex: Int, endIndex: Int) {
    require(startIndex >= 0) { "Start index should be positive: $startIndex" }
    require(endIndex >= startIndex) { "End index($endIndex) should be greater than start index($startIndex)" }
    require(endIndex <= buffer.size) {
        "End index($endIndex) shouldn't be grater than buffer size ${buffer.size}"
    }
}

internal fun checkArrayStartAndLength(array: ByteArray, startIndex: Int, length: Int) {
    require(startIndex >= 0) { "Start index should be positive: $startIndex" }
    require(length >= 0) { "Length should be positive: $length" }
    require(startIndex + length <= array.size) {
        "(start + length)(${startIndex + length}) shouldn't be grater than array size ${array.size}"
    }
}

internal fun checkSize(size: Int) {
    require(size >= 0) { "Size shouldn't be negative: $size" }
}

internal fun checkCount(count: Int) {
    require(count >= 0) { "Count should be positive: $count" }
}
