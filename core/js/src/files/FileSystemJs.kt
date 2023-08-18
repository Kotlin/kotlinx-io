/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.IOException

internal val fs: dynamic
    get(): dynamic {
        return try {
            js("require('fs')")
        } catch (t: Throwable) {
            null
        }
    }

internal val os: dynamic
    get(): dynamic {
        return try {
            js("require('os')")
        } catch (t: Throwable) {
            null
        }
    }

private class JsFileSystem : SystemFileSystemImpl() {
    companion object {
        val Instance = JsFileSystem()
    }

    override val temporaryDirectory: Path
        get() {
            check(os !== null) { "Module 'os' was not found" }
            return Path(os.tmpdir() as? String ?: "")
        }

    override fun exists(path: Path): Boolean {
        check(fs !== null) { "Module 'fs' was not found" }
        return fs.existsSync(path.path) as Boolean
    }

    override fun delete(path: Path, mustExist: Boolean) {
        check(fs !== null) { "Module 'fs' was not found" }
        if (!exists(path)) {
            if (mustExist) {
                throw FileNotFoundException("File does not exist: ${path.path}")
            }
            return
        }
        try {
            val stats = fs.statSync(path.path)
            if (stats.isDirectory() as Boolean) {
                fs.rmdirSync(path.path)
            } else {
                fs.rmSync(path.path)
            }
        } catch (t: Throwable) {
            throw IOException("Delete failed for $path", t)
        }
    }

    override fun createDirectories(path: Path, mustCreate: Boolean) {
        if (exists(path)) {
            if (mustCreate) {
                throw IOException("Path already exists: $path")
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
        check(fs !== null) { "Module 'fs' was not found" }
        if (!exists(source)) {
            throw FileNotFoundException("Source does not exist: ${source.path}")
        }
        try {
            fs.renameSync(source.path, destination.path)
        } catch (t: Throwable) {
            throw IOException("Move failed from $source to $destination", t)
        }
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        check(fs !== null) { "Module 'fs' was not found" }
        return try {
            val stat = fs.statSync(path.path)
            val mode = stat.mode as Int
            FileMetadata(
                isRegularFile = (mode and fs.constants.S_IFMT as Int) == fs.constants.S_IFREG,
                isDirectory = (mode and fs.constants.S_IFMT as Int) == fs.constants.S_IFDIR
            )
        } catch (t: Throwable) {
            if (exists(path)) throw IOException("Stat failed for $path", t)
            return null
        }
    }
}

internal actual val SystemFileSystem: FileSystem = JsFileSystem.Instance

public actual open class FileNotFoundException actual constructor(
    message: String?,
) : IOException(message)
