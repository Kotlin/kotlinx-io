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

internal actual class RealSink actual constructor(
  @JvmField actual val sink: RawSink
) : Sink {
  @JvmField val bufferField = Buffer()
  @JvmField actual var closed: Boolean = false

  @Suppress("OVERRIDE_BY_INLINE") // Prevent internal code from calling the getter.
  override val buffer: Buffer
    inline get() = bufferField

  override fun write(source: Buffer, byteCount: Long) = commonWrite(source, byteCount)

  override fun write(source: ByteArray, offset: Int, byteCount: Int) =
    commonWrite(source, offset, byteCount)

  override fun writeAll(source: RawSource) = commonWriteAll(source)
  override fun write(source: RawSource, byteCount: Long) = commonWrite(source, byteCount)
  override fun writeByte(byte: Byte) = commonWriteByte(byte)
  override fun writeShort(short: Short) = commonWriteShort(short)
  override fun writeInt(int: Int) = commonWriteInt(int)
  override fun writeLong(long: Long) = commonWriteLong(long)
  override fun emitCompleteSegments() = commonEmitCompleteSegments()
  override fun emit() = commonEmit()

  override fun flush() = commonFlush()

  override fun close() = commonClose()

  override fun toString() = commonToString()
}
