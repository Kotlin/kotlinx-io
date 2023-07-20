/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.darwin.NSInteger
import platform.darwin.NSUInteger
import platform.darwin.NSUIntegerVar
import platform.posix.uint8_tVar
import kotlin.native.ref.WeakReference

/**
 * Returns an input stream that reads from this source. Closing the stream will also close this source.
 *
 * @sample kotlinx.io.samples.KotlinxIoSamplesApple.asStream
 */
public fun Source.asNSInputStream(): NSInputStream = SourceNSInputStream(this)

@OptIn(InternalIoApi::class, UnsafeNumber::class)
private class SourceNSInputStream(
    private val source: Source
) : NSInputStream(NSData()), NSStreamDelegateProtocol {

    private val isClosed: () -> Boolean = when (source) {
        is RealSource -> source::closed
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
            source.close()
        }

    override fun streamStatus() = if (isClosed()) NSStreamStatusClosed else status

    override fun streamError() = error

    override fun open() {
        if (status == NSStreamStatusNotOpen) {
            status = NSStreamStatusOpen
            postEvent(NSStreamEventOpenCompleted)
            checkBytes()
        }
    }

    override fun close() {
        if (status == NSStreamStatusError) return
        status = NSStreamStatusClosed
        source.close()
        runLoop = null
        runLoopModes.clear()
    }

    override fun read(buffer: CPointer<uint8_tVar>?, maxLength: NSUInteger): NSInteger {
        try {
            if (isClosed()) throw IOException("Underlying source is closed.")
            if (status != NSStreamStatusOpen && status != NSStreamStatusAtEnd) {
                return -1
            }
            if (source.exhausted()) {
                status = NSStreamStatusAtEnd
                return 0
            }
            if (buffer == null) return -1

            status = NSStreamStatusReading
            val toRead = minOf(maxLength.toInt(), source.buffer.size).toInt()
            val read = source.buffer.readAtMostTo(buffer, toRead).convert<NSInteger>()
            status = NSStreamStatusOpen
            checkBytes()
            return read
        } catch (e: Exception) {
            error = e.toNSError()
            return -1
        }
    }

    override fun getBuffer(buffer: CPointer<CPointerVar<uint8_tVar>>?, length: CPointer<NSUIntegerVar>?) = false

    override fun hasBytesAvailable() = !source.exhausted()

    override fun propertyForKey(key: NSStreamPropertyKey): Any? = null

    override fun setProperty(property: Any?, forKey: NSStreamPropertyKey) = false

    private var _delegate = WeakReference<NSStreamDelegateProtocol>(this)
    private var runLoop: NSRunLoop? = null
    private var runLoopModes = mutableListOf<NSRunLoopMode>()

    private fun postEvent(event: NSStreamEvent) {
        val delegate = delegate ?: return
        runLoop?.performInModes(runLoopModes) {
            delegate.stream(this, event)
        }
    }

    private fun checkBytes(sendEndEvent: Boolean = true) {
        runLoop?.performInModes(runLoopModes) {
            try {
                if (source.exhausted()) {
                    if (sendEndEvent) delegate?.stream(this, NSStreamEventEndEncountered)
                    val timer = NSTimer.timerWithTimeInterval(0.1, false) {
                        checkBytes(sendEndEvent = false)
                    }
                    runLoopModes.forEach { mode ->
                        runLoop?.addTimer(timer, mode)
                    }
                } else {
                    delegate?.stream(this, NSStreamEventHasBytesAvailable)
                }
            } catch (e: IllegalStateException) {
                // ignore closed
            }
        }
    }

    override fun delegate() = _delegate.value

    override fun setDelegate(delegate: NSStreamDelegateProtocol?) {
        _delegate = WeakReference(delegate ?: this)
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
    }

    override fun removeFromRunLoop(aRunLoop: NSRunLoop, forMode: NSRunLoopMode) {
        if (aRunLoop == runLoop) {
            runLoopModes -= forMode
            if (runLoopModes.isEmpty()) {
                runLoop = null
            }
        }
    }

    override fun description() = "$source.asNSInputStream()"
}
