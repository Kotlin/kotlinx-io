/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.Buffer
import kotlinx.io.IOException

private const val SEGMENT_SIZE: Long = 8192L

/**
 * A source facilitating asynchonous reading by providing a buffer along with functions
 * suspending until the buffered data met specific requirements represented by a [AwaitPredicate] instances.
 *
 * The typical usage of AsyncSource it to asynchronously fetch enough data into the [buffer] using [await] or [tryAwait]
 * and then process buffered data synchronously.
 */
public class AsyncSource(private val source: AsyncRawSource) : AsyncRawSource {
    private var closed: Boolean = false

    /**
     * A buffer bound to this source. The buffer could be filled by calling [await] or [tryAwait].
     */
    public val buffer: Buffer = Buffer()
        get() {
            checkClosed()
            return field
        }

    /**
     * Fills the [buffer] with data from the underlying source until the condition represented by the
     * [until] predicate is met, the underlying source exhausted or closed. If the predicate could not be
     * fulfilled (no matter why - due to lack of interesting data or because the underlying source is
     * exhausted) this method throws [IOException].
     *
     * The [until] predicate is invoked on the [buffer] and fetches the data from the underlying source.
     *
     * @param until the predicate checking when the awaiting should stop.
     *
     * @throws IOException if the [until] predicate could not be fulfilled.
     * @throws IllegalStateException if the source is closed.
     * @throws kotlinx.coroutines.CancellationException when the coroutine executing the await is canceled.
     */
    public suspend fun await(until: AwaitPredicate) {
        if (!tryAwait(until)) {
            throw IOException("Predicate could not be fulfilled.")
        }
    }

    /**
     * Fills the [buffer] with data from the underlying source until the condition represented by the
     * [until] predicate is met, the underlying source exhausted or closed.
     * If the predicate is fulfilled the method returns `true`, otherwise it returns `false`.
     *
     * The [until] predicate is invoked on the [buffer] and fetches the data from the underlying source.
     *
     * @param until the predicate checking when the awaiting should stop.
     *
     * @return `true` if the [until] predicate is fulfilled, `false` otherwise
     *
     * @throws IllegalStateException if the source is closed.
     * @throws kotlinx.coroutines.CancellationException when the coroutine executing the tryAwait is canceled.
     */
    public suspend fun tryAwait(until: AwaitPredicate): Boolean {
        checkClosed()
        currentCoroutineContext().ensureActive()
        return until.apply(buffer) {
            currentCoroutineContext().ensureActive()
            source.readAtMostTo(buffer, SEGMENT_SIZE) >= 0
        }
    }

    override suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        checkClosed()
        return if (this.buffer.exhausted()) {
            source.readAtMostTo(sink, byteCount)
        } else {
            this.buffer.readAtMostTo(sink, byteCount)
        }
    }

    override fun close() {
        source.close()
        closed = true
    }

    private fun checkClosed() {
        check(!closed) { "The source is closed." }
    }
}
