/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.files

import kotlinx.io.*
import kotlinx.cinterop.*
import platform.posix.*

/*
 * The very base skeleton just to play around
 */

public actual class Path internal constructor(internal val path: String,
                                              @Suppress("UNUSED_PARAMETER") any: Any?)

public actual fun Path(path: String): Path = Path(path, null)

public actual fun Path.source(): Source {
    val openFile: CPointer<FILE> = fopen(path, "r")
        ?: throw IOException("Failed to open $path with ${strerror(errno)?.toKString()}")
    return FileSource(openFile).buffer()
}

public actual fun Path.sink(): Sink {
    val openFile: CPointer<FILE> = fopen(path, "w")
        ?: throw IOException("Failed to open $path with ${strerror(errno)?.toKString()}")
    return FileSink(openFile).buffer()
}

internal class FileSource(
    private val file: CPointer<FILE>
) : RawSource {
    private var closed = false

    override fun read(
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
        require(byteCount >= 0L) { "byteCount < 0: $byteCount" }
        require(source.size >= byteCount) { "source.size=${source.size} < byteCount=$byteCount" }
        check(!closed) { "closed" }

        val allContent = source.readByteArray(byteCount)
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
