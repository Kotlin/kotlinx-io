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
 * kotlinx-io assumes most applications use UTF-8 exclusively, and offers optimized implementations of
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
@file:JvmName("Utf8")

package kotlinx.io

import kotlinx.io.internal.commonReadUtf8
import kotlinx.io.internal.commonReadUtf8CodePoint
import kotlinx.io.internal.commonWriteUtf8
import kotlinx.io.internal.commonWriteUtf8CodePoint
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

/**
 * Returns the number of bytes used to encode the slice of `string` as UTF-8 when using [Sink.writeUtf8].
 *
 * @param beginIndex the index of the first character to encode.
 * @param endIndex the index of the character past the last character to encode.
 *
 * @throws IndexOutOfBoundsException when [beginIndex] or [endIndex] correspond to a range
 * out of the current string bounds.
 */
@JvmOverloads
@JvmName("size")
fun String.utf8Size(beginIndex: Int = 0, endIndex: Int = length): Long {
  require(beginIndex >= 0) { "beginIndex < 0: $beginIndex" }
  require(endIndex >= beginIndex) { "endIndex < beginIndex: $endIndex < $beginIndex" }
  require(endIndex <= length) { "endIndex > string.length: $endIndex > $length" }

  var result = 0L
  var i = beginIndex
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

// TODO combine with Buffer.writeUtf8?
// TODO combine with Buffer.writeUtf8CodePoint?
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
        yield((c.code shr 6          or 0xc0).toByte()) // 110xxxxx
        yield((c.code and 0x3f or 0x80).toByte()) // 10xxxxxx
        /* ktlint-enable no-multi-spaces */
        index++
      }

      c !in '\ud800'..'\udfff' -> {
        // Emit a 16-bit character with 3 bytes.
        /* ktlint-disable no-multi-spaces */
        yield((c.code shr 12          or 0xe0).toByte()) // 1110xxxx
        yield((c.code shr  6 and 0x3f or 0x80).toByte()) // 10xxxxxx
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
          val codePoint = (
            ((c.code shl 10) + this[index + 1].code) +
              (0x010000 - (0xd800 shl 10) - 0xdc00)
            )

          // Emit a 21-bit character with 4 bytes.
          /* ktlint-disable no-multi-spaces */
          yield((codePoint shr 18          or 0xf0).toByte()) // 11110xxx
          yield((codePoint shr 12 and 0x3f or 0x80).toByte()) // 10xxxxxx
          yield((codePoint shr 6  and 0x3f or 0x80).toByte()) // 10xxyyyy
          yield((codePoint        and 0x3f or 0x80).toByte()) // 10yyyyyy
          /* ktlint-enable no-multi-spaces */
          index += 2
        }
      }
    }
  }
}

// TODO combine with Buffer.readUtf8CodePoint?
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

// TODO combine with Buffer.readUtf8?
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
            yield(((codePoint ushr 10   ) + HIGH_SURROGATE_HEADER).toChar())
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

  val codePoint =
    (
      MASK_2BYTES
        xor (b1.toInt())
        xor (b0.toInt() shl 6)
      )

  when {
    codePoint < 0x80 -> {
      yield(REPLACEMENT_CODE_POINT) // Reject overlong code points.
    }
    else -> {
      yield(codePoint)
    }
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

  val codePoint =
    (
      MASK_3BYTES
        xor (b2.toInt())
        xor (b1.toInt() shl 6)
        xor (b0.toInt() shl 12)
      )

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

  val codePoint =
    (
      MASK_4BYTES
        xor (b3.toInt())
        xor (b2.toInt() shl 6)
        xor (b1.toInt() shl 12)
        xor (b0.toInt() shl 18)
      )

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

/**
 * Encodes [codePoint] in UTF-8 and writes it to this sink.
 *
 * @param codePoint the codePoint to be written.
 */
fun <T: Sink> T.writeUtf8CodePoint(codePoint: Int): T {
  buffer.commonWriteUtf8CodePoint(codePoint)
  emitCompleteSegments()
  return this
}

/**
 * Encodes the characters at [beginIndex] up to [endIndex] from [string] in UTF-8 and writes it to this sink.
 *
 * @param string the string to be encoded.
 * @param beginIndex the index of a first character to encode, 0 by default.
 * @param endIndex the index of a character past to a last character to encode, `string.length` by default.
 *
 * @throws IndexOutOfBoundsException when [beginIndex] or [endIndex] correspond to a range
 * out of the current string bounds.
 */
fun <T: Sink> T.writeUtf8(string: String, beginIndex: Int = 0, endIndex: Int = string.length): T {
  buffer.commonWriteUtf8(string, beginIndex, endIndex)
  emitCompleteSegments()
  return this
}

/**
 * Removes all bytes from this source, decodes them as UTF-8, and returns the string.
 *
 * Returns the empty string if this source is empty.
 */
fun Source.readUtf8(): String {
  var req: Long = 1
  while (request(req)) {
    req *= 2
  }
  return buffer.commonReadUtf8(buffer.size)
}

/**
 * Removes all bytes from this buffer, decodes them as UTF-8, and returns the string.
 *
 * Returns the empty string if this buffer is empty.
 */
fun Buffer.readUtf8(): String {
  return commonReadUtf8(buffer.size)
}

/**
 * Removes [byteCount] bytes from this source, decodes them as UTF-8, and returns the string.
 *
 * @throws EOFException when the source is exhausted before reading [byteCount] bytes from it.
 */
fun Source.readUtf8(byteCount: Long): String {
  require(byteCount)
  return buffer.commonReadUtf8(byteCount)
}

/**
 * Removes and returns a single UTF-8 code point, reading between 1 and 4 bytes as necessary.
 *
 * If this source is exhausted before a complete code point can be read, this throws an
 * [EOFException] and consumes no input.
 *
 * If this source doesn't start with a properly-encoded UTF-8 code point, this method will remove
 * 1 or more non-UTF-8 bytes and return the replacement character (`U+FFFD`). This covers encoding
 * problems (the input is not properly-encoded UTF-8), characters out of range (beyond the
 * 0x10ffff limit of Unicode), code points for UTF-16 surrogates (U+d800..U+dfff) and overlong
 * encodings (such as `0xc080` for the NUL character in modified UTF-8).
 *
 * @throws EOFException when the source is exhausted before a complete code point can be read.
 */
fun Source.readUtf8CodePoint(): Int {
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
 * @see Source.readUtf8CodePoint
 */
fun Buffer.readUtf8CodePoint(): Int {
  return buffer.commonReadUtf8CodePoint()
}

/**
 * Removes and returns characters up to but not including the next line break. A line break is
 * either `"\n"` or `"\r\n"`; these characters are not included in the result.
 *
 * On the end of the stream this method returns null. If the source doesn't end with a line break then
 * an implicit line break is assumed. Null is returned once the source is exhausted.
 */
fun Source.readUtf8Line(): String? {
  if (!request(1)) return null

  val peekSource = peek()
  var offset = 0L
  var newlineSize = 0L
  while (peekSource.request(1)) {
    val b = peekSource.readByte().toInt()
    if (b == '\n'.code) {
      newlineSize = 1L
      break
    } else if (b == '\r'.code) {
      if (peekSource.request(1) && peekSource.buffer[0].toInt() == '\n'.code) {
        newlineSize = 2L
        break
      }
    }
    offset++
  }
  val line = readUtf8(offset)
  skip(newlineSize)
  return line
}

/**
 * Removes and returns characters up to but not including the next line break, throwing
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
 */
fun Source.readUtf8LineStrict(limit: Long = Long.MAX_VALUE): String {
  if (!request(1)) throw EOFException()

  val peekSource = peek()
  var offset = 0L
  var newlineSize = 0L
  while (offset < limit && peekSource.request(1)) {
    val b = peekSource.readByte().toInt()
    if (b == '\n'.code) {
      newlineSize = 1L
      break
    } else if (b == '\r'.code) {
      if (peekSource.request(1) && peekSource.buffer[0].toInt() == '\n'.code) {
        newlineSize = 2L
        break
      }
    }
    offset++
  }
  if (offset == limit) {
    if (!peekSource.request(1)) throw EOFException()
    val nlCandidate = peekSource.readByte().toInt()
    if (nlCandidate == '\n'.code) {
      newlineSize = 1
    } else if (nlCandidate == '\r'.code && peekSource.request(1) && peekSource.readByte().toInt() == '\n'.code) {
      newlineSize = 2
    }
  }
  if (newlineSize == 0L) throw EOFException()
  val line = readUtf8(offset)
  skip(newlineSize)
  return line
}