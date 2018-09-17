package kotlinx.coroutines.io

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

internal open class ByteChannelCoroutine(
    parentContext: CoroutineContext,
    open val channel: ByteChannel
) : AbstractCoroutine<Unit>(parentContext, active = true) {
    override fun onCancellation(cause: Throwable?) {
        if (!channel.close(cause) && cause != null)
            handleCoroutineException(context, cause)
    }
}
