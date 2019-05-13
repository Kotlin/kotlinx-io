package kotlinx.io

import kotlinx.io.buffer.*

abstract class Output(bufferSize: Int = DEFAULT_BUFFER_SIZE) : Closeable {
    private val allocator = SingleBufferAllocator(bufferSize)

    // Current buffer 
    private var buffer: Buffer = allocator.allocate()

    // Current position in [buffer]
    private var position: Int = 0

    // Current number of bytes in [buffer] that were already flushed 
    private var flushed: Int = 0

    protected abstract fun flush(source: Buffer, length: Int): Int

    fun writeLong(value: Long) {
        writePrimitive(8, { buffer, offset -> buffer.storeLongAt(offset, value) }, { value })
    }

    private inline fun writePrimitive(
        primitiveSize: Int,
        writeDirect: (buffer: Buffer, offset: Int) -> Unit,
        longValue: () -> Long
    ) {
        val offset = position
        val size = buffer.size
        val targetLimit = offset + primitiveSize
        if (size >= targetLimit ) {
            position = targetLimit
            return writeDirect(buffer, offset)
        }

        if (offset == size) {
            // current buffer exhausted, we cannot expand data in this buffer, 
            // and we also don't have bytes left to be read
            // so we should fetch new buffer of data and may be read entire primitive
            flushBuffer()
            // we know we are at zero position here
            if (size >= primitiveSize) {
                position = primitiveSize
                return writeDirect(buffer, 0)
            }
        }

        // Nope, doesn't fit in a buffer, read byte by byte
        writeBytes(primitiveSize, longValue())
    }
    
    private fun writeBytes(length: Int, value: Long) {
        var remainingValue = value
        var remaining = length
        var size = buffer.size
        while (remaining > 0) {
            if (position == size) {
                flushBuffer()
                size = buffer.size
            }

            buffer.storeByteAt(position++, remainingValue.toByte())
            remainingValue = remainingValue shr 8
            remaining--
        }

    }

    private fun flushBuffer() {
        flush(buffer, position)
        buffer = allocator.allocate()
        position = 0
        flushed = 0
    }

    fun flush() {
        flushBuffer()
    }
}