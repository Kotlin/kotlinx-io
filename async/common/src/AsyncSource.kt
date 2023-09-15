/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.Buffer
import kotlinx.io.IOException

/**
 * A source facilitating asynchonous reading by providing a buffer along with functions
 * suspending until the buffered data met specific requirements represented by a [AwaitPredicate] instances.
 *
 * The typical usage of AsyncSource it to asynchronously fetch enough data into the [buffer]
 * using [awaitOrThrow] or [await] and then process buffered data synchronously.
 */
public class AsyncSource(private val source: AsyncRawSource, private val fetchHint: Long = 8192L) : AsyncRawSource {
    private var closed: Boolean = false

    /**
     * A buffer bound to this source. The buffer could be filled by calling [awaitOrThrow] or [await].
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
     * @throws IllegalStateException if the source was closed when this method was called.
     * @throws kotlinx.coroutines.CancellationException when the coroutine executing the await is canceled.
     */
    public suspend fun awaitOrThrow(until: AwaitPredicate) {
        if (!await(until).getOrThrow()) {
            throw IOException("Predicate could not be fulfilled.")
        }
    }

    /**
     * Fills the [buffer] with data from the underlying source until the condition represented by the
     * [until] predicate is met, the underlying source exhausted or closed.
     * If the predicate is fulfilled the method returns a result containing `true`,
     * otherwise it returns a result containing `false`.
     *
     * The [until] predicate is invoked on the [buffer] and fetches the data from the underlying source.
     *
     * Refer to the [awaitOrThrow] documentation for exception that the result may hold in case of failure.
     *
     * @param until the predicate checking when the awaiting should stop.
     *
     * @return [Result] holding `true` if the [until] predicate is fulfilled, holding `false` if predicate was
     * not fulfilled, or holding and exception thrown while awaiting.
     */
    public suspend fun await(until: AwaitPredicate): Result<Boolean> {
        return try {
            checkClosed()
            currentCoroutineContext().ensureActive()
            Result.success(until.apply(buffer) {
                currentCoroutineContext().ensureActive()
                source.readAtMostTo(buffer, fetchHint) >= 0
            })
        } catch (t: Throwable) {
            Result.failure(t)
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

    override fun closeAbruptly() {
        closed = true
        source.closeAbruptly()
    }

    override suspend fun close() {
        closed = true
        source.close()
    }

    private fun checkClosed() {
        check(!closed) { "The source is closed." }
    }
}
