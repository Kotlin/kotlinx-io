@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.buffer

/**
 * Represents a linear range of bytes.
 * All operations are guarded by range-checks by default however at some platforms they could be disabled
 * in release builds.
 *
 * Instance of this class has no additional state except the bytes themselves.
 */
expect class Buffer {
    /**
     * Size of buffer range in bytes.
     */
    val size: Int

    /**
     * Returns byte at [index] position.
     */
    inline fun loadByteAt(index: Int): Byte

    /**
     * Write byte [value] at the specified [index].
     */
    inline fun storeByteAt(index: Int, value: Byte)
    
    companion object {
        /**
         * Represents an empty buffer region.
         */
        val Empty: Buffer
    }
}
