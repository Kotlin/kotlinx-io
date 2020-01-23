package kotlinx.io.buffer

interface BufferAllocator {
    fun allocate(size: Int): Buffer

    fun free(instance: Buffer)
}

expect object PlatformBufferAllocator : BufferAllocator

/**
 * Allocates the buffer of the given [size], executes the given [block] function on this buffer
 * and then frees it correctly whether an exception is thrown or not.
 */
public inline fun <T> BufferAllocator.borrow(size: Int, block: (buffer: Buffer) -> T): T {
    val buffer = allocate(size)
    try {
        return block(buffer)
    } finally {
        free(buffer)
    }
}