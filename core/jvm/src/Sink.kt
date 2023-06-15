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
import java.nio.channels.WritableByteChannel
import java.nio.charset.Charset

public actual sealed interface Sink : RawSink {
  public actual val buffer: Buffer

  public actual fun write(source: ByteArray, offset: Int, byteCount: Int): Sink

  public actual fun writeAll(source: RawSource): Long

  public actual fun write(source: RawSource, byteCount: Long): Sink

  public actual fun writeByte(byte: Byte): Sink

  public actual fun writeShort(short: Short): Sink

  public actual fun writeInt(int: Int): Sink

  public actual fun writeLong(long: Long): Sink

  actual override fun flush()

  public actual fun emit(): Sink

  public actual fun emitCompleteSegments(): Sink
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
 * @throws IllegalArgumentException when [beginIndex] and [endIndex] correspond to a range out of [string] bounds.
 * @throws IllegalStateException when the sink is closed.
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

/**
 * Returns an output stream that writes to this sink. Closing the stream will also close this sink.
 */
public fun Sink.outputStream(): OutputStream {
  val isClosed: () -> Boolean = when (this) {
    is RealSink -> this::closed
    is Buffer -> { { false } }
  }

  return object : OutputStream() {
    override fun write(b: Int) {
      if (isClosed()) throw IOException("closed")
      buffer.writeByte(b.toByte())
      emitCompleteSegments()
    }

    override fun write(data: ByteArray, offset: Int, byteCount: Int) {
      if (isClosed()) throw IOException("closed")
      buffer.write(data, offset, byteCount)
      emitCompleteSegments()
    }

    override fun flush() {
      // For backwards compatibility, a flush() on a closed stream is a no-op.
      if (!isClosed()) {
        this@outputStream.flush()
      }
    }

    override fun close() = this@outputStream.close()

    override fun toString() = "${this@outputStream}.outputStream()"
  }
}

/**
 * Writes data from the [source] into this sink and returns the number of bytes written.
 *
 * @param source the source to read from.
 *
 * @throws IllegalStateException when the sink is closed.
 */
public fun Sink.write(source: ByteBuffer): Int {
  val sizeBefore = buffer.size
  buffer.readFrom(source)
  val bytesRead = buffer.size - sizeBefore
  emitCompleteSegments()
  return bytesRead.toInt()
}

/**
 * Returns [WritableByteChannel] backed by this sink. Closing the channel will also close the sink.
 */
public fun Sink.channel(): WritableByteChannel {
  val isClosed: () -> Boolean = when (this) {
    is RealSink -> this::closed
    is Buffer -> { { false } }
  }

  return object : WritableByteChannel {
    override fun close() {
      this@channel.close()
    }

    override fun isOpen(): Boolean = !isClosed()

    override fun write(source: ByteBuffer): Int {
      check(!isClosed()) { "closed" }
      return this@channel.write(source)
    }
  }
}
