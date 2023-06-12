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

import kotlinx.io.internal.commonReadUtf8
import kotlinx.io.internal.commonReadUtf8CodePoint
import kotlinx.io.internal.commonWriteUtf8
import kotlinx.io.internal.commonWriteUtf8CodePoint

/**
 * Returns the number of bytes used to encode the slice of `string` as UTF-8 when using [Sink.writeUtf8].
 *
 * @param beginIndex the index of the first character to encode, inclusive.
 * @param endIndex the index of the character past the last character to encode, exclusive.
 *
 * @throws IndexOutOfBoundsException when [beginIndex] or [endIndex] correspond to a range
 * out of the current string bounds.
 */
public fun String.utf8Size(beginIndex: Int = 0, endIndex: Int = length): Long {
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

/**
 * Encodes [codePoint] in UTF-8 and writes it to this sink.
 *
 * @param codePoint the codePoint to be written.
 */
@OptIn(DelicateIoApi::class)
public fun <T: Sink> T.writeUtf8CodePoint(codePoint: Int): T {
  buffer.commonWriteUtf8CodePoint(codePoint)
  emitCompleteSegments()
  return this
}

/**
 * Encodes the characters at [beginIndex] up to [endIndex] from [string] in UTF-8 and writes it to this sink.
 *
 * @param string the string to be encoded.
 * @param beginIndex the index of the first character to encode, 0 by default.
 * @param endIndex the index of a character past to a last character to encode, `string.length` by default.
 *
 * @throws IndexOutOfBoundsException when [beginIndex] or [endIndex] correspond to a range
 * out of the current string bounds.
 */
@OptIn(DelicateIoApi::class)
public fun <T: Sink> T.writeUtf8(string: String, beginIndex: Int = 0, endIndex: Int = string.length): T {
  buffer.commonWriteUtf8(string, beginIndex, endIndex)
  emitCompleteSegments()
  return this
}

/**
 * Removes all bytes from this source, decodes them as UTF-8, and returns the string.
 *
 * Returns the empty string if this source is empty.
 */
@OptIn(DelicateIoApi::class)
public fun Source.readUtf8(): String {
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
 */
public fun Buffer.readUtf8(): String {
  return commonReadUtf8(size)
}

/**
 * Removes [byteCount] bytes from this source, decodes them as UTF-8, and returns the string.
 *
 * @param byteCount the number of bytes to read from the source for string decoding.
 *
 * @throws IllegalArgumentException when [byteCount] is negative.
 * @throws EOFException when the source is exhausted before reading [byteCount] bytes from it.
 */
@OptIn(DelicateIoApi::class)
public fun Source.readUtf8(byteCount: Long): String {
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
 * 1 or more non-UTF-8 bytes and return the replacement character (`U+fffd`). This covers encoding
 * problems (the input is not properly-encoded UTF-8), characters out of range (beyond the
 * `0x10ffff` limit of Unicode), code points for UTF-16 surrogates (`U+d800`..`U+dfff`) and overlong
 * encodings (such as `0xc080` for the NUL character in modified UTF-8).
 *
 * @throws EOFException when the source is exhausted before a complete code point can be read.
 */
@OptIn(DelicateIoApi::class)
public fun Source.readUtf8CodePoint(): Int {
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
public fun Buffer.readUtf8CodePoint(): Int {
  return this.commonReadUtf8CodePoint()
}

/**
 * Removes and returns characters up to but not including the next line break. A line break is
 * either `"\n"` or `"\r\n"`; these characters are not included in the result.
 *
 * On the end of the stream this method returns null. If the source doesn't end with a line break, then
 * an implicit line break is assumed. Null is returned once the source is exhausted.
 */
public fun Source.readUtf8Line(): String? {
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
      if (peekSource.startsWith('\n'.code.toByte())) {
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
public fun Source.readUtf8LineStrict(limit: Long = Long.MAX_VALUE): String {
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
      if (peekSource.startsWith('\n'.code.toByte())) {
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
    } else if (nlCandidate == '\r'.code && peekSource.startsWith('\n'.code.toByte())) {
      newlineSize = 2
    }
  }
  if (newlineSize == 0L) throw EOFException()
  val line = readUtf8(offset)
  skip(newlineSize)
  return line
}
