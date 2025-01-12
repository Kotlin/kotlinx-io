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
 * @sample kotlinx.io.bytestring.samples.ByteStringSamples.builderSample
 * @sample kotlinx.io.bytestring.samples.ByteStringSamples.builderSampleWithoutAdditionalAllocs
 */
public class ByteStringBuilder private constructor(
	private var buffer: ByteArray,
	private var offset: Int,
	private val extendable: Boolean
) {
	private val safeBuffer get() = if (extendable) buffer else buffer.copyOf()

    /**
     * The number of bytes being written to this builder.
     */
    public val size: Int
        get() = if (extendable) offset else buffer.size

    /**
     * The number of bytes this builder can store without extending the internal buffer or throwing.
     */
    public val capacity: Int
        get() = buffer.size

	/**
	 * Creates a new [ByteStringBuilder].
	 *
	 * If this builder runs out of available capacity,
	 * a new byte sequence with extended capacity will be allocated and previously written data will be copied to it.
	 *
	 * @param initialCapacity the initial size of the underlying byte sequence.
	 */
	public constructor(initialCapacity: Int = 0) : this(ByteArray(initialCapacity), 0, true)

	/**
	 * Creates a new [ByteStringBuilder] on top of an existing [buffer].
	 *
	 * This builder throws when running out of available capacity.
	 *
	 * @param initialOffset the offset at which the builder will begin overriding bytes.
	 */
	public constructor(buffer: ByteArray, initialOffset: Int = 0) : this(buffer, initialOffset, false)

	/**
	 * Returns a new [ByteString] wrapping all bytes written to this builder.
	 *
	 * There will be no additional allocations or copying of data when `size == capacity`.
	 */
	public fun toByteString(): ByteString {
		if (size == 0) {
			return ByteString()
		}
		if (buffer.size == size) {
			return ByteString.wrap(safeBuffer)
		}
		return ByteString(safeBuffer, 0, size)
	}

    /**
     * Append a single byte to this builder.
     *
     * @param byte the byte to append.
     */
    public fun append(byte: Byte) {
		val newOffset = offset + 1
        ensureCapacity(newOffset)

        buffer[offset] = byte
	    offset = newOffset
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
        require(startIndex <= endIndex) { "startIndex ($startIndex) > endIndex ($endIndex)" }
        if (startIndex < 0 || endIndex > array.size) {
            throw IndexOutOfBoundsException("startIndex ($startIndex) and endIndex ($endIndex) represents " +
                    "an interval out of array's bounds [0..${array.size}).")
        }
        ensureCapacity(offset + endIndex - startIndex)

        array.copyInto(buffer, offset, startIndex, endIndex)
        offset += endIndex - startIndex
    }

    private fun ensureCapacity(requiredCapacity: Int) {
        if (buffer.size >= requiredCapacity) {
            return
        }

	    if (!extendable) throw IndexOutOfBoundsException("Needed capacity for appending to this non-extendable ByteString builder is $requiredCapacity, exceeding the available $capacity.")

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

/**
 * Appends bytes to this builder.
 */
public fun ByteStringBuilder.append(vararg bytes: Byte): Unit = append(bytes)


/**
 * Builds new byte string by populating newly created [ByteStringBuilder] initialized with the given [capacity]
 * using provided [builderAction] and then converting it to [ByteString].
 */
public inline fun buildByteString(capacity: Int = 0, builderAction: ByteStringBuilder.() -> Unit): ByteString {
    return ByteStringBuilder(capacity).apply(builderAction).toByteString()
}
