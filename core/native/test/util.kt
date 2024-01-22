/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package kotlinx.io

import kotlinx.cinterop.UnsafeNumber
import kotlinx.io.files.SystemTemporaryDirectory
import platform.posix.F_OK
import platform.posix.access
import kotlin.random.Random

@OptIn(UnsafeNumber::class, ExperimentalStdlibApi::class)
actual fun tempFileName(): String {
    val tmpDir = SystemTemporaryDirectory.path
    for (i in 0 until 10) {
        val name = Random.nextBytes(32).toHexString()
        val path = "$tmpDir/$name"
        if (access(path, F_OK) != 0) {
            return path
        }
    }
    throw IOException("Failed to generate temp file name")
}
