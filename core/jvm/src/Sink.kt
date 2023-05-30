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
import java.nio.channels.WritableByteChannel
import java.nio.charset.Charset

actual sealed interface Sink : RawSink, WritableByteChannel {
  actual val buffer: Buffer

  @Throws(IOException::class)
  actual fun write(source: ByteArray, offset: Int, byteCount: Int): Sink

  @Throws(IOException::class)
  actual fun writeAll(source: RawSource): Long

  @Throws(IOException::class)
  actual fun write(source: RawSource, byteCount: Long): Sink

  @Throws(IOException::class)
  actual fun writeUtf8(string: String, beginIndex: Int, endIndex: Int): Sink

  @Throws(IOException::class)
  actual fun writeUtf8CodePoint(codePoint: Int): Sink

  @Throws(IOException::class)
  fun writeString(string: String, charset: Charset): Sink

  @Throws(IOException::class)
  fun writeString(string: String, beginIndex: Int, endIndex: Int, charset: Charset): Sink

  @Throws(IOException::class)
  actual fun writeByte(b: Int): Sink

  @Throws(IOException::class)
  actual fun writeShort(s: Int): Sink

  @Throws(IOException::class)
  actual fun writeInt(i: Int): Sink

  @Throws(IOException::class)
  actual fun writeLong(v: Long): Sink

  @Throws(IOException::class)
  actual override fun flush()

  @Throws(IOException::class)
  actual fun emit(): Sink

  @Throws(IOException::class)
  actual fun emitCompleteSegments(): Sink

  /** Returns an output stream that writes to this sink. */
  fun outputStream(): OutputStream
}
