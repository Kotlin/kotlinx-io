package kotlinx.io

import kotlinx.io.buffer.*
import kotlinx.io.pool.*
import kotlin.math.*

/**
 * [Input] is an abstract base class for synchronous byte readers.
 *
 * It contains `read*` methods to read primitive types ([readByte], [readInt], ...) and arrays ([readByteArray]).
 *
 * [Input] is buffered. Buffer size depends on [Buffer.size] in the [bufferPool] buffer.
 * Buffer size is [DEFAULT_BUFFER_SIZE] by default.
 *
 * To ensure that the required amount bytes are available, you can prefetch it with the [prefetch] method.
 *
 * [Input] supports a rewind mechanism with the [preview] method.
 *
 * [Input] is a resource because it holds the source. The user has to close [Input] with the [close] method at the end.
 *
 * To implement [Input] over a custom source, you should override [fill] and [closeSource] methods:
 * ```
 * class ConstantInput : Input() {
 *     private var closed: Boolean = false
 *
 *     override fun fill(buffer: Buffer): Int {
 *         if (closed) {
 *            return 0
 *         }
 *
 *         buffer.storeByteAt(index = 0, value = 42)
 *         return 1
 *     }
 *
 *     override fun closeSource() {
 *         closed = true
 *     }
 * }
 * ```
 *
 * Please note that [Input] doesn't provide any synchronization over the source and doesn't support concurrent access.
 *
 * TODO: document [prefetch] and (abstract, protected) API.
 */
public abstract class Input : Closeable {
    /**
     * Pool for obtaining buffers for operations.
     */
    private val bufferPool: ObjectPool<Buffer>

    /**
     * Buffer for current operations.
     * Note, that buffer can be exhausted (position == limit).
     */
    private var buffer: Buffer

    /**
     * Current reading position in the [buffer].
     */
    private var position: Int = 0

    /**
     * Number of bytes loaded into the [buffer].
     */
    private var limit: Int = 0

    /**
     * Index of a current buffer in the [previewBytes].
     */
    private var previewIndex: Int = 0

    /**
     * Flag to indicate if current [previewBytes] are being discarded.
     * When no preview is in progress and a current [buffer] has been exhausted, it should be discarded.
     */
    private var previewDiscard: Boolean = true

    /**
     * Recorded buffers for preview operation.
     */
    private var previewBytes: Bytes? = null

    /**
     * Constructs a new Input with the given `bufferPool`.
     */
    constructor(pool: ObjectPool<Buffer>) {
        bufferPool = pool
        buffer = pool.borrow()
    }

    /**
     * Constructs a new Input with the given [bytes], pool is taken from [bytes].
     */
    internal constructor(bytes: Bytes, pool: ObjectPool<Buffer>) {
        bufferPool = pool
        if (bytes.size() == 0) {
            buffer = Buffer.EMPTY
            return
        }

        previewBytes = bytes

        bytes.pointed(0) { firstBuffer, firstLimit ->
            buffer = firstBuffer
            limit = firstLimit
        }
    }

    /**
     * Constructs a new Input with the default pool of buffers.
     */
    protected constructor() : this(DefaultBufferPool.Instance)

    /**
     * Reads the available content in current [Input] to the [destination].
     * If no bytes are available in the input, [fill] method will be called directly on
     * the [destination] underlying buffer without an extra copy.
     * Otherwise, available bytes are copied to the destination.
     *
     * TODO: fix this one along all the codebase
     * If [Input] and [Output] have different buffer pools, available bytes are copied and
     * no direct transfer is performed
     */
    public fun readAvailableTo(destination: Output): Int {
        if (!previewDiscard) {
            return copyAvailableTo(destination)
        }

        val preview = previewBytes
        // Do we have single byte in cache?
        if (position != limit || preview != null && previewIndex < preview.tail) {
            if (bufferPool !== destination.bufferPool) {
                // Can't share buffers between different pools.
                return copyAvailableTo(destination)
            }

            return readBufferRange { buffer, startOffset, endOffset ->
                destination.writeBuffer(buffer, startOffset, endOffset)
                endOffset - startOffset
            }
        }

        // No bytes in cache: fill [destination] buffer direct.
        return destination.writeBuffer { buffer, startIndex, endIndex ->
            startIndex + fill(buffer, startIndex, endIndex)
        }
    }

    /**
     * Reads the available content in the current [Input] to the [destination] buffer.
     *
     * If no bytes are available, [fill] method will be called directly on the [destination] buffer without an extra copy.
     * Otherwise, available bytes are copied to the destination.
     *
     * @return number of bytes written to the [destination].
     */
    public fun readAvailableTo(
        destination: Buffer,
        startIndex: Int = 0,
        endIndex: Int = destination.size
    ): Int {
        checkBufferAndIndexes(destination, startIndex, endIndex)

        // If we have any cached byte, we should copy it
        if (position != limit || previewBytes != null) {
            return copyAvailableTo(destination, startIndex, endIndex)
        }
        // No bytes in cache: fill [destination] buffer direct.
        return fill(destination, startIndex, endIndex)
    }

    /**
     * Allows reading from [Input] in the [reader] block without consuming its content.
     *
     * This operation saves the state of the [Input] before [reader] and accumulates buffers for replay.
     * When the [reader] finishes, it rewinds this [Input] to the state before the [reader] block.
     * Please note that [Input] holds accumulated buffers until they are't consumed outside of the [preview].
     *
     * Preview operations can be nested.
     */
    public fun <R> preview(reader: Input.() -> R): R {
        if (position == limit && fetchCachedOrFill() == 0) {
            throw EOFException("End of file while reading buffer")
        }

        val markDiscard = previewDiscard
        val markIndex = previewIndex
        val markPosition = position

        previewDiscard = false
        val result = reader()
        previewDiscard = markDiscard
        position = markPosition

        if (previewIndex == markIndex) {
            // we are at the same buffer, just restore the position & state above
            return result
        }

        val bytes = previewBytes!!
        bytes.pointed(markIndex) { buffer, limit ->
            this.buffer = buffer
            this.limit = limit
        }

        previewIndex = markIndex
        return result
    }

    /**
     * Check if at least one byte is available to read.
     *
     * This method can use [fill] if no bytes are available in the cache. Please consider using [readAvailableTo] in
     * performance-critical code.
     */
    @Suppress("NOTHING_TO_INLINE")
    public inline fun exhausted(): Boolean = !prefetch(1)

    /**
     * Checks that [size] bytes are fetched in [Input].
     *
     * @return true if this [Input] contains at least [size] bytes.
     */
    public fun prefetch(size: Int): Boolean {
        checkSize(size)

        if (position == limit && fetchCachedOrFill() == 0) {
            return false
        }

        var left = size - (limit - position)
        if (left <= 0) {
            return true // Enough bytes in current buffer
        }

        // We will fetch bytes into additional buffers, so prepare preview
        val bytes = previewBytes ?: startPreview()

        var fetchIndex = previewIndex
        while (!bytes.isAfterLast(fetchIndex)) {
            left -= bytes.limit(fetchIndex)
            if (left <= 0) {
                // Enough bytes in preview bytes
                return true
            }

            fetchIndex++
        }

        while (left > 0) {
            val (buffer, limit) = try {
                val buffer = bufferPool.borrow()
                val limit = fill(buffer)
                buffer to limit
            } catch (cause: Throwable) {
                bufferPool.recycle(buffer)
                throw cause
            }

            if (limit == 0) {
                bufferPool.recycle(buffer)
                return false
            }

            bytes.append(buffer, limit)
            left -= limit
        }
        return true
    }

    /**
     * Skips [count] bytes from input.
     *
     * @return skipped bytes count. It could be less than [count] if no more bytes available.
     */
    public fun discard(count: Int): Int {
        checkCount(count)

        var remaining = count
        while (remaining > 0) {
            val skipCount = readBufferRange { _, startOffset, endOffset ->
                min(remaining, endOffset - startOffset)
            }

            if (skipCount == 0) {
                break
            }

            remaining -= skipCount
        }

        return count - remaining
    }

    /**
     * Closes input including the underlying source. All pending bytes will be discarded.
     */
    public final override fun close() {
        closeSource()

        limit = position
        bufferPool.recycle(buffer)

        val preview = previewBytes ?: return
        previewBytes = null

        preview.discardFirst()
        while (!preview.isEmpty()) {
            preview.pointed(0) { buffer, _ ->
                bufferPool.recycle(buffer)
            }
            preview.discardFirst()
        }
    }

    /**
     * Requests to fill [buffer] from [startIndex] to [endIndex] exclusive from the underlying source.
     * This method may block and wait if no bytes are available.
     * This method copies bytes from source to [buffer] from [startIndex] to [startIndex] + `return-value`.
     *
     * The [startIndex] cannot be negative, the [endIndex] should be greater than [startIndex]
     * The [buffer] size should be positive, [endIndex] cannot be greater than [buffer] size.
     * Writing and reading to the [buffer] outside of the given range leads to unspecified behaviour.
     *
     * @return number of bytes were filled (`endIndex - startIndex` at most) or `0` if no more input is available.
     */
    protected abstract fun fill(buffer: Buffer, startIndex: Int = 0, endIndex: Int = buffer.size - startIndex): Int

    /**
     * Reads next bytes into the [destinations].
     * May block until at least one byte is available.
     *
     * TODO: purpose.
     *
     * @return number of bytes were copied or `0` if no more input is available.
     */
    protected open fun fill(destinations: Array<Buffer>): Int = destinations.sumBy { fill(it) }

    /**
     * Closes the underlying source of data used by this input.
     * This method is invoked once the input is [closed][close].
     */
    protected abstract fun closeSource()

    /**
     * Reads a primitive of [primitiveSize] either directly from a buffer with [readDirect] or byte-by-byte into a Long.
     */
    internal inline fun readPrimitive(
        primitiveSize: Int,
        readDirect: (buffer: Buffer, offset: Int) -> Long
    ): Long {
        val position = preparePrimitiveReadPosition(primitiveSize)
        if (position != -1) {
            return readDirect(buffer, position)
        }
        // Nope, doesn't fit in a buffer, read byte by byte
        return readPrimitive(primitiveSize)
    }

    private fun preparePrimitiveReadPosition(primitiveSize: Int): Int {
        val currentPosition = position
        val currentLimit = limit
        val targetLimit = currentPosition + primitiveSize

        if (currentLimit >= targetLimit) {
            position = targetLimit
            return currentPosition
        }

        if (currentLimit == currentPosition) {
            // current buffer exhausted and we also don't have bytes left to be read
            // so we should fetch new buffer of data and may be read entire primitive
            if (fetchCachedOrFill() == 0) {
                throw EOFException("End of file while reading buffer")
            }

            // we know we are at zero position here, but limit & buffer could've changed, so can't use cached value
            if (limit >= primitiveSize) {
                position = primitiveSize
                return 0
            }
        }
        return -1
    }

    private fun readPrimitive(size: Int): Long {
        var remaining = size
        var current = position
        var currentLimit = limit
        var result = 0L
        while (remaining > 0) {
            if (current == currentLimit) {
                if (fetchCachedOrFill() == 0) {
                    throw EOFException("End of file while reading buffer")
                }

                current = position
                currentLimit = limit
            }

            val byte = buffer.loadByteAt(current).toInt() and 0xFF
            result = (result shl 8) or byte.toLong()
            current++
            remaining--
        }

        position = current
        limit = currentLimit
        return result
    }

    /**
     * Allows direct read from a buffer, operates on startOffset + endOffset (exclusive), returns consumed bytes count.
     * NOTE: Dangerous to use, if non-local return then position will not be updated.
     *
     * @return consumed bytes count
     */
    internal inline fun readBufferRange(reader: (Buffer, startOffset: Int, endOffset: Int) -> Int): Int {
        if (position == limit && fetchCachedOrFill() == 0) {
            return 0
        }

        val consumed = reader(buffer, position, limit)
        position += consumed
        return consumed
    }

    /**
     * Copies currently buffered bytes to the given [destination]. If no bytes are currently buffered, [fill] method
     * is invoked first.
     *
     * @return transferred bytes count.
     */
    private fun copyAvailableTo(destination: Output): Int = destination.writeBuffer { buffer, startIndex, endIndex ->
        startIndex + copyAvailableTo(buffer, startIndex, endIndex)
    }

    /**
     * Copies currently buffered bytes to the given [destination]. If no bytes are currently buffered, [fill] method
     * is invoked first.
     *
     * @return transferred bytes count.
     */
    private fun copyAvailableTo(destination: Buffer, startIndex: Int, endIndex: Int): Int {
        if (position == limit && fetchCachedOrFill() == 0) {
            return 0
        }

        val length = min(endIndex - startIndex, limit - position)
        buffer.copyTo(destination, position, length, startIndex)
        position += length

        return length
    }

    /**
     * Prepares this Input for reading from the next buffer, either by filling it from the underlying source
     * or loading from a [previewBytes] after a [preview] operation or if reading from pre-supplied [Bytes].
     *
     * Current [buffer] should be exhausted at this moment, i.e. [position] should be equal to [limit].
     */
    private fun fetchCachedOrFill(): Int {
        val discard = previewDiscard
        val preview = previewBytes

        if (discard) {
            if (preview == null) {
                return fillFromSource()
            }

            return fetchFromPreviewAndDiscard(preview)
        }

        val bytes = preview ?: startPreview()
        return readThroughPreview(bytes)
    }

    /**
     * Instantiates [previewBytes] and fill it.
     */
    private fun startPreview(): Bytes {
        val bytes = Bytes(bufferPool).apply {
            append(buffer, limit)
        }

        previewBytes = bytes
        return bytes
    }

    /**
     * Fetches buffer from [bytes].
     * The [bytes] shouldn't be empty.
     */
    private fun fetchFromPreviewAndDiscard(bytes: Bytes): Int {
        bufferPool.recycle(buffer)
        bytes.discardFirst()

        if (bytes.isEmpty()) {
            previewBytes = null
            return fillFromSource()
        }

        bytes.pointed(0) { newBuffer, newLimit ->
            position = 0
            buffer = newBuffer
            limit = newLimit
        }

        return limit
    }

    private fun readThroughPreview(bytes: Bytes): Int {
        val nextIndex = previewIndex + 1

        if (bytes.isAfterLast(nextIndex)) {
            val fetched = fillFromSource() // received data can be empty
            bytes.append(buffer, limit) // buffer can be empty
            previewIndex = nextIndex
            return fetched
        }

        // we have a buffer already in history, i.e. replaying history inside another preview
        bytes.pointed(nextIndex) { buffer, limit ->
            this.buffer = buffer
            this.limit = limit
        }

        position = 0
        previewIndex = nextIndex
        return limit
    }

    private fun fillFromSource(): Int {
        val source = bufferPool.borrow()
        val fetched = fill(source)
        limit = fetched
        position = 0
        buffer = source
        return fetched
    }

    /**
     * Calculates how many bytes remaining in current preview instance.
     */
    internal fun remainingCacheSize(): Int {
        val previewSize = previewBytes?.size(previewIndex) ?: limit
        return previewSize - position
    }
}
