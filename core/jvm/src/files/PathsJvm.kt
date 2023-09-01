/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.files

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered
import java.io.File

public actual class Path internal constructor(internal val file: File) {
    public actual val parent: Path?
        get() {
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

    public actual val isAbsolute: Boolean
        get() = file.isAbsolute
    public actual val name: String
        get() = file.name
}

public actual fun Path(path: String): Path = Path(File(path))

// Function only exists to provide binary compatibility with the earlier releases
@JvmName("source")
@PublishedApi
@Suppress("UNUSED")
internal fun Path.sourceHack(): Source = FileSystem.System.source(this).buffered()

// Function only exists to provide binary compatibility with the earlier releases
@JvmName("sink")
@PublishedApi
@Suppress("UNUSED")
internal fun Path.sinkHack(): Sink = FileSystem.System.sink(this).buffered()
