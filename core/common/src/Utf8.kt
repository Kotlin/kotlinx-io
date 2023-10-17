/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2017 Square, Inc.
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

/**
 * `kotlinx-io` assumes most applications use UTF-8 exclusively, and offers optimized implementations of
 * common operations on UTF-8 strings.
 *
 * <table border="1" cellspacing="0" cellpadding="3" summary="">
 * <tr>
 * <th></th>
 * <th>[ByteString]</th>
 * <th>[Buffer], [BufferedSink], [BufferedSource]</th>
 * </tr>
 * <tr>
 * <td>Encode a string</td>
 * <td>[ByteString.encodeUtf8]</td>
 * <td>[BufferedSink.writeUtf8]</td>
 * </tr>
 * <tr>
 * <td>Encode a code point</td>
 * <td></td>
 * <td>[BufferedSink.writeUtf8CodePoint]</td>
 * </tr>
 * <tr>
 * <td>Decode a string</td>
 * <td>[ByteString.utf8]</td>
 * <td>[BufferedSource.readUtf8], [BufferedSource.readUtf8]</td>
 * </tr>
 * <tr>
 * <td>Decode a code point</td>
 * <td></td>
 * <td>[BufferedSource.readUtf8CodePoint]</td>
 * </tr>
 * <tr>
 * <td>Decode until the next `\r\n` or `\n`</td>
 * <td></td>
 * <td>[BufferedSource.readUtf8LineStrict],
 * [BufferedSource.readUtf8LineStrict]</td>
 * </tr>
 * <tr>
 * <td>Decode until the next `\r\n`, `\n`, or `EOF`</td>
 * <td></td>
 * <td>[BufferedSource.readUtf8Line]</td>
 * </tr>
 * <tr>
 * <td>Measure the bytes in a UTF-8 string</td>
 * <td colspan="2">[Utf8.size], [Utf8.size]</td>
 * </tr>
 * </table>
 */

package kotlinx.io

import kotlinx.io.internal.*
import kotlinx.io.unsafe.UnsafeSegmentAccessors

/**
 * Returns the number of bytes used to encode the slice of `string` as UTF-8 when using [Sink.writeString].
 *
 * @param startIndex the index (inclusive) of the first character to encode, `0` by default.
 * @param endIndex the index (exclusive) of the character past the last character to encode, `string.length` by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of string indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.utf8SizeSample
 */
internal fun String.utf8Size(startIndex: Int = 0, endIndex: Int = length): Long {
    checkBounds(length, startIndex, endIndex)

    var result = 0L
    var i = startIndex
    while (i < endIndex) {
        val c = this[i].code

        if (c < 0x80) {
            // A 7-bit character with 1 byte.
            result++
            i++
        } else if (c < 0x800) {
            // An 11-bit character with 2 bytes.
            result += 2
            i++
        } else if (c < 0xd800 || c > 0xdfff) {
            // A 16-bit character with 3 bytes.
            result += 3
            i++
        } else {
            val low = if (i + 1 < endIndex) this[i + 1].code else 0
            if (c > 0xdbff || low < 0xdc00 || low > 0xdfff) {
                // A malformed surrogate, which yields '?'.
                result++
                i++
            } else {
                // A 21-bit character with 4 bytes.
                result += 4
                i += 2
            }
        }
    }

    return result
}

/**
 * Encodes [codePoint] in UTF-8 and writes it to this sink.
 *
 * [codePoint] should represent valid Unicode code point, meaning that its value should be within the Unicode codespace
 * (`U+000000` .. `U+10ffff`), otherwise [IllegalArgumentException] will be thrown.
 *
 * Note that in general, a value retrieved from [Char.code] could not be written directly
 * as it may be a part of a [surrogate pair](https://www.unicode.org/faq/utf_bom.html#utf16-2) (that could be
 * detected using [Char.isSurrogate], or [Char.isHighSurrogate] and [Char.isLowSurrogate]).
 * Such a pair of characters needs to be manually converted back to a single code point
 * which then could be written to a [Sink].
 * Without such a conversion, data written to a [Sink] can not be converted back
 * to a string from which a surrogate pair was retrieved.
 *
 * More specifically, all code points mapping to UTF-16 surrogates (`U+d800`..`U+dfff`)
 * will be written as `?` characters (`U+0063`).
 *
 * @param codePoint the codePoint to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 * @throws IllegalArgumentException when [codePoint] value is negative, or greater than `U+10ffff`.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeUtf8CodePointSample
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeSurrogatePair
 */
@OptIn(DelicateIoApi::class)
public fun Sink.writeCodePointValue(codePoint: Int): Unit =
    writeToInternalBuffer { it.commonWriteUtf8CodePoint(codePoint) }

/**
 * Encodes the characters at [startIndex] up to [endIndex] from [string] in UTF-8 and writes it to this sink.
 *
 * @param string the string to be encoded.
 * @param startIndex the index (inclusive) of the first character to encode, 0 by default.
 * @param endIndex the index (exclusive) of a character past to a last character to encode, `string.length` by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [string] indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalStateException when the sink is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeUtf8Sample
 */
@OptIn(DelicateIoApi::class)
public fun Sink.writeString(string: String, startIndex: Int = 0, endIndex: Int = string.length): Unit =
    writeToInternalBuffer { it.commonWriteUtf8(string, startIndex, endIndex) }

/**
 * Removes all bytes from this source, decodes them as UTF-8, and returns the string.
 *
 * Returns the empty string if this source is empty.
 *
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readUtf8
 */
@OptIn(InternalIoApi::class)
public fun Source.readString(): String {
    var req: Long = Segment.SIZE.toLong()
    while (request(req)) {
        req *= 2
    }
    return buffer.commonReadUtf8(buffer.size)
}

/**
 * Removes all bytes from this buffer, decodes them as UTF-8, and returns the string.
 *
 * Returns the empty string if this buffer is empty.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readUtf8
 */
public fun Buffer.readString(): String {
    return commonReadUtf8(size)
}

/**
 * Removes [byteCount] bytes from this source, decodes them as UTF-8, and returns the string.
 *
 * @param byteCount the number of bytes to read from the source for string decoding.
 *
 * @throws IllegalArgumentException when [byteCount] is negative.
 * @throws EOFException when the source is exhausted before reading [byteCount] bytes from it.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readUtf8
 */
@OptIn(InternalIoApi::class)
public fun Source.readString(byteCount: Long): String {
    require(byteCount)
    return buffer.commonReadUtf8(byteCount)
}

/**
 * Decodes a single code point value from UTF-8 code units, reading between 1 and 4 bytes as necessary.
 *
 * If this source is exhausted before a complete code point can be read, this throws an
 * [EOFException] and consumes no input.
 *
 * If this source starts with an ill-formed UTF-8 code units sequence, this method will remove
 * 1 or more non-UTF-8 bytes and return the replacement character (`U+fffd`).
 *
 * The replacement character (`U+fffd`) will be also returned if the source starts with a well-formed
 * code units sequences, but a decoded value does not pass further validation, such as
 * the value is out of range (beyond the `0x10ffff` limit of Unicode), maps to UTF-16 surrogates (`U+d800`..`U+dfff`),
 * or an overlong encoding is detected (such as `0xc080` for the NUL character in modified UTF-8).
 *
 * Note that in general, returned value may not be directly converted to [Char] as it may be out
 * of [Char]'s values range and should be manually converted to a
 * [surrogate pair](https://www.unicode.org/faq/utf_bom.html#utf16-2).
 *
 * @throws EOFException when the source is exhausted before a complete code point can be read.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readUtf8CodePointSample
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.surrogatePairs
 */
@OptIn(InternalIoApi::class)
public fun Source.readCodePointValue(): Int {
    require(1)

    val b0 = buffer[0].toInt()
    when {
        b0 and 0xe0 == 0xc0 -> require(2)
        b0 and 0xf0 == 0xe0 -> require(3)
        b0 and 0xf8 == 0xf0 -> require(4)
    }

    return buffer.commonReadUtf8CodePoint()
}

/**
 * Removes and returns UTF-8 encoded characters up to but not including the next line break. A line break is
 * either `"\n"` or `"\r\n"`; these characters are not included in the result.
 *
 * On the end of the stream this method returns null. If the source doesn't end with a line break, then
 * an implicit line break is assumed. Null is returned once the source is exhausted.
 *
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readLinesSample
 */
@OptIn(InternalIoApi::class)
public fun Source.readLine(): String? {
    if (!request(1)) return null

    var lfIndex = this.indexOf('\n'.code.toByte())
    return when (lfIndex) {
        -1L -> readString()
        0L -> {
            skip(1)
            ""
        }

        else -> {
            var skipBytes = 1
            if (buffer[lfIndex - 1] == '\r'.code.toByte()) {
                lfIndex -= 1
                skipBytes += 1
            }
            val string = readString(lfIndex)
            skip(skipBytes.toLong())
            string
        }
    }
}

/**
 * Removes and returns UTF-8 encoded characters up to but not including the next line break, throwing
 * [EOFException] if a line break was not encountered. A line break is either `"\n"` or `"\r\n"`;
 * these characters are not included in the result.
 *
 * The returned string will have at most [limit] UTF-8 bytes, and the maximum number of bytes
 * scanned is `limit + 2`. If `limit == 0` this will always throw an [EOFException] because no
 * bytes will be scanned.
 *
 * No bytes are discarded if the match fails.
 *
 * @param limit the maximum UTF-8 bytes constituting a returned string.
 *
 * @throws EOFException when the source does not contain a string consisting with at most [limit] bytes followed by
 * line break characters.
 * @throws IllegalStateException when the source is closed.
 * @throws IllegalArgumentException when [limit] is negative.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readLinesSample
 */
@OptIn(InternalIoApi::class)
public fun Source.readLineStrict(limit: Long = Long.MAX_VALUE): String {
    require(limit >= 0) { "limit ($limit) < 0" }
    require(1)

    var lfIndex = indexOf('\n'.code.toByte(), startIndex = 0, endIndex = limit)

    if (lfIndex == 0L) {
        skip(1)
        return ""
    }

    if (lfIndex > 0) {
        var skipBytes = 1L
        if (buffer[lfIndex - 1] == '\r'.code.toByte()) {
            lfIndex -= 1
            skipBytes += 1
        }
        val str = readString(lfIndex)
        skip(skipBytes)
        return str
    }

    // we reached the end of the source before hitting the limit
    if (buffer.size < limit) throw EOFException()
    // we can't read data anymore
    if (limit == Long.MAX_VALUE) throw EOFException()
    // there is no more data
    if (!request(limit + 1)) throw EOFException()

    val b = buffer[limit]
    if (b == '\n'.code.toByte()) {
        val str = readString(limit)
        skip(1)
        return str
    }
    // check if the last byte is CR and the byte passed it is LF
    if (b != '\r'.code.toByte() || !request(limit + 2)) throw EOFException()
    if (buffer[limit + 1] != '\n'.code.toByte()) throw EOFException()
    val res = readString(limit)
    skip(2)
    return res
}

private fun Buffer.commonReadUtf8CodePoint(): Int {
    require(1)

    val b0 = this[0]
    var codePoint: Int
    val byteCount: Int
    val min: Int

    when {
        b0 and 0x80 == 0 -> {
            // 0xxxxxxx.
            codePoint = b0 and 0x7f
            byteCount = 1 // 7 bits (ASCII).
            min = 0x0
        }

        b0 and 0xe0 == 0xc0 -> {
            // 0x110xxxxx
            codePoint = b0 and 0x1f
            byteCount = 2 // 11 bits (5 + 6).
            min = 0x80
        }

        b0 and 0xf0 == 0xe0 -> {
            // 0x1110xxxx
            codePoint = b0 and 0x0f
            byteCount = 3 // 16 bits (4 + 6 + 6).
            min = 0x800
        }

        b0 and 0xf8 == 0xf0 -> {
            // 0x11110xxx
            codePoint = b0 and 0x07
            byteCount = 4 // 21 bits (3 + 6 + 6 + 6).
            min = 0x10000
        }

        else -> {
            // We expected the first byte of a code point but got something else.
            skip(1)
            return REPLACEMENT_CODE_POINT
        }
    }

    if (size < byteCount) {
        throw EOFException("size < $byteCount: $size (to read code point prefixed 0x${b0.toHexString()})")
    }

    // Read the continuation bytes. If we encounter a non-continuation byte, the sequence consumed
    // thus far is truncated and is decoded as the replacement character. That non-continuation byte
    // is left in the stream for processing by the next call to readUtf8CodePoint().
    for (i in 1 until byteCount) {
        val b = this[i.toLong()]
        if (b and 0xc0 == 0x80) {
            // 0x10xxxxxx
            codePoint = codePoint shl 6
            codePoint = codePoint or (b and 0x3f)
        } else {
            skip(i.toLong())
            return REPLACEMENT_CODE_POINT
        }
    }

    skip(byteCount.toLong())

    return when {
        codePoint > 0x10ffff -> {
            REPLACEMENT_CODE_POINT // Reject code points larger than the Unicode maximum.
        }

        codePoint in 0xd800..0xdfff -> {
            REPLACEMENT_CODE_POINT // Reject partial surrogates.
        }

        codePoint < min -> {
            REPLACEMENT_CODE_POINT // Reject overlong code points.
        }

        else -> codePoint
    }
}

@OptIn(UnsafeIoApi::class)
private fun Buffer.commonWriteUtf8(string: String, beginIndex: Int, endIndex: Int) {
    checkBounds(string.length, beginIndex, endIndex)

    var i = beginIndex
    while (i < endIndex) {
        writeUnbound(4 /* reserve enough space for the worst case */) {
            var j = 0
            val limit = it.capacity
            while (i < endIndex && limit - j >= 4) {
                var c = string[i].code

                when {
                    c < 0x80 -> {
                        val runLimit = minOf(i + limit - j, endIndex)
                        UnsafeSegmentAccessors.setUnsafe(this, it, j++, c.toByte()) // 0xxxxxxx
                        i++

                        while (i < runLimit) {
                            c = string[i].code
                            if (c >= 0x80) return@writeUnbound j
                            i++
                            UnsafeSegmentAccessors.setUnsafe(this, it, j++, c.toByte()) // 0xxxxxxx
                        }
                    }
                    c < 0x800 -> {
                        UnsafeSegmentAccessors.setUnsafe(this, it, j++, (c shr 6 or 0xc0).toByte()) // 110xxxxx
                        UnsafeSegmentAccessors.setUnsafe(this, it, j++, (c and 0x3f or 0x80).toByte()) // 10xxxxxx
                        i++
                    }
                    c < 0xd800 || c > 0xdfff -> {
                        // Emit a 16-bit character with 3 bytes.
                        UnsafeSegmentAccessors.setUnsafe(this, it, j++, (c shr 12 or 0xe0).toByte()) // 1110xxxx
                        UnsafeSegmentAccessors.setUnsafe(this, it, j++, (c shr 6 and 0x3f or 0x80).toByte()) // 10xxxxxx
                        UnsafeSegmentAccessors.setUnsafe(this, it, j++, (c and 0x3f or 0x80).toByte()) // 10xxxxxx
                        i++
                    }
                    else -> {
                        // c is a surrogate. Make sure it is a high surrogate & that its successor is a low
                        // surrogate. If not, the UTF-16 is invalid, in which case we emit a replacement
                        // character.
                        val low = (if (i + 1 < endIndex) string[i + 1].code else 0)
                        if (c > 0xdbff || low !in 0xdc00..0xdfff) {
                            UnsafeSegmentAccessors.setUnsafe(this, it, j++, '?'.code.toByte())
                            i++
                        } else {
                            // UTF-16 high surrogate: 110110xxxxxxxxxx (10 bits)
                            // UTF-16 low surrogate:  110111yyyyyyyyyy (10 bits)
                            // Unicode code point:    00010000000000000000 + xxxxxxxxxxyyyyyyyyyy (21 bits)
                            val codePoint = 0x010000 + (c and 0x03ff shl 10 or (low and 0x03ff))

                            // Emit a 21-bit character with 4 bytes.
                            UnsafeSegmentAccessors.setUnsafe(this, it, j++, (codePoint shr 18 or 0xf0).toByte()) // 11110xxx
                            UnsafeSegmentAccessors.setUnsafe(this, it, j++, (codePoint shr 12 and 0x3f or 0x80).toByte()) // 10xxxxxx
                            UnsafeSegmentAccessors.setUnsafe(this, it, j++, (codePoint shr 6 and 0x3f or 0x80).toByte()) // 10xxyyyy
                            UnsafeSegmentAccessors.setUnsafe(this, it, j++, (codePoint and 0x3f or 0x80).toByte()) // 10yyyyyy
                            i += 2
                        }
                    }
                }
            }
            j
        }
    }
}

/*
@OptIn(UnsafeIoApi::class)
private fun Buffer.commonWriteUtf8_old(string: String, beginIndex: Int, endIndex: Int) {
    checkBounds(string.length, beginIndex, endIndex)

    // Transcode a UTF-16 Java String to UTF-8 bytes.
    var i = beginIndex
    while (i < endIndex) {
        var c = string[i].code

        when {
            c < 0x80 -> {
                writeUnbound(1) {
                    val segmentOffset = - i
                    val runLimit = minOf(endIndex, it.capacity)

                    // Emit a 7-bit character with 1 byte.
                    UnsafeSegmentAccessors.setUnsafe(this, it, segmentOffset + i++, c.toByte()) // 0xxxxxxx

                    // Fast-path contiguous runs of ASCII characters. This is ugly, but yields a ~4x performance
                    // improvement over independent calls to writeByte().
                    while (i < runLimit) {
                        c = string[i].code
                        if (c >= 0x80) break
                        UnsafeSegmentAccessors.setUnsafe(this, it, segmentOffset + i++, c.toByte()) // 0xxxxxxx
                    }

                     i + segmentOffset // Equivalent to i - (previous i).
                }
            }

            c < 0x800 -> {
                // Emit a 11-bit character with 2 bytes.
                writeUnbound(2) {
                    UnsafeSegmentAccessors.setUnsafe(this, it, 0, (c shr 6 or 0xc0).toByte()) // 110xxxxx
                    UnsafeSegmentAccessors.setUnsafe(this, it, 1, (c and 0x3f or 0x80).toByte()) // 10xxxxxx
                    2
                }
                i++
            }

            c < 0xd800 || c > 0xdfff -> {
                // Emit a 16-bit character with 3 bytes.
                writeUnbound(3) {
                    UnsafeSegmentAccessors.setUnsafe(this, it, 0, (c shr 12 or 0xe0).toByte()) // 1110xxxx
                    UnsafeSegmentAccessors.setUnsafe(this, it, 1, (c shr 6 and 0x3f or 0x80).toByte()) // 10xxxxxx
                    UnsafeSegmentAccessors.setUnsafe(this, it, 2, (c and 0x3f or 0x80).toByte()) // 10xxxxxx
                    3
                }
                i++
            }

            else -> {
                // c is a surrogate. Make sure it is a high surrogate & that its successor is a low
                // surrogate. If not, the UTF-16 is invalid, in which case we emit a replacement
                // character.
                val low = (if (i + 1 < endIndex) string[i + 1].code else 0)
                if (c > 0xdbff || low !in 0xdc00..0xdfff) {
                    writeByte('?'.code.toByte())
                    i++
                } else {
                    // UTF-16 high surrogate: 110110xxxxxxxxxx (10 bits)
                    // UTF-16 low surrogate:  110111yyyyyyyyyy (10 bits)
                    // Unicode code point:    00010000000000000000 + xxxxxxxxxxyyyyyyyyyy (21 bits)
                    val codePoint = 0x010000 + (c and 0x03ff shl 10 or (low and 0x03ff))

                    // Emit a 21-bit character with 4 bytes.
                    writeUnbound(4) {
                        UnsafeSegmentAccessors.setUnsafe(this, it, 0, (codePoint shr 18 or 0xf0).toByte()) // 11110xxx
                        UnsafeSegmentAccessors.setUnsafe(this, it, 1, (codePoint shr 12 and 0x3f or 0x80).toByte()) // 10xxxxxx
                        UnsafeSegmentAccessors.setUnsafe(this, it, 2, (codePoint shr 6 and 0x3f or 0x80).toByte()) // 10xxyyyy
                        UnsafeSegmentAccessors.setUnsafe(this, it, 3, (codePoint and 0x3f or 0x80).toByte()) // 10yyyyyy
                        4
                    }
                    i += 2
                }
            }
        }
    }
}
 */

private fun Buffer.commonWriteUtf8CodePoint(codePoint: Int) {
    when {
        codePoint < 0 || codePoint > 0x10ffff -> {
            throw IllegalArgumentException(
                "Code point value is out of Unicode codespace 0..0x10ffff: 0x${codePoint.toHexString()} ($codePoint)"
            )
        }

        codePoint < 0x80 -> {
            // Emit a 7-bit code point with 1 byte.
            writeByte(codePoint.toByte())
        }

        codePoint < 0x800 -> {
            // Emit a 11-bit code point with 2 bytes.
            writeUnbound(2) {
                it[0] = (codePoint shr 6 or 0xc0).toByte() // 110xxxxx
                it[1] = (codePoint and 0x3f or 0x80).toByte() // 10xxxxxx
                2
            }
        }

        codePoint in 0xd800..0xdfff -> {
            // Emit a replacement character for a partial surrogate.
            writeByte('?'.code.toByte())
        }

        codePoint < 0x10000 -> {
            // Emit a 16-bit code point with 3 bytes.
            writeUnbound(3) {
                it[0] = (codePoint shr 12 or 0xe0).toByte() // 1110xxxx
                it[1] = (codePoint shr 6 and 0x3f or 0x80).toByte() // 10xxxxxx
                it[2] = (codePoint and 0x3f or 0x80).toByte() // 10xxxxxx
                3
            }
        }

        else -> { // [0x10000, 0x10ffff]
            // Emit a 21-bit code point with 4 bytes.
            writeUnbound(4) {
                it[0] = (codePoint shr 18 or 0xf0).toByte() // 11110xxx
                it[1] = (codePoint shr 12 and 0x3f or 0x80).toByte() // 10xxxxxx
                it[2] = (codePoint shr 6 and 0x3f or 0x80).toByte() // 10xxyyyy
                it[3] = (codePoint and 0x3f or 0x80).toByte() // 10yyyyyy
                4
            }
        }
    }
}

private fun Buffer.commonReadUtf8(byteCount: Long): String {
    require(byteCount >= 0 && byteCount <= Int.MAX_VALUE) {
        "byteCount ($byteCount) is not within the range [0..${Int.MAX_VALUE})"
    }
    require(byteCount)
    if (byteCount == 0L) return ""

    val s = head!!
    if (s.pos + byteCount > s.limit) {
        // If the string spans multiple segments, delegate to readBytes().
        return readByteArray(byteCount.toInt()).commonToUtf8String()
    }

    val result = s.commonToUtf8String(0, byteCount.toInt())
    skip(byteCount)
    return result
}
