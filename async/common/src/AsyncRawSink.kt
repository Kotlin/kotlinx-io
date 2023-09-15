/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.io.Buffer

/**
 * Receives a stream of bytes. AsyncRawSink is a base interface for asynchronous `kotlinx-io` data receivers.
 *
 * AsyncRawSink should be implemented to write data wherever it's needed, almost like [kotlinx.io.RawSink].
 * The main distinction between these interfaces is that AsyncRawSink is primarily suited for asynchronous data
 * transmitters, like non-blocking sockets. If the data could not be written, AsyncRawSink implementations should
 * suspend until it'll be possible to send the data or the sink will be closed.
 *
 * To facilitate writing into the AsyncRawSink, there's [AsyncRawSink.writeWithBuffer] function that
 * allocates a new buffer before calling a lambda and transfers its content into a sink and flushes the sink on exit.
 *
 * Kotlin's coroutines support [cancellation](https://kotlinlang.org/docs/cancellation-and-timeouts.html#cancellation-is-cooperative)
 * which is cooperative. Implementations of AsyncRawSink should provide
 * [a mechanism](https://kotlinlang.org/docs/cancellation-and-timeouts.html#making-computation-code-cancellable)
 * to check the cancellation. It's recommended to throw or rethrow [kotlinx.coroutines.CancellationException]
 * once the implementation detects that an operation was canceled.
 *
 * Implementors should abstain from throwing exceptions other than those that are documented for AsyncRawSink methods.
 */
public interface AsyncRawSink {
    /**
     * Removes [byteCount] bytes from [source] and appends them to this sink.
     * Suspends if the data could not be written immediately until the operation completes.
     *
     * @param source the source to read data from.
     * @param byteCount the number of bytes to write.
     *
     * @throws IllegalArgumentException when the [source]'s size is below [byteCount] or [byteCount] is negative.
     * @throws IllegalStateException when the sink is closed.
     * @throws kotlinx.coroutines.CancellationException when the coroutine executing the writing is canceled.
     */
    public suspend fun write(source: Buffer, byteCount: Long)

    /**
     * Pushes all buffered bytes to their final destination,
     * suspends until the operation completes.
     *
     * @throws IllegalStateException when the sink is closed.
     * @throws kotlinx.coroutines.CancellationException when the coroutine executing the flush is canceled.
     */
    public suspend fun flush()

    /**
     * Pushes all buffered bytes to their final destination and releases the resources held by this
     * sink. Suspends until the operation completes.
     * It is an error to write a closed sink.
     * It is safe to close a sink more than once.
     */
    public suspend fun close()

    /**
     * Immediately closes this sink and releases the resources held by this sink.
     * Unlike [close], this method doesn't guarantee graceful termination, and it not attempts
     * to push pending data to destination if there are any.
     * Use this method when some error condition is detected and a sink needs to be closed
     * as soon as possible, without caring about its state integrity.
     *
     * It is an error to write a closed sink.
     * It is safe to close a sink more than once.
     */
    public fun closeAbruptly()
}
