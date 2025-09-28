/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.files

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.io.IOException
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSFileType
import platform.Foundation.NSFileTypeDirectory
import platform.Foundation.NSFileTypeRegular
import platform.Foundation.NSTemporaryDirectory
import platform.posix.DIR
import platform.posix.basename
import platform.posix.closedir
import platform.posix.dirname
import platform.posix.errno
import platform.posix.free
import platform.posix.mkdir
import platform.posix.realpath
import platform.posix.rename
import platform.posix.strerror


internal actual fun atomicMoveImpl(source: Path, destination: Path) {
    if (rename(source.path, destination.path) != 0) {
        throw IOException("Move failed: ${strerror(errno)?.toKString()}")
    }
}

public actual val SystemTemporaryDirectory: Path
    get() = Path(NSTemporaryDirectory())

internal actual fun dirnameImpl(path: String): String {
    if (!path.contains(SystemPathSeparator)) {
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

internal actual fun isAbsoluteImpl(path: String): Boolean = path.startsWith('/')

internal actual fun mkdirImpl(path: String) {
    if (mkdir(path, PermissionAllowAll) != 0) {
        throw IOException("mkdir failed: ${strerror(errno)?.toKString()}")
    }
}

internal actual fun realpathImpl(path: String): String {
    val res = realpath(path, null) ?: throw IllegalStateException()
    try {
        return res.toKString()
    } finally {
        free(res)
    }
}

internal actual fun metadataOrNullImpl(path: Path): FileMetadata? {
    val attributes = NSFileManager.defaultManager().fileAttributesAtPath(path.path, traverseLink = true) ?: return null
    val fileType = attributes[NSFileType] as String
    val isFile = fileType == NSFileTypeRegular
    val isDir = fileType == NSFileTypeDirectory
    return FileMetadata(
        isRegularFile = isFile,
        isDirectory = isDir,
        size = if (isFile) attributes[NSFileSize] as Long else -1
    )
}

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

