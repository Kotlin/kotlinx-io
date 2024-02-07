/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.*
import kotlinx.io.wasi.*
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

public actual val SystemTemporaryDirectory: Path
    get() = PreOpens.roots.find { it.path.startsWith("/tmp") } ?: throw IOException("'/tmp' was not pre-opened")

@OptIn(UnsafeWasmMemoryApi::class)
public actual val SystemFileSystem: FileSystem = object : SystemFileSystemImpl() {

    override fun exists(path: Path): Boolean {
        if (!path.isAbsolute) throw UnsupportedOperationException("Can't handle relative paths")

        return metadataOrNull(path) != null
    }

    override fun delete(path: Path, mustExist: Boolean) {
        val root = PreOpens.getRoot(path)
        withScopedMemoryAllocator { allocator ->
            val (stringBuffer, stringLength) = path.path.store(allocator)

            val unlinkRes = Errno(path_unlink_file(root.fd, stringBuffer.address.toInt(), stringLength))
            if (unlinkRes == Errno.success) return
            if (unlinkRes == Errno.noent) {
                if (mustExist) throw FileNotFoundException("File does not exist: $path")
                return
            }
            if (unlinkRes != Errno.isdir && unlinkRes != Errno.perm) {
                throw IOException("Unable to remove file $path: ${unlinkRes.description}")
            }

            val removeDirRes = Errno(path_remove_directory(root.fd, stringBuffer.address.toInt(), stringLength))
            if (removeDirRes == Errno.success) return
            if (removeDirRes == Errno.noent) {
                if (mustExist) throw FileNotFoundException("File does not exist: $path")
                return
            }
            throw IOException("Unable to remove directory ${path.path}: ${unlinkRes.description}")
        }
    }

    override fun createDirectories(path: Path, mustCreate: Boolean) {
        val root = PreOpens.getRoot(path)
        val segments: List<String> = buildList {
            var currentPath: Path? = path
            while (currentPath != null && currentPath != root.path) {
                add(currentPath.path)
                currentPath = currentPath.parent
            }
        }
        var created = false
        withScopedMemoryAllocator { allocator ->
            val pathBuffer = allocator.allocate(segments.first().encodeToByteArray().size + 1)

            for (idx in segments.size - 1 downTo 0) {
                val segment = segments[idx]
                val segmentLength = segment.store(pathBuffer)

                val res = Errno(path_create_directory(root.fd, pathBuffer.address.toInt(), segmentLength))
                if (res == Errno.success) {
                    created = true
                } else if (res != Errno.exist) {
                    throw IOException("Can't create directory $path: ${res.description}")
                }
            }
            if (mustCreate && !created) throw IOException("Directory already exists")
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
        val sourceRoot = PreOpens.getRoot(source)
        val destRoot = PreOpens.getRoot(destination)

        withScopedMemoryAllocator { allocator ->
            val (sourceBuffer, sourceBufferLength) = source.path.store(allocator)
            val (destBuffer, destBufferLength) = destination.path.store(allocator)

            val res = Errno(
                path_rename(
                    oldFd = sourceRoot.fd, oldPathPtr = sourceBuffer.address.toInt(), oldPathLen = sourceBufferLength,
                    newFd = destRoot.fd, newPathPtr = destBuffer.address.toInt(), newPathLen = destBufferLength
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
        val root = PreOpens.getRoot(path)

        val fd = withScopedMemoryAllocator { allocator ->
            val fdPtr = allocator.allocate(4)
            val (stringBuffer, stringBufferLength) = path.path.store(allocator)

            val res = Errno(
                path_open(
                    fd = root.fd,
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
                throw IOException("Can't open $path for write: ${res.description}")
            }
            fdPtr.loadInt()
        }
        return FileSource(fd)
    }

    @OptIn(UnsafeWasmMemoryApi::class)
    override fun sink(path: Path, append: Boolean): RawSink {
        val root = PreOpens.getRoot(path)

        val fd = withScopedMemoryAllocator { allocator ->
            val fdPtr = allocator.allocate(4)
            val (stringBuffer, stringBufferLength) = path.path.store(allocator)

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
                    fd = root.fd,
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
        val root = PreOpens.getRoot(path)
        val md = metadataOrNullInternal(root.fd, path) ?: return null

        val filetype = md.filetype
        val isDirectory = filetype == FileType.directory
        val filesize = if (isDirectory) -1 else md.filesize
        return FileMetadata(
            isRegularFile = filetype == FileType.regular_file,
            isDirectory = isDirectory,
            size = filesize
        )
    }

    override fun resolve(path: Path): Path {
        val root = PreOpens.getRoot(path)

        val parts = mutableListOf<String>()
        var part: Path? = path
        while (part != null && part != root.path) {
            parts.add(part.name)
            part = part.parent
        }
        val stack = mutableListOf<String>()
        parts.reversed().forEach {
            if (it != ".") {
                if (it == "..") {
                    if (stack.isEmpty()) {
                        throw IOException("Path points outside root directory: $path, root: ${root.path}")
                    }
                    stack.removeLast()
                } else {
                    stack.add(it)
                }
            }
        }

        var resolvedPath = root.path
        for (subDir in stack) {
            resolvedPath = Path(resolvedPath, subDir)

            val newRoot =
                PreOpens.getRoot(resolvedPath)
            val md = metadataOrNullInternal(newRoot.fd, resolvedPath)
                ?: throw FileNotFoundException("Path does not exists: $resolvedPath")
            if (md.filetype == FileType.symbolic_link) {
                resolvedPath = readlinkInternal(newRoot.fd, resolvedPath, md.filesize.toInt())
            }
        }
        return resolvedPath
    }
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

    internal fun getRootOrNull(path: Path): PreOpen? {
        if (!path.isAbsolute) {
            return preopens.firstOrNull()
        }

        for (root in preopens) {
            var p: Path? = path
            while (p != null) {
                if (root.path == p) return root
                p = p.parent
            }
        }
        return null
    }

    internal fun getRoot(path: Path): PreOpen {
        return getRootOrNull(path) ?: throw IOException("Path does not belong to any preopened directory: $path")
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
            val buffer = allocator.allocate(TEMP_CIOVEC_BUFFER_LEN)
            var remaining = byteCount

            val ciovec = Ciovec(allocator).also {
                it.buffer = buffer
            }

            val resultPtr = allocator.allocate(4)

            while (remaining > 0) {
                val toRead = minOf(remaining, TEMP_CIOVEC_BUFFER_LEN).toInt()
                source.readToLinearMemory(buffer, toRead)
                ciovec.length = toRead

                val res = Errno(fd_write(fd, ciovec.address, 1, resultPtr.address.toInt()))
                if (res != Errno.success) {
                    throw IOException("Write failed: ${res.description}")
                }
                check(resultPtr.loadInt(0) == toRead) {
                    "Expected to write $toRead, but ${resultPtr.loadInt(0)} bytes were written"
                }
                remaining -= toRead
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
            val buffer = allocator.allocate(TEMP_CIOVEC_BUFFER_LEN)
            var remaining = byteCount

            val ciovec = Ciovec(allocator).also {
                it.buffer = buffer
            }

            val resultPtr = allocator.allocate(4)
            while (remaining > 0) {
                val toRead = minOf(remaining, TEMP_CIOVEC_BUFFER_LEN).toInt()
                ciovec.length = toRead

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
                sink.writeFromLinearMemory(buffer, readBytes)
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
private fun metadataOrNullInternal(rootFd: Fd, path: Path): InternalMetadata? {
    withScopedMemoryAllocator { allocator ->
        val (pathBuffer, pathBufferLength) = path.path.store(allocator)

        val filestat = FileStat(allocator)

        val res = Errno(
            path_filestat_get(
                fd = rootFd,
                flags = listOf(LookupFlags.symlink_follow).toBitset(),
                pathPtr = pathBuffer.address.toInt(), pathLen = pathBufferLength,
                resultPtr = filestat.address
            )
        )

        if (res == Errno.noent) return null
        if (res != Errno.success) throw IOException(res.description)

        return InternalMetadata(filestat.filetype, filestat.filesize)
    }
}

@OptIn(UnsafeWasmMemoryApi::class)
private fun readlinkInternal(rootFd: Fd, path: Path, linkSize: Int): Path {
    withScopedMemoryAllocator { allocator ->
        val resultPtr = allocator.allocate(4)
        val (pathBuffer, pathBufferLength) = path.path.store(allocator)
        val resultBuffer = allocator.allocate(linkSize)

        val res = Errno(
            path_readlink(
                fd = rootFd,
                pathPtr = pathBuffer.address.toInt(), pathLen = pathBufferLength,
                bufPtr = resultBuffer.address.toInt(), bufLen = linkSize,
                resultPtr = resultPtr.address.toInt()
            )
        )
        if (res != Errno.success) {
            throw IOException("Link resolution failed for path $path: ${res.description}")
        }
        val resultLength = resultPtr.loadInt()
        return Path(resultBuffer.loadBytes(resultLength).decodeToString())
    }
}
