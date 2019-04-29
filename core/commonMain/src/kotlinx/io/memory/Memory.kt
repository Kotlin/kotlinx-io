@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.memory

import kotlinx.io.internal.*

/**
 * Represents a linear range of bytes.
 * All operations are guarded by range-checks by default however at some platforms they could be disabled
 * in release builds.
 *
 * Instance of this class has no additional state except the bytes themselves.
 */
expect class Memory {
    /**
     * Size of memory range in bytes.
     */
    val size: Int

    /**
     * Returns byte at [index] position.
     */
    inline fun loadAt(index: Int): Byte

    /**
     * Write [value] at the specified [index]
     */
    inline fun storeAt(index: Int, value: Byte)

    /**
     * Returns memory's subrange. On some platforms it could do range checks but it is not guaranteed to be safe.
     * It also could lead to memory allocations on some platforms.
     */
    fun slice(offset: Int, length: Int): Memory

    /**
     * Copies bytes from this memory range from the specified [offset] and [length]
     * to the [destination] at [destinationOffset].
     * Copying bytes from a memory to itself is allowed.
     */
    fun copyTo(destination: Memory, offset: Int, length: Int, destinationOffset: Int)

    companion object {
        /**
         * Represents an empty memory region
         */
        val Empty: Memory

        fun allocate(size: Int): Memory
        fun release(memory: Memory)
    }
}

/**
 * Read byte at the specified [index].
 */
inline operator fun Memory.get(index: Int): Byte = loadAt(index)

/**
 * Read byte at the specified [index].
 */
inline operator fun Memory.get(index: Long): Byte = loadAt(index.toIntOrFail { "index" })

/**
 * Index write operator to write [value] at the specified [index]
 */
inline operator fun Memory.set(index: Long, value: Byte) = storeAt(index.toIntOrFail { "index" }, value)

/**
 * Index write operator to write [value] at the specified [index]
 */
inline operator fun Memory.set(index: Int, value: Byte) = storeAt(index, value)

/**
 * Index write operator to write [value] at the specified [index]
 */
inline fun Memory.storeAt(index: Long, value: UByte) = storeAt(index.toIntOrFail { "index" }, value.toByte())

/**
 * Index write operator to write [value] at the specified [index]
 */
inline fun Memory.storeAt(index: Int, value: UByte) = storeAt(index, value.toByte())

/**
 * Fill memory range starting at the specified [offset] with [value] repeated [count] times.
 */
expect fun Memory.fill(offset: Long, count: Long, value: Byte)

/**
 * Fill memory range starting at the specified [offset] with [value] repeated [count] times.
 */
expect fun Memory.fill(offset: Int, count: Int, value: Byte)

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
expect fun Memory.copyTo(destination: ByteArray, offset: Int, length: Int, destinationOffset: Int = 0)

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
expect fun Memory.copyTo(destination: ByteArray, offset: Long, length: Int, destinationOffset: Int = 0)

/**
 * Returns byte at [index] position.
 */
inline fun Memory.loadAt(index: Long): Byte = loadAt(index.toIntOrFail { "index" })

/**
 * Write [value] at the specified [index].
 */
inline fun Memory.storeAt(index: Long, value: Byte) = storeAt(index.toIntOrFail { "index" }, value)

/**
 * Returns memory's subrange. On some platforms it could do range checks but it is not guaranteed to be safe.
 * It also could lead to memory allocations on some platforms.
 */
fun Memory.slice(offset: Long, length: Long): Memory = slice(offset.toIntOrFail { "offset" }, length.toIntOrFail { "length" })

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 * Copying bytes from a memory to itself is allowed.
 */
fun Memory.copyTo(destination: Memory, offset: Long, length: Long, destinationOffset: Long) =
    copyTo(destination, offset.toIntOrFail { "offset" }, length.toIntOrFail { "length" }, destinationOffset.toIntOrFail { "destinationOffset" })

/**
 * Execute [block] of code providing a temporary instance of [Memory] view of this byte array range
 * starting at the specified [offset] and having the specified bytes [length].
 * By default, if neither [offset] nor [length] specified, the whole array is used.
 * An instance of [Memory] provided into the [block] should be never captured and used outside of lambda.
 */
expect fun <R> ByteArray.useMemory(offset: Int = 0, length: Int = size - offset, block: (Memory) -> R): R

