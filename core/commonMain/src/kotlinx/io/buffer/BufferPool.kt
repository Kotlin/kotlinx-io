package kotlinx.io.buffer

import kotlinx.io.pool.*


internal class BufferPool(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val source: BufferAllocator = PlatformBufferAllocator
) : DefaultPool<Buffer>(DEFAULT_POOL_CAPACITY) {
    override fun produceInstance(): Buffer {
        return source.allocate(bufferSize)
    }

    companion object {
        internal const val DEFAULT_BUFFER_SIZE: Int = 1024
        internal const val DEFAULT_POOL_CAPACITY: Int = 16
    }
}