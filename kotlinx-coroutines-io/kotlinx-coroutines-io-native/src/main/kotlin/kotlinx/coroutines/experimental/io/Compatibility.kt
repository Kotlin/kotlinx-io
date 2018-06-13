package kotlinx.coroutines.experimental.io

import kotlin.coroutines.experimental.*

interface CoroutineScope
interface Job

@Deprecated("Not yet supported", level = DeprecationLevel.ERROR)
actual fun reader(coroutineContext: CoroutineContext,
                  channel: ByteChannel,
                  parent: Job?,
                  block: suspend ReaderScope.() -> Unit): ReaderJob {
    TODO()
}

@Deprecated("Not yet supported", level = DeprecationLevel.ERROR)
actual fun reader(coroutineContext: CoroutineContext,
                  autoFlush: Boolean,
                  parent: Job?,
                  block: suspend ReaderScope.() -> Unit): ReaderJob {
    TODO()
}

@Deprecated("Not yet supported", level = DeprecationLevel.ERROR)
actual fun writer(coroutineContext: CoroutineContext,
                  channel: ByteChannel,
                  parent: Job?,
                  block: suspend WriterScope.() -> Unit): WriterJob {
    TODO()
}

@Deprecated("Not yet supported", level = DeprecationLevel.ERROR)
actual fun writer(coroutineContext: CoroutineContext,
                  autoFlush: Boolean,
                  parent: Job?,
                  block: suspend WriterScope.() -> Unit): WriterJob {
    TODO()
}
