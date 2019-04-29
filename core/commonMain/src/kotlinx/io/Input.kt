package kotlinx.io

import kotlinx.io.memory.*

abstract class Input : Closeable {
    // Current memory 
    private var memory: Memory = allocatePage()

    // Current position in [memory]
    private var position: Int = 0

    // Current filled number of bytes in [memory]
    private var limit: Int = 0

    // List of unreleased memory instances when preview operation is in progress
    private var memoryHistory: List<Memory>? = null


    fun readLong(): Long = readPrimitive(longPrimitiveSize, { memory, offset -> memory.loadLongAt(offset) }, { it })
    fun readULong(): ULong = readPrimitive(longPrimitiveSize, { memory, offset -> memory.loadLongAt(offset).toULong() }, { it.toULong() })
    fun readInt(): Int = readPrimitive(intPrimitiveSize, { memory, offset -> memory.loadIntAt(offset) }, { it.toInt() })
    fun readUInt(): UInt = readPrimitive(intPrimitiveSize, { memory, offset -> memory.loadIntAt(offset).toUInt() }, { it.toUInt() })
    fun readShort(): Short = readPrimitive(shortPrimitiveSize, { memory, offset -> memory.loadShortAt(offset) }, { it.toShort() })
    fun readUShort(): UShort = readPrimitive(shortPrimitiveSize, { memory, offset -> memory.loadShortAt(offset).toUShort() }, { it.toUShort() })

    private inline fun <T> readPrimitive(
        primitiveSize: Int,
        readDirect: (memory: Memory, offset: Int) -> T,
        fromLong: (Long) -> T
    ): T {
        if (position == limit)
            fetch()

        val targetLimit = position + primitiveSize
        if (memory.size >= targetLimit) {
            // we don't have data, but have enough space in memory to get it
            while (limit < targetLimit)
                fetch()
        }

        val offset = position
        if (limit >= offset + primitiveSize) {
            position = offset + primitiveSize
            return readDirect(memory, offset)
        }

        // Nope, doesn't fit in memory, read byte by byte
        var long = 0L
        readBytes(primitiveSize) { long = (long shl 8) or it.toLong() }
        return fromLong(long)
    }
    
    private inline fun readBytes(length: Int, consumer: (byte: Byte) -> Unit) {
        var remaining = length
        while (remaining > 0) {
            if (position == limit)
                fetch()

            consumer(memory[position++])
            remaining--
        }
    }

    private fun fetch() {
        val current = memory
        val currentSize = current.size
        if (position < currentSize) {
            // we have some space in current memory, use it
            val currentLimit = limit
            val fetched = fill(current, currentLimit, currentSize - currentLimit)
            if (fetched == 0) {
                throw Exception("EOF")
            }
            limit = currentLimit + fetched
        } else {
            // TODO: reuse memory, if no preview operation is in progress
            releasePage(current)
            val memory = allocatePage()
            val fetched = fill(memory, 0, memory.size)
            if (fetched == 0) {
                throw Exception("EOF")
            }
            this.limit = fetched
            this.position = 0
            this.memory = memory
        }
    }

    protected abstract fun allocatePage(): Memory
    protected abstract fun releasePage(memory: Memory)

    /**
     * Discard at most [n] bytes
     */
    fun discard(n: Long): Long {
        TODO()
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

    companion object {
        const val longPrimitiveSize = 8
        const val intPrimitiveSize = 4
        const val shortPrimitiveSize = 2
    }
}
