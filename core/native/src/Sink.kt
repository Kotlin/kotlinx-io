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

public actual sealed interface Sink : RawSink {
  public actual val buffer: Buffer

  public actual fun write(source: ByteArray, offset: Int, byteCount: Int)

  public actual fun writeAll(source: RawSource): Long

  public actual fun write(source: RawSource, byteCount: Long)

  public actual fun writeByte(byte: Byte)

  public actual fun writeShort(short: Short)

  public actual fun writeInt(int: Int)

  public actual fun writeLong(long: Long)

  public actual fun emit()

  public actual fun emitCompleteSegments()
}
