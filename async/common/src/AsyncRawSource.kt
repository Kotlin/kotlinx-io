/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async

import kotlinx.io.Buffer

/**
 * Supplies a stream of bytes. AsyncRawSource is a base interface for asynchronous `kotlinx-io` data suppliers.
 *
 * The interface should be implemented to read data from wherever it's located, almost like [kotlinx.io.RawSource].
 * The main distinction between these interfaces is that AsyncRawSource is primarily suited for asynchronous data
 * receivers, like non-blocking sockets. If the data could not be read, AsyncRawSource implementations should
 * suspend until the data will be available or the source will become exhausted or closed.
 *
 * While users may interact with AsyncRawSource instances directly, it's recommended to use buffered [AsyncSource]
 * encapsulating both a buffer and the underlying source, and provides methods for suspending until needed data
 * fetched into the buffer. Use [buffered] to wrap a source into [AsyncSource].
 *
 * Kotlin's coroutines support [cancellation](https://kotlinlang.org/docs/cancellation-and-timeouts.html#cancellation-is-cooperative)
 * which is cooperative. Implementations of AsyncRawSource should provide
 * [a mechanism](https://kotlinlang.org/docs/cancellation-and-timeouts.html#making-computation-code-cancellable)
 * to check the cancellation. It's recommended to throw or rethrow [kotlinx.coroutines.CancellationException]
 * once the implementation detects that an operation was canceled.
 *
 * Implementors should abstain from throwing exceptions other than those that are documented for AsyncRawSource methods.
 */
@OptIn(ExperimentalStdlibApi::class)
public interface AsyncRawSource : AutoCloseable {
    /**
     * Removes at least 1, and up to [byteCount] bytes from this source and appends them to [sink].
     * Returns the number of bytes read, or -1 if this source is exhausted.
     * Suspends if the data could not be read until it'll be available, the source will be closed, or
     * it'll be exhausted.
     *
     * @param sink the destination to write the data from this source.
     * @param byteCount the number of bytes to read.
     *
     * @throws IllegalArgumentException when [byteCount] is negative.
     * @throws IllegalStateException when the source is closed.
     * @throws kotlinx.coroutines.CancellationException when the coroutine executing the reading is canceled.
     **/
    public suspend fun readAtMostTo(sink: Buffer, byteCount: Long): Long

    /**
     * Closes this source and releases the resources held by this source. It is an error to read a
     * closed source. It is safe to close a source more than once.
     */
    override fun close()
}
