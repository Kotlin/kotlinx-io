package kotlinx.coroutines.experimental.io

import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.*

/**
 * A coroutine job that is reading from a byte channel
 */
interface ReaderJob : Job {
    /**
     * A reference to the channel that this coroutine is reading from
     */
    val channel: ByteWriteChannel
}

/**
 * A coroutine job that is writing to a byte channel
 */
interface WriterJob : Job {
    /**
     * A reference to the channel that this coroutine is writing to
     */
    val channel: ByteReadChannel
}

interface ReaderScope : CoroutineScope {
    val channel: ByteReadChannel
}

interface WriterScope : CoroutineScope {
    val channel: ByteWriteChannel
}

expect fun reader(
    coroutineContext: CoroutineContext,
    channel: ByteChannel,
    parent: Job? = null,
    block: suspend ReaderScope.() -> Unit
): ReaderJob

expect fun reader(
    coroutineContext: CoroutineContext,
    autoFlush: Boolean = false, parent: Job? = null,
    block: suspend ReaderScope.() -> Unit
): ReaderJob

expect fun writer(
    coroutineContext: CoroutineContext,
    channel: ByteChannel, parent: Job? = null,
    block: suspend WriterScope.() -> Unit
): WriterJob

expect fun writer(
    coroutineContext: CoroutineContext,
    autoFlush: Boolean = false, parent: Job? = null,
    block: suspend WriterScope.() -> Unit
): WriterJob
