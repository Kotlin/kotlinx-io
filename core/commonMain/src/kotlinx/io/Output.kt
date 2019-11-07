package kotlinx.io

import kotlinx.io.buffer.*
import kotlinx.io.pool.*

/**
 * [Output] is an abstract base for writable streams of bytes over some sink.
 *
 * [Output] is buffered. Buffer size depends on [Buffer.size] in the [bufferPool] buffer.
 * Buffer size is [DEFAULT_BUFFER_SIZE] by default. Buffer can be flushed using [flush] method.
 *
 * To implement [Output] over a custom sink you should override only [fill] method.
 */
abstract class Output(bufferSize: Int = DEFAULT_BUFFER_SIZE) : Closeable {
    /**
     * Pool for obtaining buffers for operations.
     */
    protected val bufferPool: ObjectPool<Buffer> = if (bufferSize == DEFAULT_BUFFER_SIZE) {
        DefaultBufferPool.Instance
    } else {
        DefaultBufferPool(bufferSize)
    }

    /**
     * Current buffer.
     */
    private var buffer: Buffer = bufferPool.borrow()

    /**
     * Write position in [buffer].
     */
    private var position: Int = 0

    /**
     * Write a [value] to this [Input].
     */
    fun writeByte(value: Byte) {
        val offset = position
        val size = buffer.size
        val targetLimit = offset + 1
        if (size >= targetLimit) {
            position = targetLimit
            return buffer.storeByteAt(offset, value)
        }

        flushBuffer()
        position = 1
        return buffer.storeByteAt(0, value)
    }

    /**
     * Write a [value] to this [Input].
     */
    fun writeShort(value: Short) {
        writePrimitive(2, { buffer, offset -> buffer.storeShortAt(offset, value) }, { value.toLong() })
    }

    /**
     * Write a [value] to this [Input].
     */
    fun writeInt(value: Int) {
        writePrimitive(4, { buffer, offset -> buffer.storeIntAt(offset, value) }, { value.toLong() })
    }

    /**
     * Write a [value] to this [Input].
     */
    fun writeLong(value: Long) {
        writePrimitive(8, { buffer, offset -> buffer.storeLongAt(offset, value) }) { value }
    }

    /**
     * Write an [array] to this [Input].
     *
     * TODO: measure
     */
    fun writeArray(array: ByteArray) {
        for (byte in array) {
            writeByte(byte)
        }
    }

    /**
     * Write all buffered bytes to underlying sink.
     */
    fun flush() {
        flushBuffer()
    }

    /**
     * Write [source] buffer to destination.
     *
     * May block until destination has no available space.
     */
    protected abstract fun flush(source: Buffer, length: Int)

    private fun flushBuffer() {
        flush(buffer, position)
        buffer = bufferPool.borrow()
        position = 0
    }

    internal inline fun writeBufferRange(writer: (buffer: Buffer, startOffset: Int, endOffset: Int) -> Int) {
        var startOffset = position
        var endOffset = buffer.size - 1
        if (startOffset > endOffset) {
            flushBuffer()
            startOffset = position
            endOffset = buffer.size - 1
        }

        val newPosition = writer(buffer, startOffset, endOffset)
        position = newPosition
    }

    private inline fun writePrimitive(
        primitiveSize: Int,
        writeDirect: (buffer: Buffer, offset: Int) -> Unit,
        longValue: () -> Long
    ) {
        val offset = position
        val size = buffer.size
        val targetLimit = offset + primitiveSize

        if (size >= targetLimit) {
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
        var remainingValue = value.reverseByteOrder() shr ((8 - length) * 8)
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
}