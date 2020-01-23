package kotlinx.io.buffer

import java.nio.*
import java.nio.ByteOrder

actual object PlatformBufferAllocator : BufferAllocator {
    override fun allocate(size: Int): Buffer {
        return Buffer(ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN))
    }

    override fun free(instance: Buffer) {}
}
