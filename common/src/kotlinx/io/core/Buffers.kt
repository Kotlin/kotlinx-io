@file:Suppress("RedundantModalityModifier")

package kotlinx.io.core

import kotlinx.io.core.internal.*
import kotlinx.io.pool.*
import kotlin.native.concurrent.*

@Deprecated(
    "Use IoBuffer instead",
    replaceWith = ReplaceWith("IoBuffer", "kotlinx.io.core.IoBuffer"),
    level = DeprecationLevel.ERROR
)
typealias BufferView = IoBuffer

/**
 * A read-write facade to actual buffer of fixed size. Multiple views could share the same actual buffer.
 * Concurrent unsafe. The only concurrent-safe operation is [release].
 * In most cases [ByteReadPacket] and [BytePacketBuilder] should be used instead.
 */
expect class IoBuffer : Input, Output {
    @Deprecated("Suppress warning.", level = DeprecationLevel.HIDDEN)
    final override val doNotImplementInputButExtendAbstractInputInstead: Nothing

    @Deprecated("Suppress warning.", level = DeprecationLevel.HIDDEN)
    final override val doNotImplementOutputButExtendAbstractOutputInstead: Nothing

    /**
     * Reference to an origin buffer view this was copied from
     */
    internal val origin: IoBuffer?

    /**
     * Mutable reference to next buffer view. Useful to chain multiple views
     */
    var next: IoBuffer?

    /**
     * User data: could be a session, connection or anything useful
     */
    @ExperimentalIoApi
    var attachment: Any?

    /**
     * Backing buffer capacity. Value for released buffer is unspecified
     */
    val capacity: Int

    /**
     * Amount of reserved bytes at the beginning
     */
    val startGap: Int

    /**
     * Amount of reserved bytes at the end
     */
    val endGap: Int

    /**
     * @return `true` if there are available bytes to be read
     */
    fun canRead(): Boolean

    /**
     * @return `true` if there is free room to for write
     */
    fun canWrite(): Boolean

    /**
     * Number of bytes available for read
     */
    val readRemaining: Int

    /**
     * Number of free bytes useful for writing. Doesn't include gaps.
     */
    val writeRemaining: Int

    /**
     * Reserves [n] bytes at the beginning. Could be invoked only once and only before writing.
     */
    fun reserveStartGap(n: Int)

    /**
     * Reserves [n] bytes at the end of buffer. Could be invoked only once and only if there are at least [n] bytes free
     */
    fun reserveEndGap(n: Int)

    /**
     * read and write operations byte-order (endianness)
     */
    @Deprecated("Read/write with readXXXLittleEndian/writeXXXLittleEndian or " +
        "do readXXX/writeXXX with X.reverseByteOrder() instead.")
    final override var byteOrder: ByteOrder

    final override fun readByte(): Byte
    final override fun readShort(): Short
    final override fun readInt(): Int
    final override fun readLong(): Long
    final override fun readFloat(): Float
    final override fun readDouble(): Double

    @Deprecated("Use readFully instead", ReplaceWith("readFully(dst, offset, length)"), level = DeprecationLevel.ERROR)
    fun read(dst: ByteArray, offset: Int, length: Int)

    final override fun readFully(dst: ByteArray, offset: Int, length: Int)
    final override fun readFully(dst: ShortArray, offset: Int, length: Int)
    final override fun readFully(dst: IntArray, offset: Int, length: Int)
    final override fun readFully(dst: LongArray, offset: Int, length: Int)
    final override fun readFully(dst: FloatArray, offset: Int, length: Int)
    final override fun readFully(dst: DoubleArray, offset: Int, length: Int)
    final override fun readFully(dst: IoBuffer, length: Int)

    final override fun readAvailable(dst: ByteArray, offset: Int, length: Int): Int
    final override fun readAvailable(dst: ShortArray, offset: Int, length: Int): Int
    final override fun readAvailable(dst: IntArray, offset: Int, length: Int): Int
    final override fun readAvailable(dst: LongArray, offset: Int, length: Int): Int
    final override fun readAvailable(dst: FloatArray, offset: Int, length: Int): Int
    final override fun readAvailable(dst: DoubleArray, offset: Int, length: Int): Int
    final override fun readAvailable(dst: IoBuffer, length: Int): Int

    final override fun tryPeek(): Int

    /**
     * Copy available bytes to the specified [buffer] but keep them available.
     * If the underlying implementation could trigger
     * bytes population from the underlying source and block until any bytes available
     *
     * Very similar to [readAvailable] but don't discard copied bytes.
     *
     * @return number of bytes were copied
     */
    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun peekTo(buffer: IoBuffer): Int

    @Deprecated("Use discardExact instead.")
    final override fun discard(n: Long): Long

    /**
     * Discards [n] bytes or fails if there is not enough bytes available for read.
     */
    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun discardExact(n: Int)

    final override fun writeByte(v: Byte)
    final override fun writeShort(v: Short)
    final override fun writeInt(v: Int)
    final override fun writeLong(v: Long)
    final override fun writeFloat(v: Float)
    final override fun writeDouble(v: Double)

    final override fun writeFully(src: ByteArray, offset: Int, length: Int)
    final override fun writeFully(src: ShortArray, offset: Int, length: Int)
    final override fun writeFully(src: IntArray, offset: Int, length: Int)
    final override fun writeFully(src: LongArray, offset: Int, length: Int)
    final override fun writeFully(src: FloatArray, offset: Int, length: Int)
    final override fun writeFully(src: DoubleArray, offset: Int, length: Int)
    final override fun writeFully(src: IoBuffer, length: Int)

    fun appendChars(csq: CharArray, start: Int, end: Int): Int
    fun appendChars(csq: CharSequence, start: Int, end: Int): Int

    final override fun append(c: Char): Appendable
    final override fun append(csq: CharSequence?): Appendable
    final override fun append(csq: CharSequence?, start: Int, end: Int): Appendable
    final override fun append(csq: CharArray, start: Int, end: Int): Appendable

    final override fun fill(n: Long, v: Byte)

    override fun close()

    /**
     * Writes exactly [length] bytes of [array] starting from [offset] position or fails if not enough free space
     */
    @Deprecated("Use writeFully instead", level = DeprecationLevel.ERROR)
    fun write(array: ByteArray, offset: Int, length: Int)

    /**
     * Writes [length] bytes of [src] buffer or fails if not enough free space available
     */
    @Deprecated("Use writeFully instead", ReplaceWith("writeFully(src, length)"), level = DeprecationLevel.ERROR)
    fun writeBuffer(src: IoBuffer, length: Int): Int

    internal fun restoreStartGap(n: Int)
    internal fun restoreEndGap(n: Int)

    internal fun writeBufferPrepend(other: IoBuffer)
    internal fun writeBufferAppend(other: IoBuffer, maxSize: Int)

    /**
     * Push back [n] bytes: only possible if there were at least [n] bytes read before this operation.
     */
    fun pushBack(n: Int)

    /**
     * Marks the whole buffer available for write and no bytes for read.
     */
    fun resetForWrite()

    /**
     * Marks up to [limit] bytes of the buffer available for write and no bytes for read
     */
    fun resetForWrite(limit: Int)

    /**
     * Marks the whole buffer available for read and no for write
     */
    fun resetForRead()

    /**
     * @return `true` if and only if the are no buffer views that share the same actual buffer. This actually does
     * refcount and only work guaranteed if other views created/not created via [makeView] function.
     * One can instantiate multiple buffers with the same buffer and this function will return `true` in spite of
     * the fact that the buffer is actually shared.
     */
    @ExperimentalIoApi
    fun isExclusivelyOwned(): Boolean

    /**
     * Creates a new view to the same actual buffer with independent read and write positions and gaps
     */
    fun makeView(): IoBuffer

    /**
     * releases buffer view and returns it to the [pool] if there are no more usages. Based on simple ref-couting so
     * it is very fragile.
     */
    fun release(pool: ObjectPool<IoBuffer>)

    final override fun flush()

    companion object {
        /**
         * Number of bytes usually reserved in the end of chunk
         * when several instances of [IoBuffer] are connected into a chain (usually inside of [ByteReadPacket]
         * or [BytePacketBuilder])
         */
        @Deprecated("This implementation detail is going to become internal.")
        val ReservedSize: Int

        /**
         * The empty buffer singleton: it has zero capacity for read and write.
         */
        val Empty: IoBuffer

        /**
         * The default buffer pool
         */
        val Pool: ObjectPool<IoBuffer>

        /**
         * Pool that always instantiates new buffers instead of reusing it
         */
        val NoPool: ObjectPool<IoBuffer>

        /**
         * A pool that always returns [IoBuffer.Empty]
         */
        val EmptyPool: ObjectPool<IoBuffer>
    }
}

@Deprecated(
    "Use IoBuffer.EmptyPool instead.",
    replaceWith = ReplaceWith("IoBuffer.EmptyPool", "kotlinx.io.core.IoBuffer"),
    level = DeprecationLevel.ERROR
)
@SharedImmutable
val EmptyBufferViewPool get() = IoBuffer.EmptyPool

internal object EmptyBufferPoolImpl : NoPoolImpl<IoBuffer>() {
    override fun borrow() = IoBuffer.Empty
}

internal tailrec fun IoBuffer?.releaseAll(pool: ObjectPool<IoBuffer>) {
    if (this == null) return
    val next = next
    this.next = null
    release(pool)
    next.releaseAll(pool)
}

/**
 * Copy every element of the chain starting from this and setup next links.
 */
internal fun IoBuffer.copyAll(): IoBuffer {
    val copied = makeView()
    val next = this.next ?: return copied

    return next.copyAll(copied, copied)
}

private tailrec fun IoBuffer.copyAll(head: IoBuffer, prev: IoBuffer): IoBuffer {
    val copied = makeView()
    prev.next = copied

    val next = this.next ?: return head

    return next.copyAll(head, copied)
}

internal tailrec fun IoBuffer.findTail(): IoBuffer {
    val next = this.next ?: return this
    return next.findTail()
}

/**
 * Summarize remainings of all elements of the chain
 */
@DangerousInternalIoApi
fun IoBuffer.remainingAll(): Long = remainingAll(0L)

private tailrec fun IoBuffer.remainingAll(n: Long): Long {
    val rem = readRemaining.toLong() + n
    val next = this.next ?: return rem

    return next.remainingAll(rem)
}

internal tailrec fun IoBuffer.isEmpty(): Boolean {
    if (readRemaining > 0) return false
    val next = this.next ?: return true
    return next.isEmpty()
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Long.coerceAtMostMaxInt(): Int = minOf(this, Int.MAX_VALUE.toLong()).toInt()

@Suppress("NOTHING_TO_INLINE")
internal inline fun Long.coerceAtMostMaxIntOrFail(message: String): Int {
    if (this > Int.MAX_VALUE.toLong()) throw IllegalArgumentException(message)
    return this.toInt()
}

class BufferLimitExceededException(message: String) : Exception(message)
