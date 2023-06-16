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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// TODO move to RealBufferedSink class: https://youtrack.jetbrains.com/issue/KT-20427
@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.internal

import kotlinx.io.*

@OptIn(DelicateIoApi::class)
internal inline fun RealSink.commonWrite(source: Buffer, byteCount: Long) {
  require(byteCount >= 0) { "byteCount ($byteCount) should not be negative." }
  check(!closed) { "closed" }
  buffer.write(source, byteCount)
  emitCompleteSegments()
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSink.commonWrite(
  source: ByteArray,
  offset: Int,
  byteCount: Int
) {
  checkOffsetAndCount(source.size.toLong(), offset.toLong(), byteCount.toLong())
  check(!closed) { "closed" }
  buffer.write(source, offset, byteCount)
  emitCompleteSegments()
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSink.commonWriteAll(source: RawSource): Long {
  var totalBytesRead = 0L
  while (true) {
    val readCount: Long = source.read(buffer, Segment.SIZE.toLong())
    if (readCount == -1L) break
    totalBytesRead += readCount
    emitCompleteSegments()
  }
  return totalBytesRead
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSink.commonWrite(source: RawSource, byteCount: Long) {
  require(byteCount >= 0) { "byteCount ($byteCount) should not be negative."}
  var remainingByteCount = byteCount
  while (remainingByteCount > 0L) {
    val read = source.read(buffer, remainingByteCount)
    if (read == -1L) throw EOFException()
    remainingByteCount -= read
    emitCompleteSegments()
  }
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSink.commonWriteByte(b: Byte) {
  check(!closed) { "closed" }
  buffer.writeByte(b)
  emitCompleteSegments()
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSink.commonWriteShort(s: Short) {
  check(!closed) { "closed" }
  buffer.writeShort(s)
  emitCompleteSegments()
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSink.commonWriteInt(i: Int) {
  check(!closed) { "closed" }
  buffer.writeInt(i)
  emitCompleteSegments()
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSink.commonWriteLong(v: Long) {
  check(!closed) { "closed" }
  buffer.writeLong(v)
  emitCompleteSegments()
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSink.commonEmitCompleteSegments() {
  check(!closed) { "closed" }
  val byteCount = buffer.completeSegmentByteCount()
  if (byteCount > 0L) sink.write(buffer, byteCount)
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSink.commonEmit() {
  check(!closed) { "closed" }
  val byteCount = buffer.size
  if (byteCount > 0L) sink.write(buffer, byteCount)
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSink.commonFlush() {
  check(!closed) { "closed" }
  if (buffer.size > 0L) {
    sink.write(buffer, buffer.size)
  }
  sink.flush()
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSink.commonClose() {
  if (closed) return

  // Emit buffered data to the underlying sink. If this fails, we still need
  // to close the sink; otherwise we risk leaking resources.
  var thrown: Throwable? = null
  try {
    if (buffer.size > 0) {
      sink.write(buffer, buffer.size)
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

internal inline fun RealSink.commonToString() = "buffer($sink)"
