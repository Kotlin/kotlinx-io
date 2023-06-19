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

/**
 * Returns the number of bytes used to encode the slice of `string` as UTF-8 when using [Sink.writeUtf8].
 *
 * @param startIndex the index (inclusive) of the first character to encode, `0` by default.
 * @param endIndex the index (exclusive) of the character past the last character to encode, `string.length` by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of string indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 */
public fun String.utf8Size(startIndex: Int = 0, endIndex: Int = length): Long {
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
 * @param codePoint the codePoint to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 */
@OptIn(DelicateIoApi::class)
public fun Sink.writeUtf8CodePoint(codePoint: Int): Unit =
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
 */
@OptIn(DelicateIoApi::class)
public fun Sink.writeUtf8(string: String, startIndex: Int = 0, endIndex: Int = string.length): Unit =
  writeToInternalBuffer { it.commonWriteUtf8(string, startIndex, endIndex) }

/**
 * Removes all bytes from this source, decodes them as UTF-8, and returns the string.
 *
 * Returns the empty string if this source is empty.
 *
 * @throws IllegalStateException when the source is closed.
 */
@OptIn(InternalIoApi::class)
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
 * @throws IllegalStateException when the source is closed.
 */
@OptIn(InternalIoApi::class)
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
 * @throws IllegalStateException when the source is closed.
 */
@OptIn(InternalIoApi::class)
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
 *
 * @throws IllegalStateException when the source is closed.
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
 * @throws IllegalStateException when the source is closed.
 * @throws IllegalArgumentException when [limit] is negative.
 */
public fun Source.readUtf8LineStrict(limit: Long = Long.MAX_VALUE): String {
  require(limit >= 0) { "limit: $limit" }
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

private fun Buffer.commonReadUtf8CodePoint(): Int {
  if (size == 0L) throw EOFException()

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

private fun Buffer.commonWriteUtf8(string: String, beginIndex: Int, endIndex: Int) {
  checkBounds(string.length, beginIndex, endIndex)

  // Transcode a UTF-16 Java String to UTF-8 bytes.
  var i = beginIndex
  while (i < endIndex) {
    var c = string[i].code

    when {
      c < 0x80 -> {
        val tail = writableSegment(1)
        val data = tail.data
        val segmentOffset = tail.limit - i
        val runLimit = minOf(endIndex, Segment.SIZE - segmentOffset)

        // Emit a 7-bit character with 1 byte.
        data[segmentOffset + i++] = c.toByte() // 0xxxxxxx

        // Fast-path contiguous runs of ASCII characters. This is ugly, but yields a ~4x performance
        // improvement over independent calls to writeByte().
        while (i < runLimit) {
          c = string[i].code
          if (c >= 0x80) break
          data[segmentOffset + i++] = c.toByte() // 0xxxxxxx
        }

        val runSize = i + segmentOffset - tail.limit // Equivalent to i - (previous i).
        tail.limit += runSize
        size += runSize.toLong()
      }

      c < 0x800 -> {
        // Emit a 11-bit character with 2 bytes.
        val tail = writableSegment(2)
        tail.data[tail.limit    ] = (c shr 6          or 0xc0).toByte() // 110xxxxx
        tail.data[tail.limit + 1] = (c       and 0x3f or 0x80).toByte() // 10xxxxxx
        tail.limit += 2
        size += 2L
        i++
      }

      c < 0xd800 || c > 0xdfff -> {
        // Emit a 16-bit character with 3 bytes.
        val tail = writableSegment(3)
        tail.data[tail.limit    ] = (c shr 12          or 0xe0).toByte() // 1110xxxx
        tail.data[tail.limit + 1] = (c shr  6 and 0x3f or 0x80).toByte() // 10xxxxxx
        tail.data[tail.limit + 2] = (c        and 0x3f or 0x80).toByte() // 10xxxxxx
        tail.limit += 3
        size += 3L
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
          val tail = writableSegment(4)
          tail.data[tail.limit    ] = (codePoint shr 18          or 0xf0).toByte() // 11110xxx
          tail.data[tail.limit + 1] = (codePoint shr 12 and 0x3f or 0x80).toByte() // 10xxxxxx
          tail.data[tail.limit + 2] = (codePoint shr  6 and 0x3f or 0x80).toByte() // 10xxyyyy
          tail.data[tail.limit + 3] = (codePoint        and 0x3f or 0x80).toByte() // 10yyyyyy
          tail.limit += 4
          size += 4L
          i += 2
        }
      }
    }
  }
}

private fun Buffer.commonWriteUtf8CodePoint(codePoint: Int) {
  when {
    codePoint < 0x80 -> {
      // Emit a 7-bit code point with 1 byte.
      writeByte(codePoint.toByte())
    }
    codePoint < 0x800 -> {
      // Emit a 11-bit code point with 2 bytes.
      val tail = writableSegment(2)
      tail.data[tail.limit    ] = (codePoint shr 6          or 0xc0).toByte() // 110xxxxx
      tail.data[tail.limit + 1] = (codePoint       and 0x3f or 0x80).toByte() // 10xxxxxx
      tail.limit += 2
      size += 2L
    }
    codePoint in 0xd800..0xdfff -> {
      // Emit a replacement character for a partial surrogate.
      writeByte('?'.code.toByte())
    }
    codePoint < 0x10000 -> {
      // Emit a 16-bit code point with 3 bytes.
      val tail = writableSegment(3)
      tail.data[tail.limit    ] = (codePoint shr 12          or 0xe0).toByte() // 1110xxxx
      tail.data[tail.limit + 1] = (codePoint shr  6 and 0x3f or 0x80).toByte() // 10xxxxxx
      tail.data[tail.limit + 2] = (codePoint        and 0x3f or 0x80).toByte() // 10xxxxxx
      tail.limit += 3
      size += 3L
    }
    codePoint <= 0x10ffff -> {
      // Emit a 21-bit code point with 4 bytes.
      val tail = writableSegment(4)
      tail.data[tail.limit    ] = (codePoint shr 18          or 0xf0).toByte() // 11110xxx
      tail.data[tail.limit + 1] = (codePoint shr 12 and 0x3f or 0x80).toByte() // 10xxxxxx
      tail.data[tail.limit + 2] = (codePoint shr  6 and 0x3f or 0x80).toByte() // 10xxyyyy
      tail.data[tail.limit + 3] = (codePoint        and 0x3f or 0x80).toByte() // 10yyyyyy
      tail.limit += 4
      size += 4L
    }
    else -> {
      throw IllegalArgumentException("Unexpected code point: 0x${codePoint.toHexString()}")
    }
  }
}

private fun Buffer.commonReadUtf8(byteCount: Long): String {
  require(byteCount >= 0 && byteCount <= Int.MAX_VALUE) { "byteCount: $byteCount" }
  if (size < byteCount) throw EOFException()
  if (byteCount == 0L) return ""

  val s = head!!
  if (s.pos + byteCount > s.limit) {
    // If the string spans multiple segments, delegate to readBytes().

    return readByteArray(byteCount.toInt()).commonToUtf8String()
  }

  val result = s.data.commonToUtf8String(s.pos, s.pos + byteCount.toInt())
  s.pos += byteCount.toInt()
  size -= byteCount

  if (s.pos == s.limit) {
    head = s.pop()
    SegmentPool.recycle(s)
  }

  return result
}
