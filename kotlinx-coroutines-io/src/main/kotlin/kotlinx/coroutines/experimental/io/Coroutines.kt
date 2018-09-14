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

fun CoroutineScope.reader(
    coroutineContext: CoroutineContext,
    channel: ByteChannel,
    block: suspend ReaderScope.() -> Unit
): ReaderJob {
    val newContext = newCoroutineContext(coroutineContext)
    return ReaderCoroutine(newContext, channel).apply {
        start(CoroutineStart.DEFAULT, this, block)
    }
}

fun CoroutineScope.reader(
    coroutineContext: CoroutineContext,
    autoFlush: Boolean,
    block: suspend ReaderScope.() -> Unit
): ReaderJob {
    val channel = ByteChannel(autoFlush)
    return reader(coroutineContext, channel, block)
}

@Deprecated("Use scope.reader instead")
fun reader(
    coroutineContext: CoroutineContext,
    channel: ByteChannel,
    parent: Job? = null,
    block: suspend ReaderScope.() -> Unit
): ReaderJob {
    val newContext = if (parent != null) GlobalScope.newCoroutineContext(coroutineContext + parent)
    else GlobalScope.newCoroutineContext(coroutineContext)

    return ReaderCoroutine(newContext, channel).apply {
        start(CoroutineStart.DEFAULT, this, block)
    }
}

@Suppress("DEPRECATION")
@Deprecated("Use scope.reader instead")
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

fun CoroutineScope.writer(
    coroutineContext: CoroutineContext,
    channel: ByteChannel,
    block: suspend WriterScope.() -> Unit
): WriterJob {
    val newContext = newCoroutineContext(coroutineContext)
    return WriterCoroutine(newContext, channel).apply {
        start(CoroutineStart.DEFAULT, this, block)
    }
}

fun CoroutineScope.writer(
    coroutineContext: CoroutineContext,
    autoFlush: Boolean = false,
    block: suspend WriterScope.() -> Unit
): WriterJob {
    val channel = ByteChannel(autoFlush)
    return writer(coroutineContext, channel, block).also {
        channel.attachJob(it)
    }
}

@Deprecated("Use scope.writer instead")
fun writer(
    coroutineContext: CoroutineContext,
    channel: ByteChannel, parent: Job? = null,
    block: suspend WriterScope.() -> Unit
): WriterJob {
    val newContext = if (parent != null) GlobalScope.newCoroutineContext(coroutineContext + parent)
    else GlobalScope.newCoroutineContext(coroutineContext)

    return WriterCoroutine(newContext, channel).apply {
        start(CoroutineStart.DEFAULT, this, block)
    }
}

@Suppress("DEPRECATION")
@Deprecated("Use scope.writer instead")
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

