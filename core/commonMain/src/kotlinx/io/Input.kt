package kotlinx.io

import kotlinx.io.buffer.*
import kotlinx.io.pool.*

private const val stateDiscardPreview = -1
private const val stateCollectPreview = 1

abstract class Input : Closeable {
    private val bufferPool: ObjectPool<Buffer>

    // Current buffer 
    private var buffer: Buffer

    // Current position in [buffer]
    var position: Int = 0

    // Current filled number of bytes in [buffer]
    private var limit: Int = 0

    constructor(bufferPool: ObjectPool<Buffer>) {
        this.bufferPool = bufferPool
        this.buffer = bufferPool.borrow()
        previewBytes = null
    }

    internal constructor(bytes: Bytes) {
        this.bufferPool = bytes.bufferPool
        previewBytes = bytes
        this.buffer = bytes.pointed(Bytes.StartPointer) { limit ->
            this.limit = limit
        }
    }

    constructor(bufferSize: Int = DEFAULT_BUFFER_SIZE) : this(
        if (bufferSize == DEFAULT_BUFFER_SIZE)
            DefaultBufferPool.Instance
        else
            DefaultBufferPool(bufferSize)
    )

    private var previewState: Int = stateDiscardPreview
    private var previewIndex: Int = Bytes.StartPointer
    private var previewBytes: Bytes?

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

    fun readUByte(): UByte = readByte().toUByte()

    fun readLong(): Long = readPrimitive(8,
        { buffer, offset -> buffer.loadLongAt(offset) },
        { it })

    fun readULong(): ULong = readPrimitive(8,
        { buffer, offset -> buffer.loadULongAt(offset) },
        { it.toULong() })

    fun readDouble(): Double = readPrimitive(8,
        { buffer, offset -> buffer.loadDoubleAt(offset) },
        { Double.fromBits(it) })

    fun readInt(): Int = readPrimitive(4,
        { buffer, offset -> buffer.loadIntAt(offset) },
        { it.toInt() })

    fun readUInt(): UInt = readPrimitive(4,
        { buffer, offset -> buffer.loadUIntAt(offset) },
        { it.toUInt() })

    fun readFloat(): Float = readPrimitive(4,
        { buffer, offset -> buffer.loadFloatAt(offset) },
        { Float.fromBits(it.toInt()) })

    fun readShort(): Short = readPrimitive(2,
        { buffer, offset -> buffer.loadShortAt(offset) },
        { it.toShort() })

    fun readUShort(): UShort = readPrimitive(2,
        { buffer, offset -> buffer.loadUShortAt(offset) },
        { it.toUShort() })

    // TODO: Dangerous to use, if non-local return then position will not be updated
    internal inline fun readBufferLength(reader: (Buffer, offset: Int, size: Int) -> Int): Int {
        if (position == limit) {
            if (fetchBuffer() == 0)
                throw EOFException("End of file while reading buffer")
        }
        val consumed = reader(buffer, position, limit - position)
        position += consumed
        return consumed
    }

    // TODO: Dangerous to use, if non-local return then position will not be updated
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

    fun <R> preview(reader: Input.() -> R): R {
        if (position == limit) {
            if (fetchBuffer() == 0)
                throw EOFException("End of file while reading buffer")
        }

        val markState = previewState
        val markIndex = previewIndex
        val markPosition = position
        logln { "PVW: Enter preview in state $markState at #$markIndex" }

        previewState = stateCollectPreview

        val result = reader()

        logln { "PVW: Finished preview in state $previewState at #$previewIndex," }

        previewState = markState
        position = markPosition

        if (previewIndex == markIndex) {
            // we are at the same buffer, just restore the position & state above
            return result
        }

        val bytes = previewBytes!!
        this.buffer = bytes.pointed(markIndex) { limit -> this.limit = limit }
        previewIndex = markIndex

        logln {
            if (markState == stateDiscardPreview) {
                "PVW: Discarding at #0"
            } else {
                "PVW: Replaying at #$previewIndex"
            }
        }

        return result
    }

    private fun fetchBuffer(): Int {
        if (position != limit) {
            // trying to fetch a buffer when previous buffer was not exhausted is an internal error
            throw IllegalStateException("Throwing bytes away")
        }

        val state = previewState
        val bytes = previewBytes ?: if (state == stateDiscardPreview) {
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

        if (state == stateDiscardPreview) {
            bufferPool.recycle(buffer)
            bytes.discardFirst()
            if (bytes.isEmpty()) {
                //bytes.close()
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

        if (previewBytes != null) {
            //TODO: previewBytes.close()
        }
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

