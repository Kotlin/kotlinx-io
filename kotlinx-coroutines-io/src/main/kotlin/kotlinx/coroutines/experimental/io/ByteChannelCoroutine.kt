package kotlinx.coroutines.io

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

@UseExperimental(InternalCoroutinesApi::class)
internal open class ByteChannelCoroutine(
    parentContext: CoroutineContext,
    open val channel: ByteChannel
) : AbstractCoroutine<Unit>(parentContext, active = true) {
    // TODO see ProducerCoroutine or ActorCoroutine, this one is prone to exception loss
    override fun onCancellation(cause: Throwable?) {
        if (!channel.close(cause) && cause != null)
            handleCoroutineException(context, cause)
    }
}
