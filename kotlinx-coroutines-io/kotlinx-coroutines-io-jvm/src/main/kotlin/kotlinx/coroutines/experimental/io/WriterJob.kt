package kotlinx.coroutines.experimental.io

import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.*

actual fun writer(coroutineContext: CoroutineContext,
           channel: ByteChannel,
           parent: Job?,
           block: suspend WriterScope.() -> Unit): WriterJob {
    val newContext = newCoroutineContext(coroutineContext, parent)
    val coroutine = WriterCoroutine(newContext, channel)
    coroutine.start(CoroutineStart.DEFAULT, coroutine, block)
    return coroutine
}

actual fun writer(coroutineContext: CoroutineContext,
           autoFlush: Boolean,
           parent: Job?,
           block: suspend WriterScope.() -> Unit): WriterJob {
    val channel = ByteChannel(autoFlush) as ByteBufferChannel
    val job = writer(coroutineContext, channel, parent, block)
    channel.attachJob(job)
    return job
}

private class WriterCoroutine(ctx: CoroutineContext, channel: ByteChannel)
    : ByteChannelCoroutine(ctx, channel), WriterScope, WriterJob

