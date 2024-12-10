/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.files

import kotlinx.cinterop.*
import kotlinx.io.*
import platform.posix.*

/*
 * The very base skeleton just to play around
 */

public actual class Path internal constructor(
    rawPath: String,
    @Suppress("UNUSED_PARAMETER") any: Any?
) {
    internal val path = removeTrailingSeparators(rawPath)

    public actual val parent: Path?
        get() {
            when {
                path.isEmpty() -> return null
            }
            val parentName = dirnameImpl(path)
            return when {
                parentName.isEmpty() -> return null
                parentName == path -> return null
                else -> Path(parentName)
            }
        }

    public actual override fun toString(): String = path
    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Path) return false

        return path == other.path
    }

    actual override fun hashCode(): Int {
        return path.hashCode()
    }

    public actual val isAbsolute: Boolean = isAbsoluteImpl(path)
    public actual val name: String
        get() {
            if (path.isEmpty() || path == SystemPathSeparator.toString()) return ""
            return basenameImpl(path)
        }
}

public actual val SystemPathSeparator: Char get() = UnixPathSeparator

internal expect fun dirnameImpl(path: String): String

internal expect fun basenameImpl(path: String): String

internal expect fun isAbsoluteImpl(path: String): Boolean

public actual fun Path(path: String): Path = Path(path, null)

internal class FileSource(
    private val file: CPointer<FILE>
) : RawSource {
    private var closed = false

    override fun readAtMostTo(
        sink: Buffer,
        byteCount: Long
    ): Long {
        val temporaryBuffer = ByteArray(byteCount.toInt())

        // Copy bytes from the file to the segment.
        val bytesRead = temporaryBuffer.usePinned { pinned ->
            variantFread(pinned.addressOf(0), byteCount.toUInt(), file).toLong()
        }

        sink.write(temporaryBuffer, 0, bytesRead.toInt())

        return when {
            bytesRead == byteCount -> bytesRead
            feof(file) != 0 -> if (bytesRead == 0L) -1L else bytesRead
            ferror(file) != 0 -> throw IOException(errno.toString())
            else -> bytesRead
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        fclose(file)
    }
}

@OptIn(UnsafeNumber::class)
internal fun variantFread(
    target: CPointer<ByteVarOf<Byte>>,
    byteCount: UInt,
    file: CPointer<FILE>
): UInt = fread(target, 1u, byteCount.convert(), file).convert()

@OptIn(UnsafeNumber::class)
internal fun variantFwrite(
    source: CPointer<ByteVar>,
    byteCount: UInt,
    file: CPointer<FILE>
): UInt = fwrite(source, 1u, byteCount.convert(), file).convert()

internal class FileSink(
    private val file: CPointer<FILE>
) : RawSink {
    private var closed = false

    override fun write(
        source: Buffer,
        byteCount: Long
    ) {
        require(byteCount >= 0L) { "byteCount: $byteCount" }
        require(source.size >= byteCount) { "source.size=${source.size} < byteCount=$byteCount" }
        check(!closed) { "closed" }

        val allContent = source.readByteArray(byteCount.toInt())
        // Copy bytes from that segment into the file.
        val bytesWritten = allContent.usePinned { pinned ->
            variantFwrite(pinned.addressOf(0), byteCount.toUInt(), file).toLong()
        }
        if (bytesWritten < byteCount) {
            throw IOException(errno.toString())
        }
    }

    override fun flush() {
        if (fflush(file) != 0) {
            throw IOException(errno.toString())
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        if (fclose(file) != 0) {
            throw IOException(errno.toString())
        }
    }
}
