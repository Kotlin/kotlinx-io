package kotlinx.io

import kotlinx.io.buffer.*
import kotlinx.io.pool.*

abstract class Input : Closeable {
    /**
     * Pool for obtaining buffers for operations
     */
    private val bufferPool: ObjectPool<Buffer>

    /**
     * Buffer for current operations 
     * Note, that buffer can be exhausted (position == limit) 
     */
    private var buffer: Buffer

    /**
     * Current reading position in the [buffer]
     */
    private var position: Int = 0

    /**
     * Number of bytes loaded into the [buffer]
     */
    private var limit: Int = 0

    /**
     * Constructs a new Input with the given `bufferPool`
     */
    constructor(bufferPool: ObjectPool<Buffer>) {
        this.bufferPool = bufferPool
        this.buffer = bufferPool.borrow()
        previewBytes = null
    }

    /**
     * Constructs a new Input with the given [bytes], pool is taken from [bytes]
     */
    internal constructor(bytes: Bytes) {
        this.bufferPool = bytes.bufferPool
        previewBytes = bytes
        this.buffer = bytes.pointed(Bytes.StartPointer) { limit ->
            this.limit = limit
        }
    }

    /**
     * Constructs a new Input with the default pool of buffers with the given [bufferSize]
     */
    constructor(bufferSize: Int = DEFAULT_BUFFER_SIZE) : this(
        if (bufferSize == DEFAULT_BUFFER_SIZE)
            DefaultBufferPool.Instance
        else
            DefaultBufferPool(bufferSize)
    )

    /**
     * Index of a current buffer in the [previewBytes]
     */
    private var previewIndex: Int = Bytes.StartPointer

    /**
     * Flag to indicate if current [previewBytes] are being discarded
     * When no preview is in progress and a current [buffer] has been exhausted, it should be discarded
     */
    private var previewDiscard: Boolean = true

    /**
     * Recorded buffers for preview operation
     */
    private var previewBytes: Bytes?

    /**
     * Reads a [Byte] from this Input
     * @throws EOFException if no more bytes can be read
     */
    fun readByte(): Byte {
        val offset = position

        if (limit > offset) {
            position = offset + 1
            return buffer.loadByteAt(offset)
        }

        if (fetchBuffer() == 0)
            throw EOFException("End of file while reading buffer")

        position = 1
        return buffer.loadByteAt(0)
    }

    /**
     * Reads an [UByte] from this Input
     * @throws EOFException if no more bytes can be read
     */
    fun readUByte(): UByte = readByte().toUByte()

    /**
     * Reads a [Long] from this Input
     * @throws EOFException if no more bytes can be read
     */
    fun readLong(): Long = readPrimitive(8,
        { buffer, offset -> buffer.loadLongAt(offset) },
        { it })

    /**
     * Reads a [ULong] from this Input
     * @throws EOFException if no more bytes can be read
     */
    fun readULong(): ULong = readPrimitive(8,
        { buffer, offset -> buffer.loadULongAt(offset) },
        { it.toULong() })

    /**
     * Reads a [Double] from this Input
     * @throws EOFException if no more bytes can be read
     */
    fun readDouble(): Double = readPrimitive(8,
        { buffer, offset -> buffer.loadDoubleAt(offset) },
        { Double.fromBits(it) })

    /**
     * Reads an [Int] from this Input
     * @throws EOFException if no more bytes can be read
     */
    fun readInt(): Int = readPrimitive(4,
        { buffer, offset -> buffer.loadIntAt(offset) },
        { it.toInt() })

    /**
     * Reads an [UInt] from this Input
     * @throws EOFException if no more bytes can be read
     */
    fun readUInt(): UInt = readPrimitive(4,
        { buffer, offset -> buffer.loadUIntAt(offset) },
        { it.toUInt() })

    /**
     * Reads a [Float] from this Input
     * @throws EOFException if no more bytes can be read
     */
    fun readFloat(): Float = readPrimitive(4,
        { buffer, offset -> buffer.loadFloatAt(offset) },
        { Float.fromBits(it.toInt()) })

    /**
     * Reads a [Short] from this Input
     * @throws EOFException if no more bytes can be read
     */
    fun readShort(): Short = readPrimitive(2,
        { buffer, offset -> buffer.loadShortAt(offset) },
        { it.toShort() })

    /**
     * Reads an [UShort] from this Input
     * @throws EOFException if no more bytes can be read
     */
    fun readUShort(): UShort = readPrimitive(2,
        { buffer, offset -> buffer.loadUShortAt(offset) },
        { it.toUShort() })

    /**
     * Allows direct read from a buffer, operates on offset+size, returns number of bytes consumed
     * NOTE: Dangerous to use, if non-local return then position will not be updated
     * @throws EOFException if no more bytes can be read
     */
    internal inline fun readBufferLength(reader: (Buffer, offset: Int, size: Int) -> Int): Int {
        if (position == limit) {
            if (fetchBuffer() == 0)
                throw EOFException("End of file while reading buffer")
        }
        val consumed = reader(buffer, position, limit - position)
        position += consumed
        return consumed
    }

    /**
     * Allows direct read from a buffer, operates on startOffset + endOffset (exclusive), returns new position
     * NOTE: Dangerous to use, if non-local return then position will not be updated
     * @throws EOFException if no more bytes can be read
     */
    internal inline fun readBufferRange(reader: (Buffer, startOffset: Int, endOffset: Int) -> Int) {
        var startOffset = position
        var endOffset = limit
        if (startOffset == endOffset) {
            if (fetchBuffer() == 0)
                throw EOFException("End of file while reading buffer")
            startOffset = 0
            endOffset = limit
        }
        val newPosition = reader(buffer, startOffset, endOffset)
        position = newPosition
    }

    /**
     * Reads a primitive of [primitiveSize] either directly from a buffer with [readDirect]
     * or byte-by-byte into a Long and using [fromLong] to convert to target type
     */
    private inline fun <T> readPrimitive(
        primitiveSize: Int,
        readDirect: (buffer: Buffer, offset: Int) -> T,
        fromLong: (Long) -> T
    ): T {
        val currentPosition = position
        val currentLimit = limit
        val targetLimit = currentPosition + primitiveSize

        if (currentLimit >= targetLimit) {
            position = targetLimit
            return readDirect(buffer, currentPosition)
        }

        if (currentLimit == currentPosition) {
            // current buffer exhausted and we also don't have bytes left to be read
            // so we should fetch new buffer of data and may be read entire primitive
            if (fetchBuffer() == 0)
                throw EOFException("End of file while reading buffer")

            // we know we are at zero position here, but limit & buffer could've changed, so can't use cached value
            if (limit >= primitiveSize) {
                position = primitiveSize
                return readDirect(buffer, 0)
            }
        }

        // Nope, doesn't fit in a buffer, read byte by byte
        var long = 0L
        readBytes(primitiveSize) {
            long = (long shl 8) or it.toLong()
        }
        return fromLong(long)
    }

    /**
     * Reads [size] unsigned bytes from an Input and calls [consumer] on each of them
     */
    private inline fun readBytes(size: Int, consumer: (byte: UByte) -> Unit) {
        var remaining = size
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

    /**
     * Begins a preview operation and calls [reader] with an instance of `Input` to read from during preview.
     * 
     * This operations saves the current state of the Input and begins to accumulate buffers for replay.
     * When `reader` finishes, it rewinds this Input to the previos state. 
     * 
     * Preview operations can be nested. 
     */
    fun <R> preview(reader: Input.() -> R): R {
        if (position == limit) {
            if (fetchBuffer() == 0)
                throw EOFException("End of file while reading buffer")
        }

        val markDiscard = previewDiscard
        val markIndex = previewIndex
        val markPosition = position
        logln { "PVW: Enter preview in state $markDiscard at #$markIndex" }

        previewDiscard = false

        val result = reader()

        logln { "PVW: Finished preview in state $previewDiscard at #$previewIndex," }

        previewDiscard = markDiscard
        position = markPosition

        if (previewIndex == markIndex) {
            // we are at the same buffer, just restore the position & state above
            return result
        }

        val bytes = previewBytes!!
        this.buffer = bytes.pointed(markIndex) { limit -> this.limit = limit }
        previewIndex = markIndex

        logln {
            if (markDiscard) {
                "PVW: Discarding at #0"
            } else {
                "PVW: Replaying at #$previewIndex"
            }
        }

        return result
    }

    /**
     * Prepares this Input for reading from the next buffer, either by filling it from the underlying source
     * or loading from a [previewBytes] after a [preview] operation or if reading from pre-supplied [Bytes]
     * 
     * Current [buffer] should be exhausted at this moment, i.e. [position] should be equal to [limit]
     */
    private fun fetchBuffer(): Int {
        if (position != limit) {
            // trying to fetch a buffer when previous buffer was not exhausted is an internal error
            throw IllegalStateException("Throwing bytes away")
        }

        val discard = previewDiscard
        val bytes = previewBytes ?: if (discard) {
            // fast path, no preview operation, reuse current buffer for new data
            val fetched = fill(buffer)
            limit = fetched
            position = 0
            logln { "PVW: None, filled buffer," }
            return fetched
        } else {
            // we are collecting bytes, so need to put current buffer to maintain invariant
            Bytes(bufferPool).also {
                previewBytes = it
                it.append(buffer, limit)
            }
        }

        if (discard) {
            bufferPool.recycle(buffer)
            bytes.discardFirst()
            if (bytes.isEmpty()) {
                bytes.close()
                previewBytes = null
                val fetched = fillBuffer(bufferPool.borrow())
                logln { "PVW: Completed discarding, filled buffer," }
                return fetched
            }
            
            val oldLimit = limit
            this.buffer = bytes.pointed(Bytes.StartPointer) { limit -> this.limit = limit }
            position = 0
            logln { "PVW: Discarded $oldLimit, get next buffer," }
            return limit
        }
        
        val nextIndex = bytes.advancePointer(previewIndex)
        if (bytes.isAfterLast(nextIndex)) {
            val fetched = fillBuffer(bufferPool.borrow())
            bytes.append(buffer, limit)
            previewIndex = nextIndex
            logln { "PVW: Preview #$previewIndex, filled buffer," }
            return fetched
        }
        
        // we have a buffer already in history, i.e. replaying history inside another preview
        this.buffer = bytes.pointed(nextIndex) { limit -> this.limit = limit }
        this.position = 0
        previewIndex = nextIndex

        logln { "PVW: Preview #$nextIndex, using prefetched buffer" }
        return limit
    }

    private fun fillBuffer(buffer: Buffer): Int {
        val fetched = fill(buffer)
        limit = fetched
        position = 0
        this.buffer = buffer
        return fetched
    }

    /**
     * Closes the underlying source.
     */
    abstract fun closeSource()

    /**
     * Closes input including the underlying source. All pending bytes will be discarded.
     */
    final override fun close() {
        closeSource()
        bufferPool.recycle(buffer)
        if (DefaultBufferPool.Instance != bufferPool)
            bufferPool.close()

        previewBytes?.close()
    }

    /**
     * Reads the next bytes into the [buffer]
     * May block until at least one byte is available.
     * TODO: ?? Usually bypass all exceptions from the underlying source.
     *
     * @return number of bytes were copied or `0` if no more input is available
     */
    protected abstract fun fill(buffer: Buffer): Int

    protected open fun fill(destinations: Array<Buffer>): Int = destinations.sumBy { fill(it) }

    private inline fun logln(text: () -> String) {
        //println(text() + " $position/$limit [${buffer[0]}, $previewBytes]")
    }
}

