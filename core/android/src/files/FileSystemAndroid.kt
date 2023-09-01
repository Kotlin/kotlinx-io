/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.__posix_basename
import platform.posix.dirname

@OptIn(ExperimentalForeignApi::class)
internal actual fun dirnameImpl(path: String): String {
    return dirname(path)?.toKString() ?: ""
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun basenameImpl(path: String): String {
    return __posix_basename(path)?.toKString() ?: ""
}

internal actual fun isAbsoluteImpl(path: String): Boolean = path.startsWith('/')
