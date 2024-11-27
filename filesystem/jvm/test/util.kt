/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.io.files.SystemTemporaryDirectory
import java.io.File
import kotlin.random.Random

@OptIn(ExperimentalStdlibApi::class)
actual fun tempFileName(): String {
    val tmpDir = SystemTemporaryDirectory.file
    while (true) {
        val randomString = Random.nextBytes(32).toHexString()
        val res = File(tmpDir, randomString)
        if (!res.exists()) {
            return res.absolutePath
        }
    }
}
