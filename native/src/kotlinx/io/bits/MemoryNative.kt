@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.bits

import kotlinx.cinterop.*
import kotlinx.io.core.internal.*
import platform.posix.*
import kotlin.require

// TODO doesn't work on Windows
// NOTE: this should be a constant to make llvm reduce redundant conditional jumps
@PublishedApi
internal const val PLATFORM_BIG_ENDIAN = platform.posix.BYTE_ORDER == platform.posix.LITTLE_ENDIAN

actual class Memory @DangerousInternalIoApi constructor(
    val pointer: CPointer<ByteVar>,
    actual inline val size: Long
) {
    init {
        requirePositiveIndex(size, "size")
    }

    /**
     * Size of memory range in bytes represented as signed 32bit integer
     * @throws IllegalStateException when size doesn't fit into a signed 32bit integer
     */
    actual inline val size32: Int get() = size.toIntOrFail("size")

    /**
     * Returns byte at [index] position.
     */
    actual inline fun loadAt(index: Int): Byte = pointer[assertIndex(index, 1)]

    /**
     * Returns byte at [index] position.
     */
    actual inline fun loadAt(index: Long): Byte = pointer[assertIndex(index, 1)]

    /**
     * Write [value] at the specified [index].
     */
    actual inline fun storeAt(index: Int, value: Byte) {
        pointer[assertIndex(index, 1)] = value
    }

    /**
     * Write [value] at the specified [index]
     */
    actual inline fun storeAt(index: Long, value: Byte) {
        pointer[assertIndex(index, 1)] = value
    }

    actual fun slice(offset: Long, length: Long): Memory {
        assertIndex(offset, length)
        if (offset == 0L && length == size) {
            return this
        }

        return Memory(pointer.plus(offset)!!, length)
    }

    actual fun slice(offset: Int, length: Int): Memory {
        assertIndex(offset, length)
        if (offset == 0 && length.toLong() == size) {
            return this
        }

        return Memory(pointer.plus(offset)!!, length.toLong())
    }

    /**
     * Copies bytes from this memory range from the specified [offset] and [length]
     * to the [destination] at [destinationOffset].
     * Copying bytes from a memory to itself is allowed.
     */
    actual fun copyTo(destination: Memory, offset: Int, length: Int, destinationOffset: Int) {
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

        platform.posix.memcpy(
            destination.pointer + destinationOffset,
            pointer + offset, length.convert()
        )
    }

    /**
     * Copies bytes from this memory range from the specified [offset] and [length]
     * to the [destination] at [destinationOffset].
     * Copying bytes from a memory to itself is allowed.
     */
    actual fun copyTo(destination: Memory, offset: Long, length: Long, destinationOffset: Long) {
        require(offset >= 0L) { "offset shouldn't be negative: $offset" }
        require(length >= 0L) { "length shouldn't be negative: $length" }
        require(destinationOffset >= 0L) { "destinationOffset shouldn't be negative: $destinationOffset" }

        if (offset + length > size) {
            throw IndexOutOfBoundsException("offset + length > size: $offset + $length > $size")
        }
        if (destinationOffset + length > destination.size) {
            throw IndexOutOfBoundsException(
                "dst offset + length > size: $destinationOffset + $length > ${destination.size}"
            )
        }

        if (length == 0L) return

        platform.posix.memcpy(
            destination.pointer + destinationOffset,
            pointer + offset, length.convert()
        )
    }

    actual companion object {
        actual val Empty: Memory = Memory(nativeHeap.allocArray(0), 0L)
    }
}

actual inline fun Memory.loadShortAt(offset: Int): Short {
    assertIndex(offset, 2)
    return pointer.plus(offset)!!.reinterpret<ShortVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadShortAt(offset: Long): Short {
    assertIndex(offset, 2)
    return pointer.plus(offset)!!.reinterpret<ShortVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadIntAt(offset: Int): Int {
    assertIndex(offset, 4)
    return pointer.plus(offset)!!.reinterpret<IntVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadIntAt(offset: Long): Int {
    assertIndex(offset, 4)
    return pointer.plus(offset)!!.reinterpret<IntVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadLongAt(offset: Int): Long {
    assertIndex(offset, 8)
    return pointer.plus(offset)!!.reinterpret<LongVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadLongAt(offset: Long): Long {
    assertIndex(offset, 8)
    return pointer.plus(offset)!!.reinterpret<LongVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadFloatAt(offset: Int): Float {
    assertIndex(offset, 4)
    return pointer.plus(offset)!!.reinterpret<FloatVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadFloatAt(offset: Long): Float {
    assertIndex(offset, 4)
    return pointer.plus(offset)!!.reinterpret<FloatVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadDoubleAt(offset: Int): Double {
    assertIndex(offset, 8)
    return pointer.plus(offset)!!.reinterpret<DoubleVar>().pointed.value.toBigEndian()
}

actual inline fun Memory.loadDoubleAt(offset: Long): Double {
    assertIndex(offset, 8)
    return pointer.plus(offset)!!.reinterpret<DoubleVar>().pointed.value.toBigEndian()
}

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeIntAt(offset: Int, value: Int) {
    assertIndex(offset, 4)
    pointer.plus(offset)!!.reinterpret<IntVar>().pointed.value = value.toBigEndian()
}

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeIntAt(offset: Long, value: Int) {
    assertIndex(offset, 4)
    pointer.plus(offset)!!.reinterpret<IntVar>().pointed.value = value.toBigEndian()
}

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeShortAt(offset: Int, value: Short) {
    assertIndex(offset, 2)
    pointer.plus(offset)!!.reinterpret<ShortVar>().pointed.value = value.toBigEndian()
}

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeShortAt(offset: Long, value: Short) {
    assertIndex(offset, 2)
    pointer.plus(offset)!!.reinterpret<ShortVar>().pointed.value = value.toBigEndian()
}

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeLongAt(offset: Int, value: Long) {
    assertIndex(offset, 8)
    pointer.plus(offset)!!.reinterpret<LongVar>().pointed.value = value.toBigEndian()
}

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeLongAt(offset: Long, value: Long) {
    assertIndex(offset, 8)
    pointer.plus(offset)!!.reinterpret<LongVar>().pointed.value = value.toBigEndian()
}

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Memory.storeFloatAt(offset: Int, value: Float) {
    assertIndex(offset, 4)
    pointer.plus(offset)!!.reinterpret<FloatVar>().pointed.value = value.toBigEndian()
}

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Memory.storeFloatAt(offset: Long, value: Float) {
    assertIndex(offset, 4)
    pointer.plus(offset)!!.reinterpret<FloatVar>().pointed.value = value.toBigEndian()
}

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Memory.storeDoubleAt(offset: Int, value: Double) {
    assertIndex(offset, 8)
    pointer.plus(offset)!!.reinterpret<DoubleVar>().pointed.value = value.toBigEndian()
}

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Memory.storeDoubleAt(offset: Long, value: Double) {
    assertIndex(offset, 8)
    pointer.plus(offset)!!.reinterpret<DoubleVar>().pointed.value = value.toBigEndian()
}

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
actual fun Memory.copyTo(
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
            destination = Memory(pinned.addressOf(0), destination.size.toLong()),
            offset = offset,
            length = length,
            destinationOffset = destinationOffset
        )
    }
}

/**
 * Copies bytes from this memory range from the specified [offset] and [length]
 * to the [destination] at [destinationOffset].
 */
actual fun Memory.copyTo(
    destination: ByteArray,
    offset: Long,
    length: Int,
    destinationOffset: Int
) {
    if (destination.isEmpty() && destinationOffset == 0 && length == 0) {
        // NOTE: this is required since pinned.getAddressOf(0) will crash with exception
        return
    }

    destination.usePinned { pinned ->
        copyTo(
            destination = Memory(pinned.addressOf(0), destination.size.toLong()),
            offset = offset,
            length = length.toLong(),
            destinationOffset = destinationOffset.toLong()
        )
    }
}

@PublishedApi
internal inline fun Memory.assertIndex(offset: Int, valueSize: Int): Int {
    assert(offset in 0..size - valueSize) {
        throw IndexOutOfBoundsException("offset $offset outside of range [0; ${size - valueSize})")
    }
    return offset
}

@PublishedApi
internal inline fun Memory.assertIndex(offset: Long, valueSize: Long): Long {
    assert(offset in 0..size - valueSize) {
        throw IndexOutOfBoundsException("offset $offset outside of range [0; ${size - valueSize})")
    }
    return offset
}

@PublishedApi
internal inline fun Short.toBigEndian(): Short = when {
    PLATFORM_BIG_ENDIAN -> this
    else -> reverseByteOrder()
}

@PublishedApi
internal inline fun Int.toBigEndian(): Int = when {
    PLATFORM_BIG_ENDIAN -> this
    else -> reverseByteOrder()
}

@PublishedApi
internal inline fun Long.toBigEndian(): Long = when {
    PLATFORM_BIG_ENDIAN -> this
    else -> reverseByteOrder()
}

@PublishedApi
internal inline fun Float.toBigEndian(): Float = when {
    PLATFORM_BIG_ENDIAN -> this
    else -> reverseByteOrder()
}

@PublishedApi
internal inline fun Double.toBigEndian(): Double = when {
    PLATFORM_BIG_ENDIAN -> this
    else -> reverseByteOrder()
}

