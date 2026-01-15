/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package kotlinx.io.files

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.io.IOException
import platform.posix.*

internal actual fun existsImpl(path: String): Boolean = access(path, F_OK) == 0

@OptIn(ExperimentalForeignApi::class)
internal actual fun deleteNoCheckImpl(path: String) {
    if (remove(path) != 0) {
        if (errno == EACCES) {
            if (rmdir(path) == 0) return
        }
        throw IOException("Delete failed for $path: ${strerror(errno)?.toKString()}")
    }
}
