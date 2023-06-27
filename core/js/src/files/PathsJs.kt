/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.files

import kotlinx.io.*

private val fs: dynamic
    get(): dynamic {
        return try {
            js("require('fs')")
        } catch (t: Throwable) {
            null
        }
    }

private val buffer: dynamic
    get(): dynamic {
        return try {
            js("require('buffer')")
        } catch (t: Throwable) {
            null
        }
    }

public actual class Path internal constructor(private val path: String,
                                              @Suppress("UNUSED_PARAMETER") any: Any?) {
    override fun toString(): String = path
}

public actual fun Path(path: String): Path {
    return Path(path, null)
}

public actual fun Path.source(): Source {
    check(fs !== null) { "Module 'fs' was not found" }
    return FileSource(this).buffered()
}

public actual fun Path.sink(): Sink {
    check(fs !== null) { "Module 'fs' was not found" }
    check(buffer !== null) { "Module 'buffer' was not found" }
    return FileSink(this).buffered()
}

private class FileSource(private val path: Path) : RawSource {
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
            buffer = fs.readFileSync(path.toString(), null)
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

private class FileSink(private val path: Path) : RawSink {
    private var closed = false
    private var append = false

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
            if (append) {
                fs.appendFileSync(path.toString(), buf)
            } else {
                fs.writeFileSync(path.toString(), buf)
                append = true
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
