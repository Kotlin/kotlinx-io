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

import kotlin.jvm.JvmField

@OptIn(InternalIoApi::class)
internal class RealSink(
  val sink: RawSink
) : Sink {
  @JvmField
  var closed: Boolean = false
  private val bufferField = Buffer()

  @DelicateIoApi
  override val buffer: Buffer
    get() = bufferField

  override fun write(source: Buffer, byteCount: Long) {
    require(byteCount >= 0) { "byteCount ($byteCount) should not be negative." }
    check(!closed) { "closed" }
    bufferField.write(source, byteCount)
    hintEmit()
  }

  override fun write(source: ByteArray, offset: Int, byteCount: Int) {
    checkOffsetAndCount(source.size.toLong(), offset.toLong(), byteCount.toLong())
    check(!closed) { "closed" }
    bufferField.write(source, offset, byteCount)
    hintEmit()
  }

  override fun writeAll(source: RawSource): Long {
    var totalBytesRead = 0L
    while (true) {
      val readCount: Long = source.read(bufferField, Segment.SIZE.toLong())
      if (readCount == -1L) break
      totalBytesRead += readCount
      hintEmit()
    }
    return totalBytesRead
  }

  override fun write(source: RawSource, byteCount: Long) {
    require(byteCount >= 0) { "byteCount ($byteCount) should not be negative." }
    var remainingByteCount = byteCount
    while (remainingByteCount > 0L) {
      val read = source.read(bufferField, remainingByteCount)
      if (read == -1L) throw EOFException()
      remainingByteCount -= read
      hintEmit()
    }
  }

  override fun writeByte(byte: Byte) {
    check(!closed) { "closed" }
    bufferField.writeByte(byte)
    hintEmit()
  }

  override fun writeShort(short: Short) {
    check(!closed) { "closed" }
    bufferField.writeShort(short)
    hintEmit()
  }

  override fun writeInt(int: Int) {
    check(!closed) { "closed" }
    bufferField.writeInt(int)
    hintEmit()
  }

  override fun writeLong(long: Long) {
    check(!closed) { "closed" }
    bufferField.writeLong(long)
    hintEmit()
  }

  @InternalIoApi
  override fun hintEmit() {
    check(!closed) { "closed" }
    val byteCount = bufferField.completeSegmentByteCount()
    if (byteCount > 0L) sink.write(bufferField, byteCount)
  }

  override fun emit() {
    check(!closed) { "closed" }
    val byteCount = bufferField.size
    if (byteCount > 0L) sink.write(bufferField, byteCount)
  }

  override fun flush() {
    check(!closed) { "closed" }
    if (bufferField.size > 0L) {
      sink.write(bufferField, bufferField.size)
    }
    sink.flush()
  }

  override fun close() {
    if (closed) return

    // Emit buffered data to the underlying sink. If this fails, we still need
    // to close the sink; otherwise we risk leaking resources.
    var thrown: Throwable? = null
    try {
      if (bufferField.size > 0) {
        sink.write(bufferField, bufferField.size)
      }
    } catch (e: Throwable) {
      thrown = e
    }

    try {
      sink.close()
    } catch (e: Throwable) {
      if (thrown == null) thrown = e
    }

    closed = true

    if (thrown != null) throw thrown
  }

  override fun toString() = "buffer($sink)"
}
