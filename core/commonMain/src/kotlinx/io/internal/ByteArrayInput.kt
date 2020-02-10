package kotlinx.io.internal

import kotlinx.io.*
import kotlinx.io.buffer.*

internal class ByteArrayInput(
    private val source: ByteArray,
    startIndex: Int,
    private val endIndex: Int
) : Input(UnmanagedBufferPool.Instance) {

    private var currentIndex = startIndex

    override fun closeSource() {
        // Nothing by default
    }

    override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
        val size = (this.endIndex - currentIndex).coerceAtMost(endIndex - startIndex)
        buffer.storeByteArray(startIndex, source, currentIndex, size)
        currentIndex += size
        return size
    }
}
