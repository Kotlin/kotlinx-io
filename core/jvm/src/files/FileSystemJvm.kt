/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.*
import java.io.FileInputStream
import java.io.FileOutputStream
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
        try {
            val _ = Files.move(
                source.file.toPath(), destination.file.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
            )
        } catch (e: Throwable) {
            if (e is IOException) throw e
            throw IOException("Move failed", e)
        }
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

@JvmField
public actual val SystemFileSystem: FileSystem = object : SystemFileSystemImpl() {

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
        if (!path.file.mkdirs()) {
            if (mustCreate) {
                throw IOException("Path already exist: $path")
            }
            if (path.file.isFile) {
                throw IOException("Path already exists and it's a file: $path")
            }
        }
    }

    override fun atomicMove(source: Path, destination: Path) {
        mover.move(source, destination)
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        if (!path.file.exists()) return null
        return FileMetadata(path.file.isFile, path.file.isDirectory,
            if (path.file.isFile) path.file.length() else -1L)
    }

    override fun source(path: Path): RawSource = FileInputStream(path.file).asSource()

    override fun sink(path: Path, append: Boolean): RawSink = FileOutputStream(path.file, append).asSink()

    override fun resolve(path: Path): Path {
        if (!path.file.exists()) throw FileNotFoundException(path.file.absolutePath)
        return Path(path.file.canonicalFile)
    }

    override fun list(directory: Path): Collection<Path> {
        val file = directory.file
        if (!file.exists()) throw FileNotFoundException(file.absolutePath)
        if (!file.isDirectory) throw IOException("Not a directory: ${file.absolutePath}")
        return buildList {
            file.list()?.forEach { childName ->
                add(Path(directory, childName))
            }
        }
    }
}

@JvmField
public actual val SystemTemporaryDirectory: Path = Path(System.getProperty("java.io.tmpdir"))

public actual typealias FileNotFoundException = java.io.FileNotFoundException

internal actual val isWindows: Boolean = System.getProperty("os.name")?.startsWith("Windows") ?: false
