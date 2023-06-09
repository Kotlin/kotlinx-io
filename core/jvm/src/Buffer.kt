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

public actual class Buffer : Source, Sink, Cloneable {
  @JvmField internal actual var head: Segment? = null

  public actual var size: Long = 0L
    internal set

  actual override val buffer: Buffer get() = this

  actual override fun emitCompleteSegments(): Buffer = this // Nowhere to emit to!

  actual override fun emit(): Buffer = this // Nowhere to emit to!

  override fun exhausted(): Boolean = size == 0L

  override fun require(byteCount: Long) {
    if (size < byteCount) throw EOFException()
  }

  override fun request(byteCount: Long): Boolean = size >= byteCount

  override fun peek(): Source {
    return PeekSource(this).buffer()
  }

  public actual fun copyTo(
    out: Buffer,
    offset: Long,
    byteCount: Long
  ): Buffer = commonCopyTo(out, offset, byteCount)

  public actual fun completeSegmentByteCount(): Long = commonCompleteSegmentByteCount()

  override fun readByte(): Byte = commonReadByte()

  public actual operator fun get(pos: Long): Byte = commonGet(pos)

  override fun readShort(): Short = commonReadShort()

  override fun readInt(): Int = commonReadInt()

  override fun readLong(): Long = commonReadLong()

  override fun readFully(sink: Buffer, byteCount: Long): Unit = commonReadFully(sink, byteCount)

  override fun readAll(sink: RawSink): Long = commonReadAll(sink)

  override fun read(sink: ByteArray, offset: Int, byteCount: Int): Int =
    commonRead(sink, offset, byteCount)

  public actual fun clear(): Unit = commonClear()

  public actual override fun skip(byteCount: Long): Unit = commonSkip(byteCount)

  actual override fun write(
    source: ByteArray,
    offset: Int,
    byteCount: Int
  ): Buffer = commonWrite(source, offset, byteCount)

  override fun writeAll(source: RawSource): Long = commonWriteAll(source)

  actual override fun write(source: RawSource, byteCount: Long): Buffer =
    commonWrite(source, byteCount)

  actual override fun writeByte(byte: Byte): Buffer = commonWriteByte(byte)

  actual override fun writeShort(short: Short): Buffer = commonWriteShort(short)

  actual override fun writeInt(int: Int): Buffer = commonWriteInt(int)

  actual override fun writeLong(long: Long): Buffer = commonWriteLong(long)

  internal actual fun writableSegment(minimumCapacity: Int): Segment =
    commonWritableSegment(minimumCapacity)

  override fun write(source: Buffer, byteCount: Long): Unit = commonWrite(source, byteCount)

  override fun read(sink: Buffer, byteCount: Long): Long = commonRead(sink, byteCount)

  public actual override fun flush(): Unit = Unit

  actual override fun close(): Unit = Unit

  override fun equals(other: Any?): Boolean = commonEquals(other)

  override fun hashCode(): Int = commonHashCode()

  /**
   * Returns a human-readable string that describes the contents of this buffer. Typically, this
   * is a string like `[text=Hello]` or `[hex=0000ffff]`.
   */
  override fun toString(): String = commonString()

  public actual fun copy(): Buffer = commonCopy()

  /**
   * Returns a deep copy of this buffer.
   *
   * This method is equivalent to [copy].
   */
  public override fun clone(): Buffer = copy()
}

/**
 * Read and exhaust bytes from [input] into this buffer. Stops reading data on [input] exhaustion.
 *
 * @param input the stream to read data from.
 *
 * @throws ???
 */
public fun Buffer.readFrom(input: InputStream): Buffer {
  readFrom(input, Long.MAX_VALUE, true)
  return this
}

/**
 * Read [byteCount] bytes from [input] into this buffer. Throws an exception when [input] is
 * exhausted before reading [byteCount] bytes.
 *
 * @param input the stream to read data from.
 * @param byteCount the number of bytes read from [input].
 *
 * @throws IOException when [input] exhausted before reading [byteCount] bytes from it.
 */
public fun Buffer.readFrom(input: InputStream, byteCount: Long): Buffer {
  require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
  readFrom(input, byteCount, false)
  return this
}

private fun Buffer.readFrom(input: InputStream, byteCount: Long, forever: Boolean) {
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

/**
 * Writes [byteCount] bytes from this buffer to [out].
 *
 * @param out the [OutputStream] to write to.
 * @param byteCount the number of bytes to be written, [Buffer.size] by default.
 *
 * @throws ???
 */
public fun Buffer.writeTo(out: OutputStream, byteCount: Long = size): Buffer {
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

/**
 * Copy [byteCount] bytes from this buffer, starting at [offset], to [out].
 *
 * @param out the destination to copy data into.
 * @param offset the offset to start copying data from, `0` by default.
 * @param byteCount the number of bytes to copy, all data starting from the [offset] by default.
 *
 * @throws IndexOutOfBoundsException when [byteCount] and [offset] represents a range out of the buffer bounds.
 */
public fun Buffer.copyTo(
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

/**
 * Writes up to [ByteBuffer.remaining] bytes from this buffer to the sink.
 * Return the number of bytes written.
 *
 * @param sink the sink to write data to.
 */
public fun Buffer.writeTo(sink: ByteBuffer): Int {
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

/**
 * Reads all data from [source] into this buffer.
 */
public fun Buffer.readFrom(source: ByteBuffer): Buffer {
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
  return this
}

public fun Buffer.channel(): ByteChannel = object : ByteChannel {
  override fun read(sink: ByteBuffer): Int = writeTo(sink)

  override fun write(source: ByteBuffer): Int {
    val sizeBefore = size
    readFrom(source)
    return (size - sizeBefore).toInt()
  }

  override fun close() {}

  override fun isOpen(): Boolean = true
}