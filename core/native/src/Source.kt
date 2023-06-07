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

public actual sealed interface Source : RawSource {
  public actual val buffer: Buffer

  public actual fun exhausted(): Boolean

  public actual fun require(byteCount: Long)

  public actual fun request(byteCount: Long): Boolean

  public actual fun readByte(): Byte

  public actual fun readShort(): Short

  public actual fun readInt(): Int

  public actual fun readLong(): Long

  public actual fun skip(byteCount: Long)

  public actual fun read(sink: ByteArray, offset: Int, byteCount: Int): Int

  public actual fun readFully(sink: Buffer, byteCount: Long)

  public actual fun readAll(sink: RawSink): Long

  public actual fun peek(): Source
}
