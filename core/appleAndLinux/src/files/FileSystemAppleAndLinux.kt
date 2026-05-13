/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.toKString
import kotlinx.io.IOException
import platform.posix.DIR
import platform.posix.closedir
import platform.posix.errno
import platform.posix.opendir
import platform.posix.strerror


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
    val dirent = opendir(path)
    if (dirent != null) return OpaqueDirEntry(dirent)

    val err = errno
    val strerr = strerror(err)?.toKString() ?: "unknown error"
    throw IOException("Can't open directory $path: $err ($strerr)")
}
