package kotlinx.io.buffer

import kotlinx.io.pool.*
import kotlin.native.concurrent.*

internal class UnmanagedBufferPool(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) : DefaultPool<Buffer>(DEFAULT_POOL_CAPACITY) {
    private val allocator = UnmanagedBufferAllocator

    override fun produceInstance(): Buffer = allocator.allocate(bufferSize)

    @ThreadLocal
    companion object {
        @ThreadLocal
        val Instance = UnmanagedBufferPool()
    }
}
