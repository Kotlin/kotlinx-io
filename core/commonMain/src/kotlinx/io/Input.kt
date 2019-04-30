package kotlinx.io

import kotlinx.io.memory.*

abstract class Input : Closeable {
    private val allocator = SingleMemoryAllocator()

    // Current memory 
    private var page: Memory = allocator.allocate()

    // Current position in [page]
    private var position: Int = 0

    // Current filled number of bytes in [page]
    private var limit: Int = 0
    
    fun readLong(): Long =
        readPrimitive(8, { page, offset -> page.loadLongAt(offset) }, { it })

    fun readULong(): ULong =
        readPrimitive(8, { page, offset -> page.loadLongAt(offset).toULong() }, { it.toULong() })

    fun readInt(): Int =
        readPrimitive(4, { page, offset -> page.loadIntAt(offset) }, { it.toInt() })

    fun readUInt(): UInt =
        readPrimitive(4, { page, offset -> page.loadIntAt(offset).toUInt() }, { it.toUInt() })

    fun readShort(): Short =
        readPrimitive(2, { page, offset -> page.loadShortAt(offset) }, { it.toShort() })

    fun readUShort(): UShort =
        readPrimitive(2, { page, offset -> page.loadShortAt(offset).toUShort() }, { it.toUShort() })

    fun readByte(): Byte =
        readPrimitive(1, { page, offset -> page.loadAt(offset) }, { it.toByte() })

    fun readUByte(): UByte =
        readPrimitive(1, { page, offset -> page.loadAt(offset).toUByte() }, { it.toUByte() })

    fun readDouble(): Double =
        readPrimitive(8, { page, offset -> page.loadDoubleAt(offset) }, { Double.fromBits(it) })

    fun readFloat(): Float =
        readPrimitive(4, { page, offset -> page.loadFloatAt(offset) }, { Float.fromBits(it.toInt()) })

    private inline fun <T> readPrimitive(
        primitiveSize: Int,
        readDirect: (page: Memory, offset: Int) -> T,
        fromLong: (Long) -> T
    ): T {
        val offset = position
        val targetLimit = offset + primitiveSize
        if (limit >= targetLimit || fetchExpand(targetLimit)) {
            position = targetLimit
            return readDirect(page, offset)
        }
        
        if (offset == limit) {
            // current page exhausted, we cannot expand data in this page, 
            // and we also don't have bytes left to be read
            // so we should fetch new page of data and may be read entire primitive
            fetchPage()
            // we know we are at zero position here
            if (limit >= primitiveSize) {
                position = primitiveSize
                return readDirect(page, 0)
            }
        }
        
        // Nope, doesn't fit in a page, read byte by byte
        var long = 0L
        readBytes(primitiveSize) { long = (long shl 8) or it.toLong() }
        return fromLong(long)
    }

    private inline fun readBytes(length: Int, consumer: (byte: Byte) -> Unit) {
        var remaining = length
        while (remaining > 0) {
            if (position == limit)
                fetchPage()

            consumer(page[position++])
            remaining--
        }
    }

    private fun fetchExpand(targetLimit: Int): Boolean {
        var currentLimit = limit
        val currentPage = page
        val currentSize = currentPage.size
        if (targetLimit > currentSize)
            return false // we can't expand data into current page

        while (currentLimit < targetLimit) {
            val fetched = fill(currentPage, currentLimit, currentSize - currentLimit)
            if (fetched == 0)
                throw Exception("EOF")
            currentLimit += fetched
        }

        limit = currentLimit
        return true
    }

    private fun fetchPage() {
        if (position != limit) {
            // trying to fetch a page when previous page was not exhausted is an internal error
            throw UnsupportedOperationException("Throwing bytes")
        }
        
        // TODO: reuse page, if no preview operation is in progress
        allocator.free(page)
        val newPage = allocator.allocate()
        val fetched = fill(newPage, 0, newPage.size)
        if (fetched == 0) 
            throw Exception("EOF")
        
        this.limit = fetched
        this.position = 0
        this.page = newPage
    }

    /**
     * Close input including the underlying source. All pending bytes will be discarded.
     * TODO: what does it mean "not recommended"?
     * It is not recommended to invoke it with read operations in-progress concurrently.
     */
    abstract override fun close()

    /**
     * Reads the next bytes into the [destination] starting at [offset] at most [length] bytes.
     * May block until at least one byte is available.
     * TODO: ?? Usually bypass all exceptions from the underlying source.
     *
     * @param offset in bytes where result should be written
     * @param length should be at least one byte
     *
     * @return number of bytes were copied or `0` if no more input is available
     */
    protected abstract fun fill(destination: Memory, offset: Int, length: Int): Int
}
