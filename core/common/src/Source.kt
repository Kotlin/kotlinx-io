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
 * A source that facilitates typed data reads and keeps a buffer internally so that callers can read chunks of data
 * without requesting it from a downstream on every call.
 *
 * [Source] is the main `kotlinx-io` interface to read data in client's code,
 * any [RawSource] could be converted into [Source] using [RawSource.buffer].
 *
 * Depending on a kind of downstream and the number of bytes read, buffering may improve the performance by hiding
 * the latency of small reads.
 *
 * The buffer is refilled on reads as necessary, but it is also possible to ensure it contains enough data
 * using [require] or [request].
 * [Sink] also allows to skip unneeded prefix of data using [skip] and
 * provides look ahead into incoming data, buffering as much as necessary, using [peek].
 */
public expect sealed interface Source : RawSource {
  /**
   * This source's internal buffer.
   */
  public val buffer: Buffer

  /**
   * Returns true if there are no more bytes in this source.
   *
   * The call of this method will block until there are bytes to read or the source is definitely exhausted.
   */
  public fun exhausted(): Boolean

  /**
   * Attempts to fill the buffer with at least [byteCount] bytes of data from the underlying source
   * and throw [EOFException] when the source is exhausted before fulfilling the requirement.
   *
   * If the buffer already contains required number of bytes then there will be no requests to
   * the underlying source.
   *
   * @param byteCount the number of bytes that the buffer should contain.
   *
   * @throws EOFException when the source is exhausted before the required bytes count could be read.
   */
  public fun require(byteCount: Long)

  /**
   * Attempts to fill the buffer with at least [byteCount] bytes of data from the underlying source
   * and returns a value indicating if the requirement was successfully fulfilled.
   *
   * `false` value returned by this method indicates that the underlying source was exhausted before
   * filling the buffer with [byteCount] bytes of data.
   *
   * @param byteCount the number of bytes that the buffer should contain.
   */
  public fun request(byteCount: Long): Boolean

  /**
   * Removes a byte from this source and returns it.
   *
   * @throws EOFException when there are no more bytes to read.
   */
  public fun readByte(): Byte

  /**
   * Removes two bytes from this source and returns a short integer composed of it according to the big-endian order.
   *
   * @throws EOFException when there are not enough data to read a short value.
   */
  public fun readShort(): Short

  /**
   * Removes four bytes from this source and returns an integer composed of it according to the big-endian order.
   *
   * @throws EOFException when there are not enough data to read an int value.
   */
  public fun readInt(): Int

  /**
   * Removes eight bytes from this source and returns a long integer composed of it according to the big-endian order.
   *
   * @throws EOFException when there are not enough data to read a long value.
   */
  public fun readLong(): Long

  /**
   * Reads and discards [byteCount] bytes from this source.
   *
   * @param byteCount the number of bytes to be skipped.
   *
   * @throws EOFException when the source is exhausted before the requested number of bytes can be skipped.
   */
  public fun skip(byteCount: Long)

  /**
   * Removes exactly `sink.length` bytes from this source and copies them into [sink].
   *
   * @throws EOFException when the requested number of bytes cannot be read.
   */
  public fun readFully(sink: ByteArray)

  /**
   * Removes up to [byteCount] bytes from this source, copies them into [sink] starting at [offset] and returns the
   * number of bytes read, or -1 if this source is exhausted.
   *
   * @param sink the array to which data will be written from this source.
   * @param offset the offset to start writing data into [sink] at, 0 by default.
   * @param byteCount the number of bytes that should be written into [sink],
   * size of the [sink] subarray starting at [offset] by default.
   *
   * @throws IndexOutOfBoundsException when a range specified by [offset] and [byteCount]
   * is out of range of [sink] array indices.
   */
  public fun read(sink: ByteArray, offset: Int = 0, byteCount: Int = sink.size - offset): Int

  /**
   * Removes exactly [byteCount] bytes from this source and writes them to [sink].
   *
   * @param sink the sink to which data will be written from this source.
   * @param byteCount the number of bytes that should be written into [sink]
   *
   * @throws IllegalArgumentException when [byteCount] is negative.
   * @throws EOFException when the requested number of bytes cannot be read.
   */
  public fun readFully(sink: Buffer, byteCount: Long)

  /**
   * Removes all bytes from this source, writes them to [sink], and returns the total number of bytes
   * written to [sink].
   *
   * Return 0 if this source is exhausted.
   *
   * @param sink the sink to which data will be written from this source.
   */
  public fun readAll(sink: RawSink): Long

  /**
   * Returns a new [Source] that can read data from this source without consuming it.
   * The returned source becomes invalid once this source is next read or closed.
   *
   * Peek could be used to lookahead and read the same data multiple times.
   */
  public fun peek(): Source
}
