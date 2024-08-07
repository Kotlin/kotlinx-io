/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
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

package kotlinx.io.internal

import kotlinx.io.and
import kotlinx.io.shr

internal fun ByteArray.commonToUtf8String(beginIndex: Int = 0, endIndex: Int = size): String {
    if (beginIndex < 0 || endIndex > size || beginIndex > endIndex) {
        throw IndexOutOfBoundsException("size=$size beginIndex=$beginIndex endIndex=$endIndex")
    }
    val chars = CharArray(endIndex - beginIndex)

    var length = 0
    processUtf16Chars(beginIndex, endIndex) { c ->
        chars[length++] = c
    }

    return chars.concatToString(0, length)
}

internal const val REPLACEMENT_BYTE: Byte = '?'.code.toByte()
internal const val REPLACEMENT_CHARACTER: Char = '\ufffd'
internal const val REPLACEMENT_CODE_POINT: Int = REPLACEMENT_CHARACTER.code

@Suppress("NOTHING_TO_INLINE") // Syntactic sugar.
internal inline fun isIsoControl(codePoint: Int): Boolean =
    (codePoint in 0x00..0x1F) || (codePoint in 0x7F..0x9F)

@Suppress("NOTHING_TO_INLINE") // Syntactic sugar.
internal inline fun isUtf8Continuation(byte: Byte): Boolean {
    // 0b10xxxxxx
    return byte and 0xc0 == 0x80
}

internal inline fun ByteArray.processUtf8CodePoints(
    beginIndex: Int,
    endIndex: Int,
    yield: (Int) -> Unit
) {
    var index = beginIndex
    while (index < endIndex) {
        val b0 = this[index]
        when {
            b0 >= 0 -> {
                // 0b0xxxxxxx
                yield(b0.toInt())
                index++

                // Assume there is going to be more ASCII
                while (index < endIndex && this[index] >= 0) {
                    yield(this[index++].toInt())
                }
            }

            b0 shr 5 == -2 -> {
                // 0b110xxxxx
                index += process2Utf8Bytes(index, endIndex) { yield(it) }
            }

            b0 shr 4 == -2 -> {
                // 0b1110xxxx
                index += process3Utf8Bytes(index, endIndex) { yield(it) }
            }

            b0 shr 3 == -2 -> {
                // 0b11110xxx
                index += process4Utf8Bytes(index, endIndex) { yield(it) }
            }

            else -> {
                // 0b10xxxxxx - Unexpected continuation
                // 0b111111xxx - Unknown encoding
                yield(REPLACEMENT_CODE_POINT)
                index++
            }
        }
    }
}

// Value added to the high UTF-16 surrogate after shifting
internal const val HIGH_SURROGATE_HEADER = 0xd800 - (0x010000 ushr 10)

// Value added to the low UTF-16 surrogate after masking
internal const val LOG_SURROGATE_HEADER = 0xdc00

internal inline fun ByteArray.processUtf16Chars(
    beginIndex: Int,
    endIndex: Int,
    yield: (Char) -> Unit
) {
    var index = beginIndex
    while (index < endIndex) {
        val b0 = this[index]
        when {
            b0 >= 0 -> {
                // 0b0xxxxxxx
                yield(b0.toInt().toChar())
                index++

                // Assume there is going to be more ASCII
                // This is almost double the performance of the outer loop
                while (index < endIndex && this[index] >= 0) {
                    yield(this[index++].toInt().toChar())
                }
            }

            b0 shr 5 == -2 -> {
                // 0b110xxxxx
                index += process2Utf8Bytes(index, endIndex) { yield(it.toChar()) }
            }

            b0 shr 4 == -2 -> {
                // 0b1110xxxx
                index += process3Utf8Bytes(index, endIndex) { yield(it.toChar()) }
            }

            b0 shr 3 == -2 -> {
                // 0b11110xxx
                index += process4Utf8Bytes(index, endIndex) { codePoint ->
                    if (codePoint != REPLACEMENT_CODE_POINT) {
                        // Unicode code point:    00010000000000000000 + xxxxxxxxxxyyyyyyyyyy (21 bits)
                        // UTF-16 high surrogate: 110110xxxxxxxxxx (10 bits)
                        // UTF-16 low surrogate:  110111yyyyyyyyyy (10 bits)
                        /* ktlint-disable no-multi-spaces paren-spacing */
                        yield(((codePoint ushr 10) + HIGH_SURROGATE_HEADER).toChar())
                        /* ktlint-enable no-multi-spaces paren-spacing */
                        yield(((codePoint and 0x03ff) + LOG_SURROGATE_HEADER).toChar())
                    } else {
                        yield(REPLACEMENT_CHARACTER)
                    }
                }
            }

            else -> {
                // 0b10xxxxxx - Unexpected continuation
                // 0b111111xxx - Unknown encoding
                yield(REPLACEMENT_CHARACTER)
                index++
            }
        }
    }
}

// ===== UTF-8 Encoding and Decoding ===== //
/*
The following 3 methods take advantage of using XOR on 2's complement store
numbers to quickly and efficiently combine the important data of UTF-8 encoded
bytes. This will be best explained using an example, so lets take the following
encoded character '∇' = \u2207.

Using the Unicode code point for this character, 0x2207, we will split the
binary representation into 3 sections as follows:

    0x2207 = 0b0010 0010 0000 0111
               xxxx yyyy yyzz zzzz

Now take each section of bits and add the appropriate header:

    utf8(0x2207) = 0b1110 xxxx 0b10yy yyyy 0b10zz zzzz
                 = 0b1110 0010 0b1000 1000 0b1000 0111
                 = 0xe2        0x88        0x87

We have now just encoded this as a 3 byte UTF-8 character. More information
about different sizes of characters can be found here:
    https://en.wikipedia.org/wiki/UTF-8

Encoding was pretty easy, but decoding is a bit more complicated. We need to
first determine the number of bytes used to represent the character, strip all
the headers, and then combine all the bits into a single integer. Let's use the
character we just encoded and work backwards, taking advantage of 2's complement
integer representation and the XOR function.

Let's look at the decimal representation of these bytes:

    0xe2, 0x88, 0x87 = -30, -120, -121

The first interesting thing to notice is that UTF-8 headers all start with 1 -
except for ASCII which is encoded as a single byte - which means all UTF-8 bytes
will be negative. So converting these to integers results in a lot of 1's added
because they are store as 2's complement:

    0xe2 =  -30 = 0xffff ffe2
    0x88 = -120 = 0xffff ff88
    0x87 = -121 = 0xffff ff87

Now let's XOR these with their corresponding UTF-8 byte headers to see what
happens:

    0xffff ffe2 xor 0xffff ffe0 = 0x0000 0002
    0xffff ff88 xor 0xffff ff80 = 0x0000 0008
    0xffff ff87 xor 0xffff ff80 = 0x0000 0007

***This is why we must first convert the byte header mask to a byte and then
back to an integer, so it is properly converted to a 2's complement negative
number which can be applied to each byte.***

Now let's look at the binary representation to see how we can combine these to
create the Unicode code point:

    0b0000 0010    0b0000 1000    0b0000 0111
    0b1110 xxxx    0b10yy yyyy    0b10zz zzzz

Combining each section will require some bit shifting, but then they can just
be OR'd together. They can also be XOR'd together which makes use of a single,
COMMUTATIVE, operator through the entire calculation.

      << 12 = 00000010
      <<  6 =       00001000
      <<  0 =             00000111
        XOR = 00000010001000000111

 code point = 0b0010 0010 0000 0111
            = 0x2207

And there we have it! The decoded UTF-8 character '∇'! And because the XOR
operator is commutative, we can re-arrange all this XOR and shifting to create
a single mask that can be applied to 3-byte UTF-8 characters after their bytes
have been shifted and XOR'd together.
 */

// Mask used to remove byte headers from a 2 byte encoded UTF-8 character
internal const val MASK_2BYTES = 0x0f80
// MASK_2BYTES =
//    (0xc0.toByte() shl 6) xor
//    (0x80.toByte().toInt())

internal inline fun ByteArray.process2Utf8Bytes(
    beginIndex: Int,
    endIndex: Int,
    yield: (Int) -> Unit
): Int {
    if (endIndex <= beginIndex + 1) {
        yield(REPLACEMENT_CODE_POINT)
        // Only 1 byte remaining - underflow
        return 1
    }

    val b0 = this[beginIndex]
    val b1 = this[beginIndex + 1]
    if (!isUtf8Continuation(b1)) {
        yield(REPLACEMENT_CODE_POINT)
        return 1
    }

    val codePoint = (MASK_2BYTES
            xor (b1.toInt())
            xor (b0.toInt() shl 6))

    when {
        codePoint < 0x80 -> yield(REPLACEMENT_CODE_POINT) // Reject overlong code points.
        else -> yield(codePoint)
    }
    return 2
}

// Mask used to remove byte headers from a 3 byte encoded UTF-8 character
internal const val MASK_3BYTES = -0x01e080
// MASK_3BYTES =
//    (0xe0.toByte() shl 12) xor
//    (0x80.toByte() shl 6) xor
//    (0x80.toByte().toInt())

internal inline fun ByteArray.process3Utf8Bytes(
    beginIndex: Int,
    endIndex: Int,
    yield: (Int) -> Unit
): Int {
    if (endIndex <= beginIndex + 2) {
        // At least 2 bytes remaining
        yield(REPLACEMENT_CODE_POINT)
        if (endIndex <= beginIndex + 1 || !isUtf8Continuation(this[beginIndex + 1])) {
            // Only 1 byte remaining - underflow
            // Or 2nd byte is not a continuation - malformed
            return 1
        } else {
            // Only 2 bytes remaining - underflow
            return 2
        }
    }

    val b0 = this[beginIndex]
    val b1 = this[beginIndex + 1]
    if (!isUtf8Continuation(b1)) {
        yield(REPLACEMENT_CODE_POINT)
        return 1
    }
    val b2 = this[beginIndex + 2]
    if (!isUtf8Continuation(b2)) {
        yield(REPLACEMENT_CODE_POINT)
        return 2
    }

    val codePoint = (MASK_3BYTES
            xor (b2.toInt())
            xor (b1.toInt() shl 6)
            xor (b0.toInt() shl 12))

    when {
        codePoint < 0x800 -> {
            yield(REPLACEMENT_CODE_POINT) // Reject overlong code points.
        }

        codePoint in 0xd800..0xdfff -> {
            yield(REPLACEMENT_CODE_POINT) // Reject partial surrogates.
        }

        else -> {
            yield(codePoint)
        }
    }
    return 3
}

// Mask used to remove byte headers from a 4 byte encoded UTF-8 character
internal const val MASK_4BYTES = 0x381f80
// MASK_4BYTES =
//    (0xf0.toByte() shl 18) xor
//    (0x80.toByte() shl 12) xor
//    (0x80.toByte() shl 6) xor
//    (0x80.toByte().toInt())

internal inline fun ByteArray.process4Utf8Bytes(
    beginIndex: Int,
    endIndex: Int,
    yield: (Int) -> Unit
): Int {
    if (endIndex <= beginIndex + 3) {
        // At least 3 bytes remaining
        yield(REPLACEMENT_CODE_POINT)
        if (endIndex <= beginIndex + 1 || !isUtf8Continuation(this[beginIndex + 1])) {
            // Only 1 byte remaining - underflow
            // Or 2nd byte is not a continuation - malformed
            return 1
        } else if (endIndex <= beginIndex + 2 || !isUtf8Continuation(this[beginIndex + 2])) {
            // Only 2 bytes remaining - underflow
            // Or 3rd byte is not a continuation - malformed
            return 2
        } else {
            // Only 3 bytes remaining - underflow
            return 3
        }
    }

    val b0 = this[beginIndex]
    val b1 = this[beginIndex + 1]
    if (!isUtf8Continuation(b1)) {
        yield(REPLACEMENT_CODE_POINT)
        return 1
    }
    val b2 = this[beginIndex + 2]
    if (!isUtf8Continuation(b2)) {
        yield(REPLACEMENT_CODE_POINT)
        return 2
    }
    val b3 = this[beginIndex + 3]
    if (!isUtf8Continuation(b3)) {
        yield(REPLACEMENT_CODE_POINT)
        return 3
    }

    val codePoint = (MASK_4BYTES
            xor (b3.toInt())
            xor (b2.toInt() shl 6)
            xor (b1.toInt() shl 12)
            xor (b0.toInt() shl 18))

    when {
        codePoint > 0x10ffff -> {
            yield(REPLACEMENT_CODE_POINT) // Reject code points larger than the Unicode maximum.
        }

        codePoint in 0xd800..0xdfff -> {
            yield(REPLACEMENT_CODE_POINT) // Reject partial surrogates.
        }

        codePoint < 0x10000 -> {
            yield(REPLACEMENT_CODE_POINT) // Reject overlong code points.
        }

        else -> {
            yield(codePoint)
        }
    }
    return 4
}