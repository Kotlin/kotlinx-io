/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.io.bytestring

import kotlin.math.max
import kotlin.math.min

/**
 * Wraps given [bytes] into a byte string.
 *
 * @param bytes a sequence of bytes to be wrapped.
 */
public fun ByteString(vararg bytes: Byte): ByteString = if (bytes.isEmpty()) {
    ByteString.EMPTY
} else {
    ByteString.wrap(bytes)
}

/**
 * An immutable wrapper around a byte sequence providing [String] like functionality.
 */
public class ByteString private constructor(
    private val data: ByteArray,
    @Suppress("UNUSED_PARAMETER") dummy: Any?
) : Comparable<ByteString> {
    /**
     * Wraps a copy of [data] subarray starting at [startIndex] and ending at [endIndex] into a byte string.
     *
     * @param data the array whose subarray should be copied and wrapped into a byte string.
     * @param startIndex the start index (inclusive) of a subarray to copy, `0` by default.
     * @param endIndex the end index (exclusive) of a subarray to copy, `data.size` be default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [data] array indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     */
    public constructor(data: ByteArray, startIndex: Int = 0, endIndex: Int = data.size) :
            this(data.copyOfRange(startIndex, endIndex), null)

    private var hashCode: Int = 0

    public companion object {
        /**
         * An empty ByteString.
         */
        internal val EMPTY: ByteString = ByteString(ByteArray(0), null)

        internal fun wrap(byteArray: ByteArray): ByteString = ByteString(byteArray, null)

        private val HEX_DIGITS = "0123456789ABCDEF".toCharArray()
    }

    /**
     * Returns size of this ByteString.
     */
    public val size: Int
        get(): Int = data.size

    /**
     * Returns `true` if [other] is a byte string containing exactly the same byte sequence.
     *
     * @param other the other object to compare this byte string for equality to.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ByteString

        if (other.data.size != data.size) return false
        if (other.hashCode != 0 && hashCode != 0 && other.hashCode != hashCode) return false
        return data.contentEquals(other.data)
    }

    /**
     * Returns a hash code based on the content of this byte string.
     */
    override fun hashCode(): Int {
        var hc = hashCode
        if (hc == 0) {
            hc = data.contentHashCode()
            hashCode = hc
        }
        return hc
    }

    /**
     * Returns a byte at the given index in this byte string.
     *
     * @param index the index to retrieve the byte at.
     *
     * @throws IndexOutOfBoundsException when [index] is negative or greater or equal to the [size].
     */
    public operator fun get(index: Int): Byte {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException(
            "index ($index) is out of byte string bounds: [0..$size)")
        return data[index]
    }

    /**
     * Returns a copy of subsequence starting at [startIndex] and ending at [endIndex] of a byte sequence
     * wrapped by this byte string.
     *
     * @param startIndex the start index (inclusive) of a subsequence to copy, `0` by default.
     * @param endIndex the end index (exclusive) of a subsequence to copy, [size] be default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of byte string indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     */
    public fun toByteArray(startIndex: Int = 0, endIndex: Int = size): ByteArray {
        require(startIndex <= endIndex) { "startIndex: $startIndex, endIndex: $endIndex" }
        return data.copyOfRange(startIndex, endIndex)
    }

    /**
     * Copies a subsequence starting at [startIndex] and ending at [endIndex] of a byte sequence
     * wrapped by this byte string and writes it into [destination] array starting at [destinationOffset] offset.
     *
     * @param destination the array to copy data into.
     * @param destinationOffset the offset starting from which data copy should be written to [destination].
     * @param startIndex the start index (inclusive) of a subsequence to copy, `0` by default.
     * @param endIndex the end index (exclusive) of a subsequence to copy, [size] be default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of byte string indices.
     * @throws IndexOutOfBoundsException when the subrange doesn't fit into the [destination] array starting at
     * the specified [destinationOffset], or when that index is out of the [destination] array indices range.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     */
    public fun copyInto(
        destination: ByteArray, destinationOffset: Int = 0,
        startIndex: Int = 0, endIndex: Int = size
    ) {
        require(startIndex <= endIndex) {
            "destinationOffset: $destinationOffset, startIndex: $startIndex, endIndex: $endIndex"
        }
        data.copyInto(destination, destinationOffset, startIndex, endIndex)
    }

    /**
     * Returns a new byte string wrapping a subsequence of bytes wrapped by this byte string starting from
     * [startIndex] and ending at [endIndex].
     *
     * @param startIndex the start index (inclusive) of a subsequence to copy.
     * @param endIndex the end index (exclusive) of a subsequence to copy, [size] be default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of byte string indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     */
    public fun substring(startIndex: Int, endIndex: Int = size): ByteString = if (startIndex == endIndex) {
        EMPTY
    } else {
        ByteString(data, startIndex, endIndex)
    }

    /**
     * Compares a byte sequence wrapped by this byte string to a byte sequence wrapped by [other]
     * in lexicographical order.
     * Byte values are compared as unsigned integers.
     *
     * The behavior is similar to [String.compareTo].
     *
     * @param other the byte string to compare this string to.
     */
    override fun compareTo(other: ByteString): Int {
        if (other === this) return 0
        val localData = data
        val otherData = other.data
        for (i in 0 until min(size, other.size)) {
            val cmp = localData[i].toUByte().compareTo(otherData[i].toUByte())
            if (cmp != 0) return cmp
        }

        return size.compareTo(other.size)
    }

    /**
     * Returns a string representation of this byte string. A string representation consists of [size] and
     * a hexadecimal-encoded string of a byte sequence wrapped by this byte string.
     *
     * The string representation has the following format `ByteString(size=3 hex=ABCDEF)`,
     * for empty strings it's always `ByteString(size=0)`.
     *
     * Note that a string representation includes the whole byte string content encoded.
     * Due to limitations exposed for the maximum string length, an attempt to return a string representation
     * of too long byte string may fail.
     */
    override fun toString(): String {
        if (isEmpty()) {
            return "ByteString(size=0)"
        }
        // format: "ByteString(size=XXX hex=YYYY)"
        val sizeStr = size.toString()
        val len = 22 + sizeStr.length + size * 2
        return with(StringBuilder(len)) {
            append("ByteString(size=")
            append(sizeStr)
            append(" hex=")
            val localData = data
            for (i in 0 until size) {
                val b = localData[i].toInt()
                append(HEX_DIGITS[(b ushr 4) and 0xf])
                append(HEX_DIGITS[b and 0xf])
            }
            append(')')
        }.toString()
    }

    /**
     * Returns a reference to the underlying array.
     *
     * These methods return reference to the underlying array, not to its copy.
     * Consider using [toByteArray] if it's impossible to guarantee that the array won't be modified.
     */
    @PublishedApi
    internal fun getBackingArrayReference(): ByteArray = data
}

/**
 * Returns the range of valid byte indices for this byte string.
 */
public val ByteString.indices: IntRange
    get() = 0 until size

/**
 * Returns the index within this byte string of the first occurrence of the specified [byte],
 * starting from the specified [startIndex].
 * If the [byte] not found, `-1` is returned.
 *
 * Behavior of this method is compatible with [CharSequence.indexOf].
 *
 * @param byte the value to search for.
 * @param startIndex the index (inclusive) starting from which the [byte] should be searched.
 */
public fun ByteString.indexOf(byte: Byte, startIndex: Int = 0): Int {
    val localData = getBackingArrayReference()
    for (i in max(startIndex, 0) until size) {
        if (localData[i] == byte) {
            return i
        }
    }
    return -1
}

/**
 * Returns the index within this byte string of the first occurrence of the specified [byteString],
 * starting from the specified [startIndex].
 * If the [byteString] not found, `-1` is returned.
 *
 * Behavior of this method is compatible with [CharSequence.indexOf].
 *
 * @param byteString the value to search for.
 * @param startIndex the index (inclusive) starting from which the [byteString] should be searched.
 */
public fun ByteString.indexOf(byteString: ByteString, startIndex: Int = 0): Int {
    if (byteString.isEmpty()) return max(min(startIndex, size), 0)
    val localData = getBackingArrayReference()
    val firstByte = byteString[0]
    for (i in max(startIndex, 0)..size - byteString.size) {
        if (localData[i] == firstByte && rangeEquals(i, byteString)) {
            return i
        }
    }
    return -1
}

/**
 * Returns the index within this byte string of the first occurrence of the specified [byteArray],
 * starting from the specified [startIndex].
 * If the [byteArray] not found, `-1` is returned.
 *
 * Behavior of this method is compatible with [CharSequence.indexOf].
 *
 * @param byteArray the value to search for.
 * @param startIndex the index (inclusive) starting from which the [byteArray] should be searched.
 */
public fun ByteString.indexOf(byteArray: ByteArray, startIndex: Int = 0): Int {
    if (byteArray.isEmpty()) return max(min(startIndex, size), 0)
    val localData = getBackingArrayReference()
    val firstByte = byteArray[0]
    for (i in max(0, startIndex)..size - byteArray.size) {
        if (localData[i] == firstByte && rangeEquals(i, byteArray)) {
            return i
        }
    }
    return -1
}

/**
 * Returns the index within this char sequence of the last occurrence of the specified [byte],
 * starting from the specified [startIndex].
 * If the [byte] not found, `-1` is returned.
 *
 * Behavior of this method is compatible with [CharSequence.lastIndexOf].
 *
 * @param byte the value to search for.
 * @param startIndex the index (inclusive) starting from which the [byte] should be searched.
 */
public fun ByteString.lastIndexOf(byte: Byte, startIndex: Int = 0): Int {
    val localData = getBackingArrayReference()
    for (i in size - 1 downTo max(0, startIndex)) {
        if (localData[i] == byte) {
            return i
        }
    }
    return -1
}

/**
 * Returns the index within this char sequence of the last occurrence of the specified [byteString],
 * starting from the specified [startIndex].
 * If the [byteString] not found, `-1` is returned.
 *
 * Behavior of this method is compatible with [CharSequence.lastIndexOf].
 *
 * @param byteString the value to search for.
 * @param startIndex the index (inclusive) starting from which the [byteString] should be searched.
 */
public fun ByteString.lastIndexOf(byteString: ByteString, startIndex: Int = 0): Int {
    if (byteString.isEmpty()) return size
    for (idx in (size - byteString.size) downTo max(0, startIndex)) {
        if (rangeEquals(idx, byteString, 0)) {
            return idx
        }
    }
    return -1
}

/**
 * Returns the index within this char sequence of the last occurrence of the specified [byteArray],
 * starting from the specified [startIndex].
 * If the [byteArray] not found, `-1` is returned.
 *
 * Behavior of this method is compatible with [CharSequence.lastIndexOf].
 *
 * @param byteArray the value to search for.
 * @param startIndex the index (inclusive) starting from which the [byteArray] should be searched.
 */
public fun ByteString.lastIndexOf(byteArray: ByteArray, startIndex: Int = 0): Int {
    if (byteArray.isEmpty()) return size
    for (idx in (size - byteArray.size) downTo max(0, startIndex)) {
        if (rangeEquals(idx, byteArray, 0)) {
            return idx
        }
    }
    return -1
}

/**
 * Returns true if this byte string starts with the prefix specified by the [byteArray].
 *
 * Behavior of this method is compatible with [CharSequence.startsWith].
 *
 * @param byteArray the prefix to check for.
 */
public fun ByteString.startsWith(byteArray: ByteArray): Boolean = when {
    byteArray.size > size -> false
    else -> rangeEquals(0, byteArray)
}

/**
 * Returns true if this byte string starts with the prefix specified by the [byteString].
 *
 * Behavior of this method is compatible with [CharSequence.startsWith].
 *
 * @param byteString the prefix to check for.
 */
public fun ByteString.startsWith(byteString: ByteString): Boolean = when {
    byteString.size > size -> false
    byteString.size == size -> equals(byteString)
    else -> rangeEquals(0, byteString)
}

/**
 * Returns true if this byte string ends with the suffix specified by the [byteArray].
 *
 * Behavior of this method is compatible with [CharSequence.endsWith].
 *
 * @param byteArray the suffix to check for.
 */
public fun ByteString.endsWith(byteArray: ByteArray): Boolean = when {
    byteArray.size > size -> false
    else -> rangeEquals(size - byteArray.size, byteArray)
}

/**
 * Returns true if this byte string ends with the suffix specified by the [byteString].
 *
 * Behavior of this method is compatible with [CharSequence.endsWith].
 *
 * @param byteString the suffix to check for.
 */
public fun ByteString.endsWith(byteString: ByteString): Boolean = when {
    byteString.size > size -> false
    byteString.size == size -> equals(byteString)
    else -> rangeEquals(size - byteString.size, byteString)
}

private fun ByteString.rangeEquals(
    offset: Int, other: ByteString, otherOffset: Int = 0,
    byteCount: Int = other.size - otherOffset
): Boolean {
    val localData = getBackingArrayReference()
    val otherData = other.getBackingArrayReference()
    for (i in 0 until byteCount) {
        if (localData[offset + i] != otherData[otherOffset + i]) {
            return false
        }
    }
    return true
}

private fun ByteString.rangeEquals(
    offset: Int, other: ByteArray, otherOffset: Int = 0,
    byteCount: Int = other.size - otherOffset
): Boolean {
    val localData = getBackingArrayReference()
    for (i in 0 until byteCount) {
        if (localData[offset + i] != other[otherOffset + i]) {
            return false
        }
    }
    return true
}

/**
 * Returns `true` if this byte string is empty.
 */
public fun ByteString.isEmpty(): Boolean = size == 0

/**
 * Returns `true` if this byte string is not empty.
 */
public fun ByteString.isNotEmpty(): Boolean = !isEmpty()

/**
 * Decodes content of a byte string into a string using UTF-8 encoding.
 */
public fun ByteString.decodeToString(): String {
    return getBackingArrayReference().decodeToString()
}

/**
 * Encodes a string into a byte sequence using UTF8-encoding and wraps it into a byte string.
 */
public fun String.encodeToByteString(): ByteString {
    return ByteString.wrap(encodeToByteArray())
}

/**
 * Returns `true` if the content of this byte string equals to the [array].
 *
 * @param array the array to test this byte string's content against.
 */
public fun ByteString.contentEquals(array: ByteArray): Boolean {
    return getBackingArrayReference().contentEquals(array)
}
