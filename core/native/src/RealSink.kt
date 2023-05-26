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

internal actual class RealSink actual constructor(
  actual val sink: RawSink
) : Sink {
  actual var closed: Boolean = false
  override val buffer = Buffer()

  override fun write(source: Buffer, byteCount: Long) = commonWrite(source, byteCount)

  override fun writeUtf8(string: String, beginIndex: Int, endIndex: Int) =
    commonWriteUtf8(string, beginIndex, endIndex)

  override fun writeUtf8CodePoint(codePoint: Int) = commonWriteUtf8CodePoint(codePoint)
  override fun write(source: ByteArray, offset: Int, byteCount: Int) =
    commonWrite(source, offset, byteCount)

  override fun writeAll(source: RawSource) = commonWriteAll(source)
  override fun write(source: RawSource, byteCount: Long): Sink = commonWrite(source, byteCount)
  override fun writeByte(b: Int) = commonWriteByte(b)
  override fun writeShort(s: Int) = commonWriteShort(s)
  override fun writeInt(i: Int) = commonWriteInt(i)
  override fun writeLong(v: Long) = commonWriteLong(v)
  override fun emitCompleteSegments() = commonEmitCompleteSegments()
  override fun emit() = commonEmit()
  override fun flush() = commonFlush()
  override fun close() = commonClose()

  override fun cancel() {
    commonCancel()
  }

  override fun toString() = commonToString()
}
