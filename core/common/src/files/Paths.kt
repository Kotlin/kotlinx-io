/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.files

import kotlinx.io.*
import kotlin.jvm.JvmName

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
    public val parent: Path?

    /**
     * Returns the name of the file or directory represented by this path.
     *
     * How the name is resolved is platform-specific.
     *
     * In general, one may expect that for a path like `Path("home", "Downloads", "file.txt")`
     * the name is `file.txt`.
     */
    public val name: String

    /**
     * Returns `true` if this path is absolute, `false` otherwise.
     *
     * How an absolute path is resolved is platform-specific.
     */
    public val isAbsolute: Boolean

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
        /**
         * A platform-specific character separating parts of the path.
         *
         * For example, it's usually `/` on Unix and `\` on Windows.
         */
        public val separator: Char
    }
}

/**
 * Returns Path for the given string without much of a validation.
 */
public expect fun Path(path: String): Path

/**
 * Returns Path for the given [base] path concatenated with [parts] using [Path.separator].
 */
public fun Path(base: String, vararg parts: String): Path {
    return Path(buildString {
        append(base)
        parts.forEach {
            if (isNotEmpty() && !endsWith(Path.separator)) {
                append(Path.separator)
            }
            append(it)
        }
    })
}

/**
 * Returns Path for the given [base] path concatenated with [parts] using [Path.separator].
 */
public fun Path(base: Path, vararg parts: String): Path {
    return Path(base.toString(), *parts)
}

/**
 * Returns [RawSource] for the given file or throws if path is not a file or does not exist
 */
@Deprecated(
    message = "Use FileSystem.source instead",
    replaceWith = ReplaceWith(
        expression = "FileSystem.System.source(this).buffered()",
        imports = arrayOf("kotlinx.io.files.FileSystem")
    ),
    level = DeprecationLevel.WARNING
)
@JvmName("sourceDeprecated")
public fun Path.source(): Source = FileSystem.System.source(this).buffered()

/**
 * Returns [RawSink] for the given path, creates file if it doesn't exist, throws if it's a directory,
 * overwrites contents.
 */
@Deprecated(
    message = "Use FileSystem.sink instead",
    replaceWith = ReplaceWith(
        expression = "FileSystem.System.sink(this).buffered()",
        imports = arrayOf("kotlinx.io.files.FileSystem")
    ),
    level = DeprecationLevel.WARNING
)
@JvmName("sinkDeprecated")
public fun Path.sink(): Sink = FileSystem.System.sink(this).buffered()
