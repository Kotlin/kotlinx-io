package kotlinx.io.buffer

interface BufferAllocator {
    fun allocate(size: Int): Buffer

    fun free(instance: Buffer)
}

expect object PlatformBufferAllocator : BufferAllocator
