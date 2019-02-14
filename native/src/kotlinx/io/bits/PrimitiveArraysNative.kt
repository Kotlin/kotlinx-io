@file:Suppress("NOTHING_TO_INLINE", "ConstantConditionIf")

package kotlinx.io.bits

import kotlinx.cinterop.*
import platform.posix.memcpy

private const val unalignedAccessSupported = true // TODO

/**
 * Copies shorts integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.loadShortArray(
    offset: Int,
    destination: ShortArray,
    destinationOffset: Int,
    count: Int
) {
    loadShortArray(offset.toLong(), destination, destinationOffset, count)
}

/**
 * Copies shorts integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.loadShortArray(
    offset: Long,
    destination: ShortArray,
    destinationOffset: Int,
    count: Int
) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(destinationOffset, "destinationOffset")
    requirePositiveIndex(count, "count")

    if (count == 0) return

    requireRange(destinationOffset, count, destination.size, "destination")
    requireRange(offset, count * 2L, size, "memory")

    if (PLATFORM_BIG_ENDIAN) {
        destination.usePinned { pinned ->
            memcpy(pinned.addressOf(destinationOffset), pointer + offset, (count * 2L).convert())
        }
    } else if (unalignedAccessSupported || isAlignedShort(offset)) {
        val source = pointer.plus(offset)!!.reinterpret<ShortVar>()

        for (index in 0 until count) {
            destination[index + destinationOffset] = source[index].reverseByteOrder()
        }
    } else {
        destination.usePinned { pinned ->
            memcpy(pinned.addressOf(destinationOffset), pointer + offset, (count * 2L).convert())
        }

        for (index in destinationOffset until destinationOffset + count) {
            destination[index] = destination[index].reverseByteOrder()
        }
    }
}

/**
 * Copies regular integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.loadIntArray(
    offset: Int,
    destination: IntArray,
    destinationOffset: Int,
    count: Int
) {
    loadIntArray(offset.toLong(), destination, destinationOffset, count)
}

/**
 * Copies regular integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.loadIntArray(
    offset: Long,
    destination: IntArray,
    destinationOffset: Int,
    count: Int
) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(destinationOffset, "destinationOffset")
    requirePositiveIndex(count, "count")

    if (count == 0) return

    requireRange(destinationOffset, count, destination.size, "destination")
    requireRange(offset, count * 4L, size, "memory")

    if (PLATFORM_BIG_ENDIAN) {
        destination.usePinned { pinned ->
            memcpy(pinned.addressOf(destinationOffset), pointer + offset, (count * 4L).convert())
        }
    } else if (unalignedAccessSupported || isAlignedInt(offset)) {
        val source = pointer.plus(offset)!!.reinterpret<IntVar>()

        for (index in 0 until count) {
            destination[index + destinationOffset] = source[index].reverseByteOrder()
        }
    } else {
        destination.usePinned { pinned ->
            memcpy(pinned.addressOf(destinationOffset), pointer + offset, (count * 4L).convert())
        }

        for (index in destinationOffset until destinationOffset + count) {
            destination[index] = destination[index].reverseByteOrder()
        }
    }
}

/**
 * Copies regular integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.loadLongArray(
    offset: Int,
    destination: LongArray,
    destinationOffset: Int,
    count: Int
) {
    loadLongArray(offset.toLong(), destination, destinationOffset, count)
}

/**
 * Copies regular integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.loadLongArray(
    offset: Long,
    destination: LongArray,
    destinationOffset: Int,
    count: Int
) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(destinationOffset, "destinationOffset")
    requirePositiveIndex(count, "count")

    if (count == 0) return

    requireRange(destinationOffset, count, destination.size, "destination")
    requireRange(offset, count * 8L, size, "memory")

    if (PLATFORM_BIG_ENDIAN) {
        destination.usePinned { pinned ->
            memcpy(pinned.addressOf(destinationOffset), pointer + offset, (count * 8L).convert())
        }
    } else if (unalignedAccessSupported || isAlignedLong(offset)) {
        val source = pointer.plus(offset)!!.reinterpret<LongVar>()

        for (index in 0 until count) {
            destination[index + destinationOffset] = source[index].reverseByteOrder()
        }
    } else {
        destination.usePinned { pinned ->
            memcpy(pinned.addressOf(destinationOffset), pointer + offset, (count * 8L).convert())
        }

        for (index in destinationOffset until destinationOffset + count) {
            destination[index] = destination[index].reverseByteOrder()
        }
    }
}

/**
 * Copies floating point numbers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.loadFloatArray(
    offset: Int,
    destination: FloatArray,
    destinationOffset: Int,
    count: Int
) {
    loadFloatArray(offset.toLong(), destination, destinationOffset, count)
}

/**
 * Copies floating point numbers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.loadFloatArray(
    offset: Long,
    destination: FloatArray,
    destinationOffset: Int,
    count: Int
) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(destinationOffset, "destinationOffset")
    requirePositiveIndex(count, "count")

    if (count == 0) return

    requireRange(destinationOffset, count, destination.size, "destination")
    requireRange(offset, count * 4L, size, "memory")

    if (PLATFORM_BIG_ENDIAN) {
        destination.usePinned { pinned ->
            memcpy(pinned.addressOf(destinationOffset), pointer + offset, (count * 4L).convert())
        }
    } else if (unalignedAccessSupported || isAlignedInt(offset)) {
        val source = pointer.plus(offset)!!.reinterpret<FloatVar>()

        for (index in 0 until count) {
            destination[index + destinationOffset] = source[index].reverseByteOrder()
        }
    } else {
        destination.usePinned { pinned ->
            memcpy(pinned.addressOf(destinationOffset), pointer + offset, (count * 4L).convert())
        }

        for (index in destinationOffset until destinationOffset + count) {
            destination[index] = destination[index].reverseByteOrder()
        }
    }
}

/**
 * Copies floating point numbers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.loadDoubleArray(
    offset: Int,
    destination: DoubleArray,
    destinationOffset: Int,
    count: Int
) {
    loadDoubleArray(offset.toLong(), destination, destinationOffset, count)
}

/**
 * Copies floating point numbers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Memory.loadDoubleArray(
    offset: Long,
    destination: DoubleArray,
    destinationOffset: Int,
    count: Int
) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(destinationOffset, "destinationOffset")
    requirePositiveIndex(count, "count")

    if (count == 0) return

    requireRange(destinationOffset, count, destination.size, "destination")
    requireRange(offset, count * 8L, size, "memory")

    if (PLATFORM_BIG_ENDIAN) {
        destination.usePinned { pinned ->
            memcpy(pinned.addressOf(destinationOffset), pointer + offset, (count * 8L).convert())
        }
    } else if (unalignedAccessSupported || isAlignedLong(offset)) {
        val source = pointer.plus(offset)!!.reinterpret<DoubleVar>()

        for (index in 0 until count) {
            destination[index + destinationOffset] = source[index].reverseByteOrder()
        }
    } else {
        destination.usePinned { pinned ->
            memcpy(pinned.addressOf(destinationOffset), pointer + offset, (count * 8L).convert())
        }

        for (index in destinationOffset until destinationOffset + count) {
            destination[index] = destination[index].reverseByteOrder()
        }
    }
}

internal inline fun requirePositiveIndex(value: Int, name: String) {
    if (value < 0) {
        throw IndexOutOfBoundsException("$name shouldn't be negative: $value")
    }
}

internal inline fun requirePositiveIndex(value: Long, name: String) {
    if (value < 0L) {
        throw IndexOutOfBoundsException("$name shouldn't be negative: $value")
    }
}

internal inline fun requireRange(offset: Int, length: Int, size: Int, name: String) {
    if (offset + length > size) {
        throw IndexOutOfBoundsException("Wrong offset/count for $name: offset $offset, length $length, size $size")
    }
}

internal inline fun requireRange(offset: Long, length: Long, size: Long, name: String) {
    if (offset + length > size) {
        throw IndexOutOfBoundsException("Wrong offset/count for $name: offset $offset, length $length, size $size")
    }
}

private inline fun Memory.isAlignedShort(offset: Long) = (pointer.toLong() + offset) and 1L == 0L
private inline fun Memory.isAlignedInt(offset: Long) = (pointer.toLong() + offset) and 11L == 0L
private inline fun Memory.isAlignedLong(offset: Long) = (pointer.toLong() + offset) and 111L == 0L
