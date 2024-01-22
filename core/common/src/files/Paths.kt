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

    /**
     * Returns hash code of this Path.
     * The hash code is calculated for the path's string representations ([toString]).
     */
    override fun hashCode(): Int

    /**
     * Compares two paths for equality using its string representation ([toString]).
     */
    override fun equals(other: Any?): Boolean
}

/**
 * A platform-specific character separating parts of the path.
 * It's inherited from the default system's filesystem.
 * It should not be used if an application is working with multiple filesystems having different separators.
 *
 * For example, the separator is usually `/` on Unix and `\` on Windows.
 */
public expect val SystemPathSeparator: Char

/**
 * Returns Path for the given string without much of a validation.
 */
public expect fun Path(path: String): Path

/**
 * Returns Path for the given [base] path concatenated with [parts] using [SystemPathSeparator].
 */
public fun Path(base: String, vararg parts: String): Path {
    return Path(buildString {
        append(base)
        parts.forEach {
            if (isNotEmpty() && !endsWith(SystemPathSeparator)) {
                append(SystemPathSeparator)
            }
            append(it)
        }
    })
}

/**
 * Returns Path for the given [base] path concatenated with [parts] using [SystemPathSeparator].
 */
public fun Path(base: Path, vararg parts: String): Path {
    return Path(base.toString(), *parts)
}

/**
 * Returns [RawSource] for the given file or throws if path is not a file or does not exist
 *
 * Use of this method is deprecated with warning since kotlinx-io 0.3.0. The method will be removed in 0.4.0.
 */
@Deprecated(
    message = "Use FileSystem.source instead",
    replaceWith = ReplaceWith(
        expression = "SystemFileSystem.source(this).buffered()",
        imports = arrayOf("kotlinx.io.files.FileSystem")
    ),
    level = DeprecationLevel.WARNING
)
@JvmName("sourceDeprecated")
public fun Path.source(): Source = SystemFileSystem.source(this).buffered()

/**
 * Returns [RawSink] for the given path, creates file if it doesn't exist, throws if it's a directory,
 * overwrites contents.
 *
 * Use of this method is deprecated with warning since kotlinx-io 0.3.0. The method will be removed in 0.4.0.
 */
@Deprecated(
    message = "Use FileSystem.sink instead",
    replaceWith = ReplaceWith(
        expression = "SystemFileSystem.sink(this).buffered()",
        imports = arrayOf("kotlinx.io.files.FileSystem")
    ),
    level = DeprecationLevel.WARNING
)
@JvmName("sinkDeprecated")
public fun Path.sink(): Sink = SystemFileSystem.sink(this).buffered()

internal fun removeTrailingSeparators(path: String, /* only for testing */ isWindows_: Boolean = isWindows): String {
    if (isWindows_) {
        // don't trim the path separator right after the drive name
        val limit = if (path.length > 1) {
            if (path[1] == ':') {
                3
            } else if (isUnc(path)) {
                2
            } else {
                1
            }
        } else {
            1
        }
        return removeTrailingSeparatorsWindws(limit, path)
    }
    return removeTrailingSeparatorsUnix(path)
}

private fun isUnc(path: String): Boolean {
    if (path.length < 2) return false
    if (path.startsWith("$WindowsPathSeparator$WindowsPathSeparator")) return true
    if (path.startsWith("$UnixPathSeparator$UnixPathSeparator")) return true
    return false
}

private fun removeTrailingSeparatorsUnix(path: String): String {
    var idx = path.length
    while (idx > 1 && path[idx - 1] == UnixPathSeparator) {
        idx--
    }
    return path.substring(0, idx)
}

private fun removeTrailingSeparatorsWindws(suffixLength: Int, path: String): String {
    require(suffixLength >= 1)
    var idx = path.length
    while (idx > suffixLength) {
        val c = path[idx - 1]
        if (c != WindowsPathSeparator && c != UnixPathSeparator) break
        idx--
    }
    return path.substring(0, idx)
}
