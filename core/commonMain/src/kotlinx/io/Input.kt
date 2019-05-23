package kotlinx.io

import kotlinx.io.buffer.*

private const val stateSourceEOFMask = 0x1
private const val stateSourceFailureMask = 0x2

abstract class Input(bufferSize: Int = DEFAULT_BUFFER_SIZE) : Closeable {

    internal constructor(bytes: Bytes) : this() {
        previewBytes = bytes
        previewIndex = Bytes.StartPointer // replay, not consume
        bytes.pointed(Bytes.StartPointer) { buffer, limit ->
            this.buffer = buffer
            this.limit = limit
        }
    }

    private val bufferPool = BufferPool(bufferSize)

    // Current state of the input
    private var state: Int = 0

    // Current buffer 
    private var buffer: Buffer = bufferPool.borrow()

    // Current position in [buffer]
    private var position: Int = 0

    // Current filled number of bytes in [buffer]
    private var limit: Int = 0

    private var previewIndex: BytesPointer = Bytes.InvalidPointer
    private var previewBytes: Bytes? = null

    fun readLong(): Long =
        readPrimitive(8, { buffer, offset -> buffer.loadLongAt(offset) }, { it })

    fun readULong(): ULong =
        readPrimitive(8, { buffer, offset -> buffer.loadULongAt(offset) }, { it.toULong() })

    fun readInt(): Int =
        readPrimitive(4, { buffer, offset -> buffer.loadIntAt(offset) }, { it.toInt() })

    fun readUInt(): UInt =
        readPrimitive(4, { buffer, offset -> buffer.loadUIntAt(offset) }, { it.toUInt() })

    fun readShort(): Short =
        readPrimitive(2, { buffer, offset -> buffer.loadShortAt(offset) }, { it.toShort() })

    fun readUShort(): UShort =
        readPrimitive(2, { buffer, offset -> buffer.loadUShortAt(offset) }, { it.toUShort() })

    fun readByte(): Byte =
        readPrimitive(1, { buffer, offset -> buffer.loadByteAt(offset) }, { it.toByte() })

    fun readUByte(): UByte =
        readPrimitive(1, { buffer, offset -> buffer.loadByteAt(offset).toUByte() }, { it.toUByte() })

    fun readDouble(): Double =
        readPrimitive(8, { buffer, offset -> buffer.loadDoubleAt(offset) }, { Double.fromBits(it) })

    fun readFloat(): Float =
        readPrimitive(4, { buffer, offset -> buffer.loadFloatAt(offset) }, { Float.fromBits(it.toInt()) })

    internal inline fun readBuffer(reader: (Buffer, offset: Int, size: Int) -> Int): Int {
        if (position == limit) {
            if (fetchBuffer() == 0)
                throw EOFException("End of file while reading buffer")
        }
        val consumed = reader(buffer, position, limit - position)
        position += consumed
        return consumed
    }

    private inline fun <T> readPrimitive(
        primitiveSize: Int,
        readDirect: (buffer: Buffer, offset: Int) -> T,
        fromLong: (Long) -> T
    ): T {
        val offset = position
        val targetLimit = offset + primitiveSize

        // TODO: fetchExpand can signal EOF
        if (limit >= targetLimit || fetchExpand(targetLimit)) {
            position = targetLimit
            return readDirect(buffer, offset)
        }

        if (offset == limit) {
            // current buffer exhausted, we cannot expand data in this buffer, 
            // and we also don't have bytes left to be read
            // so we should fetch new buffer of data and may be read entire primitive
            if (fetchBuffer() == 0)
                throw EOFException("End of file while reading buffer")

            // we know we are at zero position here
            if (limit >= primitiveSize) {
                position = primitiveSize
                return readDirect(buffer, 0)
            }
        }

        // Nope, doesn't fit in a buffer, read byte by byte
        var long = 0L
        fetchBytes(primitiveSize) { 
            long = (long shl 8) or it.toLong() 
        }
        return fromLong(long)
    }

    private inline fun fetchBytes(length: Int, consumer: (byte: UByte) -> Unit) {
        var remaining = length
        while (remaining > 0) {
            if (position == limit) {
                if (fetchBuffer() == 0) {
                    throw EOFException("End of file while reading buffer")
                }
            }

            consumer(buffer.loadUByteAt(position++))
            remaining--
        }
    }

    private fun fetchExpand(targetLimit: Int): Boolean {
        if (previewIndex != Bytes.InvalidPointer)
            return false // do not expand if in history mode TODO: expand if last buffer

        var currentLimit = limit
        val currentBuffer = buffer
        val currentSize = currentBuffer.size
        if (targetLimit > currentSize)
            return false // we can't expand data into current buffer

        while (currentLimit < targetLimit) {
            val fetched = fill(currentBuffer, currentLimit, currentSize - currentLimit)
            logln { "PGE: Loaded [$fetched]" }
            if (fetched == 0) {
                // TODO: set EOF
                return false
            }
            currentLimit += fetched
        }

        limit = currentLimit
        return true
    }

    fun <R> preview(reader: Input.() -> R): R {
        // Remember if we initiated the preview and should also start discard mode

        val initiated = if (previewIndex == Bytes.InvalidPointer) {
            // enable retaining of buffers
            previewBytes = Bytes().apply { append(buffer, limit) }
            previewIndex = Bytes.StartPointer
            true
        } else
            false

        if (previewIndex == -1) {
            // we were in discard mode, but new preview operation is starting, convert to preview mode
            previewIndex = Bytes.StartPointer
        }

        val markIndex = previewIndex
        val markPosition = position
        logln { "PVW: Enter at #$markIndex @$markPosition of $limit" }

        val result = reader()

/*
        TODO: optimize single buffer rewind
        if (previewIndex == markIndex) {
            // we are at the same buffer, just restore the position
            position = markPosition
            return result
        }
*/

        val bytes = previewBytes!!

        // restore the whole context
        previewIndex = if (initiated) -1 else markIndex
        bytes.pointed(markIndex) { buffer, limit ->
            this.buffer = buffer
            this.limit = limit
        }

        position = markPosition
        logln { "PVW: Exit at #$markIndex @$markPosition of $limit" }
        return result
    }

    private fun fetchBuffer(): Int {
        if (position != limit) {
            // trying to fetch a buffer when previous buffer was not exhausted is an internal error
            throw UnsupportedOperationException("Throwing bytes")
        }

        if (previewIndex == Bytes.InvalidPointer) {
            // no preview operation, reuse current buffer for new data
            logln { "PVW: None @$limit, filled buffer" }
            return fillBuffer(buffer)
        }

        val bytes = previewBytes!!

        // no preview operation in progress, but we still have buffers in history, we will free used buffers
        if (previewIndex == -1) {
            // return current buffer
            bufferPool.recycle(buffer)
            bytes.discardFirst()

            if (bytes.isEmpty()) {
                logln { "PVW: Finished @$limit, filled buffer" }
                // used all prefetched data, complete preview operation
                previewIndex = Bytes.InvalidPointer
                previewBytes = null

                // allocate and fetch a new buffer
                return fillBuffer(bufferPool.borrow())
            } else {
                val oldLimit = limit
                bytes.pointed(Bytes.StartPointer) { buffer, limit ->
                    this.buffer = buffer
                    this.limit = limit
                }
                position = 0
                logln { "PVW: Finished @$oldLimit, using prefetched buffer, $position/$limit" }
                return limit
            }
        }

        // let's look at the next historical buffer
        previewIndex = bytes.advancePointer(previewIndex)

        if (!bytes.isAfterLast(previewIndex)) {
            // we have a buffer already in history, i.e. replaying history inside another preview
            this.position = 0
            bytes.pointed(previewIndex) { buffer, limit ->
                this.buffer = buffer
                this.limit = limit
            }

            logln { "PVW: Preview #$previewIndex, using prefetched buffer, $position/$limit" }
            return limit
        }

        // here we are in a preview operation, but don't have any prefetched buffers ready
        // so we need to save current one and fill some more data

        logln { "PVW: Preview #$previewIndex, saved buffer, $position/$limit" }
        val fetched = fillBuffer(bufferPool.borrow())
        bytes.append(buffer, limit)
        logln { "PVW: Preview #$previewIndex, filled buffer, $position/$limit" }
        return fetched
    }

    private fun fillBuffer(buffer: Buffer): Int {
        val fetched = fill(buffer, 0, buffer.size)
        limit = fetched
        position = 0
        this.buffer = buffer
        return fetched
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
    protected abstract fun fill(destination: Buffer, offset: Int, length: Int): Int
}

private inline fun logln(text: () -> String) {

}
