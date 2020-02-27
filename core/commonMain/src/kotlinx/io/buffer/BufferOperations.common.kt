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
public inline operator fun Buffer.set(index: Int, value: Byte): Unit = storeByteAt(index, value)

/**
 * Fill buffer range starting at the specified [offset] with [value] repeated [count] times.
 */
public fun Buffer.fill(offset: Long, count: Long, value: Byte) {
    fill(offset.toIntOrFail("offset"), count.toIntOrFail("count"), value)
}

/**
 * Fill buffer range starting at the specified [offset] with [value] repeated [count] times.
 */
public expect fun Buffer.fill(offset: Int, count: Int, value: Byte)

/**
 * Copies bytes from this buffer range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 * Copying bytes from a buffer to itself is allowed.
 */
public expect fun Buffer.copyTo(destination: Buffer, offset: Int, length: Int, destinationOffset: Int = 0)

/**
 * Copies bytes from this buffer range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
public expect fun Buffer.copyTo(destination: ByteArray, offset: Int, length: Int, destinationOffset: Int = 0)

/**
 * Copies bytes from this buffer range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
public inline fun Buffer.copyTo(destination: ByteArray, offset: Long, length: Int, destinationOffset: Int = 0) {
    copyTo(destination, offset.toIntOrFail { "offset" }, length, destinationOffset)
}

/**
 * Copies bytes from this buffer range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 * Copying bytes from a buffer to itself is allowed.
 */
public fun Buffer.copyTo(destination: Buffer, offset: Long, length: Long, destinationOffset: Long): Unit = copyTo(
    destination,
    offset.toIntOrFail { "offset" },
    length.toIntOrFail { "length" },
    destinationOffset.toIntOrFail { "destinationOffset" }
)

/**
 * Copies buffer content with specified [length] from [offset] to new [ByteArray].
 */
public fun Buffer.toByteArray(offset: Int, length: Int): ByteArray {
    val result = ByteArray(length)
    loadByteArray(offset, result, 0, length)
    return result
}

/**
 * Executes [block] of code providing a temporary instance of [Buffer] view of this byte array range
 * starting at the specified [offset] and having the specified bytes [length].
 * By default, if neither [offset] nor [length] specified, the whole array is used.
 * An instance of [Buffer] provided into the [block] should be never captured and used outside of lambda.
 */
public expect fun <R> ByteArray.useBuffer(offset: Int = 0, length: Int = size - offset, block: (Buffer) -> R): R

/**
 * Compacts the [Buffer]. Moves content from ([startIndex], [endIndex]) range to (0, 'endIndex - startIndex') range.
 * The copying ranges can overlap.
 *
 * @return [endIndex] - [startIndex] (copied bytes count) or updated [endIndex]
 */
internal expect fun Buffer.compact(startIndex: Int, endIndex: Int): Int
