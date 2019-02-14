@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.bits

import java.nio.*

/**
 * Execute [block] of code providing a temporary instance of [Memory] view of this byte array range
 * starting at the specified [offset] and having the specified bytes [length].
 * By default, if neither [offset] nor [length] specified, the whole array is used.
 * An instance of [Memory] provided into the [block] should be never captured and used outside of lambda.
 */
actual inline fun <R> ByteArray.useMemory(offset: Int, length: Int, block: (Memory) -> R): R {
    return Memory(ByteBuffer.wrap(this, offset, length)).let(block)
}

/**
 * Create [Memory] view for the specified [array] range starting at [offset] and the specified bytes [length].
 */
inline fun Memory.Companion.of(array: ByteArray, offset: Int = 0, length: Int = array.size - offset): Memory {
    return Memory(ByteBuffer.wrap(array, offset, length).order(ByteOrder.BIG_ENDIAN))
}

/**
 * Create [Memory] view for the specified [buffer] range
 * starting at [ByteBuffer.position] and ending at [ByteBuffer.limit].
 * Changing the original buffer's position/limit will not affect previously created Memory instances.
 */
inline fun Memory.Companion.of(buffer: ByteBuffer): Memory {
    return Memory(buffer.slice().order(ByteOrder.BIG_ENDIAN))
}

