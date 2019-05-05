package kotlinx.io

import kotlinx.io.memory.*

abstract class Input(pageSize: Int = DEFAULT_PAGE_SIZE) : Closeable {
    private val allocator = SingleMemoryAllocator(pageSize)

    // Current memory 
    private var page: Memory = allocator.allocate()

    // Current position in [page]
    private var position: Int = 0

    // Current filled number of bytes in [page]
    private var limit: Int = 0

    // TODO: implement manual list management with Array<Memory> and IntArray
    // assume we normally preview at most 1 page ahead, so may be consider Any? for Memory and Array<Memory> here
    private var previewIndex = Int.MIN_VALUE // positive values mean replay mode, -1 means discard mode, MIN_VALUE means no preview
    private var previewPages: MutableList<Memory>? = null
    private var previewLimits: MutableList<Int>? = null

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
        if (previewIndex != Int.MIN_VALUE)
            return false // do not expand if in history mode TODO: expand if last page

        var currentLimit = limit
        val currentPage = page
        val currentSize = currentPage.size
        if (targetLimit > currentSize)
            return false // we can't expand data into current page

        while (currentLimit < targetLimit) {
            val fetched = fill(currentPage, currentLimit, currentSize - currentLimit)
            logln("PGE: Loaded [$fetched]")
            if (fetched == 0)
                throw Exception("EOF")
            currentLimit += fetched
        }

        limit = currentLimit
        return true
    }

    fun <R> preview(reader: Input.() -> R): R {
        // Remember if we initiated the preview and should also start discard mode

        val initiated = if (previewIndex == Int.MIN_VALUE) {
            // enable retaining of pages
            previewPages = mutableListOf(page)
            previewLimits = mutableListOf(limit)
            previewIndex = 0
            true
        } else
            false

        if (previewIndex == -1) {
            // we were in discard mode, but new preview operation is starting, convert to preview mode
            previewIndex = 0
        }
        
        val markIndex = previewIndex
        val markPosition = position
        logln("PVW: Enter at #$markIndex @$markPosition of $limit")

        val result = reader()

/*
        TODO: optimize single page rewind
        if (previewIndex == markIndex) {
            // we are at the same page, just restore the position
            position = markPosition
            return result
        }
*/

        val historyPages = previewPages!!
        val historyLimits = previewLimits!!

        // restore the whole context
        previewIndex = if (initiated) -1 else markIndex
        page = historyPages[markIndex]
        position = markPosition
        limit = historyLimits[markIndex]
        logln("PVW: Exit at #$markIndex @$markPosition of $limit")
        return result
    }
    
    private fun fetchPage() {
        if (position != limit) {
            // trying to fetch a page when previous page was not exhausted is an internal error
            throw UnsupportedOperationException("Throwing bytes")
        }

        if (previewIndex == Int.MIN_VALUE) {
            // no preview operation, reuse current page for new data
            logln("PVW: None @$limit, filled page")
            return fillPage(page)
        }

        val historyPages = previewPages!!
        val historyLimits = previewLimits!!

        // no preview operation in progress, but we still have pages in history, we will free used pages
        if (previewIndex == -1) {
            // return current page
            allocator.free(page)
            historyLimits.removeAt(0)
            historyPages.removeAt(0)
            
            if (historyPages.isEmpty()) {
                logln("PVW: Finished @$limit, filled page")
                // used all prefetched data, complete preview operation
                previewIndex = Int.MIN_VALUE
                previewPages = null
                previewLimits = null
                
                // allocate and fetch a new page
                return fillPage(allocator.allocate())
            } else {
                val oldLimit = limit
                // get and remove a page from history
                page = historyPages[0]
                limit = historyLimits[0]
                position = 0
                logln("PVW: Finished @$oldLimit, using prefetched page, $position/$limit")
                return
            }
        }

        // let's look at the next historical page
        previewIndex++

        if (previewIndex < historyPages.size) {
            // we have a page already in history, i.e. replaying history inside another preview
            this.position = 0
            this.page = historyPages[previewIndex]
            this.limit = historyLimits[previewIndex]
            logln("PVW: Preview #$previewIndex, using prefetched page, $position/$limit")
            return
        }

        // here we are in a preview operation, but don't have any prefetched pages ready
        // so we need to save current one and fill some more data

        logln("PVW: Preview #$previewIndex, saved page, $position/$limit")
        fillPage(allocator.allocate())
        historyPages.add(page)
        historyLimits.add(limit)
        logln("PVW: Preview #$previewIndex, filled page, $position/$limit")
    }

    private fun fillPage(page: Memory) {
        val fetched = fill(page, 0, page.size)
        logln("PG: Loaded [$fetched]")
        if (fetched == 0)
            throw Exception("EOF")

        limit = fetched
        position = 0
        this.page = page
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

private fun logln(text: String) {

}
