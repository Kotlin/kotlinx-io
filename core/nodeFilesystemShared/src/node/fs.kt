/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.node

internal external interface Fs {
    /**
     * See https://nodejs.org/api/fs.html#fsexistssyncpath
     */
    fun existsSync(path: String): Boolean

    /**
     * See https://nodejs.org/api/fs.html#fsmkdirsyncpath-options
     */
    fun mkdirSync(path: String): Boolean

    /**
     * See https://nodejs.org/api/fs.html#fsrenamesyncoldpath-newpath
     */
    fun renameSync(from: String, to: String)

    /**
     * See https://nodejs.org/api/fs.html#fsrmdirsyncpath-options
     */
    fun rmdirSync(path: String)

    /**
     * See https://nodejs.org/api/fs.html#fsrmsyncpath-options
     */
    fun rmSync(path: String)

    /**
     * See https://nodejs.org/api/fs.html#fsstatsyncpath-options
     */
    fun statSync(path: String): Stats?

    /**
     * See https://nodejs.org/api/fs.html#fsopensyncpath-flags-mode
     */
    fun openSync(path: String, mode: String): Int

    /**
     * See https://nodejs.org/api/fs.html#fsclosesyncfd
     */
    fun closeSync(fd: Int)

    /**
     * See https://nodejs.org/api/fs.html#fsreadfilesyncpath-options
     */
    fun readFileSync(fd: Int, options: String?): Buffer

    /**
     * See https://nodejs.org/api/fs.html#fswritefilesyncfile-data-options
     */
    fun writeFileSync(fd: Int, buffer: Buffer)

    val realpathSync: realpathSync

    val constants: constants
}

/**
 * Partial declaration of a class mirroring [node:fs.Stats](https://nodejs.org/api/fs.html#class-fsstats)
 */
internal external interface Stats {
    val mode: Int
    val size: Int
    fun isDirectory(): Boolean
}

/**
 * See https://nodejs.org/api/fs.html#fs-constants
 */
internal external interface constants {
    val S_IFREG: Int
    val S_IFDIR: Int
    val S_IFMT: Int
}

/**
 * See https://nodejs.org/api/fs.html#fsrealpathsyncnativepath-options
 */
internal external interface realpathSync {
    fun native(path: String): String
}

internal expect val fs: Fs
