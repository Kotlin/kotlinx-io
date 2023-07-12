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
import platform.posix.memcpy
import platform.posix.uint8_tVar

/**
 * Returns an input stream that reads from this source. Closing the stream will also close this source.
 */
public fun Source.asNSInputStream(): NSInputStream = SourceNSInputStream(this)

@OptIn(InternalIoApi::class, UnsafeNumber::class)
private class SourceNSInputStream(
    private val source: Source,
) : NSInputStream(NSData()) {

    private val isClosed: () -> Boolean = when (source) {
        is RealSource -> source::closed
        is Buffer -> {
            { false }
        }
    }

    private var error: NSError? = null
    private var pinnedBuffer: Pinned<ByteArray>? = null

    override fun streamError(): NSError? = error

    override fun open() {
        // no-op
    }

    override fun read(buffer: CPointer<uint8_tVar>?, maxLength: NSUInteger): NSInteger {
        try {
            if (isClosed()) throw IOException("Underlying source is closed.")
            if (source.exhausted()) {
                return 0
            }
            val toRead = minOf(maxLength.toInt(), source.buffer.size).toInt()
            return source.buffer.readNative(buffer, toRead).convert()
        } catch (e: Exception) {
            error = e.toNSError()
            return -1
        }
    }

    override fun getBuffer(buffer: CPointer<CPointerVar<uint8_tVar>>?, length: CPointer<NSUIntegerVar>?): Boolean {
        if (source.buffer.size > 0) {
            source.buffer.head?.let { s ->
                pinnedBuffer?.unpin()
                s.data.pin().let {
                    pinnedBuffer = it
                    buffer?.pointed?.value = it.addressOf(s.pos).reinterpret()
                    length?.pointed?.value = (s.limit - s.pos).convert()
                    return true
                }
            }
        }
        return false
    }

    override fun hasBytesAvailable() = source.buffer.size > 0

    override fun close() {
        pinnedBuffer?.unpin()
        pinnedBuffer = null
        source.close()
    }

    override fun description() = "$source.inputStream()"

    private fun Buffer.readNative(sink: CPointer<uint8_tVar>?, maxLength: Int): Int {
        val s = head ?: return 0
        val toCopy = minOf(maxLength, s.limit - s.pos)
        s.data.usePinned {
            memcpy(sink, it.addressOf(s.pos), toCopy.convert())
        }

        s.pos += toCopy
        size -= toCopy.toLong()

        if (s.pos == s.limit) {
            head = s.pop()
            SegmentPool.recycle(s)
        }

        return toCopy
    }
}
