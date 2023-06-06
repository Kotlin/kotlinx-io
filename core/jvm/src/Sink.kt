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

import java.io.IOException
import java.io.OutputStream
import java.nio.channels.WritableByteChannel
import java.nio.charset.Charset

public actual sealed interface Sink : RawSink, WritableByteChannel {
  public actual val buffer: Buffer

  @Throws(IOException::class)
  public actual fun write(source: ByteArray, offset: Int, byteCount: Int): Sink

  @Throws(IOException::class)
  public actual fun writeAll(source: RawSource): Long

  @Throws(IOException::class)
  public actual fun write(source: RawSource, byteCount: Long): Sink

  @Throws(IOException::class)
  public actual fun writeByte(byte: Int): Sink

  @Throws(IOException::class)
  public actual fun writeShort(short: Int): Sink

  @Throws(IOException::class)
  public actual fun writeInt(int: Int): Sink

  @Throws(IOException::class)
  public actual fun writeLong(long: Long): Sink

  @Throws(IOException::class)
  actual override fun flush()

  @Throws(IOException::class)
  public actual fun emit(): Sink

  @Throws(IOException::class)
  public actual fun emitCompleteSegments(): Sink

  /**
   * Returns an output stream that writes to this sink. Closing the stream will also close this sink.
   */
  public fun outputStream(): OutputStream
}

/**
 * Encodes substring of [string] starting at [beginIndex] and ending at [endIndex] using [charset]
 * and writes into this sink.
 *
 * @param string the string to encode into this sink.
 * @param charset the [Charset] to use for encoding.
 * @param beginIndex the index of the first character to encode, inclusive, 0 by default.
 * @param endIndex the index of the last character to encode, exclusive, `string.size` by default.
 *
 * @throws IndexOutOfBoundsException when [beginIndex] and [endIndex] correspond to a range out of [string] bounds.
 */
public fun <T: Sink> T.writeString(string: String, charset: Charset, beginIndex: Int = 0, endIndex: Int = string.length): T {
  require(beginIndex >= 0) { "beginIndex < 0: $beginIndex" }
  require(endIndex >= beginIndex) { "endIndex < beginIndex: $endIndex < $beginIndex" }
  require(endIndex <= string.length) { "endIndex > string.length: $endIndex > ${string.length}" }
  if (charset == Charsets.UTF_8) return writeUtf8(string, beginIndex, endIndex)
  val data = string.substring(beginIndex, endIndex).toByteArray(charset)
  write(data, 0, data.size)
  return this
}