/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.io.files.SystemTemporaryDirectory
import platform.posix.F_OK
import platform.posix.access
import kotlin.random.Random

@OptIn(ExperimentalStdlibApi::class)
actual fun tempFileName(): String {
    val tmpDir = SystemTemporaryDirectory.path
    repeat(10) {
        val name = Random.nextBytes(32).toHexString()
        val path = "$tmpDir/$name"
        if (access(path, F_OK) != 0) {
            return path
        }
    }
    throw IOException("Failed to generate temp file name")
}
