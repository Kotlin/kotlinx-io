/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:CommonJsModule("fs")
@file:CommonJsNonModule

package kotlinx.io.node.fs

import kotlinx.io.CommonJsModule
import kotlinx.io.CommonJsNonModule
import kotlinx.io.node.buffer.Buffer

/**
 * See https://nodejs.org/api/fs.html#fsexistssyncpath
 */
internal external fun existsSync(path: String): Boolean

/**
 * See https://nodejs.org/api/fs.html#fsmkdirsyncpath-options
 */
internal external fun mkdirSync(path: String): Boolean

/**
 * See https://nodejs.org/api/fs.html#fsrenamesyncoldpath-newpath
 */
internal external fun renameSync(from: String, to: String)

/**
 * See https://nodejs.org/api/fs.html#fsrmdirsyncpath-options
 */
internal external fun rmdirSync(path: String)

/**
 * See https://nodejs.org/api/fs.html#fsrmsyncpath-options
 */
internal external fun rmSync(path: String)

/**
 * See https://nodejs.org/api/fs.html#fsstatsyncpath-options
 */
internal external fun statSync(path: String): Stats?

/**
 * See https://nodejs.org/api/fs.html#fsopensyncpath-flags-mode
 */
internal external fun openSync(path: String, mode: String): Int

/**
 * See https://nodejs.org/api/fs.html#fsclosesyncfd
 */
internal external fun closeSync(fd: Int)

/**
 * See https://nodejs.org/api/fs.html#fsreadfilesyncpath-options
 */
internal external fun readFileSync(fd: Int, options: String?): Buffer

/**
 * See https://nodejs.org/api/fs.html#fswritefilesyncfile-data-options
 */
internal external fun writeFileSync(fd: Int, buffer: Buffer)

/**
 * Partial declaration of a class mirroring [node:fs.Stats](https://nodejs.org/api/fs.html#class-fsstats)
 */
internal open external class Stats {
    val mode: Int
    val size: Int
    fun isDirectory(): Boolean
}

/**
 * See https://nodejs.org/api/fs.html#fs-constants
 */
internal external object constants {
    val S_IFREG: Int
    val S_IFDIR: Int
    val S_IFMT: Int
}

/**
 * See https://nodejs.org/api/fs.html#fsrealpathsyncnativepath-options
 */
internal external object realpathSync {
    fun native(path: String): String
}
