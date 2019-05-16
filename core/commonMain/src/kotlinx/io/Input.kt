package kotlinx.io

import kotlinx.io.buffer.*

abstract class Input(bufferSize: Int = DEFAULT_BUFFER_SIZE) : Closeable {

    internal constructor(bytes: Bytes) : this() {
        previewBytes = bytes
        previewIndex = Bytes.StartPointer // replay, not consume
        bytes.pointed(Bytes.StartPointer) { buffer, limit ->
            this.buffer = buffer
            this.limit = limit
        }
    }

    private val allocator = SingleBufferAllocator(bufferSize)

    // Current buffer 
    private var buffer: Buffer = allocator.allocate()

    // Current position in [buffer]
    private var position: Int = 0

    // Current filled number of bytes in [buffer]
    private var limit: Int = 0

    // TODO: implement manual list management with Array<Buffer> and IntArray
    // assume we normally preview at most 1 buffer ahead, so may be consider Any? for Buffer and Array<Buffer> here
    private var previewIndex: BytesPointer =
        Bytes.InvalidPointer // positive values mean replay mode, -1 means discard mode, MIN_VALUE means no preview
    private var previewBytes: Bytes? = null

    fun readLong(): Long =
        readPrimitive(8, { buffer, offset -> buffer.loadLongAt(offset) }, { it })

    fun readULong(): ULong =
        readPrimitive(8, { buffer, offset -> buffer.loadLongAt(offset).toULong() }, { it.toULong() })

    fun readInt(): Int =
        readPrimitive(4, { buffer, offset -> buffer.loadIntAt(offset) }, { it.toInt() })

    fun readUInt(): UInt =
        readPrimitive(4, { buffer, offset -> buffer.loadIntAt(offset).toUInt() }, { it.toUInt() })

    fun readShort(): Short =
        readPrimitive(2, { buffer, offset -> buffer.loadShortAt(offset) }, { it.toShort() })

    fun readUShort(): UShort =
        readPrimitive(2, { buffer, offset -> buffer.loadShortAt(offset).toUShort() }, { it.toUShort() })

    fun readByte(): Byte =
        readPrimitive(1, { buffer, offset -> buffer.loadByteAt(offset) }, { it.toByte() })

    fun readUByte(): UByte =
        readPrimitive(1, { buffer, offset -> buffer.loadByteAt(offset).toUByte() }, { it.toUByte() })

    fun readDouble(): Double =
        readPrimitive(8, { buffer, offset -> buffer.loadDoubleAt(offset) }, { Double.fromBits(it) })

    fun readFloat(): Float =
        readPrimitive(4, { buffer, offset -> buffer.loadFloatAt(offset) }, { Float.fromBits(it.toInt()) })

    fun readBuffer(reader: (Buffer, offset: Int, size: Int) -> Int) : Int {
        if (position == limit) {
            fetchBuffer()
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
        if (limit >= targetLimit || fetchExpand(targetLimit)) {
            position = targetLimit
            return readDirect(buffer, offset)
        }

        if (offset == limit) {
            // current buffer exhausted, we cannot expand data in this buffer, 
            // and we also don't have bytes left to be read
            // so we should fetch new buffer of data and may be read entire primitive
            fetchBuffer()
            // we know we are at zero position here
            if (limit >= primitiveSize) {
                position = primitiveSize
                return readDirect(buffer, 0)
            }
        }

        // Nope, doesn't fit in a buffer, read byte by byte
        var long = 0L
        fetchBytes(primitiveSize) { long = (long shl 8) or it.toLong() }
        return fromLong(long)
    }

    private inline fun fetchBytes(length: Int, consumer: (byte: Byte) -> Unit) {
        var remaining = length
        while (remaining > 0) {
            if (position == limit)
                fetchBuffer()

            consumer(buffer[position++])
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
            if (fetched == 0)
                throw Exception("EOF")
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

    private fun fetchBuffer() {
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
            allocator.free(buffer)
            bytes.discardFirst()

            if (bytes.isEmpty()) {
                logln { "PVW: Finished @$limit, filled buffer" }
                // used all prefetched data, complete preview operation
                previewIndex = Bytes.InvalidPointer
                previewBytes = null

                // allocate and fetch a new buffer
                return fillBuffer(allocator.allocate())
            } else {
                val oldLimit = limit
                bytes.pointed(Bytes.StartPointer) { buffer, limit ->
                    this.buffer = buffer
                    this.limit = limit
                }
                position = 0
                logln { "PVW: Finished @$oldLimit, using prefetched buffer, $position/$limit" }
                return
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
            return
        }

        // here we are in a preview operation, but don't have any prefetched buffers ready
        // so we need to save current one and fill some more data

        logln { "PVW: Preview #$previewIndex, saved buffer, $position/$limit" }
        fillBuffer(allocator.allocate())
        bytes.append(buffer, limit)
        logln { "PVW: Preview #$previewIndex, filled buffer, $position/$limit" }
    }

    private fun fillBuffer(buffer: Buffer) {
        val fetched = fill(buffer, 0, buffer.size)
        logln { "PG: Loaded [$fetched]" }
        if (fetched == 0)
            throw Exception("EOF")

        limit = fetched
        position = 0
        this.buffer = buffer
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
