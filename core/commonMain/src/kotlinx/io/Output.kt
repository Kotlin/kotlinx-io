package kotlinx.io

import kotlinx.io.memory.*

abstract class Output(pageSize: Int = DEFAULT_PAGE_SIZE) : Closeable {
    private val allocator = SingleMemoryAllocator(pageSize)

    // Current memory 
    private var page: Memory = allocator.allocate()

    // Current position in [page]
    private var position: Int = 0

    // Current number of bytes in [page] that were already flushed 
    private var flushed: Int = 0

    protected abstract fun flush(source: Memory, length: Int): Int

    fun writeLong(value: Long) {
        writePrimitive(8, { page, offset -> page.storeLongAt(offset, value) }, { value })
    }

    private inline fun writePrimitive(
        primitiveSize: Int,
        writeDirect: (page: Memory, offset: Int) -> Unit,
        longValue: () -> Long
    ) {
        val offset = position
        val size = page.size
        val targetLimit = offset + primitiveSize
        if (size >= targetLimit ) {
            position = targetLimit
            return writeDirect(page, offset)
        }

        if (offset == size) {
            // current page exhausted, we cannot expand data in this page, 
            // and we also don't have bytes left to be read
            // so we should fetch new page of data and may be read entire primitive
            flushPage()
            // we know we are at zero position here
            if (size >= primitiveSize) {
                position = primitiveSize
                return writeDirect(page, 0)
            }
        }

        // Nope, doesn't fit in a page, read byte by byte
        writeBytes(primitiveSize, longValue())
    }
    
    private fun writeBytes(length: Int, value: Long) {
        var remainingValue = value
        var remaining = length
        var size = page.size
        while (remaining > 0) {
            if (position == size) {
                flushPage()
                size = page.size
            }

            page.storeAt(position++, remainingValue.toByte())
            remainingValue = remainingValue shr 8
            remaining--
        }

    }

    private fun flushPage() {
        flush(page, position)
        page = allocator.allocate()
        position = 0
        flushed = 0
    }

    fun flush() {
        flushPage()
    }
}