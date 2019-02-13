@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.bits

import kotlinx.io.core.internal.*
import org.khronos.webgl.*
import kotlin.require

/**
 * Represents a linear range of bytes.
 */
actual class Memory @DangerousInternalIoApi constructor(val view: DataView) {
    /**
     * Size of memory range in bytes.
     */
    actual inline val size: Long get() = view.byteLength.toLong()

    /**
     * Size of memory range in bytes represented as signed 32bit integer
     * @throws IllegalStateException when size doesn't fit into a signed 32bit integer
     */
    actual inline val size32: Int get() = view.byteLength

    /**
     * Returns byte at [index] position.
     */
    actual inline fun getAt(index: Int): Byte {
        return view.getInt8(index)
    }

    /**
     * Returns byte at [index] position.
     */
    actual inline fun getAt(index: Long): Byte {
        return view.getInt8(index.toIntOrFail("index"))
    }

    /**
     * Write [value] at the specified [index].
     */
    actual inline fun setAt(index: Int, value: Byte) {
        view.setInt8(index, value)
    }

    /**
     * Write [value] at the specified [index]
     */
    actual inline fun setAt(index: Long, value: Byte) {
        view.setInt8(index.toIntOrFail("index"), value)
    }

    /**
     * Returns memory's subrange. On some platforms it could do range checks but it is not guaranteed to be safe.
     * It also could lead to memory allocations on some platforms.
     */
    actual fun slice(offset: Int, length: Int): Memory {
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        if (offset + length > size) {
            throw IndexOutOfBoundsException("offset + length > size: $offset + $length > $size")
        }

        return Memory(
            DataView(
                view.buffer,
                view.byteOffset + offset,
                length
            )
        )
    }

    /**
     * Returns memory's subrange. On some platforms it could do range checks but it is not guaranteed to be safe.
     * It also could lead to memory allocations on some platforms.
     */
    actual fun slice(offset: Long, length: Long): Memory {
        return slice(offset.toIntOrFail("offset"), length.toIntOrFail("length"))
    }

    /**
     * Copies bytes from this memory range from the specified [offset] and [length]
     * to the [destination] at [destinationOffset].
     * Copying bytes from a memory to itself is allowed.
     */
    actual fun copyTo(
        destination: Memory,
        offset: Int,
        length: Int,
        destinationOffset: Int
    ) {
        val src = Int8Array(view.buffer, view.byteOffset + offset, length)
        val dst = Int8Array(destination.view.buffer, destination.view.byteOffset + destinationOffset, length)

        dst.set(src)
    }

    /**
     * Copies bytes from this memory range from the specified [offset] and [length]
     * to the [destination] at [destinationOffset].
     * Copying bytes from a memory to itself is allowed.
     */
    actual fun copyTo(
        destination: Memory,
        offset: Long,
        length: Long,
        destinationOffset: Long
    ) {
        copyTo(
            destination,
            offset.toIntOrFail("offset"),
            length.toIntOrFail("length"),
            destinationOffset.toIntOrFail("destinationOffset")
        )
    }

    actual companion object {
        /**
         * Represents an empty memory region
         */
        actual val Empty: Memory = Memory(DataView(ArrayBuffer(0)))
    }
}

fun Memory.Companion.of(buffer: ArrayBuffer, offset: Int = 0, length: Int = buffer.byteLength - offset): Memory {
    return Memory(DataView(buffer, offset, length))
}

fun Memory.Companion.of(view: DataView): Memory {
    return Memory(view)
}

fun Memory.Companion.of(view: ArrayBufferView, offset: Int = 0, length: Int = view.byteLength): Memory {
    return Memory.of(view.buffer, view.byteOffset + offset, length)
}

actual inline fun Memory.getShortAt(offset: Int): Short = view.getInt16(offset, false)

actual inline fun Memory.getShortAt(offset: Long): Short = getShortAt(offset.toIntOrFail("offset"))

actual inline fun Memory.getIntAt(offset: Int): Int = view.getInt32(offset, false)

actual inline fun Memory.getIntAt(offset: Long): Int = getIntAt(offset.toIntOrFail("offset"))

actual inline fun Memory.getLongAt(offset: Int): Long =
    (view.getInt32(offset, false).toLong() shl 32) or
        view.getInt32(offset + 4, false).toLong()

actual inline fun Memory.getLongAt(offset: Long): Long = getLongAt(offset.toIntOrFail("offset"))

actual inline fun Memory.getFloatAt(offset: Int): Float = view.getFloat32(offset, false)

actual inline fun Memory.getFloatAt(offset: Long): Float = getFloatAt(offset.toIntOrFail("offset"))

actual inline fun Memory.getDoubleAt(offset: Int): Double = view.getFloat64(offset, false)

actual inline fun Memory.getDoubleAt(offset: Long): Double = getDoubleAt(offset.toIntOrFail("offset"))

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.setIntAt(offset: Int, value: Int) {
    view.setInt32(offset, value, littleEndian = false)
}

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.setIntAt(offset: Long, value: Int) {
    view.setInt32(offset.toIntOrFail("offset"), value, littleEndian = false)
}

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.setShortAt(offset: Int, value: Short) {
    view.setInt16(offset, value, littleEndian = false)
}

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.setShortAt(offset: Long, value: Short) {
    view.setInt16(offset.toIntOrFail("offset"), value, littleEndian = false)
}

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.setLongAt(offset: Int, value: Long) {
    view.setInt32(offset, (value shr 32).toInt(), littleEndian = false)
    view.setInt32(offset + 4, (value and 0xffffffffL).toInt(), littleEndian = false)
}

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.setLongAt(offset: Long, value: Long) {
    setLongAt(offset.toIntOrFail("offset"), value)
}

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Memory.setFloatAt(offset: Int, value: Float) {
    view.setFloat32(offset, value, littleEndian = false)
}

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Memory.setFloatAt(offset: Long, value: Float) {
    view.setFloat32(offset.toIntOrFail("offset"), value, littleEndian = false)
}

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Memory.setDoubleAt(offset: Int, value: Double) {
    view.setFloat64(offset, value, littleEndian = false)
}

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Memory.setDoubleAt(offset: Long, value: Double) {
    view.setFloat64(offset.toIntOrFail("offset"), value, littleEndian = false)
}

@PublishedApi
internal inline fun Long.toIntOrFail(name: String): Int {
    if (this >= Int.MAX_VALUE) failLongToIntConversion(this, name)
    return toInt()
}

@PublishedApi
internal fun failLongToIntConversion(value: Long, name: String): Nothing =
    throw IllegalArgumentException("Long value $value of $name doesn't fit into 32-bit integer")

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
    @Suppress("UnsafeCastFromDynamic")
    val to: Int8Array = destination.asDynamic()

    val from = Int8Array(view.buffer, view.byteOffset + offset, length)

    to.set(from, destinationOffset)
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
    copyTo(destination, offset.toIntOrFail("offset"), length, destinationOffset)
}
