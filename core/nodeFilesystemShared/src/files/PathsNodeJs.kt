/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.*
import kotlinx.io.node.fs.*
import kotlinx.io.node.path.basename
import kotlinx.io.node.path.dirname
import kotlinx.io.node.path.isAbsolute
import kotlinx.io.node.path.sep

public actual class Path internal constructor(
    rawPath: String,
    @Suppress("UNUSED_PARAMETER") any: Any?
) {
    internal val path: String = removeTrailingSeparators(rawPath)

    public actual val parent: Path?
        get() {
            if (path.isEmpty()) return null
            if (isWindows) {
                if (!path.contains(UnixPathSeparator) && !path.contains(WindowsPathSeparator)) {
                    return null
                }
            } else if (!path.contains(SystemPathSeparator)) {
                return null
            }
            val p = dirname(path)
            return when {
                p.isEmpty() -> null
                p == path -> null
                else -> Path(p)
            }
        }

    public actual val isAbsolute: Boolean
        get() {
            return isAbsolute(path)
        }

    public actual val name: String
        get() {
            when {
                path.isEmpty() -> return ""
            }
            val p = basename(path)
            return when {
                p.isEmpty() -> ""
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
    val sep = sep
    check(sep.length == 1)
    sep[0]
}

public actual fun Path(path: String): Path {
    return Path(path, null)
}

internal class FileSource(private val path: Path) : RawSource {
    private var buffer: kotlinx.io.node.buffer.Buffer? = null
    private var closed = false
    private var offset = 0
    private val fd = open(path)

    private fun open(path: Path): Int {
        if (!existsSync(path.path)) {
            throw FileNotFoundException("File does not exist: ${path.path}")
        }
        val fd = try {
            openSync(path.path, "r")
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
                buffer = readFileSync(fd, null)
            } catch (t: Throwable) {
                throw IOException("Failed to read data from ${path.path}", t)
            }
        }
        val len: Int = buffer!!.length
        if (offset >= len) {
            return -1L
        }
        val bytesToRead = minOf(byteCount, (len - offset))
        for (i in 0 until bytesToRead) {
            sink.writeByte(buffer!!.readInt8(offset++))
        }

        return bytesToRead
    }

    override fun close() {
        if (!closed) {
            closed = true
            closeSync(fd)
        }
    }
}

internal class FileSink(path: Path, append: Boolean) : RawSink {
    private var closed = false
    private val fd = open(path, append)

    private fun open(path: Path, append: Boolean): Int {
        val flags = if (append) "a" else "w"
        val fd = try {
            openSync(path.path, flags)
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

            val buf = kotlinx.io.node.buffer.Buffer.allocUnsafe(segmentBytes)
            val data = head.data
            val pos = head.pos
            for (offset in 0 until segmentBytes) {
                buf.writeInt8(data[pos + offset], offset)
            }
            try {
                writeFileSync(fd, buf)
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
            closeSync(fd)
        }
    }
}
