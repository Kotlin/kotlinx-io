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

import kotlinx.io.internal.*

internal actual class RealSource actual constructor(
  actual val source: RawSource
) : Source {
  actual var closed: Boolean = false
  override val buffer: Buffer = Buffer()

  override fun read(sink: Buffer, byteCount: Long): Long = commonRead(sink, byteCount)
  override fun exhausted(): Boolean = commonExhausted()
  override fun require(byteCount: Long): Unit = commonRequire(byteCount)
  override fun request(byteCount: Long): Boolean = commonRequest(byteCount)
  override fun readByte(): Byte = commonReadByte()
//  override fun readByteString(): ByteString = commonReadByteString()
//  override fun readByteString(byteCount: Long): ByteString = commonReadByteString(byteCount)
  override fun select(options: Options): Int = commonSelect(options)
  override fun readByteArray(): ByteArray = commonReadByteArray()
  override fun readByteArray(byteCount: Long): ByteArray = commonReadByteArray(byteCount)
  override fun read(sink: ByteArray): Int = read(sink, 0, sink.size)
  override fun readFully(sink: ByteArray): Unit = commonReadFully(sink)
  override fun read(sink: ByteArray, offset: Int, byteCount: Int): Int =
    commonRead(sink, offset, byteCount)

  override fun readFully(sink: Buffer, byteCount: Long): Unit = commonReadFully(sink, byteCount)
  override fun readAll(sink: RawSink): Long = commonReadAll(sink)
  override fun readUtf8(): String = commonReadUtf8()
  override fun readUtf8(byteCount: Long): String = commonReadUtf8(byteCount)
  override fun readUtf8Line(): String? = commonReadUtf8Line()
  override fun readUtf8LineStrict() = readUtf8LineStrict(Long.MAX_VALUE)
  override fun readUtf8LineStrict(limit: Long): String = commonReadUtf8LineStrict(limit)
  override fun readUtf8CodePoint(): Int = commonReadUtf8CodePoint()
  override fun readShort(): Short = commonReadShort()
  override fun readShortLe(): Short = commonReadShortLe()
  override fun readInt(): Int = commonReadInt()
  override fun readIntLe(): Int = commonReadIntLe()
  override fun readLong(): Long = commonReadLong()
  override fun readLongLe(): Long = commonReadLongLe()
  override fun readDecimalLong(): Long = commonReadDecimalLong()
  override fun readHexadecimalUnsignedLong(): Long = commonReadHexadecimalUnsignedLong()
  override fun skip(byteCount: Long): Unit = commonSkip(byteCount)
  override fun indexOf(b: Byte): Long = indexOf(b, 0L, Long.MAX_VALUE)
  override fun indexOf(b: Byte, fromIndex: Long): Long = indexOf(b, fromIndex, Long.MAX_VALUE)
  override fun indexOf(b: Byte, fromIndex: Long, toIndex: Long): Long =
    commonIndexOf(b, fromIndex, toIndex)

//  override fun indexOf(bytes: ByteString): Long = indexOf(bytes, 0L)
//  override fun indexOf(bytes: ByteString, fromIndex: Long): Long = commonIndexOf(bytes, fromIndex)
//  override fun indexOfElement(targetBytes: ByteString): Long = indexOfElement(targetBytes, 0L)
//  override fun indexOfElement(targetBytes: ByteString, fromIndex: Long): Long =
//    commonIndexOfElement(targetBytes, fromIndex)
//
//  override fun rangeEquals(offset: Long, bytes: ByteString) = rangeEquals(
//    offset, bytes, 0,
//    bytes.size
//  )
//
//  override fun rangeEquals(
//    offset: Long,
//    bytes: ByteString,
//    bytesOffset: Int,
//    byteCount: Int
//  ): Boolean = commonRangeEquals(offset, bytes, bytesOffset, byteCount)

  override fun peek(): Source = commonPeek()
  override fun close(): Unit = commonClose()

  override fun cancel() {
    commonCancel()
  }

  override fun toString(): String = commonToString()
}
