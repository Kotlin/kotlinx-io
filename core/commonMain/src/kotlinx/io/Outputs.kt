package kotlinx.io

import kotlinx.io.buffer.*

/**
 * Output which uses byte array as its data storage.
 * Byte array grows automatically as data is written to it and
 * can be accessed with [toByteArray].
 * Initial capacity of the underlying array can be specified with [initialCapacity] parameter.
 *
 * Calling [close][Output.close] on this class has no effect.
 */
public class ByteArrayOutput(initialCapacity: Int = 16) : Output() {
    init {
        require(initialCapacity > 0) { "Initial capacity should be greater than 0, but has $initialCapacity" }
    }

    private var array: ByteArray = ByteArray(initialCapacity)
    private var size = 0

    /**
     * Returns a copy of all bytes that were written to the current output.
     */
    public fun toByteArray(): ByteArray = array.copyOf(size)

    /** @suppress */
    override fun flush(source: Buffer, length: Int) {
        ensureCapacity(length)
        for (i in 0 until length) {
            array[size + i] = source[i]
        }
        size += length
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

    @UseExperimental(ExperimentalStdlibApi::class)
    private fun powOf2(minCapacity: Int) = if (minCapacity >= 1 shl 30) minCapacity else (1 shl (32 - (minCapacity - 1).countLeadingZeroBits()))
}
