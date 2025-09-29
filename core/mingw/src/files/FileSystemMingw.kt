/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.files

import kotlinx.cinterop.Arena
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.wcstr
import kotlinx.io.IOException
import platform.posix.size_t
import platform.windows.CloseHandle
import platform.windows.CreateDirectoryW
import platform.windows.ERROR_FILE_NOT_FOUND
import platform.windows.ERROR_NO_MORE_FILES
import platform.windows.FALSE
import platform.windows.FindClose
import platform.windows.FindFirstFileW
import platform.windows.FindNextFileW
import platform.windows.GetFullPathNameW
import platform.windows.GetLastError
import platform.windows.GetProcAddress
import platform.windows.HANDLE
import platform.windows.HMODULE
import platform.windows.HRESULT
import platform.windows.INVALID_HANDLE_VALUE
import platform.windows.LoadLibraryW
import platform.windows.MAX_PATH
import platform.windows.MOVEFILE_REPLACE_EXISTING
import platform.windows.MoveFileExW
import platform.windows.PWSTR
import platform.windows.PathFindFileNameW
import platform.windows.PathIsRelativeW
import platform.windows.PathIsRootW
import platform.windows.TRUE
import platform.windows.WCHARVar
import platform.windows.WIN32_FIND_DATAW
import kotlin.experimental.ExperimentalNativeApi

private typealias PathCchRemoveFileSpecFunc = CPointer<CFunction<(PWSTR, size_t) -> HRESULT>>

@OptIn(ExperimentalNativeApi::class)
private val kernelBaseDll = LoadLibraryW("kernelbase.dll") ?: run {
    terminateWithUnhandledException(RuntimeException("kernelbase.dll is not supported: ${formatWin32ErrorMessage()}"))
}

@OptIn(ExperimentalNativeApi::class)
private fun <T : CPointed> getProcAddressOrFailed(module: HMODULE, name: String): CPointer<T> {
    val pointer = GetProcAddress(kernelBaseDll, name) ?: terminateWithUnhandledException(
        UnsupportedOperationException("Failed to get proc: $name: ${formatWin32ErrorMessage()}"),
    )
    return pointer.reinterpret()
}

// Available since Windows 8 / Windows Server 2012, long path and UNC path supported
private val PathCchRemoveFileSpec: PathCchRemoveFileSpecFunc =
    getProcAddressOrFailed(kernelBaseDll, "PathCchRemoveFileSpec")

internal actual fun atomicMoveImpl(source: Path, destination: Path) {
    if (MoveFileExW(source.path, destination.path, MOVEFILE_REPLACE_EXISTING.convert()) == 0) {
        throw IOException("Move failed with error code: ${formatWin32ErrorMessage()}")
    }
}

internal actual fun dirnameImpl(path: String): String {
    memScoped {
        val p = path.wcstr.ptr
        // we don't care the result, even it failed.
        PathCchRemoveFileSpec.invoke(p, path.length.convert())
        return p.toKString()
    }
}

internal actual fun basenameImpl(path: String): String {
    if (PathIsRootW(path)  == TRUE) return ""
    return PathFindFileNameW(path)?.toKString() ?: ""
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
