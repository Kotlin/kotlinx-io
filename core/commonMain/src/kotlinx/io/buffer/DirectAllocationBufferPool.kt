package kotlinx.io.buffer

import kotlinx.io.pool.*

internal class DirectAllocationBufferPool(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val source: BufferAllocator = PlatformBufferAllocator
) : DirectAllocationPool<Buffer>() {

    override fun borrow(): Buffer {
        return source.allocate(bufferSize)
    }
}
