/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(UnsafeNumber::class, ExperimentalForeignApi::class)

package kotlinx.io.files

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import kotlinx.io.IOException
import platform.posix.*

internal actual fun atomicMoveImpl(source: Path, destination: Path) {
    if (rename(source.path, destination.path) != 0) {
        throw IOException("Move failed: ${strerror(errno)?.toKString()}")
    }
}

internal actual fun realpathImpl(path: String): String {
    val result = realpath(path, null) ?: throw IllegalStateException()
    try {
        return result.toKString()
    } finally {
        free(result)
    }
}

internal actual fun mkdirImpl(path: String) {
    if (mkdir(path, PermissionAllowAll.convert()) != 0) {
        throw IOException("mkdir failed: ${strerror(errno)?.toKString()}")
    }
}
