package kotlinx.io.buffer

actual object PlatformBufferAllocator : BufferAllocator by NativeBufferAllocator()

internal class NativeBufferAllocator : BufferAllocator {
    override fun allocate(size: Int): Buffer = Buffer(ByteArray(size))

    override fun free(instance: Buffer) {}
}
