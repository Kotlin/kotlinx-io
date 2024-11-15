/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
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

@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io

internal val HEX_DIGIT_CHARS =
    charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

internal fun checkOffsetAndCount(size: Long, offset: Long, byteCount: Long) {
    if (offset < 0 || offset > size || size - offset < byteCount || byteCount < 0) {
        throw IllegalArgumentException(
            "offset ($offset) and byteCount ($byteCount) are not within the range [0..size($size))"
        )
    }
}

internal inline fun checkBounds(size: Int, startIndex: Int, endIndex: Int) =
    checkBounds(size.toLong(), startIndex.toLong(), endIndex.toLong())

internal fun checkBounds(size: Long, startIndex: Long, endIndex: Long) {
    if (startIndex < 0 || endIndex > size) {
        throw IndexOutOfBoundsException(
            "startIndex ($startIndex) and endIndex ($endIndex) are not within the range [0..size($size))"
        )
    }
    if (startIndex > endIndex) {
        throw IllegalArgumentException("startIndex ($startIndex) > endIndex ($endIndex)")
    }
}

internal inline fun checkByteCount(byteCount: Long) {
    require(byteCount >= 0) { "byteCount ($byteCount) < 0" }
}

internal expect fun Short.reverseBytes(): Short

internal inline fun Short.reverseBytesCommon(): Short {
    val i = toInt() and 0xffff
    val reversed = (i and 0xff00 ushr 8) or
            (i and 0x00ff shl 8)
    return reversed.toShort()
}

internal expect fun Int.reverseBytes(): Int

internal inline fun Int.reverseBytesCommon(): Int {
    return (this and -0x1000000 ushr 24) or
            (this and 0x00ff0000 ushr 8) or
            (this and 0x0000ff00 shl 8) or
            (this and 0x000000ff shl 24)
}

internal expect fun Long.reverseBytes(): Long

internal inline fun Long.reverseBytesCommon(): Long {
    return (this and -0x100000000000000L ushr 56) or
            (this and 0x00ff000000000000L ushr 40) or
            (this and 0x0000ff0000000000L ushr 24) or
            (this and 0x000000ff00000000L ushr 8) or
            (this and 0x00000000ff000000L shl 8) or
            (this and 0x0000000000ff0000L shl 24) or
            (this and 0x000000000000ff00L shl 40) or
            (this and 0x00000000000000ffL shl 56)
}

/* ktlint-enable no-multi-spaces indent */

// Syntactic sugar.
internal inline infix fun Byte.shr(other: Int): Int = toInt() shr other

// Syntactic sugar.
internal inline infix fun Byte.shl(other: Int): Int = toInt() shl other

// Syntactic sugar.
internal inline infix fun Byte.and(other: Int): Int = toInt() and other

// Syntactic sugar.
internal inline infix fun Byte.and(other: Long): Long = toLong() and other

// Pending `kotlin.experimental.xor` becoming stable
internal inline infix fun Byte.xor(other: Byte): Byte = (toInt() xor other.toInt()).toByte()

// Syntactic sugar.
internal inline infix fun Int.and(other: Long): Long = toLong() and other

// Syntactic sugar.
internal inline fun minOf(a: Long, b: Int): Long = minOf(a, b.toLong())

// Syntactic sugar.
internal inline fun minOf(a: Int, b: Long): Long = minOf(a.toLong(), b)

internal fun Byte.toHexString(): String {
    val result = CharArray(2)
    result[0] = HEX_DIGIT_CHARS[this shr 4 and 0xf]
    result[1] = HEX_DIGIT_CHARS[this and 0xf]
    return result.concatToString()
}

internal fun Int.toHexString(): String {
    if (this == 0) return "0" // Required as code below does not handle 0

    val result = CharArray(8)
    result[0] = HEX_DIGIT_CHARS[this shr 28 and 0xf]
    result[1] = HEX_DIGIT_CHARS[this shr 24 and 0xf]
    result[2] = HEX_DIGIT_CHARS[this shr 20 and 0xf]
    result[3] = HEX_DIGIT_CHARS[this shr 16 and 0xf]
    result[4] = HEX_DIGIT_CHARS[this shr 12 and 0xf]
    result[5] = HEX_DIGIT_CHARS[this shr 8 and 0xf]
    result[6] = HEX_DIGIT_CHARS[this shr 4 and 0xf]
    result[7] = HEX_DIGIT_CHARS[this and 0xf]

    // Find the first non-zero index
    var i = 0
    while (i < result.size) {
        if (result[i] != '0') break
        i++
    }

    return result.concatToString(i, result.size)
}

internal fun Long.toHexString(): String {
    if (this == 0L) return "0" // Required as code below does not handle 0

    val result = CharArray(16)
    result[0] = HEX_DIGIT_CHARS[(this shr 60 and 0xf).toInt()]
    result[1] = HEX_DIGIT_CHARS[(this shr 56 and 0xf).toInt()]
    result[2] = HEX_DIGIT_CHARS[(this shr 52 and 0xf).toInt()]
    result[3] = HEX_DIGIT_CHARS[(this shr 48 and 0xf).toInt()]
    result[4] = HEX_DIGIT_CHARS[(this shr 44 and 0xf).toInt()]
    result[5] = HEX_DIGIT_CHARS[(this shr 40 and 0xf).toInt()]
    result[6] = HEX_DIGIT_CHARS[(this shr 36 and 0xf).toInt()]
    result[7] = HEX_DIGIT_CHARS[(this shr 32 and 0xf).toInt()]
    result[8] = HEX_DIGIT_CHARS[(this shr 28 and 0xf).toInt()]
    result[9] = HEX_DIGIT_CHARS[(this shr 24 and 0xf).toInt()]
    result[10] = HEX_DIGIT_CHARS[(this shr 20 and 0xf).toInt()]
    result[11] = HEX_DIGIT_CHARS[(this shr 16 and 0xf).toInt()]
    result[12] = HEX_DIGIT_CHARS[(this shr 12 and 0xf).toInt()]
    result[13] = HEX_DIGIT_CHARS[(this shr 8 and 0xf).toInt()]
    result[14] = HEX_DIGIT_CHARS[(this shr 4 and 0xf).toInt()]
    result[15] = HEX_DIGIT_CHARS[(this and 0xf).toInt()]

    // Find the first non-zero index
    var i = 0
    while (i < result.size) {
        if (result[i] != '0') break
        i++
    }

    return result.concatToString(i, result.size)
}

/**
 * Returns the number of characters required to encode [v]
 * as a hexadecimal number without leading zeros (with `v == 0L` being the only exception,
 * `hexNumberLength(0) == 1`).
 */
internal inline fun hexNumberLength(v: Long): Int {
    if (v == 0L) return 1
    val exactWidth = (Long.SIZE_BITS - v.countLeadingZeroBits())
    // Round up to the nearest full nibble
    return ((exactWidth + 3) / 4)
}
