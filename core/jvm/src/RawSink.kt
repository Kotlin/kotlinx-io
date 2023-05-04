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

import java.io.Closeable
import java.io.Flushable
import java.io.IOException

actual interface RawSink : Closeable, Flushable {
  @Throws(IOException::class)
  actual fun write(source: Buffer, byteCount: Long)

  @Throws(IOException::class)
  actual override fun flush()

  actual fun cancel()

  @Throws(IOException::class)
  actual override fun close()
}