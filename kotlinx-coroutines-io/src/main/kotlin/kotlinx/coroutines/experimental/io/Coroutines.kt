package kotlinx.coroutines.experimental.io

import kotlinx.coroutines.*
import kotlin.coroutines.*

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

fun reader(
    coroutineContext: CoroutineContext,
    channel: ByteChannel,
    parent: Job? = null,
    block: suspend ReaderScope.() -> Unit
): ReaderJob {
    val newContext = newCoroutineContext(coroutineContext, parent)
    return ReaderCoroutine(newContext, channel).apply {
        start(CoroutineStart.DEFAULT, this, block)
    }
}

fun reader(
    coroutineContext: CoroutineContext,
    autoFlush: Boolean = false, parent: Job? = null,
    block: suspend ReaderScope.() -> Unit
): ReaderJob {
    val channel = ByteChannel(autoFlush)
    return reader(coroutineContext, channel, parent, block).also {
        channel.attachJob(it)
    }
}

fun writer(
    coroutineContext: CoroutineContext,
    channel: ByteChannel, parent: Job? = null,
    block: suspend WriterScope.() -> Unit
): WriterJob {
    val newContext = newCoroutineContext(coroutineContext, parent)
    return WriterCoroutine(newContext, channel).apply {
        start(CoroutineStart.DEFAULT, this, block)
    }
}

fun writer(
    coroutineContext: CoroutineContext,
    autoFlush: Boolean = false, parent: Job? = null,
    block: suspend WriterScope.() -> Unit
): WriterJob {
    val channel = ByteChannel(autoFlush)
    return writer(coroutineContext, channel, parent, block).also {
        channel.attachJob(it)
    }
}

internal class ReaderCoroutine(context: CoroutineContext, channel: ByteChannel) :
    ByteChannelCoroutine(context, channel), ReaderJob, ReaderScope

internal class WriterCoroutine(ctx: CoroutineContext, channel: ByteChannel) : ByteChannelCoroutine(ctx, channel),
    WriterScope, WriterJob

