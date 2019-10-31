package kotlinx.io.buffer

import kotlin.IndexOutOfBoundsException

/**
 * Represents a linear range of bytes.
 * Instance of this class has no additional state except the bytes themselves.
 * TODO allocation, resources, random-access documentation
 */
public expect class Buffer {
    /**
     * Size of buffer range in bytes.
     */
    public val size: Int

    /**
     * Returns byte at [index] position.
     * May throw [IndexOutOfBoundsException] if index is negative or greater than buffer size.
     */
    public fun loadByteAt(index: Int): Byte

    /**
     * Writes byte [value] at the specified [index].
     * May throw [IndexOutOfBoundsException] if index is negative or greater than buffer size.
     */
    public fun storeByteAt(index: Int, value: Byte)

    public companion object {
        /**
         * Represents an empty buffer region.
         * TODO decide on its presence
         */
        public val Empty: Buffer
    }
}
