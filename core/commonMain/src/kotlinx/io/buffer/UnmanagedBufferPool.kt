package kotlinx.io.buffer

import kotlinx.io.pool.*
import kotlin.native.concurrent.*

internal class UnmanagedBufferPool(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) : DefaultPool<Buffer>(DEFAULT_POOL_CAPACITY) {

    override fun produceInstance(): Buffer = bufferOf(ByteArray(bufferSize))

    @ThreadLocal
    companion object {
        @ThreadLocal
        val Instance = UnmanagedBufferPool()
    }
}
