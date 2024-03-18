/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.cinterop.*
import kotlinx.io.IOException
import platform.posix.*

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

@OptIn(ExperimentalForeignApi::class, ExperimentalStdlibApi::class)
internal actual class OpaqueDirEntry constructor(private val dir: CPointer<DIR>) : AutoCloseable {
    actual fun readdir(): String? {
        val entry = readdir(dir) ?: return null
        return entry[0].d_name.toKString()
    }

    override fun close() {
        closedir(dir)
    }
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun opendir(path: String): OpaqueDirEntry {
    val dirent = platform.posix.opendir(path)
    if (dirent != null) return OpaqueDirEntry(dirent)
    throw IOException("Can't open directory $path: ${strerror(errno)?.toKString() ?: "reason unknown"}")
}
