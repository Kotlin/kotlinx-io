package kotlinx.io.buffer

public interface BufferAllocator {
    public fun allocate(size: Int): Buffer

    public fun free(instance: Buffer)
}

public expect object PlatformBufferAllocator : BufferAllocator

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