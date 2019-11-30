package kotlinx.io.bytes

import kotlinx.io.*
import kotlinx.io.buffer.*

class ByteArrayOutput(initialSize: Int = 16) : Output() {
    init {
        require(initialSize > 0)
    }
    private var array: ByteArray = ByteArray(initialSize)
    private var size = 0

    public fun toArray(): ByteArray = array.copyOf(size)

    override fun flush(source: Buffer, length: Int) {
        ensureCapacity(length)
        for (i in 0 until length) {
            array[size + i] = source[i]
        }
        size += length
    }

    private fun ensureCapacity(length: Int) {
        if (array.size < size + length) {
            array = array.copyOf((size + length) * 2)
        }
    }

    override fun closeSource() {
        // Do nothing
    }
}