package kotlinx.io.memory

interface MemoryAllocator {
    fun allocate(size: Int): Memory

    fun free(instance: Memory)
}

expect object PlatformMemoryAllocator : MemoryAllocator

internal const val DEFAULT_PAGE_SIZE: Int = 1024

internal class SingleMemoryAllocator(
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
    private val source: MemoryAllocator = PlatformMemoryAllocator
) {
    private var free: Memory? = null

    fun allocate(): Memory {
        val page = free ?: source.allocate(pageSize)
        free = null
        return page
    }

    fun free(instance: Memory) {
        free = instance
    }
}