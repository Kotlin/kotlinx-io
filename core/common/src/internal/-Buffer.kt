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

// TODO move to Buffer class: https://youtrack.jetbrains.com/issue/KT-20427

@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.internal

import kotlinx.io.*
import kotlin.native.concurrent.SharedImmutable

@SharedImmutable
internal val HEX_DIGIT_BYTES = "0123456789abcdef".asUtf8ToByteArray()

/**
 * Returns true if the range within this buffer starting at `segmentPos` in `segment` is equal to
 * `bytes[bytesOffset..bytesLimit)`.
 */
// TODO: remove?
/*
internal fun rangeEquals(
  segment: Segment,
  segmentPos: Int,
  bytes: ByteArray,
  bytesOffset: Int,
  bytesLimit: Int
): Boolean {
  var segment = segment
  var segmentPos = segmentPos
  var segmentLimit = segment.limit
  var data = segment.data

  var i = bytesOffset
  while (i < bytesLimit) {
    if (segmentPos == segmentLimit) {
      segment = segment.next!!
      data = segment.data
      segmentPos = segment.pos
      segmentLimit = segment.limit
    }

    if (data[segmentPos] != bytes[i]) {
      return false
    }

    segmentPos++
    i++
  }

  return true
}
*/

internal fun Buffer.readUtf8Line(newline: Long): String {
  return when {
    newline > 0 && this[newline - 1] == '\r'.code.toByte() -> {
      // Read everything until '\r\n', then skip the '\r\n'.
      val result = readUtf8(newline - 1L)
      skip(2L)
      result
    }
    else -> {
      // Read everything until '\n', then skip the '\n'.
      val result = readUtf8(newline)
      skip(1L)
      result
    }
  }
}

/**
 * Invoke `lambda` with the segment and offset at `fromIndex`. Searches from the front or the back
 * depending on what's closer to `fromIndex`.
 */
internal inline fun <T> Buffer.seek(
  fromIndex: Long,
  lambda: (Segment?, Long) -> T
): T {
  var s: Segment = head ?: return lambda(null, -1L)

  if (size - fromIndex < fromIndex) {
    // We're scanning in the back half of this buffer. Find the segment starting at the back.
    var offset = size
    while (offset > fromIndex) {
      s = s.prev!!
      offset -= (s.limit - s.pos).toLong()
    }
    return lambda(s, offset)
  } else {
    // We're scanning in the front half of this buffer. Find the segment starting at the front.
    var offset = 0L
    while (true) {
      val nextOffset = offset + (s.limit - s.pos)
      if (nextOffset > fromIndex) break
      s = s.next!!
      offset = nextOffset
    }
    return lambda(s, offset)
  }
}
// TODO Kotlin's expect classes can't have default implementations, so platform implementations
// have to call these functions. Remove all this nonsense when expect class allow actual code.

internal inline fun Buffer.commonCopyTo(
  out: Buffer,
  offset: Long,
  byteCount: Long
): Buffer {
  checkOffsetAndCount(size, offset, byteCount)
  if (byteCount == 0L) return this

  var currentOffset = offset
  var remainingByteCount = byteCount

  out.size += remainingByteCount

  // Skip segments that we aren't copying from.
  var s = head
  while (currentOffset >= s!!.limit - s.pos) {
    currentOffset -= (s.limit - s.pos).toLong()
    s = s.next
  }

  // Copy one segment at a time.
  while (remainingByteCount > 0L) {
    val copy = s!!.sharedCopy()
    copy.pos += currentOffset.toInt()
    copy.limit = minOf(copy.pos + remainingByteCount.toInt(), copy.limit)
    if (out.head == null) {
      copy.prev = copy
      copy.next = copy.prev
      out.head = copy.next
    } else {
      out.head!!.prev!!.push(copy)
    }
    remainingByteCount -= (copy.limit - copy.pos).toLong()
    currentOffset = 0L
    s = s.next
  }

  return this
}

internal inline fun Buffer.commonCompleteSegmentByteCount(): Long {
  var result = size
  if (result == 0L) return 0L

  // Omit the tail if it's still writable.
  val tail = head!!.prev!!
  if (tail.limit < Segment.SIZE && tail.owner) {
    result -= (tail.limit - tail.pos).toLong()
  }

  return result
}

internal inline fun Buffer.commonReadByte(): Byte {
  if (size == 0L) throw EOFException()

  val segment = head!!
  var pos = segment.pos
  val limit = segment.limit

  val data = segment.data
  val b = data[pos++]
  size -= 1L

  if (pos == limit) {
    head = segment.pop()
    SegmentPool.recycle(segment)
  } else {
    segment.pos = pos
  }

  return b
}

internal inline fun Buffer.commonReadShort(): Short {
  if (size < 2L) throw EOFException()

  val segment = head!!
  var pos = segment.pos
  val limit = segment.limit

  // If the short is split across multiple segments, delegate to readByte().
  if (limit - pos < 2) {
    val s = readByte() and 0xff shl 8 or (readByte() and 0xff)
    return s.toShort()
  }

  val data = segment.data
  val s = data[pos++] and 0xff shl 8 or (data[pos++] and 0xff)
  size -= 2L

  if (pos == limit) {
    head = segment.pop()
    SegmentPool.recycle(segment)
  } else {
    segment.pos = pos
  }

  return s.toShort()
}

internal inline fun Buffer.commonReadInt(): Int {
  if (size < 4L) throw EOFException()

  val segment = head!!
  var pos = segment.pos
  val limit = segment.limit

  // If the int is split across multiple segments, delegate to readByte().
  if (limit - pos < 4L) {
    return (
      readByte() and 0xff shl 24
        or (readByte() and 0xff shl 16)
        or (readByte() and 0xff shl 8) // ktlint-disable no-multi-spaces
        or (readByte() and 0xff)
      )
  }

  val data = segment.data
  val i = (
    data[pos++] and 0xff shl 24
      or (data[pos++] and 0xff shl 16)
      or (data[pos++] and 0xff shl 8)
      or (data[pos++] and 0xff)
    )
  size -= 4L

  if (pos == limit) {
    head = segment.pop()
    SegmentPool.recycle(segment)
  } else {
    segment.pos = pos
  }

  return i
}

internal inline fun Buffer.commonReadLong(): Long {
  if (size < 8L) throw EOFException()

  val segment = head!!
  var pos = segment.pos
  val limit = segment.limit

  // If the long is split across multiple segments, delegate to readInt().
  if (limit - pos < 8L) {
    return (
      readInt() and 0xffffffffL shl 32
        or (readInt() and 0xffffffffL)
      )
  }

  val data = segment.data
  val v = (
    data[pos++] and 0xffL shl 56
      or (data[pos++] and 0xffL shl 48)
      or (data[pos++] and 0xffL shl 40)
      or (data[pos++] and 0xffL shl 32)
      or (data[pos++] and 0xffL shl 24)
      or (data[pos++] and 0xffL shl 16)
      or (data[pos++] and 0xffL shl 8) // ktlint-disable no-multi-spaces
      or (data[pos++] and 0xffL)
    )
  size -= 8L

  if (pos == limit) {
    head = segment.pop()
    SegmentPool.recycle(segment)
  } else {
    segment.pos = pos
  }

  return v
}

internal inline fun Buffer.commonGet(pos: Long): Byte {
  checkOffsetAndCount(size, pos, 1L)
  seek(pos) { s, offset ->
    return s!!.data[(s.pos + pos - offset).toInt()]
  }
}

internal inline fun Buffer.commonClear() = skip(size)

internal inline fun Buffer.commonSkip(byteCount: Long) {
  var remainingByteCount = byteCount
  while (remainingByteCount > 0) {
    val head = this.head ?: throw EOFException()

    val toSkip = minOf(remainingByteCount, head.limit - head.pos).toInt()
    size -= toSkip.toLong()
    remainingByteCount -= toSkip.toLong()
    head.pos += toSkip

    if (head.pos == head.limit) {
      this.head = head.pop()
      SegmentPool.recycle(head)
    }
  }
}

//internal inline fun Buffer.commonWrite(
//  byteString: ByteString,
//  offset: Int = 0,
//  byteCount: Int = byteString.size
//): Buffer {
//  byteString.write(this, offset, byteCount)
//  return this
//}

/*
internal inline fun Buffer.commonWriteDecimalLong(v: Long): Buffer {
  var v = v
  if (v == 0L) {
    // Both a shortcut and required since the following code can't handle zero.
    return writeByte('0'.code)
  }

  var negative = false
  if (v < 0L) {
    v = -v
    if (v < 0L) { // Only true for Long.MIN_VALUE.
      return writeUtf8("-9223372036854775808")
    }
    negative = true
  }

  // Binary search for character width which favors matching lower numbers.
  var width =
    if (v < 100000000L)
      if (v < 10000L)
        if (v < 100L)
          if (v < 10L) 1
          else 2
        else if (v < 1000L) 3
        else 4
      else if (v < 1000000L)
        if (v < 100000L) 5
        else 6
      else if (v < 10000000L) 7
      else 8
    else if (v < 1000000000000L)
      if (v < 10000000000L)
        if (v < 1000000000L) 9
        else 10
      else if (v < 100000000000L) 11
      else 12
    else if (v < 1000000000000000L)
      if (v < 10000000000000L) 13
      else if (v < 100000000000000L) 14
      else 15
    else if (v < 100000000000000000L)
      if (v < 10000000000000000L) 16
      else 17
    else if (v < 1000000000000000000L) 18
    else 19
  if (negative) {
    ++width
  }

  val tail = writableSegment(width)
  val data = tail.data
  var pos = tail.limit + width // We write backwards from right to left.
  while (v != 0L) {
    val digit = (v % 10).toInt()
    data[--pos] = HEX_DIGIT_BYTES[digit]
    v /= 10
  }
  if (negative) {
    data[--pos] = '-'.code.toByte()
  }

  tail.limit += width
  this.size += width.toLong()
  return this
}

internal inline fun Buffer.commonWriteHexadecimalUnsignedLong(v: Long): Buffer {
  var v = v
  if (v == 0L) {
    // Both a shortcut and required since the following code can't handle zero.
    return writeByte('0'.code)
  }

  // Mask every bit below the most significant bit to a 1
  // http://aggregate.org/MAGIC/#Most%20Significant%201%20Bit
  var x = v
  x = x or (x ushr 1)
  x = x or (x ushr 2)
  x = x or (x ushr 4)
  x = x or (x ushr 8)
  x = x or (x ushr 16)
  x = x or (x ushr 32)

  // Count the number of 1s
  // http://aggregate.org/MAGIC/#Population%20Count%20(Ones%20Count)
  x -= x ushr 1 and 0x5555555555555555
  x = (x ushr 2 and 0x3333333333333333) + (x and 0x3333333333333333)
  x = (x ushr 4) + x and 0x0f0f0f0f0f0f0f0f
  x += x ushr 8
  x += x ushr 16
  x = (x and 0x3f) + ((x ushr 32) and 0x3f)

  // Round up to the nearest full byte
  val width = ((x + 3) / 4).toInt()

  val tail = writableSegment(width)
  val data = tail.data
  var pos = tail.limit + width - 1
  val start = tail.limit
  while (pos >= start) {
    data[pos] = HEX_DIGIT_BYTES[(v and 0xF).toInt()]
    v = v ushr 4
    pos--
  }
  tail.limit += width
  size += width.toLong()
  return this
}
 */

internal inline fun Buffer.commonWritableSegment(minimumCapacity: Int): Segment {
  require(minimumCapacity >= 1 && minimumCapacity <= Segment.SIZE) { "unexpected capacity" }

  if (head == null) {
    val result = SegmentPool.take() // Acquire a first segment.
    head = result
    result.prev = result
    result.next = result
    return result
  }

  var tail = head!!.prev
  if (tail!!.limit + minimumCapacity > Segment.SIZE || !tail.owner) {
    tail = tail.push(SegmentPool.take()) // Append a new empty segment to fill up.
  }
  return tail
}

internal inline fun Buffer.commonWrite(source: ByteArray) = write(source, 0, source.size)

internal inline fun Buffer.commonWrite(
  source: ByteArray,
  offset: Int,
  byteCount: Int
): Buffer {
  var currentOffset = offset
  checkOffsetAndCount(source.size.toLong(), currentOffset.toLong(), byteCount.toLong())

  val limit = currentOffset + byteCount
  while (currentOffset < limit) {
    val tail = writableSegment(1)

    val toCopy = minOf(limit - currentOffset, Segment.SIZE - tail.limit)
    source.copyInto(
      destination = tail.data,
      destinationOffset = tail.limit,
      startIndex = currentOffset,
      endIndex = currentOffset + toCopy
    )

    currentOffset += toCopy
    tail.limit += toCopy
  }

  size += byteCount.toLong()
  return this
}

internal inline fun Buffer.commonReadByteArray() = readByteArray(size)

internal inline fun Buffer.commonReadByteArray(byteCount: Long): ByteArray {
  require(byteCount >= 0 && byteCount <= Int.MAX_VALUE) { "byteCount: $byteCount" }
  if (size < byteCount) throw EOFException()

  val result = ByteArray(byteCount.toInt())
  readFully(result)
  return result
}

internal inline fun Buffer.commonRead(sink: ByteArray) = read(sink, 0, sink.size)

internal inline fun Buffer.commonReadFully(sink: ByteArray) {
  var offset = 0
  while (offset < sink.size) {
    val read = read(sink, offset, sink.size - offset)
    if (read == -1) throw EOFException()
    offset += read
  }
}

internal inline fun Buffer.commonRead(sink: ByteArray, offset: Int, byteCount: Int): Int {
  checkOffsetAndCount(sink.size.toLong(), offset.toLong(), byteCount.toLong())

  val s = head ?: return -1
  val toCopy = minOf(byteCount, s.limit - s.pos)
  s.data.copyInto(
    destination = sink, destinationOffset = offset, startIndex = s.pos, endIndex = s.pos + toCopy
  )

  s.pos += toCopy
  size -= toCopy.toLong()

  if (s.pos == s.limit) {
    head = s.pop()
    SegmentPool.recycle(s)
  }

  return toCopy
}

internal const val OVERFLOW_ZONE = Long.MIN_VALUE / 10L
internal const val OVERFLOW_DIGIT_START = Long.MIN_VALUE % 10L + 1

internal inline fun Buffer.commonReadDecimalLong(): Long {
  if (size == 0L) throw EOFException()

  // This value is always built negatively in order to accommodate Long.MIN_VALUE.
  var value = 0L
  var seen = 0
  var negative = false
  var done = false

  var overflowDigit = OVERFLOW_DIGIT_START

  do {
    val segment = head!!

    val data = segment.data
    var pos = segment.pos
    val limit = segment.limit

    while (pos < limit) {
      val b = data[pos]
      if (b >= '0'.code.toByte() && b <= '9'.code.toByte()) {
        val digit = '0'.code.toByte() - b

        // Detect when the digit would cause an overflow.
        if (value < OVERFLOW_ZONE || value == OVERFLOW_ZONE && digit < overflowDigit) {
          val buffer = Buffer().writeDecimalLong(value).writeByte(b.toInt())
          if (!negative) buffer.readByte() // Skip negative sign.
          throw NumberFormatException("Number too large: ${buffer.readUtf8()}")
        }
        value *= 10L
        value += digit.toLong()
      } else if (b == '-'.code.toByte() && seen == 0) {
        negative = true
        overflowDigit -= 1
      } else {
        // Set a flag to stop iteration. We still need to run through segment updating below.
        done = true
        break
      }
      pos++
      seen++
    }

    if (pos == limit) {
      head = segment.pop()
      SegmentPool.recycle(segment)
    } else {
      segment.pos = pos
    }
  } while (!done && head != null)

  size -= seen.toLong()

  val minimumSeen = if (negative) 2 else 1
  if (seen < minimumSeen) {
    if (size == 0L) throw EOFException()
    val expected = if (negative) "Expected a digit" else "Expected a digit or '-'"
    throw NumberFormatException("$expected but was 0x${get(0).toHexString()}")
  }

  return if (negative) value else -value
}

internal inline fun Buffer.commonReadHexadecimalUnsignedLong(): Long {
  if (size == 0L) throw EOFException()

  var value = 0L
  var seen = 0
  var done = false

  do {
    val segment = head!!

    val data = segment.data
    var pos = segment.pos
    val limit = segment.limit

    while (pos < limit) {
      val digit: Int

      val b = data[pos]
      if (b >= '0'.code.toByte() && b <= '9'.code.toByte()) {
        digit = b - '0'.code.toByte()
      } else if (b >= 'a'.code.toByte() && b <= 'f'.code.toByte()) {
        digit = b - 'a'.code.toByte() + 10
      } else if (b >= 'A'.code.toByte() && b <= 'F'.code.toByte()) {
        digit = b - 'A'.code.toByte() + 10 // We never write uppercase, but we support reading it.
      } else {
        if (seen == 0) {
          throw NumberFormatException(
            "Expected leading [0-9a-fA-F] character but was 0x${b.toHexString()}"
          )
        }
        // Set a flag to stop iteration. We still need to run through segment updating below.
        done = true
        break
      }

      // Detect when the shift will overflow.
      if (value and -0x1000000000000000L != 0L) {
        val buffer = Buffer().writeHexadecimalUnsignedLong(value).writeByte(b.toInt())
        throw NumberFormatException("Number too large: " + buffer.readUtf8())
      }

      value = value shl 4
      value = value or digit.toLong()
      pos++
      seen++
    }

    if (pos == limit) {
      head = segment.pop()
      SegmentPool.recycle(segment)
    } else {
      segment.pos = pos
    }
  } while (!done && head != null)

  size -= seen.toLong()
  return value
}

//internal inline fun Buffer.commonReadByteString(): ByteString = readByteString(size)
//
//internal inline fun Buffer.commonReadByteString(byteCount: Long): ByteString {
//  require(byteCount >= 0 && byteCount <= Int.MAX_VALUE) { "byteCount: $byteCount" }
//  if (size < byteCount) throw EOFException()
//
//  if (byteCount >= SEGMENTING_THRESHOLD) {
//    return snapshot(byteCount.toInt()).also { skip(byteCount) }
//  } else {
//    return ByteString(readByteArray(byteCount))
//  }
//}

//internal inline fun Buffer.commonSelect(options: Options): Int {
//  val index = selectPrefix(options)
//  if (index == -1) return -1
//
//  // If the prefix match actually matched a full byte string, consume it and return it.
//  val selectedSize = options.byteStrings[index].size
//  skip(selectedSize.toLong())
//  return index
//}

internal inline fun Buffer.commonReadFully(sink: Buffer, byteCount: Long) {
  if (size < byteCount) {
    sink.write(this, size) // Exhaust ourselves.
    throw EOFException()
  }
  sink.write(this, byteCount)
}

internal inline fun Buffer.commonReadAll(sink: RawSink): Long {
  val byteCount = size
  if (byteCount > 0L) {
    sink.write(this, byteCount)
  }
  return byteCount
}

internal inline fun Buffer.commonReadUtf8(byteCount: Long): String {
  require(byteCount >= 0 && byteCount <= Int.MAX_VALUE) { "byteCount: $byteCount" }
  if (size < byteCount) throw EOFException()
  if (byteCount == 0L) return ""

  val s = head!!
  if (s.pos + byteCount > s.limit) {
    // If the string spans multiple segments, delegate to readBytes().

    return readByteArray(byteCount).commonToUtf8String()
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

internal inline fun Buffer.commonReadUtf8Line(): String? {
  val newline = indexOf('\n'.code.toByte())

  return when {
    newline != -1L -> readUtf8Line(newline)
    size != 0L -> readUtf8(size)
    else -> null
  }
}

internal inline fun Buffer.commonReadUtf8LineStrict(limit: Long): String {
  require(limit >= 0L) { "limit < 0: $limit" }
  val scanLength = if (limit == Long.MAX_VALUE) Long.MAX_VALUE else limit + 1L
  val newline = indexOf('\n'.code.toByte(), 0L, scanLength)
  if (newline != -1L) return readUtf8Line(newline)
  if (scanLength < size &&
    this[scanLength - 1] == '\r'.code.toByte() &&
    this[scanLength] == '\n'.code.toByte()
  ) {
    return readUtf8Line(scanLength) // The line was 'limit' UTF-8 bytes followed by \r\n.
  }
  val data = Buffer()
  copyTo(data, 0, minOf(32, size))
  // TODO fzhinkin
  throw EOFException(
    "\\n not found: limit=${minOf(
      size,
      limit
    )} content=#{NOT IMPLEMENTED}${'…'}"
  )
}

internal inline fun Buffer.commonReadUtf8CodePoint(): Int {
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

internal inline fun Buffer.commonWriteUtf8(string: String, beginIndex: Int, endIndex: Int): Buffer {
  require(beginIndex >= 0) { "beginIndex < 0: $beginIndex" }
  require(endIndex >= beginIndex) { "endIndex < beginIndex: $endIndex < $beginIndex" }
  require(endIndex <= string.length) { "endIndex > string.length: $endIndex > ${string.length}" }

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
        /* ktlint-disable no-multi-spaces */
        tail.data[tail.limit    ] = (c shr 6          or 0xc0).toByte() // 110xxxxx
        tail.data[tail.limit + 1] = (c       and 0x3f or 0x80).toByte() // 10xxxxxx
        /* ktlint-enable no-multi-spaces */
        tail.limit += 2
        size += 2L
        i++
      }

      c < 0xd800 || c > 0xdfff -> {
        // Emit a 16-bit character with 3 bytes.
        val tail = writableSegment(3)
        /* ktlint-disable no-multi-spaces */
        tail.data[tail.limit    ] = (c shr 12          or 0xe0).toByte() // 1110xxxx
        tail.data[tail.limit + 1] = (c shr  6 and 0x3f or 0x80).toByte() // 10xxxxxx
        tail.data[tail.limit + 2] = (c        and 0x3f or 0x80).toByte() // 10xxxxxx
        /* ktlint-enable no-multi-spaces */
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
          writeByte('?'.code)
          i++
        } else {
          // UTF-16 high surrogate: 110110xxxxxxxxxx (10 bits)
          // UTF-16 low surrogate:  110111yyyyyyyyyy (10 bits)
          // Unicode code point:    00010000000000000000 + xxxxxxxxxxyyyyyyyyyy (21 bits)
          val codePoint = 0x010000 + (c and 0x03ff shl 10 or (low and 0x03ff))

          // Emit a 21-bit character with 4 bytes.
          val tail = writableSegment(4)
          /* ktlint-disable no-multi-spaces */
          tail.data[tail.limit    ] = (codePoint shr 18          or 0xf0).toByte() // 11110xxx
          tail.data[tail.limit + 1] = (codePoint shr 12 and 0x3f or 0x80).toByte() // 10xxxxxx
          tail.data[tail.limit + 2] = (codePoint shr  6 and 0x3f or 0x80).toByte() // 10xxyyyy
          tail.data[tail.limit + 3] = (codePoint        and 0x3f or 0x80).toByte() // 10yyyyyy
          /* ktlint-enable no-multi-spaces */
          tail.limit += 4
          size += 4L
          i += 2
        }
      }
    }
  }

  return this
}

internal inline fun Buffer.commonWriteUtf8CodePoint(codePoint: Int): Buffer {
  when {
    codePoint < 0x80 -> {
      // Emit a 7-bit code point with 1 byte.
      writeByte(codePoint)
    }
    codePoint < 0x800 -> {
      // Emit a 11-bit code point with 2 bytes.
      val tail = writableSegment(2)
      /* ktlint-disable no-multi-spaces */
      tail.data[tail.limit    ] = (codePoint shr 6          or 0xc0).toByte() // 110xxxxx
      tail.data[tail.limit + 1] = (codePoint       and 0x3f or 0x80).toByte() // 10xxxxxx
      /* ktlint-enable no-multi-spaces */
      tail.limit += 2
      size += 2L
    }
    codePoint in 0xd800..0xdfff -> {
      // Emit a replacement character for a partial surrogate.
      writeByte('?'.code)
    }
    codePoint < 0x10000 -> {
      // Emit a 16-bit code point with 3 bytes.
      val tail = writableSegment(3)
      /* ktlint-disable no-multi-spaces */
      tail.data[tail.limit    ] = (codePoint shr 12          or 0xe0).toByte() // 1110xxxx
      tail.data[tail.limit + 1] = (codePoint shr  6 and 0x3f or 0x80).toByte() // 10xxxxxx
      tail.data[tail.limit + 2] = (codePoint        and 0x3f or 0x80).toByte() // 10xxxxxx
      /* ktlint-enable no-multi-spaces */
      tail.limit += 3
      size += 3L
    }
    codePoint <= 0x10ffff -> {
      // Emit a 21-bit code point with 4 bytes.
      val tail = writableSegment(4)
      /* ktlint-disable no-multi-spaces */
      tail.data[tail.limit    ] = (codePoint shr 18          or 0xf0).toByte() // 11110xxx
      tail.data[tail.limit + 1] = (codePoint shr 12 and 0x3f or 0x80).toByte() // 10xxxxxx
      tail.data[tail.limit + 2] = (codePoint shr  6 and 0x3f or 0x80).toByte() // 10xxyyyy
      tail.data[tail.limit + 3] = (codePoint        and 0x3f or 0x80).toByte() // 10yyyyyy
      /* ktlint-enable no-multi-spaces */
      tail.limit += 4
      size += 4L
    }
    else -> {
      throw IllegalArgumentException("Unexpected code point: 0x${codePoint.toHexString()}")
    }
  }

  return this
}

internal inline fun Buffer.commonWriteAll(source: RawSource): Long {
  var totalBytesRead = 0L
  while (true) {
    val readCount = source.read(this, Segment.SIZE.toLong())
    if (readCount == -1L) break
    totalBytesRead += readCount
  }
  return totalBytesRead
}

internal inline fun Buffer.commonWrite(source: RawSource, byteCount: Long): Buffer {
  var remainingByteCount = byteCount
  while (remainingByteCount > 0L) {
    val read = source.read(this, remainingByteCount)
    if (read == -1L) throw EOFException()
    remainingByteCount -= read
  }
  return this
}

internal inline fun Buffer.commonWriteByte(b: Int): Buffer {
  val tail = writableSegment(1)
  tail.data[tail.limit++] = b.toByte()
  size += 1L
  return this
}

internal inline fun Buffer.commonWriteShort(s: Int): Buffer {
  val tail = writableSegment(2)
  val data = tail.data
  var limit = tail.limit
  data[limit++] = (s ushr 8 and 0xff).toByte()
  data[limit++] = (s        and 0xff).toByte() // ktlint-disable no-multi-spaces
  tail.limit = limit
  size += 2L
  return this
}

internal inline fun Buffer.commonWriteInt(i: Int): Buffer {
  val tail = writableSegment(4)
  val data = tail.data
  var limit = tail.limit
  data[limit++] = (i ushr 24 and 0xff).toByte()
  data[limit++] = (i ushr 16 and 0xff).toByte()
  data[limit++] = (i ushr  8 and 0xff).toByte() // ktlint-disable no-multi-spaces
  data[limit++] = (i         and 0xff).toByte() // ktlint-disable no-multi-spaces
  tail.limit = limit
  size += 4L
  return this
}

internal inline fun Buffer.commonWriteLong(v: Long): Buffer {
  val tail = writableSegment(8)
  val data = tail.data
  var limit = tail.limit
  data[limit++] = (v ushr 56 and 0xffL).toByte()
  data[limit++] = (v ushr 48 and 0xffL).toByte()
  data[limit++] = (v ushr 40 and 0xffL).toByte()
  data[limit++] = (v ushr 32 and 0xffL).toByte()
  data[limit++] = (v ushr 24 and 0xffL).toByte()
  data[limit++] = (v ushr 16 and 0xffL).toByte()
  data[limit++] = (v ushr  8 and 0xffL).toByte() // ktlint-disable no-multi-spaces
  data[limit++] = (v         and 0xffL).toByte() // ktlint-disable no-multi-spaces
  tail.limit = limit
  size += 8L
  return this
}

internal inline fun Buffer.commonWrite(source: Buffer, byteCount: Long) {
  // Move bytes from the head of the source buffer to the tail of this buffer
  // while balancing two conflicting goals: don't waste CPU and don't waste
  // memory.
  //
  //
  // Don't waste CPU (ie. don't copy data around).
  //
  // Copying large amounts of data is expensive. Instead, we prefer to
  // reassign entire segments from one buffer to the other.
  //
  //
  // Don't waste memory.
  //
  // As an invariant, adjacent pairs of segments in a buffer should be at
  // least 50% full, except for the head segment and the tail segment.
  //
  // The head segment cannot maintain the invariant because the application is
  // consuming bytes from this segment, decreasing its level.
  //
  // The tail segment cannot maintain the invariant because the application is
  // producing bytes, which may require new nearly-empty tail segments to be
  // appended.
  //
  //
  // Moving segments between buffers
  //
  // When writing one buffer to another, we prefer to reassign entire segments
  // over copying bytes into their most compact form. Suppose we have a buffer
  // with these segment levels [91%, 61%]. If we append a buffer with a
  // single [72%] segment, that yields [91%, 61%, 72%]. No bytes are copied.
  //
  // Or suppose we have a buffer with these segment levels: [100%, 2%], and we
  // want to append it to a buffer with these segment levels [99%, 3%]. This
  // operation will yield the following segments: [100%, 2%, 99%, 3%]. That
  // is, we do not spend time copying bytes around to achieve more efficient
  // memory use like [100%, 100%, 4%].
  //
  // When combining buffers, we will compact adjacent buffers when their
  // combined level doesn't exceed 100%. For example, when we start with
  // [100%, 40%] and append [30%, 80%], the result is [100%, 70%, 80%].
  //
  //
  // Splitting segments
  //
  // Occasionally we write only part of a source buffer to a sink buffer. For
  // example, given a sink [51%, 91%], we may want to write the first 30% of
  // a source [92%, 82%] to it. To simplify, we first transform the source to
  // an equivalent buffer [30%, 62%, 82%] and then move the head segment,
  // yielding sink [51%, 91%, 30%] and source [62%, 82%].

  require(source !== this) { "source == this" }
  checkOffsetAndCount(source.size, 0, byteCount)

  var remainingByteCount = byteCount

  while (remainingByteCount > 0L) {
    // Is a prefix of the source's head segment all that we need to move?
    if (remainingByteCount < source.head!!.limit - source.head!!.pos) {
      val tail = if (head != null) head!!.prev else null
      if (tail != null && tail.owner &&
        remainingByteCount + tail.limit - (if (tail.shared) 0 else tail.pos) <= Segment.SIZE
      ) {
        // Our existing segments are sufficient. Move bytes from source's head to our tail.
        source.head!!.writeTo(tail, remainingByteCount.toInt())
        source.size -= remainingByteCount
        size += remainingByteCount
        return
      } else {
        // We're going to need another segment. Split the source's head
        // segment in two, then move the first of those two to this buffer.
        source.head = source.head!!.split(remainingByteCount.toInt())
      }
    }

    // Remove the source's head segment and append it to our tail.
    val segmentToMove = source.head
    val movedByteCount = (segmentToMove!!.limit - segmentToMove.pos).toLong()
    source.head = segmentToMove.pop()
    if (head == null) {
      head = segmentToMove
      segmentToMove.prev = segmentToMove
      segmentToMove.next = segmentToMove.prev
    } else {
      var tail = head!!.prev
      tail = tail!!.push(segmentToMove)
      tail.compact()
    }
    source.size -= movedByteCount
    size += movedByteCount
    remainingByteCount -= movedByteCount
  }
}

internal inline fun Buffer.commonRead(sink: Buffer, byteCount: Long): Long {
  require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
  if (size == 0L) return -1L
  val bytesWritten = if (byteCount > size) size else byteCount
  sink.write(this, bytesWritten)
  return bytesWritten
}

/*
internal inline fun Buffer.commonIndexOf(b: Byte, fromIndex: Long, toIndex: Long): Long {
  var fromIndex = fromIndex
  var toIndex = toIndex
  require(fromIndex in 0..toIndex) { "size=$size fromIndex=$fromIndex toIndex=$toIndex" }

  if (toIndex > size) toIndex = size
  if (fromIndex == toIndex) return -1L

  seek(fromIndex) { s, offset ->
    var s = s ?: return -1L
    var offset = offset

    // Scan through the segments, searching for b.
    while (offset < toIndex) {
      val data = s.data
      val limit = minOf(s.limit.toLong(), s.pos + toIndex - offset).toInt()
      var pos = (s.pos + fromIndex - offset).toInt()
      while (pos < limit) {
        if (data[pos] == b) {
          return pos - s.pos + offset
        }
        pos++
      }

      // Not in this segment. Try the next one.
      offset += (s.limit - s.pos).toLong()
      fromIndex = offset
      s = s.next!!
    }

    return -1L
  }
}
 */

internal inline fun Buffer.commonEquals(other: Any?): Boolean {
  if (this === other) return true
  if (other !is Buffer) return false
  if (size != other.size) return false
  if (size == 0L) return true // Both buffers are empty.

  var sa = this.head!!
  var sb = other.head!!
  var posA = sa.pos
  var posB = sb.pos

  var pos = 0L
  var count: Long
  while (pos < size) {
    count = minOf(sa.limit - posA, sb.limit - posB).toLong()

    for (i in 0L until count) {
      if (sa.data[posA++] != sb.data[posB++]) return false
    }

    if (posA == sa.limit) {
      sa = sa.next!!
      posA = sa.pos
    }

    if (posB == sb.limit) {
      sb = sb.next!!
      posB = sb.pos
    }
    pos += count
  }

  return true
}

internal inline fun Buffer.commonHashCode(): Int {
  var s = head ?: return 0
  var result = 1
  do {
    var pos = s.pos
    val limit = s.limit
    while (pos < limit) {
      result = 31 * result + s.data[pos]
      pos++
    }
    s = s.next!!
  } while (s !== head)
  return result
}

internal inline fun Buffer.commonCopy(): Buffer {
  val result = Buffer()
  if (size == 0L) return result

  val head = head!!
  val headCopy = head.sharedCopy()

  result.head = headCopy
  headCopy.prev = result.head
  headCopy.next = headCopy.prev

  var s = head.next
  while (s !== head) {
    headCopy.prev!!.push(s!!.sharedCopy())
    s = s.next
  }

  result.size = size
  return result
}

// TODO: optimize implementation
internal inline fun Buffer.commonString(): String {
  if (size == 0L) return "[size=0]"

  val peekSrc = peek()
  val data = if (peekSrc.request(128)) {
    peekSrc.readByteArray(128)
  } else {
    peekSrc.readByteArray()
  }
  val i = codePointIndexToCharIndex(data, 64)
  if (i == -1) {
    return if (data.size <= 64) {
      "[hex=${data.hex()}]"
    } else {
      "[size=${size} hex=${data.hex(64)}…]"
    }
  }

  val text = data.decodeToString()
    .substring(0, i)
    .replace("\\", "\\\\")
    .replace("\n", "\\n")
    .replace("\r", "\\r")

  return if (i < text.length) {
    "[size=${data.size} text=$text…]"
  } else {
    "[text=$text]"
  }
}

private fun ByteArray.hex(count: Int = this.size): String {
  val builder = StringBuilder(count * 2)
  forEach {
    builder.append(HEX_DIGIT_CHARS[it.shr(4) and 0x0f])
    builder.append(HEX_DIGIT_CHARS[it and 0x0f])
  }
  return builder.toString()
}

private fun codePointIndexToCharIndex(s: ByteArray, codePointCount: Int): Int {
  var charCount = 0
  var j = 0
  s.processUtf8CodePoints(0, s.size) { c ->
    if (j++ == codePointCount) {
      return charCount
    }

    if ((c != '\n'.code && c != '\r'.code && isIsoControl(c)) ||
      c == REPLACEMENT_CODE_POINT
    ) {
      return -1
    }

    charCount += if (c < 0x10000) 1 else 2
  }
  return charCount
}

internal inline fun forEachSegment(
  segments: Array<ByteArray>,
  directory: IntArray,
  action: (data: ByteArray, offset: Int, byteCount: Int) -> Unit
) {
  val segmentCount = segments.size
  var s = 0
  var pos = 0
  while (s < segmentCount) {
    val segmentPos = directory[segmentCount + s]
    val nextSegmentOffset = directory[s]

    action(segments[s], segmentPos, nextSegmentOffset - pos)
    pos = nextSegmentOffset
    s++
  }
}