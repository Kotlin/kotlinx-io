/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.files

import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * A wrapper around a string representing a file path allowing to read from and write to a
 * corresponding file using [Sink] and [Source].
 *
 * **This API is unstable and subject to change.**
 */
public expect class Path {
    /**
     * Returns a path representing a parent directory for this path,
     * or `null` if there is no parent directory for this path.
     *
     * How the parent path is resolved is platform-specific.
     */
    public fun parent(): Path?

    /**
     * Returns a string representation of this path.
     *
     * Note that the returned value will represent the same path as the value
     * passed to [Path], but it may not be identical to it.
     */
    override fun toString(): String

    override fun hashCode(): Int

    /**
     * Compares two paths for equality using its string representation ([toString]).
     */
    override fun equals(other: Any?): Boolean

    public companion object {
        public val separator: Char
    }
}

/**
 * Returns Path for the given string without much of a validation.
 */
public expect fun Path(path: String): Path

public fun Path(base: String, vararg parts: String): Path {
    val mappedBase = if (base == "/") Path.separator.toString() else base
    return Path(buildString {
        append(mappedBase)
        parts.forEach {
            if (!endsWith(Path.separator)) {
                append(Path.separator)
            }
            append(it)
        }
    })
}

public fun Path(base: Path, path: String, vararg parts: String): Path {
    return Path(buildString {
        val basePath = base.toString()
        append(basePath)
        if (!endsWith(Path.separator)) {
            append(Path.separator)
        }
        append(path)
        parts.forEach {
            if (!endsWith(Path.separator)) {
                append(Path.separator)
            }
            append(it)
        }
    })
}

/**
 * Returns [Source] for the given file or throws if path is not a file or does not exist
 */
@Deprecated(
    message = "Use FileSystem.read instead",
    replaceWith = ReplaceWith(
        expression = "FileSystem.System.read(this)",
        imports = arrayOf("kotlinx.io.files.FileSystem")
    ),
    level = DeprecationLevel.WARNING
)
public expect fun Path.source(): Source

/**
 * Returns [Sink] for the given path, creates file if it doesn't exist, throws if it's a directory,
 * overwrites contents.
 */
@Deprecated(
    message = "Use FileSystem.sink instead",
    replaceWith = ReplaceWith(
        expression = "FileSystem.System.write(this)",
        imports = arrayOf("kotlinx.io.files.FileSystem")
    ),
    level = DeprecationLevel.WARNING
)
public expect fun Path.sink(): Sink
