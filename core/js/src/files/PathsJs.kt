/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.*

internal val buffer: dynamic
    get(): dynamic {
        return try {
            js("require('buffer')")
        } catch (t: Throwable) {
            null
        }
    }

private val pathLib: dynamic
    get(): dynamic {
        return try {
            js("require('path')")
        } catch (t: Throwable) {
            null
        }
    }

public actual class Path internal constructor(
    internal val path: String,
    @Suppress("UNUSED_PARAMETER") any: Any?
) {
    public actual val parent: Path?
        get() {
            check(pathLib !== null) { "Path module not found" }
            when {
                path.isBlank() -> return null
                !path.contains(SystemPathSeparator) -> return null
            }
            val p = pathLib.dirname(path) as String?
            return when {
                p.isNullOrBlank() -> null
                p == path -> null
                else -> Path(p)
            }
        }

    public actual val isAbsolute: Boolean
        get() {
            check(pathLib !== null) { "Path module not found" }
            return pathLib.isAbsolute(path) as Boolean
        }

    public actual val name: String
        get() {
            check(pathLib !== null) { "Path module not found" }
            when {
                path.isBlank() -> return ""
            }
            val p = pathLib.basename(path) as String?
            return when {
                p.isNullOrBlank() -> ""
                else -> p
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
}

public actual val SystemPathSeparator: Char by lazy {
    check(pathLib != null) { "Path module not found" }
    val sep = pathLib.sep as String
    check(sep.length == 1)
    sep[0]
}

public actual fun Path(path: String): Path {
    return Path(path, null)
}

internal class FileSource(private val path: Path) : RawSource {
    private var buffer: dynamic = null
    private var closed = false
    private var offset = 0
    private val fd = open(path)

    private fun open(path: Path): dynamic {
        if (!(fs.existsSync(path.path) as Boolean)) {
            throw FileNotFoundException("File does not exist: ${path.path}")
        }
        val fd = try {
            fs.openSync(path.path, "r")
        } catch (e: Throwable) {
            throw IOException("Failed to open a file ${path.path}.", e)
        }
        if (fd < 0) throw IOException("Failed to open a file ${path.path}.")
        return fd
    }

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        check(!closed) { "Source is closed." }
        if (byteCount == 0L) {
            return 0
        }
        if (buffer === null) {
            try {
                buffer = fs.readFileSync(fd, null)
            } catch (t: Throwable) {
                throw IOException("Failed to read data from ${path.path}", t)
            }
        }
        val len: Int = buffer.length as Int
        if (offset >= len) {
            return -1L
        }
        val bytesToRead = minOf(byteCount, (len - offset))
        for (i in 0 until bytesToRead) {
            sink.writeByte(buffer.readInt8(offset++) as Byte)
        }

        return bytesToRead
    }

    override fun close() {
        if (!closed) {
            closed = true
            fs.closeSync(fd)
        }
    }
}

internal class FileSink(path: Path, append: Boolean) : RawSink {
    private var closed = false
    private val fd = open(path, append)

    private fun open(path: Path, append: Boolean): dynamic {
        val flags = if (append) "a" else "w"
        val fd = try {
            fs.openSync(path.path, flags)
        } catch (e: Throwable) {
            throw IOException("Failed to open a file ${path.path}.", e)
        }
        if (fd < 0) throw IOException("Failed to open a file ${path.path}.")
        return fd
    }

    override fun write(source: Buffer, byteCount: Long) {
        check(!closed) { "Sink is closed." }
        if (byteCount == 0L) {
            return
        }

        var remainingBytes = minOf(byteCount, source.size)
        while (remainingBytes > 0) {
            val head = source.head!!
            val segmentBytes = head.limit - head.pos
            val buf = buffer.Buffer.allocUnsafe(segmentBytes)
            buf.fill(head.data, head.pos, segmentBytes)
            try {
                fs.writeFileSync(fd, buf)
            } catch (e: Throwable) {
                throw IOException("Write failed", e)
            }

            source.skip(segmentBytes.toLong())
            remainingBytes -= segmentBytes
        }
    }

    override fun flush() = Unit

    override fun close() {
        if (!closed) {
            closed = true
            fs.closeSync(fd)
        }
    }
}
