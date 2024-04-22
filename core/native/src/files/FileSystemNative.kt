/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.cinterop.*
import kotlinx.io.IOException
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import platform.posix.*
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class)
public actual val SystemFileSystem: FileSystem = object : SystemFileSystemImpl() {
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
        val metadata = metadataOrNull(path)
        if (metadata != null) {
            if (mustCreate) {
                throw IOException("Path already exists: $path")
            }
            if (metadata.isRegularFile) {
                throw IOException("Path already exists and it's a file: $path")
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

    override fun metadataOrNull(path: Path): FileMetadata? = metadataOrNullImpl(path)

    override fun resolve(path: Path): Path {
        if (!exists(path)) throw FileNotFoundException(path.path)
        return Path(realpathImpl(path.path))
    }

    override fun source(path: Path): RawSource {
        val openFile: CPointer<FILE>? = fopen(path.path, "rb")
        if (openFile == null) {
            if (errno == ENOENT) {
                throw FileNotFoundException("File does not exist: $path")
            }
            throw IOException("Failed to open $path with ${strerror(errno)?.toKString()}")
        }
        return FileSource(openFile)
    }

    override fun sink(path: Path, append: Boolean): RawSink {
        val openFile: CPointer<FILE> = fopen(path.path, if (append) "ab" else "wb")
            ?: throw IOException("Failed to open $path with ${strerror(errno)?.toKString()}")
        return FileSink(openFile)
    }
}

internal expect fun metadataOrNullImpl(path: Path): FileMetadata?

internal expect fun atomicMoveImpl(source: Path, destination: Path)

internal expect fun mkdirImpl(path: String)

internal expect fun realpathImpl(path: String): String

public actual open class FileNotFoundException actual constructor(
    message: String?
) : IOException(message)

// 777 in octal, rwx for all (owner, group and others).
internal const val PermissionAllowAll: UShort = 511u

@OptIn(ExperimentalNativeApi::class)
internal actual val isWindows: Boolean = Platform.osFamily == OsFamily.WINDOWS
