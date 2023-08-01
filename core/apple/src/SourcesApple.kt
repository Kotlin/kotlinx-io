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

    override fun streamStatus() = if (status != NSStreamStatusError && isClosed()) NSStreamStatusClosed else status

    override fun streamError() = error

    override fun open() {
        if (status == NSStreamStatusNotOpen) {
            status = NSStreamStatusOpening
            status = NSStreamStatusOpen
            postEvent(NSStreamEventOpenCompleted)
            checkBytes()
        }
    }

    override fun close() {
        if (status == NSStreamStatusError || status == NSStreamStatusNotOpen) return
        status = NSStreamStatusClosed
        runLoop = null
        runLoopModes = listOf()
        source.close()
    }

    override fun read(buffer: CPointer<uint8_tVar>?, maxLength: NSUInteger): NSInteger {
        if (streamStatus != NSStreamStatusOpen && streamStatus != NSStreamStatusAtEnd || buffer == null) return -1
        status = NSStreamStatusReading
        try {
            if (source.exhausted()) {
                status = NSStreamStatusAtEnd
                return 0
            }
            val toRead = minOf(maxLength.toLong(), source.buffer.size, Int.MAX_VALUE.toLong()).toInt()
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

    override fun hasBytesAvailable() = !isFinished

    private val isFinished
        get() = when (streamStatus) {
            NSStreamStatusClosed, NSStreamStatusError -> true
            else -> false
        }

    override fun propertyForKey(key: NSStreamPropertyKey): Any? = null

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

    private fun checkBytes() {
        val runLoop = runLoop ?: return
        runLoop.performInModes(runLoopModes) {
            if (runLoop != this.runLoop || isFinished) return@performInModes
            try {
                val event = if (source.exhausted()) {
                    status = NSStreamStatusAtEnd
                    NSStreamEventEndEncountered
                } else {
                    NSStreamEventHasBytesAvailable
                }
                delegateOrSelf.stream(this, event)
            } catch (e: IllegalStateException) {
                // ignore closed
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
            checkBytes()
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
