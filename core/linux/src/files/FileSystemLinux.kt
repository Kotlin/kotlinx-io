/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.io.IOException
import platform.posix.DIR
import platform.posix.__xpg_basename
import platform.posix.closedir
import platform.posix.dirname
import platform.posix.errno
import platform.posix.strerror

@OptIn(ExperimentalForeignApi::class)
internal actual fun dirnameImpl(path: String): String {
    if (!path.contains(SystemPathSeparator)) {
        return ""
    }
    memScoped {
        return dirname(path.cstr.ptr)?.toKString() ?: ""
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun basenameImpl(path: String): String {
    memScoped {
        return __xpg_basename(path.cstr.ptr)?.toKString() ?: ""
    }
}

internal actual fun isAbsoluteImpl(path: String): Boolean = path.startsWith('/')


@OptIn(ExperimentalForeignApi::class)
internal actual class OpaqueDirEntry(private val dir: CPointer<DIR>) : AutoCloseable {
    actual fun readdir(): String? {
        val entry = platform.posix.readdir(dir) ?: return null
        return entry[0].d_name.toKString()
    }

    actual override fun close() {
        if (closedir(dir) != 0) {
            val err = errno
            val strerr = strerror(err)?.toKString() ?: "unknown error"
            throw IOException("closedir failed with errno $err ($strerr)")
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun opendir(path: String): OpaqueDirEntry {
    val dirent = platform.posix.opendir(path)
    if (dirent != null) return OpaqueDirEntry(dirent)

    val err = errno
    val strerr = strerror(err)?.toKString() ?: "unknown error"
    throw IOException("Can't open directory $path: $err ($strerr)")
}
