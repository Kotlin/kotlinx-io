package kotlinx.io.buffer

import java.nio.ByteBuffer
import java.nio.ByteOrder

public actual object PlatformBufferAllocator : BufferAllocator {
    override fun allocate(size: Int): Buffer {
        return Buffer(ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN))
    }

    override fun free(instance: Buffer) {}
}
