/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node.fs

import kotlinx.io.node.buffer.Buffer
import kotlinx.io.node.loadModule

private val fsModule: dynamic by lazy {
    loadModule("fs") { js("require('fs')") }
}

/**
 * See https://nodejs.org/api/fs.html#fsexistssyncpath
 */
internal actual fun existsSync(path: String): Boolean = fsModule.existsSync(path) as Boolean

/**
 * See https://nodejs.org/api/fs.html#fsmkdirsyncpath-options
 */
internal actual fun mkdirSync(path: String) = fsModule.mkdirSync(path)

/**
 * See https://nodejs.org/api/fs.html#fsrenamesyncoldpath-newpath
 */
internal actual fun renameSync(from: String, to: String) {
    fsModule.renameSync(from, to)
}

/**
 * See https://nodejs.org/api/fs.html#fsrmdirsyncpath-options
 */
internal actual fun rmdirSync(path: String) {
    fsModule.rmdirSync(path)
}

/**
 * See https://nodejs.org/api/fs.html#fsrmsyncpath-options
 */
internal actual fun rmSync(path: String) {
    fsModule.rmSync(path)
}

/**
 * See https://nodejs.org/api/fs.html#fsstatsyncpath-options
 */
internal actual fun statSync(path: String): Stats? {
    val stats = fsModule.statSync(path) ?: return null
    return Stats(stats)
}

/**
 * See https://nodejs.org/api/fs.html#fsopensyncpath-flags-mode
 */
internal actual fun openSync(path: String, mode: String): Int = fsModule.openSync(path, mode) as Int

/**
 * See https://nodejs.org/api/fs.html#fsclosesyncfd
 */
internal actual fun closeSync(fd: Int) {
    fsModule.closeSync(fd)
}

/**
 * See https://nodejs.org/api/fs.html#fsreadfilesyncpath-options
 */
internal actual fun readFileSync(fd: Int, options: String?): Buffer {
    return Buffer(fsModule.readFileSync(fd, options))
}

/**
 * See https://nodejs.org/api/fs.html#fswritefilesyncfile-data-options
 */
internal actual fun writeFileSync(fd: Int, buffer: Buffer) {
    fsModule.writeFileSync(fd, buffer.buffer)
}

/**
 * Partial declaration of a class mirroring [node:fs.Stats](https://nodejs.org/api/fs.html#class-fsstats)
 */
internal actual class Stats(val stats: dynamic) {
    actual val mode: Int
        get() = stats.mode as Int
    actual val size: Int
        get() = stats.size as Int

    actual fun isDirectory(): Boolean = stats.isDirectory() as Boolean
}

/**
 * See https://nodejs.org/api/fs.html#fs-constants
 */
internal actual val S_IFREG: Int by lazy { fsModule.constants.S_IFREG as Int }
internal actual val S_IFDIR: Int by lazy { fsModule.constants.S_IFDIR as Int }
internal actual val S_IFMT: Int by lazy { fsModule.constants.S_IFMT as Int }

/**
 * See https://nodejs.org/api/fs.html#fsrealpathsyncnativepath-options
 */
internal actual fun realpathSyncNative(path: String): String {
    return fsModule.realpathSync.native(path) as String
}
