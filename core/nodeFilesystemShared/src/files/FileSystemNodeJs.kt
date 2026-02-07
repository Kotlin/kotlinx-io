/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.IOException
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.node.fs
import kotlinx.io.node.os
import kotlinx.io.withCaughtException
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
public actual val SystemFileSystem: FileSystem = object : SystemFileSystemImpl() {
    override fun exists(path: Path): Boolean {
        return fs.existsSync(path.path)
    }

    override fun delete(path: Path, mustExist: Boolean) {
        if (!exists(path)) {
            if (mustExist) {
                throw FileNotFoundException("File does not exist: $path")
            }
            return
        }
        withCaughtException {
            val stats = fs.statSync(path.path) ?: throw FileNotFoundException("File does not exist: $path")
            if (stats.isDirectory()) {
                fs.rmdirSync(path.path)
            } else {
                fs.rmSync(path.path)
            }
        }?.also {
            throw IOException("Delete failed for $path", it)
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
            fs.mkdirSync(it)
        }
    }

    override fun atomicMove(source: Path, destination: Path) {
        if (!exists(source)) {
            throw FileNotFoundException("Source does not exist: ${source.path}")
        }
        withCaughtException {
            fs.renameSync(source.path, destination.path)
        }?.also {
            throw IOException("Move failed from $source to $destination", it)
        }
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        if (!exists(path)) return null
        var metadata: FileMetadata? = null
        withCaughtException {
            val stat = fs.statSync(path.path) ?: return@withCaughtException
            val mode = stat.mode
            val isFile = (mode and fs.constants.S_IFMT) == fs.constants.S_IFREG
            metadata = FileMetadata(
                isRegularFile = isFile,
                isDirectory = (mode and fs.constants.S_IFMT) == fs.constants.S_IFDIR,
                size = if (isFile) stat.size.toLong() else -1L,
                createdAt = stat.ctimeMs.toInstant(),
                updatedAt = stat.mtimeMs.toInstant(),
            )
        }?.also {
            throw IOException("Stat failed for $path", it)
        }
        return metadata
    }

    override fun source(path: Path): RawSource {
        return FileSource(path)
    }

    override fun sink(path: Path, append: Boolean): RawSink {
        return FileSink(path, append)
    }

    override fun resolve(path: Path): Path {
        if (!exists(path)) throw FileNotFoundException(path.path)
        return Path(fs.realpathSync.native(path.path))
    }

    override fun list(directory: Path): Collection<Path> {
        val metadata = metadataOrNull(directory) ?: throw FileNotFoundException(directory.path)
        if (!metadata.isDirectory) throw IOException("Not a directory: ${directory.path}")
        val dir = fs.opendirSync(directory.path) ?: throw IOException("Unable to read directory: ${directory.path}")
        try {
            return buildList {
                var child = dir.readSync()
                while (child != null) {
                    add(Path(directory, child.name))
                    child = dir.readSync()
                }
            }
        } finally {
            dir.closeSync()
        }
    }
}

public actual val SystemTemporaryDirectory: Path
    get() {
        return Path(os.tmpdir() ?: "")
    }

public actual open class FileNotFoundException actual constructor(
    message: String?,
) : IOException(message)

internal actual val isWindows = os.platform() == "win32"

@OptIn(ExperimentalTime::class)
private fun Double.toInstant(): Instant {
    val epoch = this * 0.001
    val seconds = epoch.toLong()
    val nanos = (epoch - seconds).times(1e9).toLong()
    return Instant.fromEpochSeconds(seconds, nanos)
}