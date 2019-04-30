package kotlinx.io.internal

import kotlinx.io.memory.*

internal interface MemoryAllocator {
    fun allocate(size: Int): Memory

    fun free(instance: Memory)
}

internal expect object PlatformMemoryAllocator : MemoryAllocator

internal const val DEFAULT_BUFFER_SIZE: Int = 1024

internal class PoolingMemoryAllocator(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val source: MemoryAllocator = PlatformMemoryAllocator
)  {

    private val freeList = mutableListOf<Memory>()

    fun allocate(): Memory {
        if (freeList.isEmpty())
            return source.allocate(bufferSize)

        return freeList.removeAt(freeList.lastIndex)
    }

    fun free(instance: Memory) {
        freeList.add(instance)
    }

}