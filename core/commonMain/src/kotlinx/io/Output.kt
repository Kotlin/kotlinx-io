package kotlinx.io

import kotlinx.io.buffer.*

abstract class Output(bufferSize: Int = DEFAULT_BUFFER_SIZE) : Closeable {
    private val bufferPool = DefaultBufferPool(bufferSize)

    // Current buffer 
    private var buffer: Buffer = bufferPool.borrow()

    // Current position in [buffer]
    private var position: Int = 0

    // Current number of bytes in [buffer] that were already flushed 
    private var flushed: Int = 0

    protected abstract fun flush(source: Buffer, length: Int): Int

    fun writeByte(value: Byte) {
        writePrimitive(1, { buffer, offset -> buffer.storeByteAt(offset, value) }, { value.toLong() })
    }

    fun writeUByte(value: UByte) {
        writePrimitive(1, { buffer, offset -> buffer.storeUByteAt(offset, value) }, { value.toLong() })
    }

    fun writeShort(value: Short) {
        writePrimitive(2, { buffer, offset -> buffer.storeShortAt(offset, value) }, { value.toLong() })
    }

    fun writeUShort(value: UShort) {
        writePrimitive(2, { buffer, offset -> buffer.storeUShortAt(offset, value) }, { value.toLong() })
    }

    fun writeInt(value: Int) {
        writePrimitive(4, { buffer, offset -> buffer.storeIntAt(offset, value) }, { value.toLong() })
    }

    fun writeUInt(value: UInt) {
        writePrimitive(4, { buffer, offset -> buffer.storeUIntAt(offset, value) }, { value.toLong() })
    }

    fun writeLong(value: Long) {
        writePrimitive(8, { buffer, offset -> buffer.storeLongAt(offset, value) }, { value.toLong() })
    }

    fun writeULong(value: ULong) {
        writePrimitive(8, { buffer, offset -> buffer.storeULongAt(offset, value) }, { value.toLong() })
    }

    fun writeFloat(value: Float) {
        writePrimitive(4, { buffer, offset -> buffer.storeFloatAt(offset, value) }, { value.toBits().toLong() })
    }

    fun writeDouble(value: Double) {
        writePrimitive(8, { buffer, offset -> buffer.storeDoubleAt(offset, value) }, { value.toBits() })
    }

    fun writeArray(array: UByteArray) {
        for (byte in array)
            writeUByte(byte)
    }

    fun writeArray(array: ByteArray) {
        for (byte in array)
            writeByte(byte)
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
        buffer = bufferPool.borrow()
        position = 0
        flushed = 0
    }

    fun flush() {
        flushBuffer()
    }
}