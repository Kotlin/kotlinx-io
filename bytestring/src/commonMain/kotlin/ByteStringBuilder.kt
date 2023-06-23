/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.bytestring

import kotlin.math.max

/**
 * A helper class facilitating [ByteString] construction.
 *
 * A builder is characterized by the [capacity] - the number of bytes that an instance of builder
 * can receive before extending an underlying byte sequence, and [size] - the number of bytes being written
 * to the builder.
 *
 * The builder avoids additional copies and allocations when `size == capacity` when [toByteString] called,
 * thus it's recommended to specify expected [ByteString] size as `initialCapacity` when creating a builder.
 *
 * When a builder runs out of available capacity, a new byte sequence with extended capacity
 * will be allocated and previously written data will be copied into it.
 *
 * @param initialCapacity the initial size of an underlying byte sequence.
 */
public class ByteStringBuilder(initialCapacity: Int = 0) {
    private var buffer = ByteArray(initialCapacity)
    private var offset: Int = 0

    /**
     * The number of bytes being written to this builder.
     */
    public val size: Int
        get() = offset

    /**
     * The number of bytes this builder can store without an extension of an internal buffer.
     */
    public val capacity: Int
        get() = buffer.size

    /**
     * Returns a new [ByteString] wrapping all bytes written to this builder.
     *
     * There will be no additional allocations or copying of data when `size == capacity`.
     */
    public fun toByteString(): ByteString {
        if (size == 0) {
            return ByteString.EMPTY
        }
        if (buffer.size == size) {
            return ByteString.wrap(buffer)
        }
        return ByteString(buffer, 0, size)
    }

    /**
     * Append a single byte to this builder.
     *
     * @param byte the byte to append.
     */
    public fun append(byte: Byte) {
        ensureCapacity(size + 1)
        buffer[offset++] = byte
    }

    /**
     * Appends a subarray of [array] starting at [startIndex] and ending at [endIndex] to this builder.
     *
     * @param array the array whose subarray should be appended.
     * @param startIndex the first index (inclusive) to copy data from the [array].
     * @param endIndex the last index (exclusive) to copy data from the [array]
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [array] array indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     */
    public fun append(array: ByteArray, startIndex: Int = 0, endIndex: Int = array.size) {
        require(startIndex <= endIndex) { "startIndex: $startIndex, endIndex: $endIndex" }
        if (startIndex < 0 || endIndex > array.size) {
            throw IndexOutOfBoundsException("startIndex: $startIndex, endIndex: $endIndex")
        }
        ensureCapacity(offset + endIndex - startIndex)

        array.copyInto(buffer, offset, startIndex, endIndex)
        offset += endIndex - startIndex
    }

    private fun ensureCapacity(requiredCapacity: Int) {
        if (buffer.size >= requiredCapacity) {
            return
        }

        var desiredSize = if (buffer.isEmpty()) 16 else (buffer.size * 1.5).toInt()
        desiredSize = max(desiredSize, requiredCapacity)
        val newBuffer = ByteArray(desiredSize)
        buffer.copyInto(newBuffer)
        buffer = newBuffer
    }
}

/**
 * Appends unsigned byte to this builder.
 */
public fun ByteStringBuilder.append(byte: UByte): Unit = append(byte.toByte())

/**
 * Appends a byte string to this builder.
 */
public fun ByteStringBuilder.append(byteString: ByteString) {
    append(byteString.getBackingArrayReference())
}