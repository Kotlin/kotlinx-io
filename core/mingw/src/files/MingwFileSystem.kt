/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.files

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toKString
import kotlinx.io.IOException
import platform.posix.dirname
import platform.posix.errno
import platform.posix.mkdir
import platform.posix.strerror
import platform.windows.GetLastError
import platform.windows.MOVEFILE_REPLACE_EXISTING
import platform.windows.MoveFileExA

internal actual fun atomicMoveImpl(source: Path, destination: Path) {
    if (MoveFileExA(source.path, destination.path, MOVEFILE_REPLACE_EXISTING.convert()) == 0) {
        // TODO: get formatted error message
        throw IOException("Move failed with error code: ${GetLastError()}")
    }
}

internal actual fun dirnameImpl(path: String): String {
    return dirname(path.cstr)?.toKString() ?: ""
}

internal actual fun mkdirImpl(path: String) {
    if (mkdir(path) != 0) {
        throw IOException("mkdir failed: ${strerror(errno)?.toKString()}")
    }
}
