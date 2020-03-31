package kotlinx.io.internal

import kotlinx.io.*
import kotlinx.io.buffer.Buffer
import kotlinx.io.buffer.UnmanagedBufferPool
import kotlinx.io.buffer.storeByteArray

internal class ByteArrayInput(
    private val source: ByteArray,
    startIndex: Int,
    private val endIndex: Int
) : CountingInput(UnmanagedBufferPool.Instance) {

//    private var currentIndex = startIndex

    init {
        position(startIndex)
    }

    override fun closeSource() {
        // Nothing by default
    }

//    override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int {
//        val size = (this.endIndex - currentIndex).coerceAtMost(endIndex - startIndex)
//        buffer.storeByteArray(startIndex, source, currentIndex, size)
//        currentIndex += size
//        return size
//    }

    override fun fillCounting(buffer: Buffer, startIndex: Int, endIndex: Int, absoluteBufferIndex: Int): Int {
        val size = (this.endIndex - absoluteBufferIndex).coerceAtMost(endIndex - startIndex)
        buffer.storeByteArray(startIndex, source, absoluteBufferIndex, size)
        return size
    }

    /**
     * Copy-free binary generation from an array
     */
    override fun readBinary(size: Int): Binary {
        //TODO could be done via
        checkSize(size)
        return ByteArrayBinary(source, absolutePosition(), size).also {
            discardExact(size) // throws error if trying to read more, than possible
        }
    }
}
