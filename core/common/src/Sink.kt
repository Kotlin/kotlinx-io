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
 * A sink that keeps a buffer internally so that callers can do small writes without a performance
 * penalty.
 */
expect sealed interface Sink : RawSink {
  /**
   * This sink's internal buffer.
   */
  val buffer: Buffer

  /**
   * Writes bytes from [source] array or its subrange to this sink.
   *
   * @param source the array from which bytes will be written into this sink.
   * @param offset the beginning of data within the [source], 0 by default.
   * @param byteCount amount of bytes to write, size of the [source] subarray starting at [offset] by default.
   *
   * @throws IndexOutOfBoundsException when a range specified by [offset] and [byteCount]
   * is out of range of [source] array indices.
   *
   * @sample kotlinx.io.AbstractSinkTest.writeByteArray
   */
  fun write(source: ByteArray, offset: Int = 0, byteCount: Int = source.size - offset): Sink

  /**
   * Removes all bytes from [source] and write them to this sink.
   * Returns the number of bytes read which will be 0 if [source] is exhausted.
   *
   * @param source the source to consume data from.
   *
   * @sample kotlinx.io.AbstractSinkTest.writeAll
   * @sample kotlinx.io.AbstractSinkTest.writeAllExhausted
   */
  fun writeAll(source: RawSource): Long

  /**
   * Removes [byteCount] bytes from [source] and write them to this sink.
   *
   * If [source] will be exhausted before reading [byteCount] from it then an exception throws on
   * attempt to read remaining bytes will be propagated to a caller of this method.
   *
   * @param source the source to consume data from.
   * @param byteCount amount of bytes to read from [source] and to write into this sink.
   *
   * @throws IllegalArgumentException when [byteCount] is negative.
   *
   * @sample kotlinx.io.AbstractSinkTest.writeSource
   */
  fun write(source: RawSource, byteCount: Long): Sink

  /**
   * Writes a byte to this sink.
   *
   * @param byte the byte to be written.
   *
   * @sample kotlinx.io.AbstractSinkTest.writeByte
   */
  fun writeByte(byte: Int): Sink

  /**
   * Writes two bytes containing [short], in the big-endian order, to this sink.
   *
   * @param short the short integer to be written.
   *
   * @sample kotlinx.io.AbstractSinkTest.writeShort
   */
  fun writeShort(short: Int): Sink

  /**
   * Writes four bytes containing [int], in the big-endian order, to this sink.
   *
   * @param int the integer to be written.
   *
   * @sample kotlinx.io.AbstractSinkTest.writeInt
   */
  fun writeInt(int: Int): Sink

  /**
   * Writes eight bytes containing [long], in the big-endian order, to this sink.
   *
   * @param long the long integer to be written.
   *
   * @sample kotlinx.io.AbstractSinkTest.writeLong
   */
  fun writeLong(long: Long): Sink

  /**
   * Writes all buffered data to the underlying sink, if one exists.
   * Then the underlying sink is explicitly flushed.
   */
  override fun flush()

  /**
   * Writes all buffered data to the underlying sink, if one exists.
   * The underlying sink will not be explicitly flushed.
   *
   * This method behaves like [flush], but has weaker guarantees.
   * Call this method before a buffered sink goes out of scope so that its data can reach its destination.
   */
  fun emit(): Sink

  /**
   * Writes complete segments to the underlying sink, if one exists.
   * The underlying sink will not be explicitly flushed.
   *
   * Use this to limit the memory held in the buffer to a single segment.
   * Typically, application code will not need to call this: it is only necessary when
   * application code writes directly to this [buffer].
   */
  fun emitCompleteSegments(): Sink
}
