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

/**
 * A collection of bytes in memory.
 *
 * The buffer can be viewed as an unbound queue whose size grows with the data being written
 * and shrinks with data being consumed. Internally, the buffer consists of data segments and buffer's capacity
 * grows and shrinks in units of data segments instead of individual bytes.
 *
 * The buffer was designed to reduce memory allocations when possible. Instead of copying bytes
 * from one place in memory to another, this class just changes ownership of the underlying data segments.
 *
 * To reduce allocations and speed up buffer's extension, it may use data segments pooling.
 *
 * [Buffer] implements both [Source] and [Sink] and could be used as a source or a sink,
 * but unlike regular sinks and sources its [close], [cancel], [flush], [emit], [emitCompleteSegments]
 * does not affect buffer's state and [exhausted] only indicates that a buffer is empty.
 */
expect class Buffer() : Source, Sink {
  internal var head: Segment?

  /**
   * The number of bytes accessible for read from this buffer.
   */
  var size: Long
    internal set

  /**
   * Returns the buffer itself.
   */
  override val buffer: Buffer

  /**
   * This method does not affect the buffer's content as there is no upstream to write data to.
   */
  override fun emitCompleteSegments(): Buffer

  /**
   * This method does not affect the buffer's content as there is no upstream to write data to.
   */
  override fun emit(): Buffer

  /**
   * This method does not affect the buffer's content as there is no upstream to write data to.
   */
  override fun flush()

  /**
   * Copy [byteCount] bytes from this buffer, starting at [offset], to [out] buffer.
   *
   * @param out the destination buffer to copy data into.
   * @param offset the offset to the first byte of data in this buffer to start copying from.
   * @param byteCount the number of bytes to copy.
   *
   * @throws IndexOutOfBoundsException when [offset] and [byteCount] correspond to a range out of this buffer bounds.
   */
  fun copyTo(
    out: Buffer,
    offset: Long = 0L,
    byteCount: Long = size - offset
  ): Buffer

  /**
   * Returns the number of bytes in segments that are fully filled and are no longer writable.
   *
   * TODO: is it?
   * This is the number of bytes that can be flushed immediately to an underlying sink without harming throughput.
   */
  fun completeSegmentByteCount(): Long

  /**
   * Returns the byte at [pos].
   *
   * Use of this method may expose significant performance penalties and it's not recommended to use it
   * for sequential access to a range of bytes within the buffer.
   *
   * @throws IndexOutOfBoundsException when [pos] is out of this buffer's bounds.
   */
  operator fun get(pos: Long): Byte

  /**
   * Discards all bytes in this buffer.
   *
   * Call to this method is equivalent to [skip] with `byteCount = size`.
   */
  fun clear()

  // TODO: figure out what this method may actually throw
  /**
   * Discards [byteCount]` bytes from the head of this buffer.
   *
   * @throws IllegalArgumentException when [byteCount] is negative.
   * @throws ??? when [byteCount] exceeds buffer's [size].
   */
  override fun skip(byteCount: Long)

  /**
   * Returns a tail segment that we can write at least `minimumCapacity`
   * bytes to, creating it if necessary.
   */
  internal fun writableSegment(minimumCapacity: Int): Segment

  override fun write(source: ByteArray, offset: Int, byteCount: Int): Buffer

  override fun write(source: RawSource, byteCount: Long): Buffer

  override fun writeByte(byte: Int): Buffer

  override fun writeShort(short: Int): Buffer

  override fun writeInt(int: Int): Buffer

  override fun writeLong(long: Long): Buffer

  /**
   * Returns a deep copy of this buffer.
   */
  fun copy(): Buffer

  /**
   * This method does not affect the buffer.
   */
  override fun close()

  /**
   * This method does not affect the buffer.
   */
  override fun cancel()
}
