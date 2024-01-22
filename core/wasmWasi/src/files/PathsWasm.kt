/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

public actual class Path internal constructor(rawPath: String, @Suppress("UNUSED_PARAMETER") obj: Any?) {
    // TODO: how to detect host OS?
    private val path: String = removeTrailingSeparators(rawPath, false)

    actual override fun toString(): String = path

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as Path
        return path == other.path
    }

    actual override fun hashCode(): Int {
        return path.hashCode()
    }

    public actual val parent: Path?
        get() {
            if (path == SystemPathSeparator.toString()) return null

            val idx = path.lastIndexOf(SystemPathSeparator)
            if (idx < 0) return null
            // path ends with '/', but as it was normalized there is only one case -> it's "/"
            if (idx == path.length - 1) return null
            val rawBase = if (idx == 0) "$SystemPathSeparator" else path.substring(0, idx)
            // TODO: what if it's not Unix?
            val base = removeTrailingSeparators(rawBase, false)
            // there was nothing but multiple '/'
            return Path(base, null)
        }

    public actual val name: String
        get() {
            // TODO: name of '/'?
            val idx = path.lastIndexOf(SystemPathSeparator)
            return if (idx < 0) {
                path
            } else {
                path.substring(idx + 1)
            }
        }

    public actual val isAbsolute: Boolean = path.startsWith(SystemPathSeparator)
}

// TODO: what if we're running on Windows?
public actual val SystemPathSeparator: Char = UnixPathSeparator

public actual fun Path(path: String): Path = Path(path, null as Any?)
