/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */
package kotlinx.io

import java.nio.file.*

actual fun createTempFile(): String = Files.createTempFile(null, null).toString()

actual fun deleteFile(path: String) {
    if (!Files.isRegularFile(Paths.get(path))) {
        throw IllegalArgumentException("Path is not a file: $path.")
    }
    Files.delete(Paths.get(path))
}