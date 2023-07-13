/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.darwin.NSInteger
import platform.darwin.NSUInteger
import platform.posix.memcpy
import platform.posix.uint8_tVar

/**
 * Returns an output stream that writes to this sink. Closing the stream will also close this sink.
 *
 * @sample kotlinx.io.samples.KotlinxIoSamplesApple.asStream
 */
public fun Sink.asNSOutputStream(): NSOutputStream = SinkNSOutputStream(this)

@OptIn(UnsafeNumber::class)
private class SinkNSOutputStream(
    private val sink: Sink,
) : NSOutputStream(toMemory = Unit) {

    private val isClosed: () -> Boolean = when (sink) {
        is RealSink -> sink::closed
        is Buffer -> {
            { false }
        }
    }

    private var status = NSStreamStatusNotOpen
    private var error: NSError? = null
        set(value) {
            status = NSStreamStatusError
            field = value
        }

    override fun streamStatus() = status

    override fun streamError() = error

    override fun open() {
        if (status == NSStreamStatusNotOpen) {
            status = NSStreamStatusOpen
        }
    }

    @OptIn(DelicateIoApi::class)
    override fun write(buffer: CPointer<uint8_tVar>?, maxLength: NSUInteger): NSInteger {
        return try {
            if (isClosed()) throw IOException("Underlying sink is closed.")
            if (status != NSStreamStatusOpen) return -1
            status = NSStreamStatusWriting
            sink.writeToInternalBuffer {
                it.writeNative(buffer, maxLength.toInt())
            }
            status = NSStreamStatusOpen
            maxLength.convert()
        } catch (e: Exception) {
            error = e.toNSError()
            -1
        }
    }

    override fun hasSpaceAvailable() = true

    override fun close() {
        status = NSStreamStatusClosed
        sink.close()
    }

    override fun description() = "$sink.asNSOutputStream()"

    private fun Buffer.writeNative(source: CPointer<uint8_tVar>?, maxLength: Int) {
        var currentOffset = 0
        while (currentOffset < maxLength) {
            val tail = writableSegment(1)

            val toCopy = minOf(maxLength - currentOffset, Segment.SIZE - tail.limit)
            tail.data.usePinned {
                memcpy(it.addressOf(tail.pos), source + currentOffset, toCopy.convert())
            }

            currentOffset += toCopy
            tail.limit += toCopy
        }
        size += maxLength
    }
}
