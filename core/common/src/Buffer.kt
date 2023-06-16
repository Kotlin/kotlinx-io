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

import kotlinx.io.internal.REPLACEMENT_CODE_POINT
import kotlinx.io.internal.isIsoControl
import kotlinx.io.internal.processUtf8CodePoints
import kotlin.jvm.JvmField

/**
 * A collection of bytes in memory.
 *
 * The buffer can be viewed as an unbound queue whose size grows with the data being written
 * and shrinks with data being consumed. Internally, the buffer consists of data segments,
 * and the buffer's capacity grows and shrinks in units of data segments instead of individual bytes.
 *
 * The buffer was designed to reduce memory allocations when possible. Instead of copying bytes
 * from one place in memory to another, this class just changes ownership of the underlying data segments.
 *
 * To reduce allocations and speed up the buffer's extension, it may use data segments pooling.
 *
 * [Buffer] implements both [Source] and [Sink] and could be used as a source or a sink,
 * but unlike regular sinks and sources its [close], [flush], [emit], [emitCompleteSegments]
 * does not affect buffer's state and [exhausted] only indicates that a buffer is empty.
 */
public class Buffer : Source, Sink {
  @JvmField internal var head: Segment? = null

  /**
   * The number of bytes accessible for read from this buffer.
   */
  public var size: Long = 0L
    internal set

  /**
   * Returns the buffer itself.
   */
  @DelicateIoApi
  override val buffer: Buffer = this
  override fun exhausted(): Boolean = size == 0L

  override fun require(byteCount: Long) {
    if (size < byteCount) throw EOFException()
  }

  override fun request(byteCount: Long): Boolean = size >= byteCount

  override fun readByte(): Byte {
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

  override fun readShort(): Short {
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

  override fun readInt(): Int {
    if (size < 4L) throw EOFException()

    val segment = head!!
    var pos = segment.pos
    val limit = segment.limit

    // If the int is split across multiple segments, delegate to readByte().
    if (limit - pos < 4L) {
      return (
              readByte() and 0xff shl 24
                      or (readByte() and 0xff shl 16)
                      or (readByte() and 0xff shl 8)
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

  override fun readLong(): Long {
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
                    or (data[pos++] and 0xffL shl 8)
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

  /**
   * This method does not affect the buffer's content as there is no upstream to write data to.
   */
  @DelicateIoApi
  override fun emitCompleteSegments(): Unit = Unit

  /**
   * This method does not affect the buffer's content as there is no upstream to write data to.
   */
  override fun emit(): Unit = Unit

  /**
   * This method does not affect the buffer's content as there is no upstream to write data to.
   */
  override fun flush(): Unit = Unit

  /**
   * Copy [byteCount] bytes from this buffer, starting at [offset], to [out] buffer.
   *
   * @param out the destination buffer to copy data into.
   * @param offset the offset to the first byte of data in this buffer to start copying from.
   * @param byteCount the number of bytes to copy.
   *
   * @throws IllegalArgumentException when [offset] and [byteCount] correspond to a range out of this buffer bounds.
   */
  public fun copyTo(
    out: Buffer,
    offset: Long = 0L,
    byteCount: Long = size - offset
  ) {
    checkOffsetAndCount(size, offset, byteCount)
    if (byteCount == 0L) return

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
  }

  /**
   * Returns the number of bytes in segments that are fully filled and are no longer writable.
   *
   * This is the number of bytes that can be flushed immediately to an underlying sink without harming throughput.
   */
  internal fun completeSegmentByteCount(): Long {
    var result = size
    if (result == 0L) return 0L

    // Omit the tail if it's still writable.
    val tail = head!!.prev!!
    if (tail.limit < Segment.SIZE && tail.owner) {
      result -= (tail.limit - tail.pos).toLong()
    }

    return result
  }

  /**
   * Returns the byte at [position].
   *
   * Use of this method may expose significant performance penalties and it's not recommended to use it
   * for sequential access to a range of bytes within the buffer.
   *
   * @throws IllegalArgumentException when [position] is out of this buffer's bounds.
   */
  public operator fun get(position: Long): Byte {
    checkOffsetAndCount(size, position, 1L)
    seek(position) { s, offset ->
      return s!!.data[(s.pos + position - offset).toInt()]
    }
  }

  /**
   * Discards all bytes in this buffer.
   *
   * Call to this method is equivalent to [skip] with `byteCount = size`.
   */
  public fun clear(): Unit = skip(size)

  /**
   * Discards [byteCount]` bytes from the head of this buffer.
   *
   * @throws IllegalArgumentException when [byteCount] is negative.
   */
  override fun skip(byteCount: Long) {
    var remainingByteCount = byteCount
    while (remainingByteCount > 0) {
      val head = head ?: throw EOFException()

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

  override fun read(sink: ByteArray, offset: Int, byteCount: Int): Int {
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

  override fun read(sink: Buffer, byteCount: Long): Long {
    require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
    if (size == 0L) return -1L
    val bytesWritten = if (byteCount > size) size else byteCount
    sink.write(this, bytesWritten)
    return bytesWritten
  }

  override fun readFully(sink: RawSink, byteCount: Long) {
    if (size < byteCount) {
      sink.write(this, size) // Exhaust ourselves.
      throw EOFException()
    }
    sink.write(this, byteCount)
  }

  override fun readAll(sink: RawSink): Long {
    val byteCount = size
    if (byteCount > 0L) {
      sink.write(this, byteCount)
    }
    return byteCount
  }

  override fun peek(): Source = PeekSource(this).buffer()

  /**
   * Returns a tail segment that we can write at least `minimumCapacity`
   * bytes to, creating it if necessary.
   */
  internal fun writableSegment(minimumCapacity: Int): Segment {
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

  override fun write(source: ByteArray, offset: Int, byteCount: Int): Unit {
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
  }

  override fun write(source: RawSource, byteCount: Long) {
    var remainingByteCount = byteCount
    while (remainingByteCount > 0L) {
      val read = source.read(this, remainingByteCount)
      if (read == -1L) throw EOFException()
      remainingByteCount -= read
    }
  }

  override fun write(source: Buffer, byteCount: Long) {
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
    require(byteCount >= 0) { "byteCount ($byteCount) should not be negative." }
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

  override fun writeAll(source: RawSource): Long {
    var totalBytesRead = 0L
    while (true) {
      val readCount = source.read(this, Segment.SIZE.toLong())
      if (readCount == -1L) break
      totalBytesRead += readCount
    }
    return totalBytesRead
  }

  override fun writeByte(byte: Byte) {
    val tail = writableSegment(1)
    tail.data[tail.limit++] = byte
    size += 1L
  }

  override fun writeShort(short: Short) {
    val tail = writableSegment(2)
    val data = tail.data
    var limit = tail.limit
    data[limit++] = (short.toInt() ushr 8 and 0xff).toByte()
    data[limit++] = (short.toInt() and 0xff).toByte()
    tail.limit = limit
    size += 2L
  }

  override fun writeInt(int: Int) {
    val tail = writableSegment(4)
    val data = tail.data
    var limit = tail.limit
    data[limit++] = (int ushr 24 and 0xff).toByte()
    data[limit++] = (int ushr 16 and 0xff).toByte()
    data[limit++] = (int ushr 8 and 0xff).toByte()
    data[limit++] = (int and 0xff).toByte()
    tail.limit = limit
    size += 4L
  }

  override fun writeLong(long: Long) {
    val tail = writableSegment(8)
    val data = tail.data
    var limit = tail.limit
    data[limit++] = (long ushr 56 and 0xffL).toByte()
    data[limit++] = (long ushr 48 and 0xffL).toByte()
    data[limit++] = (long ushr 40 and 0xffL).toByte()
    data[limit++] = (long ushr 32 and 0xffL).toByte()
    data[limit++] = (long ushr 24 and 0xffL).toByte()
    data[limit++] = (long ushr 16 and 0xffL).toByte()
    data[limit++] = (long ushr 8 and 0xffL).toByte()
    data[limit++] = (long and 0xffL).toByte()
    tail.limit = limit
    size += 8L
  }

  /**
   * Returns a deep copy of this buffer.
   */
  public fun copy(): Buffer {
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

  /**
   * This method does not affect the buffer.
   */
  override fun close(): Unit = Unit

  /**
   * Returns a human-readable string that describes the contents of this buffer. For buffers containing
   * few bytes, this is a string like `[text=Hello]` or `[hex=0000ffff]`. However, if the buffer is too large,
   * a string will contain its size and only a prefix of data, like `[size=1024 hex=01234…]`. Thus, the string could not
   * be used to compare buffers or verify buffer's content.
   */
  override fun toString(): String {
    // TODO: optimize implementation
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
    val escapedText = text
      .substring(0, i)
      .replace("\\", "\\\\")
      .replace("\n", "\\n")
      .replace("\r", "\\r")

    return if (i < text.length) {
      "[size=${data.size} text=$escapedText…]"
    } else {
      "[text=$escapedText]"
    }
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

/**
 * Invoke `lambda` with the segment and offset at `fromIndex`. Searches from the front or the back
 * depending on what's closer to `fromIndex`.
 */
private inline fun <T> Buffer.seek(
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
