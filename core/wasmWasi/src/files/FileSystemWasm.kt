/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.*
import kotlinx.io.wasi.*
import kotlin.math.min
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

/**
 * Path to a directory suitable for creating temporary files.
 *
 * This path is always `/tmp`, meaning that either `/` or `/tmp` should be pre-opened to use this path.
 */
public actual val SystemTemporaryDirectory: Path = Path("/tmp")

/**
 * An instance of [FileSystem] representing a default system-wide filesystem.
 *
 * The implementation is built upon Wasm WASI preview 1.
 *
 * To use files, at least one directory should be pre-opened.
 *
 * Operations on all absolute paths that are not sub-paths of pre-opened directories will fail.
 *
 * Relative paths are treated as paths relative to the first pre-opened directory.
 * For example, if following directories were pre-opened: `/work`, `/tmp`, `/persistent`, then
 * the path `a/b/c/d` will be resolved to `/work/a/b/c/d` as `/work` is the first pre-opened directory.
 */
public actual val SystemFileSystem: FileSystem = WasiFileSystem

@OptIn(UnsafeWasmMemoryApi::class)
internal object WasiFileSystem : SystemFileSystemImpl() {

    override fun exists(path: Path): Boolean {
        return metadataOrNull(path) != null
    }

    override fun delete(path: Path, mustExist: Boolean) {
        val preOpen = PreOpens.findPreopen(path)
        withScopedMemoryAllocator { allocator ->
            val (stringBuffer, stringLength) = allocator.storeString(path.path)

            val unlinkRes = Errno(path_unlink_file(preOpen.fd, stringBuffer.address.toInt(), stringLength))
            if (unlinkRes == Errno.success) return
            if (unlinkRes == Errno.noent) {
                if (mustExist) throw FileNotFoundException("File does not exist: $path")
                return
            }
            // In case the path corresponding to a directory, either Error.isdir, or Errno.perm will be returned.
            // In all other cases, there's no sense to continue, so we'll bail out with an exception.
            if (unlinkRes != Errno.isdir && unlinkRes != Errno.perm) {
                throw IOException("Unable to remove file $path: ${unlinkRes.description}")
            }

            val removeDirRes = Errno(path_remove_directory(preOpen.fd, stringBuffer.address.toInt(), stringLength))
            if (removeDirRes == Errno.success) return
            if (removeDirRes == Errno.noent) {
                if (mustExist) throw FileNotFoundException("File does not exist: $path")
                return
            }
            throw IOException("Unable to remove directory ${path.path}: ${unlinkRes.description}")
        }
    }

    override fun createDirectories(path: Path, mustCreate: Boolean) {
        val preOpen = PreOpens.findPreopen(path)
        if (path == preOpen.path) {
            if (!mustCreate) return
            throw IOException("Directory already exists: $path")
        }
        val segments: List<String> = buildList {
            var currentPath: Path? = path
            while (currentPath != null && currentPath != preOpen.path) {
                add(currentPath.path)
                currentPath = currentPath.parent
            }
        }
        var created = false
        withScopedMemoryAllocator { allocator ->
            // Allocating one extra byte to place the NULL-byte there
            val pathBuffer = allocator.allocate(segments.first().encodeToByteArray().size + 1)

            for (idx in segments.size - 1 downTo 0) {
                val segment = segments[idx]
                val segmentLength = pathBuffer.allocateString(segment)

                val res = Errno(path_create_directory(preOpen.fd, pathBuffer.address.toInt(), segmentLength))
                if (res == Errno.success) {
                    created = true
                } else if (res != Errno.exist) {
                    throw IOException(
                        "Can't create directory $path. " +
                                "Creation of an intermediate directory $segment failed: ${res.description}"
                    )
                }
            }
            if (mustCreate && !created) throw IOException("Directory already exists: $path")
            if (!created) {
                if (!metadataOrNull(path)!!.isDirectory) {
                    throw IOException("Path already exists and it's not a directory: $path")
                }
            }
        }
    }

    /**
     * The move is not atomic (well, we don't know what kind of move it is), but there are no
     * alternatives.
     */
    override fun atomicMove(source: Path, destination: Path) {
        val sourcePreOpen = PreOpens.findPreopen(source)
        val destPreOpen = PreOpens.findPreopen(destination)

        withScopedMemoryAllocator { allocator ->
            val (sourceBuffer, sourceBufferLength) = allocator.storeString(source.path)
            val (destBuffer, destBufferLength) = allocator.storeString(destination.path)

            val res = Errno(
                path_rename(
                    oldFd = sourcePreOpen.fd,
                    oldPathPtr = sourceBuffer.address.toInt(),
                    oldPathLen = sourceBufferLength,
                    newFd = destPreOpen.fd,
                    newPathPtr = destBuffer.address.toInt(),
                    newPathLen = destBufferLength
                )
            )
            when (res) {
                Errno.success -> return
                Errno.noent -> throw FileNotFoundException(
                    "Failed to rename $source to $destination as either source file/directory, " +
                            "or destination's parent directory does not exist."
                )

                else -> throw IOException("Failed to rename $source to $destination: ${res.description}")
            }
        }
    }

    override fun source(path: Path): RawSource {
        val preOpen = PreOpens.findPreopen(path)

        val fd = withScopedMemoryAllocator { allocator ->
            val fdPtr = allocator.allocateInt()
            val (stringBuffer, stringBufferLength) = allocator.storeString(path.path)

            val res = Errno(
                path_open(
                    fd = preOpen.fd,
                    dirflags = listOf(LookupFlags.symlink_follow).toBitset(),
                    pathPtr = stringBuffer.address.toInt(), pathLen = stringBufferLength,
                    oflags = 0,
                    fsRightsBase = listOf(Rights.fd_read).toBitset(),
                    fsRightsInheriting = 0,
                    fdFlags = 0,
                    resultPtr = fdPtr.address.toInt()
                )
            )
            if (res != Errno.success) {
                if (res == Errno.noent) throw FileNotFoundException("File does not exist: $path")
                throw IOException("Can't open $path for read: ${res.description}")
            }
            fdPtr.loadInt()
        }
        return FileSource(fd)
    }

    @OptIn(UnsafeWasmMemoryApi::class)
    override fun sink(path: Path, append: Boolean): RawSink {
        val preOpen = PreOpens.findPreopen(path)

        val fd = withScopedMemoryAllocator { allocator ->
            val fdPtr = allocator.allocateInt()
            val (stringBuffer, stringBufferLength) = allocator.storeString(path.path)

            val fdFlags = buildList {
                if (append) {
                    add(FdFlags.append)
                }
            }.toBitset()

            val openFlags = buildList {
                add(OpenFlags.creat)
                if (!append) {
                    add(OpenFlags.trunc)
                }
            }.toBitset()

            val res = Errno(
                path_open(
                    fd = preOpen.fd,
                    dirflags = listOf(LookupFlags.symlink_follow).toBitset(),
                    pathPtr = stringBuffer.address.toInt(), pathLen = stringBufferLength,
                    oflags = openFlags,
                    fsRightsBase = listOf(Rights.fd_write, Rights.fd_sync).toBitset(),
                    fsRightsInheriting = 0,
                    fdFlags = fdFlags,
                    resultPtr = fdPtr.address.toInt()
                )
            )
            if (res != Errno.success) {
                throw IOException("Can't open $path for write: ${res.description}")
            }
            fdPtr.loadInt()
        }
        return FileSink(fd)
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        val preOpen = PreOpens.findPreopenOrNull(path) ?: return null
        val md = metadataOrNullInternal(preOpen.fd, path, true) ?: return null

        val filetype = md.filetype
        val isDirectory = filetype == FileType.directory
        val filesize = if (isDirectory) -1 else md.filesize
        return FileMetadata(
            isRegularFile = filetype == FileType.regular_file,
            isDirectory = isDirectory,
            size = filesize
        )
    }

    /**
     * Returns an absolute path to the same file or directory the [path] is pointing to.
     * All symbolic links are solved, extra path separators and references to current (`.`) or
     * parent (`..`) directories are removed.
     * If the [path] is a relative path then it'll be resolved against current working directory.
     * If there is no file or directory to which the [path] is pointing to then [FileNotFoundException] will be thrown.
     *
     * The behavior of this method differs from other platforms as the resolution
     * may not fail if there is no filesystem-node (file, directory, symlink, etc.) corresponding
     * to some interior path. This allows successfully resolving paths like `/a/b/c/../../d/e` when
     * pre-opened directories are `/a/b/c` and `/a/d/e`.
     *
     * @param path the path to resolve.
     * @return a resolved path.
     * @throws FileNotFoundException if there is no file or directory corresponding to the specified path.
     */
    override fun resolve(path: Path): Path {
        val absolutePath = if (path.isAbsolute) {
            path
        } else {
            Path(PreOpens.roots.first(), path.path)
        }

        val resolvedPath = resolvePathImpl(absolutePath, 0)
            ?: throw FileNotFoundException("Path does not exist: $path")
        check(resolvedPath.isAbsolute) {
            "Path is not absolute after symlinks resolution"
        }

        val normalizedPath = resolvedPath.normalized()
        if (!exists(normalizedPath)) throw FileNotFoundException("Path does not exist: $path")
        return normalizedPath
    }

    internal fun symlink(linked: Path, target: Path) {
        val preOpen = PreOpens.findPreopen(target)

        withScopedMemoryAllocator { allocator ->
            val (fromBuffer, fromBufferLength) = allocator.storeString(linked.path)
            val (toBuffer, toBufferLength) = allocator.storeString(target.path)
            val res = Errno(
                path_symlink(
                    fromBuffer.address.toInt(), fromBufferLength,
                    preOpen.fd, toBuffer.address.toInt(), toBufferLength
                )
            )

            if (res == Errno.success) return
            throw IOException("Can't create symbolic link $target pointing to $linked: ${res.description}")
        }
    }

    override fun list(directory: Path): Collection<Path> {
        val preOpen = PreOpens.findPreopen(directory)

        val metadata = metadataOrNullInternal(preOpen.fd, directory, true)
            ?: throw FileNotFoundException(directory.path)
        if (metadata.filetype != FileType.directory) throw IOException("Not a directory: ${directory.path}")

        val children = mutableListOf<Path>()
        val dir_fd = withScopedMemoryAllocator { allocator ->
            val fdPtr = allocator.allocateInt()
            val (stringBuffer, stringBufferLength) = allocator.storeString(directory.path)

            val res = Errno(
                path_open(
                    fd = preOpen.fd,
                    dirflags = listOf(LookupFlags.symlink_follow).toBitset(),
                    pathPtr = stringBuffer.address.toInt(), pathLen = stringBufferLength,
                    oflags = setOf(OpenFlags.directory).toBitset(),
                    fsRightsBase = listOf(Rights.fd_readdir, Rights.fd_read).toBitset(),
                    fsRightsInheriting = 0,
                    fdFlags = 0,
                    resultPtr = fdPtr.address.toInt()
                )
            )
            if (res != Errno.success) throw IOException("Can't open directory ${directory.path}: ${res.description}")
            fdPtr.loadInt()
        }
        try {
            withScopedMemoryAllocator { allocator ->
                val resultSizePtr = allocator.allocateInt()
                // directory's filesize expected to be larger than the actual buffer size required to fit all entries
                val bufferSize = metadata.filesize.toInt()
                val buffer = allocator.allocate(bufferSize)
                val resultSize: Int
                // Unsuported on Windows and Android:
                // https://github.com/nodejs/node/blob/6f4d6011ea1b448cf21f5d363c44e4a4c56ca34c/deps/uvwasi/src/uvwasi.c#L19
                val res = Errno(
                    fd_readdir(
                        fd = dir_fd,
                        bufPtr = buffer.address.toInt(),
                        bufLen = bufferSize,
                        cookie = 0L,
                        resultPtr = resultSizePtr.address.toInt()
                    )
                )
                if (res != Errno.success) {
                    throw IOException("Can't read directory ${directory.path}: ${res.description}")
                }
                resultSize = resultSizePtr.loadInt()
                check(resultSize <= bufferSize) { "Result size: $resultSize, buffer size: $bufferSize" }
                var entryPtr = buffer
                val endPtr = entryPtr + resultSize
                while (entryPtr.address < endPtr.address) {
                    // read dirent: https://github.com/WebAssembly/WASI/blob/main/legacy/preview1/docs.md#-dirent-record
                    // Each entry is 24-byte-wide dirent with filename length at offset 16, followed by
                    // filename length bytes of data.
                    val entryLen = entryPtr.loadInt(16)
                    entryPtr += 24
                    val name = entryPtr.loadString(entryLen)
                    entryPtr += entryLen
                    if (name != "." && name != "..") {
                        children.add(Path(directory, name))
                    }
                }
            }
            return children
        } finally {
            fd_close(dir_fd)
        }
    }
}

private fun Path.normalized(): Path {
    require(isAbsolute)

    val parts = path.split(UnixPathSeparator)
    val constructedPath = mutableListOf<String>()
    // parts[0] is always empty
    for (idx in 1 until parts.size) {
        when (val part = parts[idx]) {
            "." -> continue
            ".." -> constructedPath.removeLastOrNull()
            else -> constructedPath.add(part)
        }
    }
    return Path(UnixPathSeparator.toString(), *constructedPath.toTypedArray())
}

public actual open class FileNotFoundException actual constructor(
    message: String?,
) : IOException(message)

// The property affects only paths processing and in Wasi paths are always '/'-delimited.
internal actual val isWindows: Boolean = false

internal object PreOpens {
    data class PreOpen(val path: Path, val fd: Int)

    internal val roots: List<Path> by lazy {
        preopens.map { it.path }
    }

    internal val preopens: List<PreOpen> by lazy {
        // 0 - stdin, 1 - stdout, 2 - stderr, 3 - if the first preopened directory, if any
        val firstPreopenFd = 3
        val rootPaths = mutableListOf<PreOpen>()

        for (fd in firstPreopenFd..<Int.MAX_VALUE) {
            if (!loadPreopenInfo(fd, rootPaths)) {
                break
            }
        }

        rootPaths
    }

    internal fun findPreopenOrNull(path: Path): PreOpen? {
        if (!path.isAbsolute) {
            return preopens.firstOrNull()
        }

        for (preopen in preopens) {
            var p: Path? = path
            while (p != null) {
                if (preopen.path == p) return preopen
                p = p.parent
            }
        }
        return null
    }

    internal fun findPreopen(path: Path): PreOpen {
        return findPreopenOrNull(path)
            ?: throw IOException("Path does not belong to any preopened directory: $path")
    }

    @OptIn(UnsafeWasmMemoryApi::class)
    private fun loadPreopenInfo(fd: Int, outputList: MutableList<PreOpen>): Boolean {
        return withScopedMemoryAllocator { allocator ->
            val prestat = Prestat(allocator)
            val res = Errno(fd_prestat_get(fd, prestat.address))

            if (res == Errno.badf) {
                return@withScopedMemoryAllocator false
            }

            if (res != Errno.success) {
                throw IOException("Unable to process fd=$fd as preopen: ${res.description}")
            }

            check(prestat.type == PrestatType.dir) { "Unsupported prestat type" }

            val nameLength = prestat.nameLength
            val pathBuffer = allocator.allocate(nameLength)
            val dirnameRes = Errno(fd_prestat_dir_name(fd, pathBuffer.address.toInt(), nameLength))
            if (dirnameRes != Errno.success) {
                throw IOException("Unable to get preopen dir name for fd=$fd: ${dirnameRes.description}")
            }
            outputList.add(PreOpen(Path(pathBuffer.loadBytes(nameLength).decodeToString()), fd))

            true
        }
    }
}

private const val TEMP_CIOVEC_BUFFER_LEN = 8192

private class FileSink(private val fd: Fd) : RawSink {
    private var closed: Boolean = false

    @OptIn(UnsafeWasmMemoryApi::class)
    override fun write(source: Buffer, byteCount: Long) {
        check(byteCount >= 0)
        if (byteCount == 0L) return
        withScopedMemoryAllocator { allocator ->
            val temporaryWriteBuffer = allocator.allocate(TEMP_CIOVEC_BUFFER_LEN)
            var remaining = byteCount

            val ciovec = Ciovec(allocator).also {
                it.buffer = temporaryWriteBuffer
            }

            val resultPtr = allocator.allocateInt()

            while (remaining > 0) {
                val bytesToWrite = minOf(remaining, TEMP_CIOVEC_BUFFER_LEN).toInt()
                source.readToLinearMemory(temporaryWriteBuffer, bytesToWrite)
                ciovec.length = bytesToWrite

                val res = Errno(fd_write(fd, ciovec.address, 1, resultPtr.address.toInt()))
                if (res != Errno.success) {
                    throw IOException("Write failed: ${res.description}")
                }
                check(resultPtr.loadInt(0) == bytesToWrite) {
                    "Expected to write $bytesToWrite, but ${resultPtr.loadInt(0)} bytes were written"
                }
                remaining -= bytesToWrite
            }
        }
    }

    override fun flush() {
        val res = Errno(fd_sync(fd))
        if (res != Errno.success) {
            throw IOException("fd_sync failed: ${res.description}")
        }
    }

    override fun close() {
        if (!closed) {
            closed = true
            fd_close(fd)
        }
    }
}

private class FileSource(private val fd: Fd) : RawSource {
    private var closed: Boolean = false

    @OptIn(UnsafeWasmMemoryApi::class)
    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        check(byteCount >= 0)
        if (byteCount == 0L) return 0L
        var totalRead = 0L

        withScopedMemoryAllocator { allocator ->
            val temporaryReadBuffer = allocator.allocate(TEMP_CIOVEC_BUFFER_LEN)
            var remaining = byteCount

            val ciovec = Ciovec(allocator).also {
                it.buffer = temporaryReadBuffer
            }

            val resultPtr = allocator.allocateInt()
            while (remaining > 0) {
                val bytesToRead = minOf(remaining, TEMP_CIOVEC_BUFFER_LEN).toInt()
                ciovec.length = bytesToRead

                val res = Errno(fd_read(fd, ciovec.address, 1, resultPtr.address.toInt()))
                if (res != Errno.success) {
                    throw IOException("Read failed: ${res.description}")
                }
                val readBytes = resultPtr.loadInt()
                // we're done
                if (readBytes == 0) {
                    if (totalRead == 0L) return -1L
                    break
                }
                sink.writeFromLinearMemory(temporaryReadBuffer, readBytes)
                remaining -= readBytes
                totalRead += readBytes
            }
        }
        return totalRead
    }

    override fun close() {
        if (!closed) {
            closed = true
            fd_close(fd)
        }
    }
}

private data class InternalMetadata(val filetype: FileType, val filesize: Long)

@OptIn(UnsafeWasmMemoryApi::class)
private fun metadataOrNullInternal(rootFd: Fd, path: Path, followSymlinks: Boolean): InternalMetadata? {
    withScopedMemoryAllocator { allocator ->
        val (pathBuffer, pathBufferLength) = allocator.storeString(path.path)

        val filestat = FileStat(allocator)

        val res = Errno(
            path_filestat_get(
                fd = rootFd,
                flags = if (followSymlinks) 1 else 0,
                pathPtr = pathBuffer.address.toInt(), pathLen = pathBufferLength,
                resultPtr = filestat.address
            )
        )

        if (res == Errno.noent || res == Errno.notcapable) return null
        if (res != Errno.success) throw IOException(res.description)

        return InternalMetadata(filestat.filetype, filestat.filesize)
    }
}

@OptIn(UnsafeWasmMemoryApi::class)
private fun readlinkInternal(rootFd: Fd, path: Path, linkSize: Int): Path {
    withScopedMemoryAllocator { allocator ->
        val resultPtr = allocator.allocateInt()
        val (pathBuffer, pathBufferLength) = allocator.storeString(path.path)
        // Allocating one extra byte to have enough space for the NULL-byte
        val resultBuffer = allocator.allocate(linkSize + 1)

        val res = Errno(
            path_readlink(
                fd = rootFd,
                pathPtr = pathBuffer.address.toInt(), pathLen = pathBufferLength,
                bufPtr = resultBuffer.address.toInt(), bufLen = linkSize + 1,
                resultPtr = resultPtr.address.toInt()
            )
        )
        if (res != Errno.success) {
            throw IOException("Link resolution failed for path $path: ${res.description}")
        }
        val resultLength = resultPtr.loadInt()
        // resultLength includes the NULL-byte, we don't have to read it
        return Path(resultBuffer.loadBytes(min(resultLength, linkSize)).decodeToString())
    }
}

// The value compatible with current Linux defaults
private const val PATH_RESOLUTION_MAX_LINKS_DEPTH = 40

private fun resolvePathImpl(path: Path, recursion: Int): Path? {
    if (recursion >= PATH_RESOLUTION_MAX_LINKS_DEPTH) {
        throw IOException("Too many levels of symbolic links")
    }
    val resolvedParent = when (val parent = path.parent) {
        null -> null
        else -> resolvePathImpl(parent, recursion + 1)
    }
    val withResolvedParent = when (resolvedParent) {
        null -> path
        else -> Path(resolvedParent, path.name)
    }
    // There are cases when we simply can't resolve the intermediate path, but the resulting path should be fine:
    // pre-opened directories: [/a/b/c, /a/d/e]
    // path to resolve: /a/b/c/../../d/e/f
    // The path, after normalization, is /a/d/e/f and its metadata could be fetched using the root /a/d/e.
    // However, normalization could not be performed before links resolution, and intermediate non-normalized path
    // may point to a directory not belonging to any of pre-opened directories (like /a/b/c/../..).
    // So, let's hope for the best and throw an exception after resolution and normalization.
    val preOpen = PreOpens.findPreopenOrNull(withResolvedParent) ?: return withResolvedParent
    val metadata = metadataOrNullInternal(preOpen.fd, withResolvedParent, false) ?: return withResolvedParent
    if (metadata.filetype == FileType.symbolic_link) {
        val linkTarget = readlinkInternal(preOpen.fd, withResolvedParent, metadata.filesize.toInt())
        val resolvedPath = if (linkTarget.isAbsolute || resolvedParent == null) {
            linkTarget
        } else {
            Path(resolvedParent, linkTarget.path)
        }
        return resolvePathImpl(resolvedPath, recursion + 1)
    }
    return withResolvedParent
}
