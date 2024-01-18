/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.*

public actual class Path internal constructor(
    rawPath: String,
    @Suppress("UNUSED_PARAMETER") any: Any?
) {
    internal val path: String = removeTrailingSeparators(rawPath)

    public actual val parent: Path?
        get() {
            check(pathLib !== null) { "Path module not found" }
            if (path.isEmpty()) return null
            if (isWindows) {
                if (!path.contains(UnixPathSeparator) && !path.contains(WindowsPathSeparator)) {
                    return null
                }
            } else if (!path.contains(SystemPathSeparator)) {
                return null
            }
            val p = pathLib.dirname(path) as String?
            return when {
                p.isNullOrEmpty() -> null
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
                path.isNullOrEmpty() -> return ""
            }
            val p = pathLib.basename(path) as String?
            return when {
                p.isNullOrEmpty() -> ""
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

private const val WindowsPathSeparator: Char = '\\'
private const val UnixPathSeparator: Char = '/'
private val isWindows = os.platform() == "win32"

public actual fun Path(path: String): Path {
    return Path(path, null)
}

internal class FileSource(private val path: Path) : RawSource {
    private var buffer: dynamic = null
    private var closed = false
    private var offset = 0

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        check(!closed) { "Source is closed." }
        if (byteCount == 0L) {
            return 0
        }
        if (buffer === null) {
            try {
                buffer = fs.readFileSync(path.toString(), null)
            } catch (t: Throwable) {
                if (fs.existsSync(path.path) as Boolean) {
                    throw IOException("Failed to read data from $path", t)
                }
                throw FileNotFoundException("File does not exist: $path")
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
        closed = true
    }
}

internal class FileSink(private val path: Path, private var append: Boolean) : RawSink {
    private var closed = false

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
                if (append) {
                    fs.appendFileSync(path.toString(), buf)
                } else {
                    fs.writeFileSync(path.toString(), buf)
                    append = true
                }
            } catch (e: Throwable) {
                throw IOException("Write failed", e)
            }

            source.skip(segmentBytes.toLong())
            remainingBytes -= segmentBytes
        }
    }

    override fun flush() = Unit

    override fun close() {
        closed = true
    }
}

internal fun removeTrailingSeparators(path: String): String {
    if (isWindows) {
        // don't trim the path separator right after the drive name
        val limit = if (path.length > 1 && path[1] == ':') 3 else 1
        return removeTrailingSeparators(limit, path, UnixPathSeparator, WindowsPathSeparator)
    }
    return removeTrailingSeparators(path, SystemPathSeparator)
}
