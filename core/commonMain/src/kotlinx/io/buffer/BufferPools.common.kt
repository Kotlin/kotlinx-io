package kotlinx.io.buffer

import kotlin.native.concurrent.ThreadLocal
import kotlinx.io.pool.DefaultPool

internal const val DEFAULT_BUFFER_SIZE: Int = 1024
internal const val DEFAULT_POOL_CAPACITY: Int = 16

internal class DefaultBufferPool(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val source: BufferAllocator = PlatformBufferAllocator
) : DefaultPool<Buffer>(DEFAULT_POOL_CAPACITY) {

    override fun produceInstance(): Buffer {
        return source.allocate(bufferSize)
    }

    override fun validateInstance(instance: Buffer) {
        require(instance.size == bufferSize) { "Invalid buffer size, expected $bufferSize, got ${instance.size}" }
    }

    override fun disposeInstance(instance: Buffer) {
        source.free(instance)
    }
}

@ThreadLocal
internal val defaultBufferPool: DefaultBufferPool = DefaultBufferPool()