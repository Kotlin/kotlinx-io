package kotlinx.coroutines.io

import kotlinx.io.core.*

@Deprecated("Use writeMemory instead.")
interface WriterSession {
    fun request(min: Int): IoBuffer?
    fun written(n: Int)
    fun flush()
}

@Deprecated("Use writeMemory instead.")
interface WriterSuspendSession : WriterSession {
    suspend fun tryAwait(n: Int)
}
