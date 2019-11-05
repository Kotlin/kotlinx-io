@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.buffer

import kotlinx.io.internal.*

/**
 * Read byte at the specified [index].
 * May throw [IndexOutOfBoundsException] if index is negative or greater than buffer size.
 */
public inline operator fun Buffer.get(index: Int): Byte = loadByteAt(index)

/**
 * Read byte at the specified [index].
 * May throw [IndexOutOfBoundsException] if index is negative or greater than buffer size.
 */
public inline operator fun Buffer.get(index: Long): Byte = loadByteAt(index)

/**
 * Index write operator to write [value] at the specified [index].
 * May throw [IndexOutOfBoundsException] if index is negative or greater than buffer size.
 */
public inline operator fun Buffer.set(index: Long, value: Byte): Unit = storeByteAt(index, value)

/**
 * Index write operator to write [value] at the specified [index].
 * May throw [IndexOutOfBoundsException] if index is negative or greater than buffer size.
 */
public inline operator fun Buffer.set(index: Int, value: Byte) = storeByteAt(index, value)

/**
 * Fill buffer range starting at the specified [offset] with [value] repeated [count] times.
 */
public fun Buffer.fill(offset: Long, count: Long, value: Byte) {
    fill(offset.toIntOrFail("offset"), count.toIntOrFail("count"), value)
}

/**
 * Fill buffer range starting at the specified [offset] with [value] repeated [count] times.
 */
expect fun Buffer.fill(offset: Int, count: Int, value: Byte)

/**
 * Copies bytes from this buffer range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 * Copying bytes from a buffer to itself is allowed.
 */
expect fun Buffer.copyTo(destination: Buffer, offset: Int, length: Int, destinationOffset: Int)

/**
 * Copies bytes from this buffer range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
expect fun Buffer.copyTo(destination: ByteArray, offset: Int, length: Int, destinationOffset: Int = 0)

/**
 * Copies bytes from this buffer range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
inline fun Buffer.copyTo(destination: ByteArray, offset: Long, length: Int, destinationOffset: Int = 0) {
    copyTo(destination, offset.toIntOrFail { "offset" }, length, destinationOffset)
}

/**
 * Copies bytes from this buffer range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 * Copying bytes from a buffer to itself is allowed.
 */
fun Buffer.copyTo(destination: Buffer, offset: Long, length: Long, destinationOffset: Long) =
    copyTo(destination, offset.toIntOrFail { "offset" }, length.toIntOrFail { "length" }, destinationOffset.toIntOrFail { "destinationOffset" })

/**
 * Execute [block] of code providing a temporary instance of [Buffer] view of this byte array range
 * starting at the specified [offset] and having the specified bytes [length].
 * By default, if neither [offset] nor [length] specified, the whole array is used.
 * An instance of [Buffer] provided into the [block] should be never captured and used outside of lambda.
 */
expect fun <R> ByteArray.useBuffer(offset: Int = 0, length: Int = size - offset, block: (Buffer) -> R): R

