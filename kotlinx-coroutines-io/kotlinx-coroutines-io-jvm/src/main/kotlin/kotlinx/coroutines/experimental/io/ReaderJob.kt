package kotlinx.coroutines.experimental.io

import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.*

actual fun reader(coroutineContext: CoroutineContext,
           channel: ByteChannel,
           parent: Job?,
           block: suspend ReaderScope.() -> Unit): ReaderJob {
    val newContext = newCoroutineContext(coroutineContext, parent)
    val coroutine = ReaderCoroutine(newContext, channel)
    coroutine.start(CoroutineStart.DEFAULT, coroutine, block)
    return coroutine
}

actual fun reader(coroutineContext: CoroutineContext,
           autoFlush: Boolean,
           parent: Job?,
           block: suspend ReaderScope.() -> Unit): ReaderJob {
    val channel = ByteChannel(autoFlush) as ByteBufferChannel
    val job = reader(coroutineContext, channel, parent, block)
    channel.attachJob(job)
    return job
}

private class ReaderCoroutine(context: CoroutineContext, channel: ByteChannel)
    : ByteChannelCoroutine(context, channel), ReaderJob, ReaderScope
