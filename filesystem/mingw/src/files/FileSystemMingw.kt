/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.files

import kotlinx.cinterop.*
import kotlinx.io.IOException
import platform.posix.*
import platform.windows.*

internal actual fun atomicMoveImpl(source: Path, destination: Path) {
    if (MoveFileExA(source.path, destination.path, MOVEFILE_REPLACE_EXISTING.convert()) == 0) {
        // TODO: get formatted error message
        throw IOException("Move failed with error code: ${GetLastError()}")
    }
}

internal actual fun dirnameImpl(path: String): String {
    if (!path.contains(UnixPathSeparator) && !path.contains(WindowsPathSeparator)) {
        return ""
    }
    memScoped {
        return dirname(path.cstr.ptr)?.toKString() ?: ""
    }
}

internal actual fun basenameImpl(path: String): String {
    memScoped {
        return basename(path.cstr.ptr)?.toKString() ?: ""
    }
}

internal actual fun isAbsoluteImpl(path: String): Boolean {
    if (path.startsWith(SystemPathSeparator)) return true
    if (path.length > 1 && path[1] == ':') {
        if (path.length == 2) return false
        val next = path[2]
        return next == WindowsPathSeparator || next == SystemPathSeparator
    }
    return PathIsRelativeA(path) == 0
}

internal actual fun mkdirImpl(path: String) {
    if (mkdir(path) != 0) {
        throw IOException("mkdir failed: ${strerror(errno)?.toKString()}")
    }
}

private const val MAX_PATH_LENGTH = 32767

internal actual fun realpathImpl(path: String): String {
    memScoped {
        val buffer = allocArray<CHARVar>(MAX_PATH_LENGTH)
        val len = GetFullPathNameA(path, MAX_PATH_LENGTH.convert(), buffer, null)
        if (len == 0u) throw IllegalStateException()
        return buffer.toKString()
    }
}
