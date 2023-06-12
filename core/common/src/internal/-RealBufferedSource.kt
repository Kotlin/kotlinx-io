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

// TODO move to RealBufferedSource class: https://youtrack.jetbrains.com/issue/KT-20427

@file:Suppress("NOTHING_TO_INLINE")

package kotlinx.io.internal

import kotlinx.io.*

@OptIn(DelicateIoApi::class)
internal inline fun RealSource.commonRead(sink: Buffer, byteCount: Long): Long {
  require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
  check(!closed) { "closed" }

  if (buffer.size == 0L) {
    val read = source.read(buffer, Segment.SIZE.toLong())
    if (read == -1L) return -1L
  }

  val toRead = minOf(byteCount, buffer.size)
  return buffer.read(sink, toRead)
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSource.commonExhausted(): Boolean {
  check(!closed) { "closed" }
  return buffer.exhausted() && source.read(buffer, Segment.SIZE.toLong()) == -1L
}

internal inline fun RealSource.commonRequire(byteCount: Long) {
  if (!request(byteCount)) throw EOFException()
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSource.commonRequest(byteCount: Long): Boolean {
  require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
  check(!closed) { "closed" }
  while (buffer.size < byteCount) {
    if (source.read(buffer, Segment.SIZE.toLong()) == -1L) return false
  }
  return true
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSource.commonReadByte(): Byte {
  require(1)
  return buffer.readByte()
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSource.commonRead(sink: ByteArray, offset: Int, byteCount: Int): Int {
  checkOffsetAndCount(sink.size.toLong(), offset.toLong(), byteCount.toLong())

  if (buffer.size == 0L) {
    val read = source.read(buffer, Segment.SIZE.toLong())
    if (read == -1L) return -1
  }

  val toRead = minOf(byteCount, buffer.size).toInt()
  return buffer.read(sink, offset, toRead)
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSource.commonReadFully(sink: RawSink, byteCount: Long) {
  try {
    require(byteCount)
  } catch (e: EOFException) {
    // The underlying source is exhausted. Copy the bytes we got before rethrowing.
    //sink.writeAll(buffer)
    // TODO: it seems to be incorrect.
    sink.write(buffer, buffer.size)
    throw e
  }

  buffer.readFully(sink, byteCount)
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSource.commonReadAll(sink: RawSink): Long {
  var totalBytesWritten: Long = 0
  while (source.read(buffer, Segment.SIZE.toLong()) != -1L) {
    val emitByteCount = buffer.completeSegmentByteCount()
    if (emitByteCount > 0L) {
      totalBytesWritten += emitByteCount
      sink.write(buffer, emitByteCount)
    }
  }
  if (buffer.size > 0L) {
    totalBytesWritten += buffer.size
    sink.write(buffer, buffer.size)
  }
  return totalBytesWritten
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSource.commonReadShort(): Short {
  require(2)
  return buffer.readShort()
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSource.commonReadInt(): Int {
  require(4)
  return buffer.readInt()
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSource.commonReadLong(): Long {
  require(8)
  return buffer.readLong()
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSource.commonSkip(byteCount: Long) {
  var remainingByteCount = byteCount
  check(!closed) { "closed" }
  while (remainingByteCount > 0) {
    if (buffer.size == 0L && source.read(buffer, Segment.SIZE.toLong()) == -1L) {
      throw EOFException()
    }
    val toSkip = minOf(remainingByteCount, buffer.size)
    buffer.skip(toSkip)
    remainingByteCount -= toSkip
  }
}

internal inline fun RealSource.commonPeek(): Source {
  return PeekSource(this).buffer()
}

@OptIn(DelicateIoApi::class)
internal inline fun RealSource.commonClose() {
  if (closed) return
  closed = true
  source.close()
  buffer.clear()
}

internal inline fun RealSource.commonToString() = "buffer($source)"
