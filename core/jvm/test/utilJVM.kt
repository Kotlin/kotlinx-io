/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */
package kotlinx.io

import kotlinx.io.files.SystemTemporaryDirectory
import java.io.File
import kotlin.random.Random
import kotlin.test.assertEquals

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

fun assertByteArrayEquals(expectedUtf8: String, b: ByteArray) {
    assertEquals(expectedUtf8, b.toString(Charsets.UTF_8))
}

actual val isWindows: Boolean = System.getProperty("os.name")?.startsWith("Windows") ?: false
