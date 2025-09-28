/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.files

import kotlinx.cinterop.Arena
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.io.IOException
import platform.posix.basename
import platform.posix.dirname
import platform.posix.errno
import platform.posix.mkdir
import platform.posix.strerror
import platform.windows.CloseHandle
import platform.windows.ERROR_FILE_NOT_FOUND
import platform.windows.ERROR_NO_MORE_FILES
import platform.windows.FindClose
import platform.windows.FindFirstFileW
import platform.windows.FindNextFileW
import platform.windows.GetFullPathNameW
import platform.windows.GetLastError
import platform.windows.HANDLE
import platform.windows.INVALID_HANDLE_VALUE
import platform.windows.MOVEFILE_REPLACE_EXISTING
import platform.windows.MoveFileExA
import platform.windows.PathIsRelativeW
import platform.windows.TRUE
import platform.windows.WCHARVar
import platform.windows.WIN32_FIND_DATAW

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
    return PathIsRelativeW(path) == 0
}

internal actual fun mkdirImpl(path: String) {
    if (mkdir(path) != 0) {
        throw IOException("mkdir failed: ${strerror(errno)?.toKString()}")
    }
}

private const val MAX_PATH_LENGTH = 32767

internal actual fun realpathImpl(path: String): String {
    memScoped {
        val buffer = allocArray<WCHARVar>(MAX_PATH_LENGTH)
        val len = GetFullPathNameW(path, MAX_PATH_LENGTH.convert(), buffer, null)
        if (len == 0u) throw IllegalStateException()
        return buffer.toKString()
    }
}

internal actual class OpaqueDirEntry(directory: String) : AutoCloseable {
    private val arena = Arena()
    private val data = arena.alloc<WIN32_FIND_DATAW>()
    private var handle: HANDLE? = INVALID_HANDLE_VALUE
    private var firstName: String? = null

    init {
        try {
            val directory0 = if (directory.endsWith('/') || directory.endsWith('\\')) "$directory*" else "$directory/*"
            handle = FindFirstFileW(directory0, data.ptr)
            if (handle != INVALID_HANDLE_VALUE) {
                firstName = data.cFileName.toKString()
            } else {
                val le = GetLastError()
                if (le != ERROR_FILE_NOT_FOUND.toUInt()) {
                    val strerr = formatWin32ErrorMessage(le)
                    throw IOException("Can't open directory $directory: $le ($strerr)")
                }
            }
        } catch (th: Throwable) {
            if (handle != INVALID_HANDLE_VALUE) {
                CloseHandle(handle)
            }
            arena.clear()
            throw th
        }
    }

    actual fun readdir(): String? {
        if (firstName != null) {
            return firstName.also { firstName = null }
        }
        if (handle == INVALID_HANDLE_VALUE) {
            return null
        }
        if (FindNextFileW(handle, data.ptr) == TRUE) {
            return data.cFileName.toKString()
        }
        val le = GetLastError()
        if (le == ERROR_NO_MORE_FILES.toUInt()) {
            return null
        }
        val strerr = formatWin32ErrorMessage(le)
        throw IOException("Can't readdir: $le ($strerr)")
    }

    actual override fun close() {
        if (handle != INVALID_HANDLE_VALUE) {
            FindClose(handle)
        }
        arena.clear()
    }

}

internal actual fun opendir(path: String): OpaqueDirEntry = OpaqueDirEntry(path)
