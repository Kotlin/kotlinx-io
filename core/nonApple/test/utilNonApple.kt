/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toKString
import platform.posix.errno
import platform.posix.mktemp
import platform.posix.strerror

@OptIn(ExperimentalForeignApi::class)
actual fun createTempFile(): String {
    val template = "tmp-XXXXXX"
    val path = mktemp(template.cstr) ?: throw IOException("Filed to create temp file: ${strerror(errno)}")
    val pathString = path.toKString()
    if (pathString.isBlank()) {
        throw IOException("Failed to create temp file.")
    }
    return pathString
}
