/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.cinterop.*
import platform.Foundation.NSInputStream
import platform.Foundation.NSStreamStatusNotOpen
import platform.darwin.UInt8Var

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
