/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */
package kotlinx.io

import kotlinx.io.files.FileSystem
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.random.Random
import kotlin.test.assertEquals

@OptIn(ExperimentalStdlibApi::class)
actual fun tempFileName(): String {
    val tmpDir = FileSystem.System.temporaryDirectory.file
    while (true) {
        val randomString = Random.nextBytes(32).toHexString()
        val res = File(tmpDir, randomString)
        if (!res.exists()) {
            return res.absolutePath
        }
    }
}

actual fun deleteFile(path: String) {
    if (!Files.isRegularFile(Paths.get(path))) {
        throw IllegalArgumentException("Path is not a file: $path.")
    }
    Files.delete(Paths.get(path))
}

fun assertByteArrayEquals(expectedUtf8: String, b: ByteArray) {
    assertEquals(expectedUtf8, b.toString(Charsets.UTF_8))
}
