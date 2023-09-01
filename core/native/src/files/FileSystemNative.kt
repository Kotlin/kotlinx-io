/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.cinterop.*
import kotlinx.io.IOException
import platform.posix.*

internal actual val SystemFileSystem: FileSystem = object : SystemFileSystemImpl() {
    override fun exists(path: Path): Boolean {
        return access(path.path, F_OK) == 0
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun delete(path: Path, mustExist: Boolean) {
        if (!exists(path)) {
            if (mustExist) {
                throw FileNotFoundException("File does not exist: $path")
            }
            return
        }
        if (remove(path.path) != 0) {
            if (errno == EACCES) {
                if (rmdir(path.path) == 0) return
            }
            throw IOException("Delete failed for $path: ${strerror(errno)?.toKString()}")
        }
    }

    override fun createDirectories(path: Path, mustCreate: Boolean) {
        if (exists(path)) {
            if (mustCreate) {
                throw IOException("Path already exists: $path")
            }
            return
        }
        val paths = arrayListOf<String>()
        var p: Path? = path
        while (p != null && !exists(p)) {
            paths.add(p.toString())
            p = p.parent
        }
        paths.asReversed().forEach {
            mkdirImpl(it)
        }
    }

    override fun atomicMove(source: Path, destination: Path) {
        if (!exists(source)) {
            throw FileNotFoundException("Source does not exist: ${source.path}")
        }
        atomicMoveImpl(source, destination)
    }

    @OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
    override fun metadataOrNull(path: Path): FileMetadata? {
        memScoped {
            val struct_stat = alloc<stat>()
            if (stat(path.path, struct_stat.ptr) != 0) {
                if (errno == ENOENT) return null
                throw IOException("stat failed to ${path.path}: ${strerror(errno)?.toKString()}")
            }
            val mode = struct_stat.st_mode.toInt()
            val isFile = (mode and S_IFMT) == S_IFREG
            return FileMetadata(
                isRegularFile = isFile,
                isDirectory = (mode and S_IFMT) == S_IFDIR,
                if (isFile) struct_stat.st_size.toLong() else -1L
            )
        }
    }
}

internal expect fun atomicMoveImpl(source: Path, destination: Path)

internal expect fun mkdirImpl(path: String)

public actual open class FileNotFoundException actual constructor(
    message: String?
) : IOException(message)
