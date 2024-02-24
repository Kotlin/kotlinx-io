/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node.fs

import kotlinx.io.node.buffer.Buffer

/**
 * See https://nodejs.org/api/fs.html#fsexistssyncpath
 */
internal expect fun existsSync(path: String): Boolean

/**
 * See https://nodejs.org/api/fs.html#fsmkdirsyncpath-options
 */
internal expect fun mkdirSync(path: String)

/**
 * See https://nodejs.org/api/fs.html#fsrenamesyncoldpath-newpath
 */
internal expect fun renameSync(from: String, to: String)

/**
 * See https://nodejs.org/api/fs.html#fsrmdirsyncpath-options
 */
internal expect fun rmdirSync(path: String)

/**
 * See https://nodejs.org/api/fs.html#fsrmsyncpath-options
 */
internal expect fun rmSync(path: String)

/**
 * See https://nodejs.org/api/fs.html#fsstatsyncpath-options
 */
internal expect fun statSync(path: String): Stats?

/**
 * See https://nodejs.org/api/fs.html#fsopensyncpath-flags-mode
 */
internal expect fun openSync(path: String, mode: String): Int

/**
 * See https://nodejs.org/api/fs.html#fsclosesyncfd
 */
internal expect fun closeSync(fd: Int)

/**
 * See https://nodejs.org/api/fs.html#fsreadfilesyncpath-options
 */
internal expect fun readFileSync(fd: Int, options: String?): Buffer

/**
 * See https://nodejs.org/api/fs.html#fswritefilesyncfile-data-options
 */
internal expect fun writeFileSync(fd: Int, buffer: Buffer)

/**
 * Partial declaration of a class mirroring [node:fs.Stats](https://nodejs.org/api/fs.html#class-fsstats)
 */
internal expect class Stats {
    val mode: Int
    val size: Int
    fun isDirectory(): Boolean
}

/**
 * See https://nodejs.org/api/fs.html#fs-constants
 */
internal expect val S_IFREG: Int
internal expect val S_IFDIR: Int
internal expect val S_IFMT: Int

/**
 * See https://nodejs.org/api/fs.html#fsrealpathsyncnativepath-options
 */
internal expect fun realpathSyncNative(path: String): String
