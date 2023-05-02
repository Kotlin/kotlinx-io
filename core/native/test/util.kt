/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */
package kotlinx.io

import kotlinx.cinterop.*
import platform.posix.*

actual fun createTempFile(): String {
    val template = "tmp-XXXXXX"
    val path = mktemp(template.cstr) ?: throw IOException("Filed to create temp file: ${strerror(errno)}")
    return path.toKString()
}

actual fun deleteFile(path: String) {
    if (access(path, F_OK) != 0) throw IOException("File does not exist: $path")
    if (remove(path) != 0) {
        throw IOException("Failed to delete file $path: ${strerror(errno)}")
    }
}