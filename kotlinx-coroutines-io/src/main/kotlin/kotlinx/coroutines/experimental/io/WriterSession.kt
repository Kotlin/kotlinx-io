package kotlinx.coroutines.experimental.io

import kotlinx.io.core.*

interface WriterSession {
    fun request(min: Int): IoBuffer?
    fun written(n: Int)
    fun flush()
}

interface WriterSuspendSession : WriterSession {
    suspend fun tryAwait(n: Int)
}
