/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.files

import kotlinx.cinterop.*
import kotlinx.io.IOException
import kotlinx.io.internal.winapi.PathCchIsRoot
import kotlinx.io.internal.winapi.PathCchRemoveFileSpec
import platform.posix.wmemcpy
import platform.windows.*


internal actual fun atomicMoveImpl(source: Path, destination: Path) {
    if (MoveFileExW(source.path, destination.path, MOVEFILE_REPLACE_EXISTING.convert()) == 0) {
        throw IOException("Move failed with error code: ${formatWin32ErrorMessage()}")
    }
}

internal actual fun dirnameImpl(path: String): String {
    val path = path.replace(UnixPathSeparator, WindowsPathSeparator)
    val len = path.length + 1 /* it is null-terminated */
    memScoped {
        val buffer = allocArray<UShortVarOf<UShort>>(len)
        wmemcpy(buffer, path.wcstr, len.convert())
        PathCchRemoveFileSpec(buffer, len.convert())
        return buffer.toKString()
    }
}

internal actual fun basenameImpl(path: String): String {
    memScoped {
        if (PathCchIsRoot(path.wcstr.ptr) == TRUE) return ""
    }
    return PathFindFileNameW(path)?.toKString() ?: ""
}

internal actual fun isAbsoluteImpl(path: String): Boolean {
    val p = path.replace(UnixPathSeparator, WindowsPathSeparator)
    if (PathIsRelativeW(p) == TRUE) {
        return false
    }
    // PathIsRelativeW returns FALSE for paths like "C:relative\path" which are not absolute, in DoS
    if (p.length >= 2 && p[0].isLetter() && p[1] == ':') {
        return p.length > 2 && (p[2] == WindowsPathSeparator || p[2] == UnixPathSeparator)
    }
    return true
}

internal actual fun mkdirImpl(path: String) {
    if (CreateDirectoryW(path, null) == FALSE) {
        throw IOException("mkdir failed: $path: ${formatWin32ErrorMessage()}")
    }
}

internal actual fun realpathImpl(path: String): String {
    memScoped {
        // in practice, MAX_PATH is enough for most cases
        var buf = allocArray<WCHARVar>(MAX_PATH)
        var r = GetFullPathNameW(path, MAX_PATH.convert(), buf, null)
        if (r >= MAX_PATH.toUInt()) {
            // if not, we will retry with the required size
            buf = allocArray<WCHARVar>(r.toInt())
            r = GetFullPathNameW(path, r, buf, null)
        }
        if (r == 0u) {
            error("GetFullPathNameW failed for $path: ${formatWin32ErrorMessage()}")
        }
        return buf.toKString()
    }
}

internal actual class OpaqueDirEntry(private val directory: String) : AutoCloseable {
    private val arena = Arena()
    private val data = arena.alloc<WIN32_FIND_DATAW>()
    private var handle: HANDLE? = INVALID_HANDLE_VALUE
    private var firstName: String? = null

    init {
        try {
            // since the root
            val directory0 =
                if (directory.endsWith(UnixPathSeparator) || directory.endsWith(WindowsPathSeparator)) "$directory*" else "$directory/*"
            handle = FindFirstFileW(directory0, data.ptr)
            if (handle != INVALID_HANDLE_VALUE) {
                firstName = data.cFileName.toKString()
            } else {
                val e = GetLastError()
                if (e != ERROR_FILE_NOT_FOUND.toUInt()) {
                    throw IOException("Can't open directory $directory: ${formatWin32ErrorMessage(e)}")
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
        throw IOException("Can't readdir from $directory: ${formatWin32ErrorMessage(le)}")
    }

    actual override fun close() {
        if (handle != INVALID_HANDLE_VALUE) {
            FindClose(handle)
        }
        arena.clear()
    }

}

internal actual fun opendir(path: String): OpaqueDirEntry = OpaqueDirEntry(path)

internal actual fun existsImpl(path: String): Boolean = PathFileExistsW(path) == TRUE

internal actual fun deleteNoCheckImpl(path: String) {
    if (DeleteFileW(path) != FALSE) return
    var e = GetLastError()
    if (e == ERROR_FILE_NOT_FOUND.toUInt()) return // ignore it
    if (e == ERROR_ACCESS_DENIED.toUInt()) {
        // might be a directory
        if (RemoveDirectoryW(path) != FALSE) return
        e = GetLastError()
        if (e == ERROR_FILE_NOT_FOUND.toUInt()) return // ignore it
    }
    throw IOException("Delete failed for $path: ${formatWin32ErrorMessage(e)}")
}
