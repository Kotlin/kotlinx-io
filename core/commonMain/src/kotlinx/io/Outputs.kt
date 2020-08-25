package kotlinx.io

import kotlinx.io.buffer.*

/**
 * Output which uses a byte array as its data storage.
 * Byte array automatically grows as data is written to it and
 * can be accessed with [toByteArray].
 * The initial capacity of the underlying array can be specified with initialCapacity parameter.
 *
 * Calling [close][Output.close] on this class has no effect.
 */
public class ByteArrayOutput(initialCapacity: Int = 16) : Output(UnmanagedBufferPool.Instance) {
    init {
        require(initialCapacity > 0) { "Initial capacity should be greater than 0, but has $initialCapacity" }
    }

    private var array: ByteArray = ByteArray(initialCapacity)
    private var size = 0

    /**
     * Returns a copy of all bytes that were written to the current output.
     */
    public fun toByteArray(): ByteArray {
        flush()
        return array.copyOf(size)
    }

    /** @suppress */
    override fun flush(source: Buffer, startIndex: Int, endIndex: Int) {
        ensureCapacity(endIndex - startIndex)
        for (i in startIndex until endIndex) {
            array[size + i] = source[i]
        }
        size += endIndex
    }

    /** @suppress */
    override fun closeSource() {
        // Nothing by default
    }

    private fun ensureCapacity(length: Int) {
        if (array.size < size + length) {
            val minCapacity = size + length
            val powOf2 = powOf2(minCapacity)
            array = array.copyOf(powOf2)
        }
    }

    private fun powOf2(minCapacity: Int) = if (minCapacity >= 1 shl 30) minCapacity else (1 shl (32 - (minCapacity - 1).countLeadingZeroBits()))
}
