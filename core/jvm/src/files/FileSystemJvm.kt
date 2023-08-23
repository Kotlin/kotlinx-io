/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal annotation class AnimalSnifferIgnore()

private interface Mover {
    fun move(source: Path, destination: Path)
}

private class NioMover : Mover {
    @AnimalSnifferIgnore
    override fun move(source: Path, destination: Path) {
        if (!source.file.exists()) {
            throw FileNotFoundException("Source file does not exist: ${source.file}")
        }
        Files.move(
            source.file.toPath(), destination.file.toPath(),
            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
        )
    }
}

private val mover: Mover by lazy {
    try {
        Class.forName("java.nio.file.Files")
        NioMover()
    } catch (e: ClassNotFoundException) {
        object : Mover {
            override fun move(source: Path, destination: Path) {
                throw UnsupportedOperationException("Atomic move not supported")
            }
        }
    }
}

private class JvmFileSystem : SystemFileSystemImpl() {
    companion object {
        val Instance = JvmFileSystem()
    }

    override val temporaryDirectory: Path
        get() = Path(System.getProperty("java.io.tmpdir"))

    override fun exists(path: Path): Boolean {
        return path.file.exists()
    }

    override fun delete(path: Path, mustExist: Boolean) {
        if (!exists(path)) {
            if (mustExist) {
                throw FileNotFoundException("File does not exist: ${path.file}")
            }
            return
        }
        if (!path.file.delete()) {
            throw IOException("Deletion failed")
        }
    }

    override fun createDirectories(path: Path, mustCreate: Boolean) {
        if (!path.file.mkdirs() && mustCreate) {
            throw IOException("Path already exist: ${path}")
        }
    }

    override fun atomicMove(source: Path, destination: Path) {
        mover.move(source, destination)
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        if (!path.file.exists()) return null
        return FileMetadata(path.file.isFile, path.file.isDirectory)
    }
}

internal actual val SystemFileSystem: FileSystem = JvmFileSystem.Instance

public actual typealias FileNotFoundException = java.io.FileNotFoundException
