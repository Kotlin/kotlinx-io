/*
 * Copyright 2017-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.files

import kotlinx.cinterop.*
import kotlinx.io.*
import kotlinx.io.unsafe.UnsafeBufferOperations
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

private fun throwIOExceptionForErrno(operation: String): Nothing {
    val err = errno
    val strerr = strerror(err)?.toKString() ?: "unknown error"
    throw IOException("$operation failed with errno $err ($strerr)")
}

internal class FileSource(
    private val fd: Int
) : RawSource {
    private var closed = false

    @OptIn(UnsafeIoApi::class)
    override fun readAtMostTo(
        sink: Buffer,
        byteCount: Long
    ): Long {
        val bytesRead = UnsafeBufferOperations.writeToTail(sink, 1) { bytes, startIndex, endIndex ->
            val toRead = minOf((endIndex - startIndex).toLong(), byteCount)
            val bytesRead = bytes.usePinned {
                @OptIn(UnsafeNumber::class)
                @Suppress("REDUNDANT_CALL_OF_CONVERSION_METHOD") // https://youtrack.jetbrains.com/issue/KT-81896
                read(fd, it.addressOf(startIndex), toRead.convert()).toInt()
            }
            if (bytesRead == -1) {
                throwIOExceptionForErrno("read")
            }
            bytesRead
        }

        return when (bytesRead) {
            0 -> -1L
            else -> bytesRead.toLong()
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        if (close(fd) != 0) {
            throwIOExceptionForErrno("close")
        }
    }
}

internal class FileSink(
    private val fd: Int
) : RawSink {
    private var closed = false

    @OptIn(UnsafeIoApi::class)
    override fun write(
        source: Buffer,
        byteCount: Long
    ) {
        require(byteCount >= 0L) { "byteCount: $byteCount" }
        require(source.size >= byteCount) { "source.size=${source.size} < byteCount=$byteCount" }
        check(!closed) { "closed" }

        var remaining = byteCount
        while (remaining > 0) {
            remaining -= UnsafeBufferOperations.readFromHead(source) { bytes, startIndex, endIndex ->
                val toWrite = minOf((endIndex - startIndex).toLong(), remaining)
                val bytesWritten = bytes.usePinned {
                    @OptIn(UnsafeNumber::class)
                    @Suppress("REDUNDANT_CALL_OF_CONVERSION_METHOD") // https://youtrack.jetbrains.com/issue/KT-81896
                    write(fd, it.addressOf(startIndex), toWrite.convert()).toInt()
                }
                if (bytesWritten == -1) {
                    throwIOExceptionForErrno("write")
                }
                bytesWritten
            }
        }
    }

    /**
     * This method does nothing as writes are sent directly to the file.
     */
    override fun flush() = Unit

    override fun close() {
        if (closed) return
        closed = true
        if (close(fd) != 0) {
            throwIOExceptionForErrno("close")
        }
    }
}
