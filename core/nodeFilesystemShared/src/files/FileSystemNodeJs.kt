/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.IOException
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.node.fs.*
import kotlinx.io.node.os.platform
import kotlinx.io.node.os.tmpdir

public actual val SystemFileSystem: FileSystem = object : SystemFileSystemImpl() {
    override fun exists(path: Path): Boolean {
        return existsSync(path.path)
    }

    override fun delete(path: Path, mustExist: Boolean) {
        if (!exists(path)) {
            if (mustExist) {
                throw FileNotFoundException("File does not exist: ${path.path}")
            }
            return
        }
        try {
            val stats = statSync(path.path)
            if (stats!!.isDirectory()) {
                rmdirSync(path.path)
            } else {
                rmSync(path.path)
            }
        } catch (t: Throwable) {
            throw IOException("Delete failed for $path", t)
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

        val parts = arrayListOf<String>()
        var p: Path? = path
        while (p != null && !exists(p)) {
            parts.add(p.toString())
            p = p.parent
        }
        parts.asReversed().forEach {
            mkdirSync(it)
        }
    }

    override fun atomicMove(source: Path, destination: Path) {
        if (!exists(source)) {
            throw FileNotFoundException("Source does not exist: ${source.path}")
        }
        try {
            renameSync(source.path, destination.path)
        } catch (t: Throwable) {
            throw IOException("Move failed from $source to $destination", t)
        }
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        if (!exists(path)) return null
        return try {
            val stat = statSync(path.path)
            val mode = stat!!.mode
            val isFile = (mode and constants.S_IFMT) == constants.S_IFREG
            FileMetadata(
                isRegularFile = isFile,
                isDirectory = (mode and constants.S_IFMT) == constants.S_IFDIR,
                if (isFile) stat.size.toLong() else -1L
            )
        } catch(t: Throwable) {
            throw IOException("Stat failed for $path", t)
        }
    }

    override fun source(path: Path): RawSource {
        return FileSource(path)
    }

    override fun sink(path: Path, append: Boolean): RawSink {
        return FileSink(path, append)
    }

    override fun resolve(path: Path): Path {
        if (!exists(path)) throw FileNotFoundException(path.path)
        return Path(realpathSync.native(path.path))
    }
}

public actual val SystemTemporaryDirectory: Path
    get() {
        return Path(tmpdir() ?: "")
    }

public actual open class FileNotFoundException actual constructor(
    message: String?,
) : IOException(message)

internal actual val isWindows = platform() == "win32"
