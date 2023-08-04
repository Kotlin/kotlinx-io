/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.io.Buffer

/**
 * A sink facilitating writes into asynchronous sink by providing a buffer
 * that could be used for synchronously writing data that then could asynchronously be flushed
 * into the underlying sink.
 */
public class AsyncSink(private val sink: AsyncRawSink) : AsyncRawSink {
    private var closed: Boolean = false

    /**
     * A buffer bound to this sink. This particular buffer received the data in [AsyncSink.write] and
     * data from this buffer is flushed into the underlying sink when [flush] or [close] is invoked.
     *
     * The buffer is mainly intended for writing the data that should be then pushed downstream.
     */
    public val buffer: Buffer = Buffer()
        get() {
            checkClosed()
            return field
        }

    /**
     * Removes [byteCount] bytes from [source] and appends them to this sink's [buffer].
     *
     * Despite the declaration, this method never suspends.
     *
     * @param source the source to read data from.
     * @param byteCount the number of bytes to write.
     *
     * @throws IllegalArgumentException when the [source]'s size is below [byteCount] or [byteCount] is negative.
     * @throws IllegalStateException when the sink is closed.
     * @throws kotlinx.coroutines.CancellationException when the coroutine executing the writing is canceled.
     */
    override suspend fun write(source: Buffer, byteCount: Long) {
        checkClosed()
        this.buffer.write(source, byteCount)
    }

    override suspend fun flush() {
        checkClosed()
        emit()
        sink.flush()
    }

    override suspend fun close() {
        if (closed) {
            return
        }
        flush()
        sink.close()
        closed = true
    }

    private suspend fun emit() {
        sink.write(buffer, buffer.size)
    }

    private fun checkClosed() {
        check(!closed) { "The source is closed." }
    }
}
