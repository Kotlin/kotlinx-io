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

internal actual fun ByteArray.toUtf8String(): String = String(this, Charsets.UTF_8)

internal actual fun String.asUtf8ToByteArray(): ByteArray = toByteArray(Charsets.UTF_8)

// TODO remove if https://youtrack.jetbrains.com/issue/KT-20641 provides a better solution
actual typealias ArrayIndexOutOfBoundsException = java.lang.ArrayIndexOutOfBoundsException

internal actual inline fun <R> synchronized(lock: Any, block: () -> R): R {
  return kotlin.synchronized(lock, block)
}

actual typealias IOException = java.io.IOException

actual typealias ProtocolException = java.net.ProtocolException

actual typealias EOFException = java.io.EOFException

actual typealias FileNotFoundException = java.io.FileNotFoundException

actual typealias Closeable = java.io.Closeable

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "NOTHING_TO_INLINE")
internal inline fun Any.wait() = (this as Object).wait()

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "NOTHING_TO_INLINE")
internal inline fun Any.notify() = (this as Object).notify()

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "NOTHING_TO_INLINE")
internal inline fun Any.notifyAll() = (this as Object).notifyAll()
