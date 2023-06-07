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

public actual sealed interface Source : RawSource, ReadableByteChannel {
  public actual val buffer: Buffer

  public actual fun exhausted(): Boolean

  public actual fun require(byteCount: Long)

  public actual fun request(byteCount: Long): Boolean

  public actual fun readByte(): Byte

  public actual fun readShort(): Short

  public actual fun readInt(): Int

  public actual fun readLong(): Long

  public actual fun skip(byteCount: Long)

  public actual fun readFully(sink: ByteArray)

  public actual fun read(sink: ByteArray, offset: Int, byteCount: Int): Int

  public actual fun readFully(sink: Buffer, byteCount: Long)

  public actual fun readAll(sink: RawSink): Long

  public actual fun peek(): Source
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
public fun Source.readString(charset: Charset): String {
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
public fun Source.readString(byteCount: Long, charset: Charset): String {
  require(byteCount)
  return buffer.readStringImpl(byteCount, charset)
}

/**
 * Returns an input stream that reads from this source. Closing the stream will also close this source.
 */
public fun Source.inputStream(): InputStream {
  return object : InputStream() {
    override fun read(): Int {
      if (!isOpen) throw IOException("closed")
      if (exhausted()) {
        return -1
      }
      return readByte() and 0xff
    }

    override fun read(data: ByteArray, offset: Int, byteCount: Int): Int {
      if (!isOpen) throw IOException("closed")
      checkOffsetAndCount(data.size.toLong(), offset.toLong(), byteCount.toLong())

      return this@inputStream.read(data, offset, byteCount)
    }

    override fun available(): Int {
      if (!isOpen) throw IOException("closed")
      return minOf(buffer.size, Integer.MAX_VALUE).toInt()
    }

    override fun close() = this@inputStream.close()

    override fun toString() = "${this@inputStream}.inputStream()"
  }
}