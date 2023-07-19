/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.darwin.NSInteger
import platform.darwin.NSUInteger
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
            sink.close()
        }

    override fun streamStatus() = if (isClosed()) NSStreamStatusClosed else status

    override fun streamError() = error

    override fun open() {
        if (status == NSStreamStatusNotOpen) {
            status = NSStreamStatusOpen
        }
    }

    override fun close() {
        if (status == NSStreamStatusError) return
        status = NSStreamStatusClosed
        sink.close()
    }

    @OptIn(DelicateIoApi::class)
    override fun write(buffer: CPointer<uint8_tVar>?, maxLength: NSUInteger): NSInteger {
        try {
            if (isClosed()) throw IOException("Underlying sink is closed.")
            if (status != NSStreamStatusOpen) return -1
            if (buffer == null) return -1

            status = NSStreamStatusWriting
            sink.writeToInternalBuffer {
                it.write(buffer, maxLength.toInt())
            }
            status = NSStreamStatusOpen
            return maxLength.convert()
        } catch (e: Exception) {
            error = e.toNSError()
            return -1
        }
    }

    override fun hasSpaceAvailable() = !isClosed()

    @OptIn(InternalIoApi::class)
    override fun propertyForKey(key: NSStreamPropertyKey): Any? = when (key) {
        NSStreamDataWrittenToMemoryStreamKey -> sink.buffer.snapshotAsNSData()
        else -> null
    }

    override fun description() = "$sink.asNSOutputStream()"
}
