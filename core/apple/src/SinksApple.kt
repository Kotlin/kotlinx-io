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
import kotlin.native.ref.WeakReference

/**
 * Returns an output stream that writes to this sink. Closing the stream will also close this sink.
 *
 * @sample kotlinx.io.samples.KotlinxIoSamplesApple.asStream
 */
public fun Sink.asNSOutputStream(): NSOutputStream = SinkNSOutputStream(this)

@OptIn(UnsafeNumber::class)
private class SinkNSOutputStream(
    private val sink: Sink
) : NSOutputStream(toMemory = Unit), NSStreamDelegateProtocol {

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
            postEvent(NSStreamEventErrorOccurred)
            sink.close()
        }

    override fun streamStatus() = if (status != NSStreamStatusError && isClosed()) NSStreamStatusClosed else status

    override fun streamError() = error

    override fun open() {
        if (status == NSStreamStatusNotOpen) {
            status = NSStreamStatusOpening
            status = NSStreamStatusOpen
            postEvent(NSStreamEventOpenCompleted)
            postEvent(NSStreamEventHasSpaceAvailable)
        }
    }

    override fun close() {
        if (status == NSStreamStatusError || status == NSStreamStatusNotOpen) return
        status = NSStreamStatusClosed
        runLoop = null
        runLoopModes = listOf()
        sink.close()
    }

    @OptIn(DelicateIoApi::class)
    override fun write(buffer: CPointer<uint8_tVar>?, maxLength: NSUInteger): NSInteger {
        if (streamStatus != NSStreamStatusOpen || buffer == null) return -1
        status = NSStreamStatusWriting
        val toWrite = minOf(maxLength, Int.MAX_VALUE.convert()).toInt()
        return try {
            sink.writeToInternalBuffer {
                it.write(buffer, toWrite)
            }
            status = NSStreamStatusOpen
            toWrite.convert()
        } catch (e: Exception) {
            error = e.toNSError()
            -1
        }
    }

    override fun hasSpaceAvailable() = !isFinished

    private val isFinished
        get() = when (streamStatus) {
            NSStreamStatusClosed, NSStreamStatusError -> true
            else -> false
        }

    @OptIn(InternalIoApi::class)
    override fun propertyForKey(key: NSStreamPropertyKey): Any? = when (key) {
        NSStreamDataWrittenToMemoryStreamKey -> sink.buffer.snapshotAsNSData()
        else -> null
    }

    override fun setProperty(property: Any?, forKey: NSStreamPropertyKey) = false

    // WeakReference as delegate should not be retained
    // https://developer.apple.com/documentation/foundation/nsstream/1418423-delegate
    private var _delegate: WeakReference<NSStreamDelegateProtocol>? = null
    private var runLoop: NSRunLoop? = null
    private var runLoopModes = listOf<NSRunLoopMode>()

    private fun postEvent(event: NSStreamEvent) {
        val runLoop = runLoop ?: return
        runLoop.performInModes(runLoopModes) {
            if (runLoop == this.runLoop) {
                delegateOrSelf.stream(this, event)
            }
        }
    }

    override fun delegate() = _delegate?.value

    private val delegateOrSelf get() = delegate ?: this

    override fun setDelegate(delegate: NSStreamDelegateProtocol?) {
        _delegate = delegate?.let { WeakReference(it) }
    }

    override fun stream(aStream: NSStream, handleEvent: NSStreamEvent) {
        // no-op
    }

    override fun scheduleInRunLoop(aRunLoop: NSRunLoop, forMode: NSRunLoopMode) {
        if (runLoop == null) {
            runLoop = aRunLoop
        }
        if (runLoop == aRunLoop) {
            runLoopModes += forMode
        }
        if (status == NSStreamStatusOpen) {
            postEvent(NSStreamEventHasSpaceAvailable)
        }
    }

    override fun removeFromRunLoop(aRunLoop: NSRunLoop, forMode: NSRunLoopMode) {
        if (aRunLoop == runLoop) {
            runLoopModes -= forMode
            if (runLoopModes.isEmpty()) {
                runLoop = null
            }
        }
    }

    override fun description() = "$sink.asNSOutputStream()"
}
