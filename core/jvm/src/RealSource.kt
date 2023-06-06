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
import java.io.InputStream
import java.nio.ByteBuffer
import kotlinx.io.internal.*

internal actual class RealSource actual constructor(
  @JvmField actual val source: RawSource
) : Source {
  @JvmField val bufferField = Buffer()
  @JvmField actual var closed: Boolean = false

  @Suppress("OVERRIDE_BY_INLINE") // Prevent internal code from calling the getter.
  override val buffer: Buffer
    inline get() = bufferField

  override fun read(sink: Buffer, byteCount: Long): Long = commonRead(sink, byteCount)
  override fun exhausted(): Boolean = commonExhausted()
  override fun require(byteCount: Long): Unit = commonRequire(byteCount)
  override fun request(byteCount: Long): Boolean = commonRequest(byteCount)
  override fun readByte(): Byte = commonReadByte()
  override fun readByteArray(): ByteArray = commonReadByteArray()
  override fun readByteArray(byteCount: Long): ByteArray = commonReadByteArray(byteCount)
  override fun readFully(sink: ByteArray): Unit = commonReadFully(sink)
  override fun read(sink: ByteArray, offset: Int, byteCount: Int): Int =
    commonRead(sink, offset, byteCount)

  override fun read(sink: ByteBuffer): Int {
    if (buffer.size == 0L) {
      val read = source.read(buffer, Segment.SIZE.toLong())
      if (read == -1L) return -1
    }

    return buffer.read(sink)
  }

  override fun readFully(sink: Buffer, byteCount: Long): Unit = commonReadFully(sink, byteCount)
  override fun readAll(sink: RawSink): Long = commonReadAll(sink)

  override fun readShort(): Short = commonReadShort()
  override fun readInt(): Int = commonReadInt()
  override fun readLong(): Long = commonReadLong()
  override fun skip(byteCount: Long): Unit = commonSkip(byteCount)

  override fun peek(): Source = commonPeek()

  override fun inputStream(): InputStream {
    return object : InputStream() {
      override fun read(): Int {
        if (closed) throw IOException("closed")
        if (buffer.size == 0L) {
          val count = source.read(buffer, Segment.SIZE.toLong())
          if (count == -1L) return -1
        }
        return buffer.readByte() and 0xff
      }

      override fun read(data: ByteArray, offset: Int, byteCount: Int): Int {
        if (closed) throw IOException("closed")
        checkOffsetAndCount(data.size.toLong(), offset.toLong(), byteCount.toLong())

        if (buffer.size == 0L) {
          val count = source.read(buffer, Segment.SIZE.toLong())
          if (count == -1L) return -1
        }

        return buffer.read(data, offset, byteCount)
      }

      override fun available(): Int {
        if (closed) throw IOException("closed")
        return minOf(buffer.size, Integer.MAX_VALUE).toInt()
      }

      override fun close() = this@RealSource.close()

      override fun toString() = "${this@RealSource}.inputStream()"
    }
  }

  override fun isOpen() = !closed

  override fun close(): Unit = commonClose()

  override fun cancel() {
    commonCancel()
  }

  override fun toString(): String = commonToString()
}
