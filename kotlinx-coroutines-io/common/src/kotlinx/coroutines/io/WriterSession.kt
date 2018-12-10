package kotlinx.coroutines.io

import kotlinx.io.core.*

@ExperimentalIoApi
interface WriterSession {
    fun request(min: Int): IoBuffer?
    fun written(n: Int)
    fun flush()
}

@ExperimentalIoApi
interface WriterSuspendSession : WriterSession {
    suspend fun tryAwait(n: Int)
}
