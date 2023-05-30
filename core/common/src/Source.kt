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
  /** This source's internal buffer. */
  val buffer: Buffer

  /**
   * Returns true if there are no more bytes in this source. This will block until there are bytes
   * to read or the source is definitely exhausted.
   */
  fun exhausted(): Boolean

  /**
   * Returns when the buffer contains at least `byteCount` bytes. Throws an
   * [java.io.EOFException] if the source is exhausted before the required bytes can be read.
   */
  fun require(byteCount: Long)

  /**
   * Returns true when the buffer contains at least `byteCount` bytes, expanding it as
   * necessary. Returns false if the source is exhausted before the requested bytes can be read.
   */
  fun request(byteCount: Long): Boolean

  /** Removes a byte from this source and returns it. */
  fun readByte(): Byte

  /**
   * Removes two bytes from this source and returns a big-endian short.
   * ```
   * Buffer buffer = new Buffer()
   *     .writeByte(0x7f)
   *     .writeByte(0xff)
   *     .writeByte(0x00)
   *     .writeByte(0x0f);
   * assertEquals(4, buffer.size());
   *
   * assertEquals(32767, buffer.readShort());
   * assertEquals(2, buffer.size());
   *
   * assertEquals(15, buffer.readShort());
   * assertEquals(0, buffer.size());
   * ```
   */
  fun readShort(): Short

  /**
   * Removes four bytes from this source and returns a big-endian int.
   * ```
   * Buffer buffer = new Buffer()
   *     .writeByte(0x7f)
   *     .writeByte(0xff)
   *     .writeByte(0xff)
   *     .writeByte(0xff)
   *     .writeByte(0x00)
   *     .writeByte(0x00)
   *     .writeByte(0x00)
   *     .writeByte(0x0f);
   * assertEquals(8, buffer.size());
   *
   * assertEquals(2147483647, buffer.readInt());
   * assertEquals(4, buffer.size());
   *
   * assertEquals(15, buffer.readInt());
   * assertEquals(0, buffer.size());
   * ```
   */
  fun readInt(): Int

  /**
   * Removes eight bytes from this source and returns a big-endian long.
   * ```
   * Buffer buffer = new Buffer()
   *     .writeByte(0x7f)
   *     .writeByte(0xff)
   *     .writeByte(0xff)
   *     .writeByte(0xff)
   *     .writeByte(0xff)
   *     .writeByte(0xff)
   *     .writeByte(0xff)
   *     .writeByte(0xff)
   *     .writeByte(0x00)
   *     .writeByte(0x00)
   *     .writeByte(0x00)
   *     .writeByte(0x00)
   *     .writeByte(0x00)
   *     .writeByte(0x00)
   *     .writeByte(0x00)
   *     .writeByte(0x0f);
   * assertEquals(16, buffer.size());
   *
   * assertEquals(9223372036854775807L, buffer.readLong());
   * assertEquals(8, buffer.size());
   *
   * assertEquals(15, buffer.readLong());
   * assertEquals(0, buffer.size());
   * ```
   */
  fun readLong(): Long

  /**
   * Reads and discards `byteCount` bytes from this source. Throws an [java.io.EOFException] if the
   * source is exhausted before the requested bytes can be skipped.
   */
  fun skip(byteCount: Long)

  /** Removes all bytes from this and returns them as a byte array. */
  fun readByteArray(): ByteArray

  /** Removes `byteCount` bytes from this and returns them as a byte array. */
  fun readByteArray(byteCount: Long): ByteArray

  /**
   * Removes exactly `sink.length` bytes from this and copies them into `sink`. Throws an
   * [java.io.EOFException] if the requested number of bytes cannot be read.
   */
  fun readFully(sink: ByteArray)

  // TODO(filipp): fix javadoc
  /**
   * Removes up to `sink.length` bytes from this and copies them into `sink`. Returns the number of
   * bytes read, or -1 if this source is exhausted.
   */
  //fun read(sink: ByteArray): Int

  /**
   * Removes up to `byteCount` bytes from this and copies them into `sink` at `offset`. Returns the
   * number of bytes read, or -1 if this source is exhausted.
   */
  fun read(sink: ByteArray, offset: Int = 0, byteCount: Int = sink.size): Int

  /**
   * Removes exactly `byteCount` bytes from this and appends them to `sink`. Throws an
   * [java.io.EOFException] if the requested number of bytes cannot be read.
   */
  fun readFully(sink: Buffer, byteCount: Long)

  /**
   * Removes all bytes from this and appends them to `sink`. Returns the total number of bytes
   * written to `sink` which will be 0 if this is exhausted.
   */
  fun readAll(sink: RawSink): Long

  /**
   * Returns a new `BufferedSource` that can read data from this `BufferedSource` without consuming
   * it. The returned source becomes invalid once this source is next read or closed.
   *
   * For example, we can use `peek()` to lookahead and read the same data multiple times.
   *
   * ```
   * val buffer = Buffer()
   * buffer.writeUtf8("abcdefghi")
   *
   * buffer.readUtf8(3) // returns "abc", buffer contains "defghi"
   *
   * val peek = buffer.peek()
   * peek.readUtf8(3) // returns "def", buffer contains "defghi"
   * peek.readUtf8(3) // returns "ghi", buffer contains "defghi"
   *
   * buffer.readUtf8(3) // returns "def", buffer contains "ghi"
   * ```
   */
  fun peek(): Source
}
