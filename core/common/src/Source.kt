/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
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
 * any [RawSource] could be converted into [Source] using [RawSource.buffered].
 *
 * Depending on the kind of downstream and the number of bytes read, buffering may improve the performance by hiding
 * the latency of small reads.
 *
 * The buffer is refilled on reads as necessary, but it is also possible to ensure it contains enough data
 * using [require] or [request].
 * [Sink] also allows skipping unneeded prefix of data using [skip] and
 * provides look ahead into incoming data, buffering as much as necessary, using [peek].
 *
 * Source's read* methods have different guarantees of how much data will be consumed from the source
 * and what to expect in case of error.
 *
 * ### Read methods' behavior and naming conventions
 *
 * Unless stated otherwise, all read methods consume the exact number of bytes
 * requested (or the number of bytes required to represent a value of a requested type).
 * If a source contains fewer bytes than requested, these methods will throw an exception.
 *
 * Methods reading up to requested number of bytes are named as `readAtMost`
 * in contrast to methods reading exact number of bytes, which don't have `AtMost` suffix in their names.
 * If a source contains fewer bytes than requested, these methods will not treat it as en error and will return
 * gracefully.
 *
 * Methods returning a value as a result are named `read<Type>`, like [readInt] or [readByte].
 * These methods don't consume source's content in case of an error.
 *
 * Methods reading data into a consumer supplied as one of its arguments are named `read*To`,
 * like [readTo] or [readAtMostTo]. These methods consume a source even when an error occurs.
 *
 * Methods moving all data from a source to some other sink are named `transferTo`, like [transferTo].
 *
 * It is recommended to follow the same naming convention for Source extensions.
 *
 * ### Thread-safety guarantees
 *
 * Until stated otherwise, [Source] implementations are not thread safe.
 * If a [Source] needs to be accessed from multiple threads, an additional synchronization is required.
 */
public sealed interface Source : RawSource {
  /**
   * This source's internal buffer. It contains data fetched from the downstream, but not yet consumed by the upstream.
   *
   * Incorrect use of the buffer may cause data loss or unexpected data being read by the upstream.
   * Consider using alternative APIs to read data from the source, if possible:
   * - use [peek] for lookahead into a source;
   * - implement [RawSource] and wrap a downstream source into it to intercept data being read.
   */
  @InternalIoApi
  public val buffer: Buffer

  /**
   * Returns true if there are no more bytes in this source.
   *
   * The call of this method will block until there are bytes to read or the source is definitely exhausted.
   *
   * @throws IllegalStateException when the source is closed.
   * @throws IOException when some I/O error occurs.
   *
   * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.exhausted
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
   * @throws IllegalStateException when the source is closed.
   * @throws IllegalArgumentException when [byteCount] is negative.
   * @throws IOException when some I/O error occurs.
   *
   * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.require
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
   *
   * @throws IllegalArgumentException when [byteCount] is negative.
   * @throws IllegalStateException when the source is closed.
   * @throws IOException when some I/O error occurs.
   *
   * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.request
   */
  public fun request(byteCount: Long): Boolean

  /**
   * Removes a byte from this source and returns it.
   *
   * @throws EOFException when there are no more bytes to read.
   * @throws IllegalStateException when the source is closed.
   * @throws IOException when some I/O error occurs.
   *
   * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readByte
   */
  public fun readByte(): Byte

  /**
   * Removes two bytes from this source and returns a short integer composed of it according to the big-endian order.
   *
   * @throws EOFException when there are not enough data to read a short value.
   * @throws IllegalStateException when the source is closed.
   * @throws IOException when some I/O error occurs.
   *
   * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readShort
   */
  public fun readShort(): Short

  /**
   * Removes four bytes from this source and returns an integer composed of it according to the big-endian order.
   *
   * @throws EOFException when there are not enough data to read an int value.
   * @throws IllegalStateException when the source is closed.
   * @throws IOException when some I/O error occurs.
   *
   * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readInt
   */
  public fun readInt(): Int

  /**
   * Removes eight bytes from this source and returns a long integer composed of it according to the big-endian order.
   *
   * @throws EOFException when there are not enough data to read a long value.
   * @throws IllegalStateException when the source is closed.
   * @throws IOException when some I/O error occurs.
   *
   * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readLong
   */
  public fun readLong(): Long

  /**
   * Reads and discards [byteCount] bytes from this source.
   *
   * @param byteCount the number of bytes to be skipped.
   *
   * @throws EOFException when the source is exhausted before the requested number of bytes can be skipped.
   * @throws IllegalArgumentException when [byteCount] is negative.
   * @throws IllegalStateException when the source is closed.
   * @throws IOException when some I/O error occurs.
   *
   * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.skip
   */
  public fun skip(byteCount: Long)

  /**
   * Removes up to `endIndex - startIndex` bytes from this source, copies them into [sink] subrange starting at
   * [startIndex] and ending at [endIndex], and returns the number of bytes read, or -1 if this source is exhausted.
   *
   * @param sink the array to which data will be written from this source.
   * @param startIndex the startIndex (inclusive) of the [sink] subrange to read data into, 0 by default.
   * @param endIndex the endIndex (exclusive) of the [sink] subrange to read data into, `sink.size` by default.
   *
   * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [sink] array indices.
   * @throws IllegalArgumentException when `startIndex > endIndex`.
   * @throws IllegalStateException when the source is closed.
   * @throws IOException when some I/O error occurs.
   *
   * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readAtMostToByteArray
   */
  public fun readAtMostTo(sink: ByteArray, startIndex: Int = 0, endIndex: Int = sink.size): Int

  /**
   * Removes exactly [byteCount] bytes from this source and writes them to [sink].
   *
   * @param sink the sink to which data will be written from this source.
   * @param byteCount the number of bytes that should be written into [sink]
   *
   * @throws IllegalArgumentException when [byteCount] is negative.
   * @throws EOFException when the requested number of bytes cannot be read.
   * @throws IllegalStateException when the source or [sink] is closed.
   * @throws IOException when some I/O error occurs.
   *
   * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readSourceToSink
   */
  public fun readTo(sink: RawSink, byteCount: Long)

  /**
   * Removes all bytes from this source, writes them to [sink], and returns the total number of bytes
   * written to [sink].
   *
   * Return 0 if this source is exhausted.
   *
   * @param sink the sink to which data will be written from this source.
   *
   * @throws IllegalStateException when the source or [sink] is closed.
   * @throws IOException when some I/O error occurs.
   *
   * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.transferTo
   */
  @IgnorableReturnValue
  public fun transferTo(sink: RawSink): Long

  /**
   * Returns a new [Source] that can read data from this source without consuming it.
   * The returned source becomes invalid once this source is next read or closed.
   *
   * Peek could be used to lookahead and read the same data multiple times.
   *
   * If peek source needs to access more data that this [Source] has in its buffer,
   * more data will be requested from the underlying source and on success,
   * it'll be added to the buffer of this [Source].
   * If the underlying source was exhausted or some error occurred on attempt to fill the buffer,
   * a corresponding exception will be thrown.
   *
   * @throws IllegalStateException when the source is closed.
   *
   * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.peekSample
   */
  public fun peek(): Source
}
