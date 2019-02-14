@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.bits

import kotlinx.io.core.internal.*
import java.nio.*
import java.util.*

@Suppress("ACTUAL_WITHOUT_EXPECT", "EXPERIMENTAL_FEATURE_WARNING")
actual inline class Memory @DangerousInternalIoApi constructor(val buffer: ByteBuffer) {

    /**
     * Size of memory range in bytes.
     */
    actual inline val size: Long get() = buffer.limit().toLong()

    /**
     * Size of memory range in bytes represented as signed 32bit integer
     * @throws IllegalStateException when size doesn't fit into a signed 32bit integer
     */
    actual inline val size32: Int get() = buffer.limit()

    /**
     * Returns byte at [index] position.
     */
    actual inline fun loadAt(index: Int): Byte = buffer.get(index)

    /**
     * Returns byte at [index] position.
     */
    actual inline fun loadAt(index: Long): Byte = buffer.get(index.toIntOrFail("index"))

    /**
     * Write [value] at the specified [index].
     */
    actual inline fun storeAt(index: Int, value: Byte) {
        buffer.put(index, value)
    }

    /**
     * Write [value] at the specified [index]
     */
    actual inline fun storeAt(index: Long, value: Byte) {
        buffer.put(index.toIntOrFail("index"), value)
    }

    actual fun slice(offset: Int, length: Int): Memory =
        Memory(buffer.sliceSafe(offset, length))

    actual fun slice(offset: Long, length: Long): Memory {
        return slice(offset.toIntOrFail("offset"), length.toIntOrFail("length"))
    }

    /**
     * Copies bytes from this memory range from the specified [offset] and [length]
     * to the [destination] at [destinationOffset].
     * Copying bytes from a memory to itself is allowed.
     */
    actual fun copyTo(destination: Memory, offset: Int, length: Int, destinationOffset: Int) {
        if (buffer.hasArray() && destination.buffer.hasArray() &&
            !buffer.isReadOnly && !destination.buffer.isReadOnly
        ) {
            System.arraycopy(
                buffer.array(), buffer.arrayOffset() + offset,
                destination.buffer.array(), destination.buffer.arrayOffset() + destinationOffset,
                length
            )
            return
        }

        // NOTE: it is ok here to make copy since it will be escaped by JVM
        // while temporary moving position/offset makes memory concurrent unsafe that is unacceptable

        val srcCopy = buffer.duplicate().apply {
            position(offset)
            limit(offset + length)
        }
        val dstCopy = destination.buffer.duplicate().apply {
            position(destinationOffset)
        }

        dstCopy.put(srcCopy)
    }

    /**
     * Copies bytes from this memory range from the specified [offset] and [length]
     * to the [destination] at [destinationOffset].
     * Copying bytes from a memory to itself is allowed.
     */
    actual fun copyTo(destination: Memory, offset: Long, length: Long, destinationOffset: Long) {
        copyTo(
            destination, offset.toIntOrFail("offset"),
            length.toIntOrFail("length"),
            destinationOffset.toIntOrFail("destinationOffset")
        )
    }

    actual companion object {
        actual val Empty: Memory = Memory(ByteBuffer.allocate(0).order(ByteOrder.BIG_ENDIAN))
    }
}

actual inline fun Memory.loadShortAt(offset: Int): Short {
    return buffer.getShort(offset)
}

actual inline fun Memory.loadShortAt(offset: Long): Short {
    return buffer.getShort(offset.toIntOrFail("offset"))
}

actual inline fun Memory.loadIntAt(offset: Int): Int {
    return buffer.getInt(offset)
}

actual inline fun Memory.loadIntAt(offset: Long): Int {
    return buffer.getInt(offset.toIntOrFail("offset"))
}

actual inline fun Memory.loadLongAt(offset: Int): Long {
    return buffer.getLong(offset)
}

actual inline fun Memory.loadLongAt(offset: Long): Long {
    return buffer.getLong(offset.toIntOrFail("offset"))
}

actual inline fun Memory.loadFloatAt(offset: Int): Float {
    return buffer.getFloat(offset)
}

actual inline fun Memory.loadFloatAt(offset: Long): Float {
    return buffer.getFloat(offset.toIntOrFail("offset"))
}

actual inline fun Memory.loadDoubleAt(offset: Int): Double {
    return buffer.getDouble(offset)
}

actual inline fun Memory.loadDoubleAt(offset: Long): Double {
    return buffer.getDouble(offset.toIntOrFail("offset"))
}

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeIntAt(offset: Int, value: Int) {
    buffer.putInt(offset, value)
}

/**
 * Write regular signed 32bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeIntAt(offset: Long, value: Int) {
    storeIntAt(offset.toIntOrFail("offset"), value)
}

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeShortAt(offset: Int, value: Short) {
    buffer.putShort(offset, value)
}

/**
 * Write short signed 16bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeShortAt(offset: Long, value: Short) {
    storeShortAt(offset.toIntOrFail("offset"), value)
}

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeLongAt(offset: Int, value: Long) {
    buffer.putLong(offset, value)
}

/**
 * Write short signed 64bit integer in the network byte order (Big Endian)
 */
actual inline fun Memory.storeLongAt(offset: Long, value: Long) {
    storeLongAt(offset.toIntOrFail("offset"), value)
}

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Memory.storeFloatAt(offset: Int, value: Float) {
    buffer.putFloat(offset, value)
}

/**
 * Write short signed 32bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Memory.storeFloatAt(offset: Long, value: Float) {
    storeFloatAt(offset.toIntOrFail("offset"), value)
}

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Memory.storeDoubleAt(offset: Int, value: Double) {
    buffer.putDouble(offset, value)
}

/**
 * Write short signed 64bit floating point number in the network byte order (Big Endian)
 */
actual inline fun Memory.storeDoubleAt(offset: Long, value: Double) {
    storeDoubleAt(offset.toIntOrFail("offset"), value)
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
    if (buffer.hasArray() && !buffer.isReadOnly) {
        System.arraycopy(
            buffer.array(), buffer.arrayOffset() + offset,
            destination, destinationOffset, length
        )
        return
    }

    // we need to make a copy to prevent moving position
    buffer.duplicate().get(destination, destinationOffset, length)
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

private inline fun ByteBuffer.myDuplicate(): ByteBuffer {
    duplicate().apply { return suppressNullCheck() }
}

private inline fun ByteBuffer.mySlice(): ByteBuffer {
    slice().apply { return suppressNullCheck() }
}

private inline fun ByteBuffer.suppressNullCheck(): ByteBuffer {
    return this
}

private fun ByteBuffer.sliceSafe(offset: Int, length: Int): ByteBuffer {
    return myDuplicate().apply { position(offset); limit(offset + length) }.mySlice()
}

/**
 * Fill memory range starting at the specified [offset] with [value] repeated [count] times.
 */
actual fun Memory.fill(offset: Long, count: Long, value: Byte) {
    fill(offset.toIntOrFail("offset"), count.toIntOrFail("count"), value)
}

/**
 * Fill memory range starting at the specified [offset] with [value] repeated [count] times.
 */
actual fun Memory.fill(offset: Int, count: Int, value: Byte) {
    for (index in offset until offset + count) {
        buffer.put(index, value)
    }
}