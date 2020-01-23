package kotlinx.io.buffer

import kotlin.IndexOutOfBoundsException
import kotlinx.io.*

/**
 * [Buffer] represents a linear range of sequentially placed and randomly accessible bytes.
 * [Buffer] primitive is used as the underlying storage for higher-level primitives such as [Bytes], [Input] and [Output].
 *
 * ### State
 * Instances of this class have no additional state except the storage of the bytes itself.
 * Whether the particular instance of the buffer is readable, writable, or reusable is defined
 * by the implementation of the enclosing primitive.
 *
 * For example, [Input.fill] provides a writeable one-shot buffer that cannot be reused, while
 * [Output.flush] provides a buffer that is readable only withing a given limit.
 *
 * The underlying storage *may* represent a resource that should be released by the same entity that was responsible
 * for buffer allocation.
 *
 * ### Allocation
 * Buffers cannot be created directly, only with an instance of [BufferAllocator].
 * When [BufferAllocator] is not specified, [PlatformBufferAllocator] is used.
 * Depending on the used allocator, the allocated buffer may represent a resource
 * that should be released. In that case, [free][BufferAllocator.free] should be used.
 * Refer to the documentation of a particular allocator for more details about allocated buffers.
 * Using a buffer after it was released leads to unspecified behaviour.
 *
 * ### Random-access
 * [Buffer] provides random-access stores and loads with the only restriction on
 * indices being withing 0..[size][Buffer.size] range.
 *
 * ### Endianness
 * [Buffer] itself does not have any built-in endianness, and its content interpretation is defined
 * by the owner/user of the buffer. By default, all stores and loads are performed in network byte order (big-endian).
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
         * Represents an empty buffer.
         */
        public val EMPTY: Buffer
    }
}

/**
 * Wrap [array] into [Buffer] from [startIndex] to [endIndex].
 */
internal expect fun bufferOf(array: ByteArray, startIndex: Int = 0, endIndex: Int = array.size): Buffer
