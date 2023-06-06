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

import kotlinx.io.internal.*

public actual class Buffer : Source, Sink {
  internal actual var head: Segment? = null

  public actual var size: Long = 0L
    internal set

  actual override val buffer: Buffer get() = this

  actual override fun emitCompleteSegments(): Buffer = this // Nowhere to emit to!

  actual override fun emit(): Buffer = this // Nowhere to emit to!

  override fun exhausted(): Boolean = size == 0L

  override fun require(byteCount: Long) {
    if (size < byteCount) throw EOFException(null)
  }

  override fun request(byteCount: Long): Boolean = size >= byteCount

  override fun peek(): Source = PeekSource(this).buffer()

  public actual fun copyTo(
    out: Buffer,
    offset: Long,
    byteCount: Long
  ): Buffer = commonCopyTo(out, offset, byteCount)

  public actual operator fun get(pos: Long): Byte = commonGet(pos)

  public actual fun completeSegmentByteCount(): Long = commonCompleteSegmentByteCount()

  override fun readByte(): Byte = commonReadByte()

  override fun readShort(): Short = commonReadShort()

  override fun readInt(): Int = commonReadInt()

  override fun readLong(): Long = commonReadLong()

  override fun readFully(sink: Buffer, byteCount: Long): Unit = commonReadFully(sink, byteCount)

  override fun readAll(sink: RawSink): Long = commonReadAll(sink)

  override fun readByteArray(): ByteArray = commonReadByteArray()

  override fun readByteArray(byteCount: Long): ByteArray = commonReadByteArray(byteCount)

  override fun readFully(sink: ByteArray): Unit = commonReadFully(sink)

  override fun read(sink: ByteArray, offset: Int, byteCount: Int): Int =
    commonRead(sink, offset, byteCount)

  public actual fun clear(): Unit = commonClear()

  actual override fun skip(byteCount: Long): Unit = commonSkip(byteCount)

  internal actual fun writableSegment(minimumCapacity: Int): Segment =
    commonWritableSegment(minimumCapacity)

  actual override fun write(source: ByteArray, offset: Int, byteCount: Int): Buffer =
    commonWrite(source, offset, byteCount)

  override fun writeAll(source: RawSource): Long = commonWriteAll(source)

  actual override fun write(source: RawSource, byteCount: Long): Buffer =
    commonWrite(source, byteCount)

  actual override fun writeByte(byte: Int): Buffer = commonWriteByte(byte)

  actual override fun writeShort(short: Int): Buffer = commonWriteShort(short)

  actual override fun writeInt(int: Int): Buffer = commonWriteInt(int)

  actual override fun writeLong(long: Long): Buffer = commonWriteLong(long)

  override fun write(source: Buffer, byteCount: Long): Unit = commonWrite(source, byteCount)

  override fun read(sink: Buffer, byteCount: Long): Long = commonRead(sink, byteCount)

  actual override fun flush(): Unit = Unit

  actual override fun close(): Unit = Unit

  actual override fun cancel(): Unit = Unit

  override fun equals(other: Any?): Boolean = commonEquals(other)

  override fun hashCode(): Int = commonHashCode()

  override fun toString(): String = commonString()

  public actual fun copy(): Buffer = commonCopy()
}
