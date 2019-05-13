package kotlinx.io.buffer

interface BufferAllocator {
    fun allocate(size: Int): Buffer

    fun free(instance: Buffer)
}

expect object PlatformBufferAllocator : BufferAllocator

internal const val DEFAULT_BUFFER_SIZE: Int = 1024

internal class SingleBufferAllocator(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val source: BufferAllocator = PlatformBufferAllocator
) {
    private var free: Buffer? = null

    fun allocate(): Buffer {
        val buffer = free ?: source.allocate(bufferSize)
        free = null
        return buffer
    }

    fun free(instance: Buffer) {
        free = instance
    }
}