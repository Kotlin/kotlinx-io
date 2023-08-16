/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.io.IOException
import platform.posix.*

internal expect val NativeTempDir: Path

private class NativeFileSystem : FileSystem {
    companion object {
        val Instance = NativeFileSystem()
    }

    override val temporaryDirectory: Path
        get() = NativeTempDir

    override fun exists(path: Path): Boolean {
        return access(path.path, F_OK) == 0
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun delete(path: Path, mustExist: Boolean) {
        if (!exists(path)) {
            if (mustExist) {
                throw IOException("File does not exist: $path")
            }
            return
        }
        if (remove(path.path) != 0) {
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
            p = p.parent()
        }
        paths.asReversed().forEach {
            mkdirImpl(it)
        }
    }

    override fun atomicMove(source: Path, destination: Path) {
        atomicMoveImpl(source, destination)
    }
}

internal expect fun atomicMoveImpl(source: Path, destination: Path)

internal expect fun mkdirImpl(path: String)

public actual val SystemFileSystem: FileSystem
    get() = NativeFileSystem.Instance
