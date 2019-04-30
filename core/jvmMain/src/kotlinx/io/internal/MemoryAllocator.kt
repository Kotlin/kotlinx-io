package kotlinx.io.internal

import kotlinx.io.memory.*
import java.nio.*

internal actual object PlatformMemoryAllocator : MemoryAllocator {
    override fun allocate(size: Int): Memory {
        return Memory(ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN))
    }

    override fun free(instance: Memory) {}
}