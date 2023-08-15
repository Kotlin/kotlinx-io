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
    public fun parent(): Path?

    public fun asString(): String

    public companion object {
        public val pathSeparator: Char
    }
}

/**
 * Returns Path for the given string without much of a validation.
 */
public expect fun Path(path: String): Path

public fun Path(base: String, vararg parts: String): Path {
    return Path(buildString {
        append(base)
        parts.forEach {
            if (!endsWith(Path.pathSeparator)) {
                append(Path.pathSeparator)
            }
            append(it)
        }
    })
}

public fun Path(base: Path, path: String, vararg parts: String): Path {
    return Path(buildString {
        val basePath = base.asString()
        append(basePath)
        if (!endsWith(Path.pathSeparator)) {
            append(Path.pathSeparator)
        }
        append(path)
        parts.forEach {
            if (!endsWith(Path.pathSeparator)) {
                append(Path.pathSeparator)
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
