package kotlinx.io.buffer

import kotlinx.cinterop.*
import kotlinx.io.*
import kotlinx.io.bits.internal.utils.*
import platform.posix.*

/**
 * Copies shorts integers from this memory range from the specified [offset] and [count]
 * to the [destination] at [destinationOffset] interpreting numbers in the network order (Big Endian).
 * @param destinationOffset items
 */
actual fun Buffer.loadShortArray(
    offset: Int,
    destination: ShortArray,
    destinationOffset: Int,
    count: Int
) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(destinationOffset, "destinationOffset")
    requirePositiveIndex(count, "count")

    if (count == 0)
        return

    requireRange(destinationOffset, count, destination.size, "destination")
    requireRange(offset, count * 2, size, "memory")

    if (PLATFORM_BIG_ENDIAN == 1) {
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
actual fun Buffer.loadIntArray(
    offset: Int,
    destination: IntArray,
    destinationOffset: Int,
    count: Int
) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(destinationOffset, "destinationOffset")
    requirePositiveIndex(count, "count")

    if (count == 0)
        return

    requireRange(destinationOffset, count, destination.size, "destination")
    requireRange(offset, count * 4, size, "memory")

    if (PLATFORM_BIG_ENDIAN == 1) {
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
actual fun Buffer.loadLongArray(
    offset: Int,
    destination: LongArray,
    destinationOffset: Int,
    count: Int
) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(destinationOffset, "destinationOffset")
    requirePositiveIndex(count, "count")

    if (count == 0)
        return

    requireRange(destinationOffset, count, destination.size, "destination")
    requireRange(offset, count * 8, size, "memory")

    if (PLATFORM_BIG_ENDIAN == 1) {
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
actual fun Buffer.loadFloatArray(
    offset: Int,
    destination: FloatArray,
    destinationOffset: Int,
    count: Int
) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(destinationOffset, "destinationOffset")
    requirePositiveIndex(count, "count")

    if (count == 0)
        return

    requireRange(destinationOffset, count, destination.size, "destination")
    requireRange(offset, count * 4, size, "memory")

    if (PLATFORM_BIG_ENDIAN == 1) {
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
actual fun Buffer.loadDoubleArray(
    offset: Int,
    destination: DoubleArray,
    destinationOffset: Int,
    count: Int
) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(destinationOffset, "destinationOffset")
    requirePositiveIndex(count, "count")

    if (count == 0)
        return

    requireRange(destinationOffset, count, destination.size, "destination")
    requireRange(offset, count * 8, size, "memory")

    if (PLATFORM_BIG_ENDIAN == 1) {
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

/**
 * Copies shorts integers from from the [source] array at [sourceOffset] to this memory at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
actual fun Buffer.storeShortArray(
    offset: Int,
    source: ShortArray,
    sourceOffset: Int,
    count: Int
) {
    storeArrayIndicesCheck(offset, sourceOffset, count, 2, source.size, size)
    if (count == 0) return

    if (PLATFORM_BIG_ENDIAN == 1) {
        copy(source, pointer.plus(offset)!!, sourceOffset, count)
    } else if (unalignedAccessSupported || isAlignedShort(offset)) {
        val destination = pointer.plus(offset)!!.reinterpret<ShortVar>()

        for (index in 0 until count) {
            destination[index] = source[index + sourceOffset].reverseByteOrder()
        }
    } else {
        val destination = pointer.plus(offset)!!

        for (index in 0 until count) {
            storeShortSlowAt(destination.plus(index * 2)!!, source[index + sourceOffset])
        }
    }
}

/**
 * Copies regular integers from from the [source] array at [sourceOffset] to this memory at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
actual fun Buffer.storeIntArray(
    offset: Int,
    source: IntArray,
    sourceOffset: Int,
    count: Int
) {
    storeArrayIndicesCheck(offset, sourceOffset, count, 4, source.size, size)
    if (count == 0) return

    if (PLATFORM_BIG_ENDIAN == 1) {
        copy(source, pointer.plus(offset)!!, sourceOffset, count)
    } else if (unalignedAccessSupported || isAlignedInt(offset)) {
        val destination = pointer.plus(offset)!!.reinterpret<IntVar>()

        for (index in 0 until count) {
            destination[index] = source[index + sourceOffset].reverseByteOrder()
        }
    } else {
        val destination = pointer.plus(offset)!!

        for (index in 0 until count) {
            storeIntSlowAt(destination.plus(index * 4)!!, source[index + sourceOffset])
        }
    }
}

/**
 * Copies regular integers from from the [source] array at [sourceOffset] to this memory at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
actual fun Buffer.storeLongArray(
    offset: Int,
    source: LongArray,
    sourceOffset: Int,
    count: Int
) {
    storeArrayIndicesCheck(offset, sourceOffset, count, 8, source.size, size)
    if (count == 0) return

    if (PLATFORM_BIG_ENDIAN == 1) {
        copy(source, pointer.plus(offset)!!, sourceOffset, count)
    } else if (unalignedAccessSupported || isAlignedShort(offset)) {
        val destination = pointer.plus(offset)!!.reinterpret<LongVar>()

        for (index in 0 until count) {
            destination[index] = source[index + sourceOffset].reverseByteOrder()
        }
    } else {
        val destination = pointer.plus(offset)!!

        for (index in 0 until count) {
            storeLongSlowAt(destination.plus(index * 8L)!!, source[index + sourceOffset])
        }
    }
}

/**
 * Copies floating point numbers from from the [source] array at [sourceOffset] to this memory at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
actual fun Buffer.storeFloatArray(
    offset: Int,
    source: FloatArray,
    sourceOffset: Int,
    count: Int
) {
    storeArrayIndicesCheck(offset, sourceOffset, count, 4, source.size, size)
    if (count == 0) return

    if (PLATFORM_BIG_ENDIAN == 1) {
        copy(source, pointer.plus(offset)!!, sourceOffset, count)
    } else if (unalignedAccessSupported || isAlignedInt(offset)) {
        val destination = pointer.plus(offset)!!.reinterpret<FloatVar>()

        for (index in 0 until count) {
            destination[index] = source[index + sourceOffset].reverseByteOrder()
        }
    } else {
        val destination = pointer.plus(offset)!!

        for (index in 0 until count) {
            storeFloatSlowAt(destination.plus(index * 4)!!, source[index + sourceOffset])
        }
    }
}

/**
 * Copies floating point numbers from from the [source] array at [sourceOffset] to this memory at the specified [offset]
 * interpreting numbers in the network order (Big Endian).
 * @param sourceOffset items
 */
actual fun Buffer.storeDoubleArray(
    offset: Int,
    source: DoubleArray,
    sourceOffset: Int,
    count: Int
) {
    storeArrayIndicesCheck(offset, sourceOffset, count, 8, source.size, size)
    if (count == 0) return

    if (PLATFORM_BIG_ENDIAN == 1) {
        copy(source, pointer.plus(offset)!!, sourceOffset, count)
    } else if (unalignedAccessSupported || isAlignedShort(offset)) {
        val destination = pointer.plus(offset)!!.reinterpret<DoubleVar>()

        for (index in 0 until count) {
            destination[index] = source[index + sourceOffset].reverseByteOrder()
        }
    } else {
        val destination = pointer.plus(offset)!!

        for (index in 0 until count) {
            storeDoubleSlowAt(destination.plus(index * 8L)!!, source[index + sourceOffset])
        }
    }
}
