/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.files

import kotlinx.io.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

public actual class Path internal constructor(internal val file: File) {
    public actual fun parent(): Path? {
        val parentFile = file.parentFile ?: return null
        return Path(parentFile)
    }

    public actual override fun toString(): String = file.toString()

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Path) return false

        return toString() == other.toString()
    }

    actual override fun hashCode(): Int {
        return toString().hashCode()
    }

    public actual companion object {
        public actual val separator: Char = File.separatorChar
    }
}

public actual fun Path(path: String): Path = Path(File(path))

public actual fun Path.source(): Source = FileInputStream(file).asSource().buffered()

public actual fun Path.sink(): Sink = FileOutputStream(file).asSink().buffered()
