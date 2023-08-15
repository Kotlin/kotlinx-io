/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toKString
import platform.posix.dirname

@OptIn(ExperimentalForeignApi::class)
internal actual fun dirnameImpl(path: String): String {
    return dirname(path.cstr)?.toKString() ?: ""
}

