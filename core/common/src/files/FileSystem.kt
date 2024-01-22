/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.IOException
import kotlinx.io.RawSink
import kotlinx.io.RawSource

/**
 * An interface providing basic operations on a filesystem, such as reading and writing files,
 * creating directories, gathering file metadata and so on.
 *
 * Default implementation of this interface is [SystemFileSystem]. It provides access to files and directories
 * on disk.
 *
 * The interface is sealed until API stabilization, but in the future it will allow
 * creating custom filesystems that could be used as mocks for testing or provide
 * access to some network resources and allow working with them as with regular files, for example.
 *
 * **This API is unstable and subject to change.**
 */
public sealed interface FileSystem {
    /**
     * Returns `true` if there is a filesystem entity a [path] points to,
     * otherwise returns `false`.
     *
     * @param path the path that should be checked for existence.
     *
     * @throws kotlinx.io.IOException when the attempt to check the existence of the [path] failed.
     */
    public fun exists(path: Path): Boolean

    /**
     * Deletes a file or directory the [path] points to from a filesystem.
     * If there is no filesystem entity represented by the [path]
     * this method throws [kotlinx.io.files.FileNotFoundException] when [mustExist] is `true`.
     *
     * Note that in the case of a directory, this method will not attempt to delete it recursively,
     * so deletion of non-empty directory will fail.
     *
     * @param path the path to a file or directory to be deleted.
     * @param mustExist the flag indicating whether missing [path] is an error, `true` by default.
     *
     * @throws kotlinx.io.files.FileNotFoundException when [path] does not exist and [mustExist] is `true`.
     * @throws kotlinx.io.IOException if deletion failed.
     */
    public fun delete(path: Path, mustExist: Boolean = true)

    /**
     * Creates a directory tree represented by the [path].
     * If [path] already exists then the method throws [kotlinx.io.IOException] when [mustCreate] is `true`.
     * The call will attempt to create only missing directories.
     * The method is not atomic and if it fails after creating some
     * directories, these directories will not be deleted automatically.
     * Permissions for created directories are platform-specific.
     *
     * @param path the path to be created.
     * @param mustCreate the flag indicating that existence of [path] should be treated as an error,
     * by default it is `false`.
     *
     * @throws kotlinx.io.IOException when [path] already exists and [mustCreate] is `true`.
     * @throws kotlinx.io.IOException when the creation of one of the directories fails.
     * @throws kotlinx.io.IOException when [path] is an existing file and [mustCreate] is `false`.
     */
    public fun createDirectories(path: Path, mustCreate: Boolean = false)

    /**
     * Atomically renames [source] to [destination] overriding [destination] if it already exists.
     *
     * When the filesystem does not support atomic move of [source] and [destination] corresponds to different
     * filesystems (or different volumes, on Windows) and the operation could not be performed atomically,
     * [UnsupportedOperationException] is thrown.
     *
     * When [destination] is an existing directory, the operation may fail on some platforms
     * (on Windows, particularly).
     *
     * @param source the path to rename.
     * @param destination desired path name.
     *
     * @throws kotlinx.io.files.FileNotFoundException when the [source] does not exist.
     * @throws kotlinx.io.IOException when the move failed.
     * @throws kotlin.UnsupportedOperationException when the filesystem does not support atomic move.
     */
    public fun atomicMove(source: Path, destination: Path)

    /**
     * Returns [RawSource] to read from a file the [path] points to.
     *
     * How a source will read the data is implementation-specific and failures caused
     * by the missing file or, for example, lack of permissions may not be reported immediately,
     * but postponed until the source will try to fetch data.
     *
     * If [path] points to a directory, this method will fail with [IOException].
     *
     * @param path the path to read from.
     *
     * @throws kotlinx.io.files.FileNotFoundException when the file does not exist.
     * @throws kotlinx.io.IOException when it's not possible to open the file for reading.
     */
    public fun source(path: Path): RawSource

    /**
     * Returns [RawSink] to write into a file the [path] points to.
     * Depending on [append] value, the file will be overwritten or data will be appened to it.
     * File will be created if it does not exist yet.
     *
     * How a sink will write the data is implementation-specific and failures caused,
     * for example, by the lack of permissions may not be reported immediately,
     * but postponed until the sink will try to store data.
     *
     * If [path] points to a directory, this method will fail with [IOException]
     *
     * @param path the path to a file to write data to.
     * @param append the flag indicating whether the data should be appended to an existing file or it
     * should be overwritten, `false` by default, meaning the file will be overwritten.
     *
     * @throws kotlinx.io.IOException when it's not possible to open the file for writing.
     */
    public fun sink(path: Path, append: Boolean = false): RawSink

    /**
     * Return [FileMetadata] associated with a file or directory the [path] points to.
     * If there is no such file or directory, or it's impossible to fetch metadata,
     * `null` is returned.
     *
     * @param path the path to get the metadata for.
     */
    public fun metadataOrNull(path: Path): FileMetadata?

    /**
     * Returns an absolute path to the same file or directory the [path] is pointing to.
     * All symbolic links are solved, extra path separators and references to current (`.`) or
     * parent (`..`) directories are removed.
     * If the [path] is a relative path then it'll be resolved against current working directory.
     * If there is no file or directory to which the [path] is pointing to then [FileNotFoundException] will be thrown.
     *
     * @param path the path to resolve.
     * @return a resolved path.
     * @throws FileNotFoundException if there is no file or directory corresponding to the specified path.
     */
    public fun resolve(path: Path): Path
}

internal abstract class SystemFileSystemImpl : FileSystem

/**
 * An instance of [FileSystem] representing a default system-wide filesystem.
 */
public expect val SystemFileSystem: FileSystem

/**
 * Path to a directory suitable for creating temporary files.
 *
 * This path is not bound to a particular filesystem, but rather a system-wide temporary directory.
 */
public expect val SystemTemporaryDirectory: Path

/**
 * Represents information about a file or directory obtainable from a filesystem like
 * a type of filesystem node (is it a directory or a file?), its size, and so on.
 */
public class FileMetadata(
    /**
     * Flag indicating that the metadata was retrieved for a regular file.
     */
    public val isRegularFile: Boolean = false,
    /**
     * Flag indicating that the metadata was retrieved for a directory.
     */
    public val isDirectory: Boolean = false,
    /**
     * File size. Defined only for regular files, for other filesystem entities it will be `-1`.
     */
    public val size: Long = 0L
)

/**
 * Signals an I/O operation's failure due to a missing file or directory.
 */
public expect class FileNotFoundException(message: String?) : IOException

internal const val WindowsPathSeparator: Char = '\\'
internal const val UnixPathSeparator: Char = '/'
