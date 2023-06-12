/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2018 Square, Inc.
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

package kotlinx.io

internal expect fun String.asUtf8ToByteArray(): ByteArray

public expect open class IOException(message: String?, cause: Throwable?) : Exception {
  public constructor(message: String? = null)
}

public expect open class EOFException(message: String? = null) : IOException

public expect interface Closeable {
  /**
   * Closes this object and releases the resources it holds. It is an error to use an object after
   * it has been closed. It is safe to close an object more than once.
   */
  public fun close()
}
