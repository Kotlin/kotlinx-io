/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2014 Square, Inc.
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

import kotlinx.io.internal.*
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel

actual class Buffer : Source, Sink, Cloneable, ByteChannel {
  @JvmField internal actual var head: Segment? = null

  @get:JvmName("size")
  actual var size: Long = 0L
    internal set

  actual override val buffer get() = this

  override fun outputStream(): OutputStream {
    return object : OutputStream() {
      override fun write(b: Int) {
        writeByte(b)
      }

      override fun write(data: ByteArray, offset: Int, byteCount: Int) {
        this@Buffer.write(data, offset, byteCount)
      }

      override fun flush() {}

      override fun close() {}

      override fun toString(): String = "${this@Buffer}.outputStream()"
    }
  }

  actual override fun emitCompleteSegments() = this // Nowhere to emit to!

  actual override fun emit() = this // Nowhere to emit to!

  override fun exhausted() = size == 0L

  @Throws(EOFException::class)
  override fun require(byteCount: Long) {
    if (size < byteCount) throw EOFException()
  }

  override fun request(byteCount: Long) = size >= byteCount

  override fun peek(): Source {
    return PeekSource(this).buffer()
  }

  override fun inputStream(): InputStream {
    return object : InputStream() {
      override fun read(): Int {
        return if (size > 0L) {
          readByte() and 0xff
        } else {
          -1
        }
      }

      override fun read(sink: ByteArray, offset: Int, byteCount: Int): Int {
        return this@Buffer.read(sink, offset, byteCount)
      }

      override fun available() = minOf(size, Integer.MAX_VALUE).toInt()

      override fun close() {}

      override fun toString() = "${this@Buffer}.inputStream()"
    }
  }

  /** Copy `byteCount` bytes from this, starting at `offset`, to `out`. */
  @Throws(IOException::class)
  @JvmOverloads
  fun copyTo(
    out: OutputStream,
    offset: Long = 0L,
    byteCount: Long = size - offset
  ): Buffer {
    checkOffsetAndCount(size, offset, byteCount)
    if (byteCount == 0L) return this

    var currentOffset = offset
    var remainingByteCount = byteCount

    // Skip segments that we aren't copying from.
    var s = head
    while (currentOffset >= s!!.limit - s.pos) {
      currentOffset -= (s.limit - s.pos).toLong()
      s = s.next
    }

    // Copy from one segment at a time.
    while (remainingByteCount > 0L) {
      val pos = (s!!.pos + currentOffset).toInt()
      val toCopy = minOf(s.limit - pos, remainingByteCount).toInt()
      out.write(s.data, pos, toCopy)
      remainingByteCount -= toCopy.toLong()
      currentOffset = 0L
      s = s.next
    }

    return this
  }

  actual fun copyTo(
    out: Buffer,
    offset: Long,
    byteCount: Long
  ): Buffer = commonCopyTo(out, offset, byteCount)

  /** Write `byteCount` bytes from this to `out`. */
  @Throws(IOException::class)
  @JvmOverloads
  fun writeTo(out: OutputStream, byteCount: Long = size): Buffer {
    checkOffsetAndCount(size, 0, byteCount)
    var remainingByteCount = byteCount

    var s = head
    while (remainingByteCount > 0L) {
      val toCopy = minOf(remainingByteCount, s!!.limit - s.pos).toInt()
      out.write(s.data, s.pos, toCopy)

      s.pos += toCopy
      size -= toCopy.toLong()
      remainingByteCount -= toCopy.toLong()

      if (s.pos == s.limit) {
        val toRecycle = s
        s = toRecycle.pop()
        head = s
        SegmentPool.recycle(toRecycle)
      }
    }

    return this
  }

  /** Read and exhaust bytes from `input` into this. */
  @Throws(IOException::class)
  fun readFrom(input: InputStream): Buffer {
    readFrom(input, Long.MAX_VALUE, true)
    return this
  }

  /** Read `byteCount` bytes from `input` into this. */
  @Throws(IOException::class)
  fun readFrom(input: InputStream, byteCount: Long): Buffer {
    require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
    readFrom(input, byteCount, false)
    return this
  }

  @Throws(IOException::class)
  private fun readFrom(input: InputStream, byteCount: Long, forever: Boolean) {
    var remainingByteCount = byteCount
    while (remainingByteCount > 0L || forever) {
      val tail = writableSegment(1)
      val maxToCopy = minOf(remainingByteCount, Segment.SIZE - tail.limit).toInt()
      val bytesRead = input.read(tail.data, tail.limit, maxToCopy)
      if (bytesRead == -1) {
        if (tail.pos == tail.limit) {
          // We allocated a tail segment, but didn't end up needing it. Recycle!
          head = tail.pop()
          SegmentPool.recycle(tail)
        }
        if (forever) return
        throw EOFException()
      }
      tail.limit += bytesRead
      size += bytesRead.toLong()
      remainingByteCount -= bytesRead.toLong()
    }
  }

  actual fun completeSegmentByteCount(): Long = commonCompleteSegmentByteCount()

  @Throws(EOFException::class)
  override fun readByte(): Byte = commonReadByte()

  @JvmName("getByte")
  actual operator fun get(pos: Long): Byte = commonGet(pos)

  @Throws(EOFException::class)
  override fun readShort(): Short = commonReadShort()

  @Throws(EOFException::class)
  override fun readInt(): Int = commonReadInt()

  @Throws(EOFException::class)
  override fun readLong(): Long = commonReadLong()

  @Throws(EOFException::class)
  override fun readFully(sink: Buffer, byteCount: Long): Unit = commonReadFully(sink, byteCount)

  @Throws(IOException::class)
  override fun readAll(sink: RawSink): Long = commonReadAll(sink)

  override fun readByteArray() = commonReadByteArray()

  @Throws(EOFException::class)
  override fun readByteArray(byteCount: Long): ByteArray = commonReadByteArray(byteCount)

  @Throws(EOFException::class)
  override fun readFully(sink: ByteArray) = commonReadFully(sink)

  override fun read(sink: ByteArray, offset: Int, byteCount: Int): Int =
    commonRead(sink, offset, byteCount)

  @Throws(IOException::class)
  override fun read(sink: ByteBuffer): Int {
    val s = head ?: return -1

    val toCopy = minOf(sink.remaining(), s.limit - s.pos)
    sink.put(s.data, s.pos, toCopy)

    s.pos += toCopy
    size -= toCopy.toLong()

    if (s.pos == s.limit) {
      head = s.pop()
      SegmentPool.recycle(s)
    }

    return toCopy
  }

  actual fun clear() = commonClear()

  @Throws(EOFException::class)
  actual override fun skip(byteCount: Long) = commonSkip(byteCount)

  actual override fun write(
    source: ByteArray,
    offset: Int,
    byteCount: Int
  ): Buffer = commonWrite(source, offset, byteCount)

  @Throws(IOException::class)
  override fun write(source: ByteBuffer): Int {
    val byteCount = source.remaining()
    var remaining = byteCount
    while (remaining > 0) {
      val tail = writableSegment(1)

      val toCopy = minOf(remaining, Segment.SIZE - tail.limit)
      source.get(tail.data, tail.limit, toCopy)

      remaining -= toCopy
      tail.limit += toCopy
    }

    size += byteCount.toLong()
    return byteCount
  }

  @Throws(IOException::class)
  override fun writeAll(source: RawSource): Long = commonWriteAll(source)

  @Throws(IOException::class)
  actual override fun write(source: RawSource, byteCount: Long): Buffer =
    commonWrite(source, byteCount)

  actual override fun writeByte(byte: Int): Buffer = commonWriteByte(byte)

  actual override fun writeShort(short: Int): Buffer = commonWriteShort(short)

  actual override fun writeInt(int: Int): Buffer = commonWriteInt(int)

  actual override fun writeLong(long: Long): Buffer = commonWriteLong(long)

  internal actual fun writableSegment(minimumCapacity: Int): Segment =
    commonWritableSegment(minimumCapacity)

  override fun write(source: Buffer, byteCount: Long): Unit = commonWrite(source, byteCount)

  override fun read(sink: Buffer, byteCount: Long): Long = commonRead(sink, byteCount)

  actual override fun flush() = Unit

  override fun isOpen() = true

  actual  override fun close() = Unit

  actual override fun cancel() = Unit

  override fun equals(other: Any?): Boolean = commonEquals(other)

  override fun hashCode(): Int = commonHashCode()

  /**
   * Returns a human-readable string that describes the contents of this buffer. Typically, this
   * is a string like `[text=Hello]` or `[hex=0000ffff]`.
   */
  override fun toString() = commonString()

  actual fun copy(): Buffer = commonCopy()

  /**
   * Returns a deep copy of this buffer.
   *
   * This method is equivalent to [copy].
   */
  public override fun clone(): Buffer = copy()
}
