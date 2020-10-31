package kotlinx.io.buffer

import kotlinx.cinterop.*
import kotlinx.io.bits.internal.utils.*
import kotlinx.io.reverseByteOrder
import kotlinx.io.swap
import platform.posix.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 * Copying bytes from a memory to itself is allowed.
 */
public actual fun Buffer.copyTo(destination: Buffer, offset: Int, length: Int, destinationOffset: Int) {
    require(offset >= 0) { "offset shouldn't be negative: $offset" }
    require(length >= 0) { "length shouldn't be negative: $length" }
    require(destinationOffset >= 0) { "destinationOffset shouldn't be negative: $destinationOffset" }

    if (offset + length > size)
        throw IndexOutOfBoundsException("offset + length > size: $offset + $length > $size")

    if (destinationOffset + length > destination.size)
        throw IndexOutOfBoundsException(
            "dst offset + length > size: $destinationOffset + $length > ${destination.size}"
        )

    if (length == 0) return

    usePointer { source ->
        destination.usePointer { destinationPointer ->
            memcpy(destinationPointer + destinationOffset, source + offset, length.convert())
        }
    }
}

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
public actual fun Buffer.copyTo(
    destination: ByteArray,
    offset: Int,
    length: Int,
    destinationOffset: Int
) {
    if (destination.isEmpty() && destinationOffset == 0 && length == 0) {
        // NOTE: this is required since pinned.getAddressOf(0) will crash with exception
        return
    }

    usePointer { source ->
        destination.usePinned { array ->
            memcpy(
                array.addressOf(0) + destinationOffset,
                source + offset, length.convert()
            )
        }
    }
}

internal fun Short.toBigEndian(): Short = when (PLATFORM_BIG_ENDIAN) {
    1 -> this
    else -> swap(this)
}

internal fun Int.toBigEndian(): Int = when (PLATFORM_BIG_ENDIAN) {
    1 -> this
    else -> swap(this)
}

internal fun Long.toBigEndian(): Long = when (PLATFORM_BIG_ENDIAN) {
    1 -> this
    else -> reverseByteOrder()
}

/**
 * Fills the memory range starting at the specified [offset] with [value] repeated [count] times.
 */
public actual fun Buffer.fill(offset: Int, count: Int, value: Byte) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(count, "count")
    requireRange(offset, count, size, "fill")
    require(count.toULong() <= size_t.MAX_VALUE.toULong()) { "count is too big: it shouldn't exceed size_t.MAX_VALUE" }

    usePointer {
        memset(it + offset, value.toInt(), count.convert())
    }
}

/**
 * Copies the content bytes to the memory addressed by the [destination] pointer with
 * the specified [destinationOffset] in bytes.
 */
public fun Buffer.copyTo(destination: CPointer<ByteVar>, offset: Int, length: Int, destinationOffset: Int) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(length, "length")
    requirePositiveIndex(destinationOffset, "destinationOffset")
    requireRange(offset, length, size, "source memory")

    usePointer {
        memcpy(destination + destinationOffset, it + offset, length.convert())
    }
}

/**
 * Copies the [length] bytes to the [destination] at the specified [destinationOffset]
 * from the memory addressed by this pointer with [offset] in bytes.
 */
public fun CPointer<ByteVar>.copyTo(destination: Buffer, offset: Int, length: Int, destinationOffset: Int) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(length, "length")
    requirePositiveIndex(destinationOffset, "destinationOffset")
    requireRange(destinationOffset, length, destination.size, "source memory")

    destination.usePointer {
        memcpy(it + destinationOffset, this + offset, length.convert())
    }
}

/**
 * Executes the [block] of code providing a temporary instance of [Buffer] view of this byte array range
 * starting at the specified [offset] and having the specified bytes [length].
 * By default, if neither [offset] nor [length] specified, the whole array is used.
 * An instance of [Buffer] provided into the [block] should be never captured and used outside of lambda.
 */
public actual inline fun <R> ByteArray.useBuffer(offset: Int, length: Int, block: (Buffer) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val buffer = if (isEmpty() && length == 0)
        Buffer.EMPTY
    else
        Buffer(this, offset, length)

    return block(buffer)
}

/**
 * Compacts the [Buffer]. Moves the of the buffer content from [startIndex] to [endIndex] range to the beginning of the buffer.
 * The copying ranges can overlap.
 *
 * @return [endIndex] - [startIndex] (copied bytes count) or updated [endIndex]
 */
internal actual fun Buffer.compact(startIndex: Int, endIndex: Int): Int {
    val length = endIndex - startIndex

    usePointer {
        memmove(it, it + startIndex, length.convert())
    }

    return length
}
