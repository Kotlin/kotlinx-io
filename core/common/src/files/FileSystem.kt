/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * An interface providing basic operations on a file system.
 *
 * **This API is unstable and subject to change.**
 */
public interface FileSystem {
    /**
     * Path to a directory suitable for creating temporary files.
     */
    public val temporaryDirectory: Path

    /**
     * Returns `true` if there is a file system entity corresponding to a [path],
     * otherwise returns `false`.
     *
     * @param path the path that should be checked for existence.
     */
    public fun exists(path: Path): Boolean

    /**
     * Deletes [path] from a file system. If there is no file system entity
     * represented by the [path] this method throws [kotlinx.io.IOException] when [mustExist]
     * is `true`.
     *
     * @param path the path to be deleted.
     * @param mustExist the flag indicating whether missing [path] is an error, `true` by default.
     *
     * @throws kotlinx.io.IOException if the [path] could not be deleted.
     */
    public fun delete(path: Path, mustExist: Boolean = true)

    /**
     * Creates a directory tree represented by the [path].
     * If [path] already exists then the method throws [kotlinx.io.IOException] when [mustCreate] is `true`.
     * The call will attempt to create only missing directories.
     * The method is not atomic and if it fails after creating some
     * directories these directories will not be deleted automatically.
     * Permissions for created directories are platform-specific.
     *
     * @param path the path to be created.
     * @param mustCreate the flag indicating that existence of [path] should be treated as an error,
     * by default it is `false`.
     *
     * @throws kotlinx.io.IOException when [path] already exists and [mustCreate] is `true`.
     * @throws kotlinx.io.IOException when the creation of one of the directories fails.
     */
    public fun createDirectories(path: Path, mustCreate: Boolean = false)

    /**
     * Atomically renames [source] to [destination] overriding [destination] if it already exists.
     *
     * When the file system does not support atomic move of [source] and [destination] corresponds to different
     * file systems and the operation could not be performed atomically,
     * [UnsupportedOperationException] is thrown.
     *
     * @param source the path to rename.
     * @param destination desired path name.
     *
     * @throws kotlinx.io.IOException if [source] does not exist or move failed.
     * @throws kotlin.UnsupportedOperationException when the file system does not support atomic move.
     */
    public fun atomicMove(source: Path, destination: Path)

    /**
     * Returns [Source] to read from a file represented by the [path].
     *
     * @param path the path to read from.
     *
     * @throws kotlinx.io.IOException when the file does not exists.
     */
    public fun read(path: Path): Source = path.source()

    /**
     * Returns [Sink] to write into a file represented by the [path].
     * File will be created if it does not exist yet.
     */
    public fun write(path: Path): Sink = path.sink()

    /**
     * Return [FileMetadata] associated with a file or directory represented by the [path].
     * If there is not such file or directory, the `null` is returned.
     *
     * @param path the path to get the metadata for.
     */
    public fun metadataOrNull(path: Path): FileMetadata?

    public companion object {
        /**
         * An instance of [FileSystem] representing a default system-wide file system.
         */
        public val System: FileSystem = SystemFileSystem
    }
}

internal expect val SystemFileSystem: FileSystem

public class FileMetadata(
    public val isRegularFile: Boolean = false,
    public val isDirectory: Boolean = false
)

public expect open class FileNotFoundException(message: String?) : IOException
