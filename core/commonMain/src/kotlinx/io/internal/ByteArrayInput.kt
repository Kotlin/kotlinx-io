package kotlinx.io.internal

import kotlinx.io.*
import kotlinx.io.buffer.*

internal class ByteArrayInput(
    private val source: ByteArray, startIndex: Int,
    private val endIndex: Int
) : Input() {

    private var currentIndex = startIndex

    override fun closeSource() {
       // Nothing by default
    }

    override fun fill(buffer: Buffer): Int {
        val size = (endIndex - currentIndex).coerceAtMost(buffer.size)
        repeat(size) {
            buffer[it] = source[currentIndex]
            ++currentIndex
        }
        return size
    }
}
