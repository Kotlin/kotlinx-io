/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.cinterop.*
import platform.Foundation.NSInputStream
import platform.Foundation.NSOutputStream
import platform.Foundation.NSStreamStatusNotOpen
import platform.darwin.UInt8Var

/**
 * Returns [RawSink] that writes to an output stream.
 *
 * Use [RawSink.buffered] to create a buffered sink from it.
 *
 * @sample kotlinx.io.samples.KotlinxIoSamplesApple.outputStreamAsSink
 */
public fun NSOutputStream.asSink(): RawSink = OutputStreamSink(this)

private open class OutputStreamSink(
    private val out: NSOutputStream,
) : RawSink {

    @OptIn(UnsafeNumber::class)
    override fun write(source: Buffer, byteCount: Long) {
        if (out.streamStatus == NSStreamStatusNotOpen) out.open()

        checkOffsetAndCount(source.size, 0, byteCount)
        var remaining = byteCount
        while (remaining > 0) {
            val head = source.head!!
            val toCopy = minOf(remaining, head.limit - head.pos).toInt()
            val bytesWritten = head.data.usePinned {
                val bytes = it.addressOf(head.pos).reinterpret<UInt8Var>()
                out.write(bytes, toCopy.convert()).toLong()
            }

            if (bytesWritten < 0L) throw IOException(out.streamError?.localizedDescription ?: "Unknown error")
            if (bytesWritten == 0L) throw IOException("NSOutputStream reached capacity")

            head.pos += bytesWritten.toInt()
            remaining -= bytesWritten
            source.size -= bytesWritten

            if (head.pos == head.limit) {
                source.head = head.pop()
                SegmentPool.recycle(head)
            }
        }
    }

    override fun flush() {
        // no-op
    }

    override fun close() = out.close()

    override fun toString() = "RawSink($out)"
}

/**
 * Returns [RawSource] that reads from an input stream.
 *
 * Use [RawSource.buffered] to create a buffered source from it.
 *
 * @sample kotlinx.io.samples.KotlinxIoSamplesApple.inputStreamAsSource
 */
public fun NSInputStream.asSource(): RawSource = NSInputStreamSource(this)

private open class NSInputStreamSource(
    private val input: NSInputStream,
) : RawSource {

    @OptIn(UnsafeNumber::class)
    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (input.streamStatus == NSStreamStatusNotOpen) input.open()

        if (byteCount == 0L) return 0L
        checkByteCount(byteCount)

        val tail = sink.writableSegment(1)
        val maxToCopy = minOf(byteCount, Segment.SIZE - tail.limit)
        val bytesRead = tail.data.usePinned {
            val bytes = it.addressOf(tail.limit).reinterpret<UInt8Var>()
            input.read(bytes, maxToCopy.convert()).toLong()
        }

        if (bytesRead < 0L) throw IOException(input.streamError?.localizedDescription ?: "Unknown error")
        if (bytesRead == 0L) {
            if (tail.pos == tail.limit) {
                // We allocated a tail segment, but didn't end up needing it. Recycle!
                sink.head = tail.pop()
                SegmentPool.recycle(tail)
            }
            return -1
        }
        tail.limit += bytesRead.toInt()
        sink.size += bytesRead
        return bytesRead
    }

    override fun close() = input.close()

    override fun toString() = "RawSource($input)"
}
