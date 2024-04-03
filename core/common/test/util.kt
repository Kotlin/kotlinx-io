/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kotlinx.io

import kotlinx.io.internal.REPLACEMENT_BYTE
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun segmentSizes(buffer: Buffer): List<Int> {
    var segment = buffer.head
    if (segment == null) return emptyList()

    val sizes = mutableListOf(segment.limit - segment.pos)
    segment = segment.next
    while (segment !== null) {
        sizes.add(segment.limit - segment.pos)
        segment = segment.next
    }
    return sizes
}

fun assertNoEmptySegments(buffer: Buffer) {
    assertTrue(segmentSizes(buffer).all { it != 0 }, "Expected all segments to be non-empty")
}

expect fun tempFileName(): String

private fun fromHexChar(char: Char): Int {
    val code = char.code
    return when (code) {
        in '0'.code..'9'.code -> code - '0'.code
        in 'a'.code..'f'.code -> code - 'a'.code + 10
        in 'A'.code..'F'.code -> code - 'A'.code + 10
        else -> throw NumberFormatException("Not a hexadecimal digit: $char")
    }
}

fun String.decodeHex(): ByteArray {
    if (length % 2 != 0) throw IllegalArgumentException("Even number of bytes is expected.")

    val result = ByteArray(length / 2)

    for (idx in result.indices) {
        val byte = fromHexChar(this[idx * 2]).shl(4).or(fromHexChar(this[idx * 2 + 1]))
        result[idx] = byte.toByte()
    }

    return result
}

fun Char.repeat(count: Int): String {
    return toString().repeat(count)
}

fun assertArrayEquals(a: ByteArray, b: ByteArray) {
    assertEquals(a.contentToString(), b.contentToString())
}

internal fun String.commonAsUtf8ToByteArray(): ByteArray {
    val bytes = ByteArray(4 * length)

    // Assume ASCII until a UTF-8 code point is observed. This is ugly but yields
    // about a 2x performance increase for pure ASCII.
    for (index in indices) {
        val b0 = this[index]
        if (b0 >= '\u0080') {
            var size = index
            processUtf8Bytes(index, length) { c ->
                bytes[size++] = c
            }
            return bytes.copyOf(size)
        }
        bytes[index] = b0.code.toByte()
    }

    return bytes.copyOf(length)
}

internal inline fun String.processUtf8Bytes(
    beginIndex: Int,
    endIndex: Int,
    yield: (Byte) -> Unit
) {
    // Transcode a UTF-16 String to UTF-8 bytes.
    var index = beginIndex
    while (index < endIndex) {
        val c = this[index]

        when {
            c < '\u0080' -> {
                // Emit a 7-bit character with 1 byte.
                yield(c.code.toByte()) // 0xxxxxxx
                index++

                // Assume there is going to be more ASCII
                while (index < endIndex && this[index] < '\u0080') {
                    yield(this[index++].code.toByte())
                }
            }

            c < '\u0800' -> {
                // Emit a 11-bit character with 2 bytes.
                /* ktlint-disable no-multi-spaces */
                yield((c.code shr 6 or 0xc0).toByte()) // 110xxxxx
                yield((c.code and 0x3f or 0x80).toByte()) // 10xxxxxx
                /* ktlint-enable no-multi-spaces */
                index++
            }

            c !in '\ud800'..'\udfff' -> {
                // Emit a 16-bit character with 3 bytes.
                /* ktlint-disable no-multi-spaces */
                yield((c.code shr 12 or 0xe0).toByte()) // 1110xxxx
                yield((c.code shr 6 and 0x3f or 0x80).toByte()) // 10xxxxxx
                yield((c.code and 0x3f or 0x80).toByte()) // 10xxxxxx
                /* ktlint-enable no-multi-spaces */
                index++
            }

            else -> {
                // c is a surrogate. Make sure it is a high surrogate & that its successor is a low
                // surrogate. If not, the UTF-16 is invalid, in which case we emit a replacement
                // byte.
                if (c > '\udbff' ||
                    endIndex <= index + 1 ||
                    this[index + 1] !in '\udc00'..'\udfff'
                ) {
                    yield(REPLACEMENT_BYTE)
                    index++
                } else {
                    // UTF-16 high surrogate: 110110xxxxxxxxxx (10 bits)
                    // UTF-16 low surrogate:  110111yyyyyyyyyy (10 bits)
                    // Unicode code point:    00010000000000000000 + xxxxxxxxxxyyyyyyyyyy (21 bits)
                    val codePoint = (((c.code shl 10) + this[index + 1].code) + (0x010000 - (0xd800 shl 10) - 0xdc00))

                    // Emit a 21-bit character with 4 bytes.
                    /* ktlint-disable no-multi-spaces */
                    yield((codePoint shr 18 or 0xf0).toByte()) // 11110xxx
                    yield((codePoint shr 12 and 0x3f or 0x80).toByte()) // 10xxxxxx
                    yield((codePoint shr 6 and 0x3f or 0x80).toByte()) // 10xxyyyy
                    yield((codePoint and 0x3f or 0x80).toByte()) // 10yyyyyy
                    /* ktlint-enable no-multi-spaces */
                    index += 2
                }
            }
        }
    }
}

internal expect fun String.asUtf8ToByteArray(): ByteArray
