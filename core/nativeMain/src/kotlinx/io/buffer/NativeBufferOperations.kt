package kotlinx.io.buffer

import kotlinx.cinterop.*
import kotlinx.io.bits.internal.utils.*
import platform.posix.*
import kotlin.contracts.*
import kotlinx.io.*
/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 * Copying bytes from a memory to itself is allowed.
 */
actual fun Buffer.copyTo(destination: Buffer, offset: Int, length: Int, destinationOffset: Int) {
    require(offset >= 0) { "offset shouldn't be negative: $offset" }
    require(length >= 0) { "length shouldn't be negative: $length" }
    require(destinationOffset >= 0) { "destinationOffset shouldn't be negative: $destinationOffset" }

    if (offset + length > size) {
        throw IndexOutOfBoundsException("offset + length > size: $offset + $length > $size")
    }
    if (destinationOffset + length > destination.size) {
        throw IndexOutOfBoundsException(
            "dst offset + length > size: $destinationOffset + $length > ${destination.size}"
        )
    }

    if (length == 0) return

    memcpy(
        destination.pointer + destinationOffset,
        pointer + offset, length.convert()
    )
}

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
actual fun Buffer.copyTo(
    destination: ByteArray,
    offset: Int,
    length: Int,
    destinationOffset: Int
) {
    if (destination.isEmpty() && destinationOffset == 0 && length == 0) {
        // NOTE: this is required since pinned.getAddressOf(0) will crash with exception
        return
    }

    destination.usePinned { pinned ->
        copyTo(
            destination = Buffer(pinned.addressOf(0), destination.size),
            offset = offset,
            length = length,
            destinationOffset = destinationOffset
        )
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

internal fun Float.toBigEndian(): Float = when (PLATFORM_BIG_ENDIAN) {
    1 -> this
    else -> swap(this)
}

internal fun Double.toBigEndian(): Double = when (PLATFORM_BIG_ENDIAN) {
    1 -> this
    else -> swap(this)
}

/**
 * Fill memory range starting at the specified [offset] with [value] repeated [count] times.
 */
actual fun Buffer.fill(offset: Int, count: Int, value: Byte) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(count, "count")
    requireRange(offset, count, size, "fill")
    require(count.toULong() <= size_t.MAX_VALUE.toULong()) { "count is too big: it shouldn't exceed size_t.MAX_VALUE" }
    memset(pointer + offset, value.toInt(), count.convert())
}

/**
 * Copy content bytes to the memory addressed by the [destination] pointer with
 * the specified [destinationOffset] in bytes.
 */
fun Buffer.copyTo(
    destination: CPointer<ByteVar>,
    offset: Int,
    length: Int,
    destinationOffset: Int
) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(length, "length")
    requirePositiveIndex(destinationOffset, "destinationOffset")
    requireRange(offset, length, size, "source memory")

    memcpy(destination + destinationOffset, pointer + offset, length.convert())
}

/**
 * Copy [length] bytes to the [destination] at the specified [destinationOffset]
 * from the memory addressed by this pointer with [offset] in bytes.
 */
fun CPointer<ByteVar>.copyTo(destination: Buffer, offset: Int, length: Int, destinationOffset: Int) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(length, "length")
    requirePositiveIndex(destinationOffset, "destinationOffset")
    requireRange(destinationOffset, length, destination.size, "source memory")

    memcpy(destination.pointer + destinationOffset, this + offset, length.convert())
}

actual inline fun <R> ByteArray.useBuffer(offset: Int, length: Int, block: (Buffer) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return usePinned { pinned ->
        val memory = when {
            isEmpty() && offset == 0 && length == 0 -> Buffer.EMPTY
            else -> Buffer(pinned.addressOf(offset), length)
        }

        block(memory)
    }
}
