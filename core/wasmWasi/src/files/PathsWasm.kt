/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

public actual class Path internal constructor(rawPath: String, @Suppress("UNUSED_PARAMETER") obj: Any?) {
    private val path: String = rawPath

    actual override fun toString(): String = path
    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class !== other::class) return false

        other as Path

        return path == other.path
    }

    actual override fun hashCode(): Int {
        return path.hashCode()
    }

    public actual val parent: Path?
        get() = unsupported()

    public actual val name: String
        get() = unsupported()

    public actual val isAbsolute: Boolean
        get() = unsupported()
}

public actual val SystemPathSeparator: Char
    get() = unsupported()

public actual fun Path(path: String): Path = Path(path, null as Any?)
