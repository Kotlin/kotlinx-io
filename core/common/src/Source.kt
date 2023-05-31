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
 * A source that keeps a buffer internally so that callers can do small reads without a performance
 * penalty. It also allows clients to read ahead, buffering as much as necessary before consuming
 * input.
 */
expect sealed interface Source : RawSource {
  /**
   * This source's internal buffer.
   */
  val buffer: Buffer

  /**
   * Returns true if there are no more bytes in this source.
   *
   * The call of this method will block until there are bytes to read or the source is definitely exhausted.
   */
  fun exhausted(): Boolean

  /**
   * Attempts to fill the buffer with at least [byteCount] bytes of data from the underlying source
   * and throw [EOFException] when the source is exhausted before fulfilling the requirement.
   *
   * If the buffer already contains required amount of bytes then there will be no requests to
   * the underlying source.
   *
   * @param byteCount amount of bytes that the buffer should contain.
   *
   * @throws EOFException when the source is exhausted before the required bytes count could be read.
   */
  fun require(byteCount: Long)

  /**
   * Attempts to fill the buffer with at least [byteCount] bytes of data from the underlying source
   * and returns a value indicating if the requirement was successfully fulfilled.
   *
   * `false` value returned by this method indicates that the underlying source was exhausted before
   * filling the buffer with [byteCount] bytes of data.
   *
   * @param byteCount amount of bytes that the buffer should contain.
   */
  fun request(byteCount: Long): Boolean

  /**
   * Removes a byte from this source and returns it.
   *
   * @throws EOFException when there are no more bytes to read.
   */
  fun readByte(): Byte

  /**
   * Removes two bytes from this source and returns a short integer composed of it according to the big-endian order.
   *
   * @throws EOFException when there are not enough data to read a short value.
   */
  fun readShort(): Short

  /**
   * Removes four bytes from this source and returns an integer composed of it according to the big-endian order.
   *
   * @throws EOFException when there are not enough data to read an int value.
   */
  fun readInt(): Int

  /**
   * Removes eight bytes from this source and returns a long integer composed of it according to the big-endian order.
   *
   * @throws EOFException when there are not enough data to read a long value.
   */
  fun readLong(): Long

  /**
   * Reads and discards [byteCount] bytes from this source.
   *
   * @param byteCount amount of bytes to be skipped.
   *
   * @throws EOFException when the source is exhausted before the requested amount of bytes can be skipped.
   */
  fun skip(byteCount: Long)

  /**
   * Removes all bytes from this source and returns them as a byte array.
   */
  fun readByteArray(): ByteArray

  /**
   * Removes [byteCount] bytes from this source and returns them as a byte array.
   *
   * @param byteCount amount of bytes that should be read from the source.
   *
   * @throws IllegalArgumentException when byteCount is negative.
   * @throws EOFException when the underlying source is exhausted before [byteCount] bytes of data could be read.
   */
  fun readByteArray(byteCount: Long): ByteArray

  /**
   * Removes exactly `sink.length` bytes from this source and copies them into [sink].
   *
   * @throws EOFException when the requested number of bytes cannot be read.
   */
  fun readFully(sink: ByteArray)

  /**
   * Removes up to [byteCount] bytes from this source, copies them into [sink] starting at [offset] and returns the
   * number of bytes read, or -1 if this source is exhausted.
   *
   * @param sink the array to which data will be written from this source.
   * @param offset the offset to start writing data into [sink] at, 0 by default.
   * @param byteCount amount of bytes that should be written into [sink],
   * size of the [sink] subarray starting at [offset] by default.
   *
   * @throws IndexOutOfBoundsException when a range specified by [offset] and [byteCount]
   * is out of range of [sink] array indices.
   */
  fun read(sink: ByteArray, offset: Int = 0, byteCount: Int = sink.size - offset): Int

  /**
   * Removes exactly [byteCount] bytes from this source and writes them to [sink].
   *
   * @param sink the sink to which data will be written from this source.
   * @param byteCount amount of bytes that should be written into [sink]
   *
   * @throws IllegalArgumentException when [byteCount] is negative.
   * @throws EOFException when the requested number of bytes cannot be read.
   */
  fun readFully(sink: Buffer, byteCount: Long)

  /**
   * Removes all bytes from this source, writes them to [sink], and returns the total number of bytes
   * written to [sink].
   *
   * Return 0 if this source is exhausted.
   *
   * @param sink the sink to which data will be written from this source.
   */
  fun readAll(sink: RawSink): Long

  /**
   * Returns a new [Source] that can read data from this source without consuming it.
   * The returned source becomes invalid once this source is next read or closed.
   *
   * Peek could be used to lookahead and read the same data multiple times.
   */
  fun peek(): Source
}
