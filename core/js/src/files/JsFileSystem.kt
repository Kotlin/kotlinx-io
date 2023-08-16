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

internal actual val SystemFileSystem: FileSystem = object : FileSystem {
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
                throw IOException("File does not exist: ${path.path}")
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
            throw IOException("Delete failed", t)
        }
    }

    override fun createDirectories(path: Path, mustCreate: Boolean) {
        if (exists(path)) {
            if (mustCreate) {
                throw IOException("Path already exists: ${path}")
            }
            return
        }

        val parts = arrayListOf<String>()
        var p: Path? = path
        while (p != null && !exists(p)) {
            parts.add(p.toString())
            p = p.parent()
        }
        parts.asReversed().forEach {
            fs.mkdirSync(it)
        }
    }

    override fun atomicMove(source: Path, destination: Path) {
        check(fs !== null) { "Module 'fs' was not found" }
        try {
            fs.renameSync(source.path, destination.path)
        } catch (t: Throwable) {
            throw IOException("Move failed", t)
        }
    }

}
