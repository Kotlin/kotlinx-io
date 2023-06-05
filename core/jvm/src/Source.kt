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

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.nio.channels.ReadableByteChannel
import java.nio.charset.Charset

actual sealed interface Source : RawSource, ReadableByteChannel {
  actual val buffer: Buffer

  @Throws(IOException::class)
  actual fun exhausted(): Boolean

  @Throws(IOException::class)
  actual fun require(byteCount: Long)

  @Throws(IOException::class)
  actual fun request(byteCount: Long): Boolean

  @Throws(IOException::class)
  actual fun readByte(): Byte

  @Throws(IOException::class)
  actual fun readShort(): Short

  @Throws(IOException::class)
  actual fun readInt(): Int

  @Throws(IOException::class)
  actual fun readLong(): Long

  @Throws(IOException::class)
  actual fun skip(byteCount: Long)

  @Throws(IOException::class)
  actual fun readByteArray(): ByteArray

  @Throws(IOException::class)
  actual fun readByteArray(byteCount: Long): ByteArray

  @Throws(IOException::class)
  actual fun readFully(sink: ByteArray)

  @Throws(IOException::class)
  actual fun read(sink: ByteArray, offset: Int, byteCount: Int): Int

  @Throws(IOException::class)
  actual fun readFully(sink: Buffer, byteCount: Long)

  @Throws(IOException::class)
  actual fun readAll(sink: RawSink): Long

  actual fun peek(): Source

  /**
   * Returns an input stream that reads from this source. Closing the stream will also close this source.
   */
  fun inputStream(): InputStream
}

private fun Buffer.readStringImpl(byteCount: Long, charset: Charset): String {
  require(byteCount >= 0 && byteCount <= Integer.MAX_VALUE) { "byteCount: $byteCount" }
  if (size < byteCount) throw EOFException()
  if (byteCount == 0L) return ""

  val s = head!!
  if (s.pos + byteCount > s.limit) {
    // If the string spans multiple segments, delegate to readBytes().
    return String(readByteArray(byteCount), charset)
  }

  val result = String(s.data, s.pos, byteCount.toInt(), charset)
  s.pos += byteCount.toInt()
  size -= byteCount

  if (s.pos == s.limit) {
    head = s.pop()
    SegmentPool.recycle(s)
  }

  return result
}

/**
 * Decodes whole content of this stream into a string using [charset]. Returns empty string if the source is exhausted.
 *
 * @param charset the [Charset] to use for string decoding.
 */
@Throws(IOException::class)
fun Source.readString(charset: Charset): String {
  var req = 1L
  while (request(req)) {
    req *= 2
  }
  return buffer.readStringImpl(buffer.size, charset)
}

/**
 * Decodes [byteCount] bytes of this stream into a string using [charset].
 *
 * @param byteCount the number of bytes to read from the source for decoding.
 * @param charset the [Charset] to use for string decoding.
 *
 * @throws EOFException when the source exhausted before [byteCount] bytes could be read from it.
 */
@Throws(IOException::class)
fun Source.readString(byteCount: Long, charset: Charset): String {
  require(byteCount)
  return buffer.readStringImpl(byteCount, charset)
}