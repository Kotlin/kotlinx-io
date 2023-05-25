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

actual class Buffer : Source, Sink {
  internal actual var head: Segment? = null

  actual var size: Long = 0L
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

  actual fun copyTo(
    out: Buffer,
    offset: Long,
    byteCount: Long
  ): Buffer = commonCopyTo(out, offset, byteCount)

  actual fun copyTo(
    out: Buffer,
    offset: Long
  ): Buffer = copyTo(out, offset, size - offset)

  actual operator fun get(pos: Long): Byte = commonGet(pos)

  actual fun completeSegmentByteCount(): Long = commonCompleteSegmentByteCount()

  override fun readByte(): Byte = commonReadByte()

  override fun readShort(): Short = commonReadShort()

  override fun readInt(): Int = commonReadInt()

  override fun readLong(): Long = commonReadLong()

  override fun readShortLe(): Short = readShort().reverseBytes()

  override fun readIntLe(): Int = readInt().reverseBytes()

  override fun readLongLe(): Long = readLong().reverseBytes()

  override fun readDecimalLong(): Long = commonReadDecimalLong()

  override fun readHexadecimalUnsignedLong(): Long = commonReadHexadecimalUnsignedLong()

  override fun readFully(sink: Buffer, byteCount: Long): Unit = commonReadFully(sink, byteCount)

  override fun readAll(sink: RawSink): Long = commonReadAll(sink)

  override fun readUtf8(): String = readUtf8(size)

  override fun readUtf8(byteCount: Long): String = commonReadUtf8(byteCount)

  override fun readUtf8Line(): String? = commonReadUtf8Line()

  override fun readUtf8LineStrict(): String = readUtf8LineStrict(Long.MAX_VALUE)

  override fun readUtf8LineStrict(limit: Long): String = commonReadUtf8LineStrict(limit)

  override fun readUtf8CodePoint(): Int = commonReadUtf8CodePoint()

  override fun readByteArray(): ByteArray = commonReadByteArray()

  override fun readByteArray(byteCount: Long): ByteArray = commonReadByteArray(byteCount)

  override fun read(sink: ByteArray): Int = commonRead(sink)

  override fun readFully(sink: ByteArray): Unit = commonReadFully(sink)

  override fun read(sink: ByteArray, offset: Int, byteCount: Int): Int =
    commonRead(sink, offset, byteCount)

  actual fun clear(): Unit = commonClear()

  actual override fun skip(byteCount: Long): Unit = commonSkip(byteCount)

  internal actual fun writableSegment(minimumCapacity: Int): Segment =
    commonWritableSegment(minimumCapacity)

  actual override fun writeUtf8(string: String): Buffer = writeUtf8(string, 0, string.length)

  actual override fun writeUtf8(string: String, beginIndex: Int, endIndex: Int): Buffer =
    commonWriteUtf8(string, beginIndex, endIndex)

  actual override fun writeUtf8CodePoint(codePoint: Int): Buffer =
    commonWriteUtf8CodePoint(codePoint)

  actual override fun write(source: ByteArray): Buffer = commonWrite(source)

  actual override fun write(source: ByteArray, offset: Int, byteCount: Int): Buffer =
    commonWrite(source, offset, byteCount)

  override fun writeAll(source: RawSource): Long = commonWriteAll(source)

  actual override fun write(source: RawSource, byteCount: Long): Buffer =
    commonWrite(source, byteCount)

  actual override fun writeByte(b: Int): Buffer = commonWriteByte(b)

  actual override fun writeShort(s: Int): Buffer = commonWriteShort(s)

  actual override fun writeShortLe(s: Int): Buffer = writeShort(s.toShort().reverseBytes().toInt())

  actual override fun writeInt(i: Int): Buffer = commonWriteInt(i)

  actual override fun writeIntLe(i: Int): Buffer = writeInt(i.reverseBytes())

  actual override fun writeLong(v: Long): Buffer = commonWriteLong(v)

  actual override fun writeLongLe(v: Long): Buffer = writeLong(v.reverseBytes())

  actual override fun writeDecimalLong(v: Long): Buffer = commonWriteDecimalLong(v)

  actual override fun writeHexadecimalUnsignedLong(v: Long): Buffer =
    commonWriteHexadecimalUnsignedLong(v)

  override fun write(source: Buffer, byteCount: Long): Unit = commonWrite(source, byteCount)

  override fun read(sink: Buffer, byteCount: Long): Long = commonRead(sink, byteCount)

  override fun indexOf(b: Byte): Long = indexOf(b, 0, Long.MAX_VALUE)

  override fun indexOf(b: Byte, fromIndex: Long): Long = indexOf(b, fromIndex, Long.MAX_VALUE)

  override fun indexOf(b: Byte, fromIndex: Long, toIndex: Long): Long =
    commonIndexOf(b, fromIndex, toIndex)

//  override fun indexOf(bytes: ByteString): Long = indexOf(bytes, 0)
//
//  override fun indexOf(bytes: ByteString, fromIndex: Long): Long = commonIndexOf(bytes, fromIndex)
//
//  override fun indexOfElement(targetBytes: ByteString): Long = indexOfElement(targetBytes, 0L)
//
//  override fun indexOfElement(targetBytes: ByteString, fromIndex: Long): Long =
//    commonIndexOfElement(targetBytes, fromIndex)
//
//  override fun rangeEquals(offset: Long, bytes: ByteString): Boolean =
//    rangeEquals(offset, bytes, 0, bytes.size)
//
//  override fun rangeEquals(
//    offset: Long,
//    bytes: ByteString,
//    bytesOffset: Int,
//    byteCount: Int
//  ): Boolean = commonRangeEquals(offset, bytes, bytesOffset, byteCount)

  override fun flush() = Unit

  override fun close() = Unit

  override fun cancel() {
    // Not cancelable.
  }

  override fun equals(other: Any?): Boolean = commonEquals(other)

  override fun hashCode(): Int = commonHashCode()

  /**
   * Returns a human-readable string that describes the contents of this buffer. Typically this
   * is a string like `[text=Hello]` or `[hex=0000ffff]`.
   */
  override fun toString() = commonString()

  actual fun copy(): Buffer = commonCopy()

//  actual fun readUnsafe(unsafeCursor: UnsafeCursor): UnsafeCursor = commonReadUnsafe(unsafeCursor)
//
//  actual fun readAndWriteUnsafe(unsafeCursor: UnsafeCursor): UnsafeCursor =
//    commonReadAndWriteUnsafe(unsafeCursor)

  actual class UnsafeCursor {
    actual var buffer: Buffer? = null
    actual var readWrite: Boolean = false

    internal actual var segment: Segment? = null
    actual var offset = -1L
    actual var data: ByteArray? = null
    actual var start = -1
    actual var end = -1

    actual fun next(): Int = commonNext()

    actual fun seek(offset: Long): Int = commonSeek(offset)

    actual fun resizeBuffer(newSize: Long): Long = commonResizeBuffer(newSize)

    actual fun expandBuffer(minByteCount: Int): Long = commonExpandBuffer(minByteCount)

    actual fun close() {
      commonClose()
    }
  }
}
