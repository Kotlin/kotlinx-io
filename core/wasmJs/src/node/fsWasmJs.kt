/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node.fs

import kotlinx.io.node.buffer.Buffer
import kotlinx.io.node.loadModule

private val fsModule: JsAny by lazy {
    loadModule("fs")
}

/**
 * See https://nodejs.org/api/fs.html#fsexistssyncpath
 */
internal actual fun existsSync(path: String): Boolean = existsSync(path, fsModule)

private fun existsSync(path: String, mod: JsAny): Boolean = js("mod.existsSync(path)")

/**
 * See https://nodejs.org/api/fs.html#fsmkdirsyncpath-options
 */
internal actual fun mkdirSync(path: String): Unit = mkdirSync(path, fsModule)

private fun mkdirSync(path: String, mod: JsAny): Unit = js("mod.mkdirSync(path)")

/**
 * See https://nodejs.org/api/fs.html#fsrenamesyncoldpath-newpath
 */
internal actual fun renameSync(from: String, to: String): Unit = renameSync(from, to, fsModule)

private fun renameSync(from: String, to: String, mod: JsAny): Unit = js("mod.renameSync(from, to)")

/**
 * See https://nodejs.org/api/fs.html#fsrmdirsyncpath-options
 */
internal actual fun rmdirSync(path: String): Unit = rmdirSync(path, fsModule)

private fun rmdirSync(path: String, mod: JsAny): Unit = js("mod.rmdirSync(path)")

/**
 * See https://nodejs.org/api/fs.html#fsrmsyncpath-options
 */
internal actual fun rmSync(path: String): Unit = rmSync(path, fsModule)

private fun rmSync(path: String, mod: JsAny): Unit = js("mod.rmSync(path)")

/**
 * See https://nodejs.org/api/fs.html#fsstatsyncpath-options
 */
internal actual fun statSync(path: String): Stats? {
    val stats = statSync(path, fsModule)
    if (stats == null) return null
    return Stats(stats)
}
private fun statSync(path: String, mod: JsAny): JsAny? = js("mod.statSync(path)")

/**
 * See https://nodejs.org/api/fs.html#fsopensyncpath-flags-mode
 */
internal actual fun openSync(path: String, mode: String): Int = openSync(path, mode, fsModule)

private fun openSync(path: String, mode: String, mod: JsAny): Int = js("mod.openSync(path, mode)")

/**
 * See https://nodejs.org/api/fs.html#fsclosesyncfd
 */
internal actual fun closeSync(fd: Int): Unit = closeSync(fd, fsModule)

private fun closeSync(fd: Int, mod: JsAny): Unit = js("mod.closeSync(fd)")

/**
 * See https://nodejs.org/api/fs.html#fsreadfilesyncpath-options
 */
internal actual fun readFileSync(fd: Int, options: String?): Buffer {
    return Buffer(readFileSync(fd, options, fsModule))
}

private fun readFileSync(fd: Int, options: String?, mod: JsAny): JsAny = js("mod.readFileSync(fd, options)")

/**
 * See https://nodejs.org/api/fs.html#fswritefilesyncfile-data-options
 */
internal actual fun writeFileSync(fd: Int, buffer: Buffer) {
    writeFileSync(fd, buffer.buffer, fsModule)
}
private fun writeFileSync(fd: Int, buffer: JsAny, mod: JsAny): Unit = js("mod.writeFileSync(fd, buffer)")

/**
 * Partial declaration of a class mirroring [node:fs.Stats](https://nodejs.org/api/fs.html#class-fsstats)
 */
internal actual class Stats(private val stats: JsAny) {
    actual val mode: Int by lazy { mode(stats) }
    actual val size: Int by lazy { size(stats) }
    actual fun isDirectory(): Boolean = isDirectory(stats)
}

private fun mode(stats: JsAny): Int = js("stats.mode")
private fun size(stats: JsAny): Int = js("stats.size")
private fun isDirectory(stats: JsAny): Boolean = js("stats.isDirectory()")

/**
 * See https://nodejs.org/api/fs.html#fs-constants
 */
internal actual val S_IFREG: Int by lazy { S_IFREG(fsModule) }
internal actual val S_IFDIR: Int by lazy { S_IFDIR(fsModule) }
internal actual val S_IFMT: Int by lazy { S_IFMT(fsModule) }

private fun S_IFREG(mod: JsAny): Int = js("mod.constants.S_IFREG")
private fun S_IFDIR(mod: JsAny): Int = js("mod.constants.S_IFDIR")
private fun S_IFMT(mod: JsAny): Int = js("mod.constants.S_IFMT")


/**
 * See https://nodejs.org/api/fs.html#fsrealpathsyncnativepath-options
 */
internal actual fun realpathSyncNative(path: String): String = realpath(path, fsModule)

private fun realpath(path: String, mod: JsAny): String = js("mod.realpathSync.native(path)")
