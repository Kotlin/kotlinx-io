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
import java.nio.ByteBuffer
import java.nio.charset.Charset
import kotlinx.io.internal.*

internal actual class RealSink actual constructor(
  @JvmField actual val sink: RawSink
) : Sink {
  @JvmField val bufferField = Buffer()
  @JvmField actual var closed: Boolean = false

  @Suppress("OVERRIDE_BY_INLINE") // Prevent internal code from calling the getter.
  override val buffer: Buffer
    inline get() = bufferField

  override fun buffer() = bufferField

  override fun write(source: Buffer, byteCount: Long) = commonWrite(source, byteCount)
//  override fun write(byteString: ByteString) = commonWrite(byteString)
//  override fun write(byteString: ByteString, offset: Int, byteCount: Int) =
//    commonWrite(byteString, offset, byteCount)
  override fun writeUtf8(string: String) = commonWriteUtf8(string)
  override fun writeUtf8(string: String, beginIndex: Int, endIndex: Int) =
    commonWriteUtf8(string, beginIndex, endIndex)

  override fun writeUtf8CodePoint(codePoint: Int) = commonWriteUtf8CodePoint(codePoint)

  override fun writeString(string: String, charset: Charset): Sink {
    check(!closed) { "closed" }
    buffer.writeString(string, charset)
    return emitCompleteSegments()
  }

  override fun writeString(
    string: String,
    beginIndex: Int,
    endIndex: Int,
    charset: Charset
  ): Sink {
    check(!closed) { "closed" }
    buffer.writeString(string, beginIndex, endIndex, charset)
    return emitCompleteSegments()
  }

  override fun write(source: ByteArray) = commonWrite(source)
  override fun write(source: ByteArray, offset: Int, byteCount: Int) =
    commonWrite(source, offset, byteCount)

  override fun write(source: ByteBuffer): Int {
    check(!closed) { "closed" }
    val result = buffer.write(source)
    emitCompleteSegments()
    return result
  }

  override fun writeAll(source: RawSource) = commonWriteAll(source)
  override fun write(source: RawSource, byteCount: Long): Sink = commonWrite(source, byteCount)
  override fun writeByte(b: Int) = commonWriteByte(b)
  override fun writeShort(s: Int) = commonWriteShort(s)
  override fun writeShortLe(s: Int) = commonWriteShortLe(s)
  override fun writeInt(i: Int) = commonWriteInt(i)
  override fun writeIntLe(i: Int) = commonWriteIntLe(i)
  override fun writeLong(v: Long) = commonWriteLong(v)
  override fun writeLongLe(v: Long) = commonWriteLongLe(v)
  override fun writeDecimalLong(v: Long) = commonWriteDecimalLong(v)
  override fun writeHexadecimalUnsignedLong(v: Long) = commonWriteHexadecimalUnsignedLong(v)
  override fun emitCompleteSegments() = commonEmitCompleteSegments()
  override fun emit() = commonEmit()

  override fun outputStream(): OutputStream {
    return object : OutputStream() {
      override fun write(b: Int) {
        if (closed) throw IOException("closed")
        buffer.writeByte(b.toByte().toInt())
        emitCompleteSegments()
      }

      override fun write(data: ByteArray, offset: Int, byteCount: Int) {
        if (closed) throw IOException("closed")
        buffer.write(data, offset, byteCount)
        emitCompleteSegments()
      }

      override fun flush() {
        // For backwards compatibility, a flush() on a closed stream is a no-op.
        if (!closed) {
          this@RealSink.flush()
        }
      }

      override fun close() = this@RealSink.close()

      override fun toString() = "${this@RealSink}.outputStream()"
    }
  }

  override fun flush() = commonFlush()

  override fun isOpen() = !closed


  override fun close() = commonClose()

  override fun cancel() {
    commonCancel()
  }

  override fun toString() = commonToString()
}
