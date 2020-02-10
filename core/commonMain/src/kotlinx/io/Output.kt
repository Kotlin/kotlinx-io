package kotlinx.io

import kotlinx.io.buffer.*
import kotlinx.io.pool.*

/**
 * [Output] is an abstract base for writable streams of bytes over some sink.
 *
 * [Output] is buffered. Buffer size depends on [Buffer.size] in the [pool] buffer.
 * Buffer size is [DEFAULT_BUFFER_SIZE] by default. Buffer can be flushed using [flush] method.
 *
 * To implement [Output] over a custom sink you should override only [fill] method.
 */
public abstract class Output(
    protected val pool: ObjectPool<Buffer> = DefaultBufferPool.Instance
) : Closeable {
    internal val bufferPool: ObjectPool<Buffer> get() = pool

    /**
     * Current buffer.
     */
    private var buffer: Buffer = pool.borrow()

    /**
     * Write position in [buffer].
     */
    private var position: Int = 0

    /**
     * Write a [value] to this [Input].
     */
    public fun writeByte(value: Byte) {
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
     * Bypass [data] from [startOffset] to [endOffset] by using [Output.flush].
     * If [Output] is not empty, all data will flushed beforehand.
     */
    public fun writeBuffer(data: Buffer, startIndex: Int = 0, endIndex: Int = data.size) {
        checkBufferAndIndexes(data, startIndex, endIndex)

        if (position != 0) {
            flushBuffer()
        }

        flush(data, startIndex, endIndex)
    }

    /**
     * Write all buffered bytes to underlying sink.
     */
    public fun flush() {
        if (position == 0) {
            return
        }

        flushBuffer()
    }

    /**
     * Closes the current output, flushing all buffered data to the underlying source
     * and [closing][closeSource] it
     */
    public final override fun close() {
        val flushException = runCatching { flush() }.exceptionOrNull()
        val closeException = runCatching { closeSource() }.exceptionOrNull()
        if (flushException !== null) {
            if (closeException !== null && closeException !== flushException) {
                flushException.addSuppressedInternal(closeException)
            }
            throw flushException
        }

        if (closeException !== null) {
            throw closeException
        }
    }

    /**
     * Request to fill [buffer] from [startIndex] to [endIndex] exclusive To destination.
     *
     * This method won't modify [source] and will block until all bytes from [source] won't be flushed.
     */
    protected abstract fun flush(source: Buffer, startIndex: Int, endIndex: Int)

    /**
     * Closes the underlying source of data used by this output.
     * This method is invoked once the output is [closed][close].
     */
    protected abstract fun closeSource()

    private fun flushBuffer() {
        flush(buffer, 0, position)
        buffer = pool.borrow()
        position = 0
    }

    /**
     * Call [writer] block with [buffer] and write [startOffset]
     *
     * The [writer] expected to return new [buffer] position.
     *
     * @return number of written bytes.
     */
    internal inline fun writeBuffer(writer: (buffer: Buffer, bufferStart: Int, bufferEnd: Int) -> Int): Int {
        if (position == buffer.size) {
            flushBuffer()
        }

        val newPosition = writer(buffer, position, buffer.size)
        val result = newPosition - position
        position = newPosition
        return result
    }

    internal fun size(): Int {
        return position
    }

    internal fun writePrimitive(
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

        // Nope, doesn't fit in a buffer, write byte by byte
        writeBytes(primitiveSize, longValue())
    }

    private fun writeBytes(primitiveSize: Int, value: Long) {
        // 8 -- max size aka Long.SIZE_BYTES, write in BE (most significant byte first)
        var remainingValue = value.reverseByteOrder() shr ((8 - primitiveSize) * 8)
        var size = buffer.size
        repeat(primitiveSize) {
            if (position == size) {
                flushBuffer()
                size = buffer.size
            }
            buffer.storeByteAt(position++, remainingValue.toByte())
            remainingValue = remainingValue shr 8
        }
    }
}