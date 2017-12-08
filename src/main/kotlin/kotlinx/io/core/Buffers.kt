package kotlinx.io.core

import kotlinx.io.pool.*

/**
 * A read-write facade to actual buffer of fixed size. Multiple views could share the same actual buffer.
 * Concurrent unsafe. The only concurrent-safe operation is [release].
 * In most cases [ByteReadPacket] and [BytePacketBuilder] should be used instead.
 */
expect class BufferView {
    /**
     * Reference to an origin buffer view this was copied from
     */
    internal val origin: BufferView?

    /**
     * Mutable reference to next buffer view. Useful to chain multiple views
     */
    var next: BufferView?

    /**
     * User data: could be a session, connection or anything useful
     */
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
    var byteOrder: ByteOrder

    fun readByte(): Byte
    fun readShort(): Short
    fun readInt(): Int
    fun readLong(): Long
    fun readFloat(): Float
    fun readDouble(): Double
    fun read(dst: ByteArray, offset: Int, length: Int)

    /**
     * Discards [n] bytes or fails if there is not enough bytes available for read.
     */
    fun discardExact(n: Int)

    fun writeByte(v: Byte)
    fun writeShort(v: Short)
    fun writeInt(v: Int)
    fun writeLong(v: Long)
    fun writeFloat(v: Float)
    fun writeDouble(v: Double)

    /**
     * Writes exactly [length] bytes of [array] starting from [offset] position or fails if not enough free space
     */
    fun write(array: ByteArray, offset: Int, length: Int)

    /**
     * Writes [length] bytes of [src] buffer or fails if not enough free space available
     */
    fun writeBuffer(src: BufferView, length: Int): Int
    internal fun writeBufferPrepend(other: BufferView)
    internal fun writeBufferAppend(other: BufferView, maxSize: Int)

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
    fun isExclusivelyOwned(): Boolean

    /**
     * Creates a new view to the same actual buffer with independant read and write positions and gaps
     */
    fun makeView(): BufferView

    /**
     * releases buffer view and returns it to the [pool] if there are no more usages. Based on simple ref-couting so
     * it is very fragile.
     */
    fun release(pool: ObjectPool<BufferView>)

    companion object {
        val Empty: BufferView
        val Pool: ObjectPool<BufferView>
        val NoPool: ObjectPool<BufferView>
    }
}

object EmptyBufferViewPool : NoPoolImpl<BufferView>() {
    override fun borrow() = BufferView.Empty
}

internal tailrec fun BufferView?.releaseAll(pool: ObjectPool<BufferView>) {
    if (this == null) return
    release(pool)
    next.releaseAll(pool)
}

/**
 * Copy every element of the chain starting from this and setup next links.
 */
internal fun BufferView.copyAll(): BufferView {
    val copied = makeView()
    val next = this.next ?: return copied

    return next.copyAll(copied, copied)
}

private tailrec fun BufferView.copyAll(head: BufferView, prev: BufferView): BufferView {
    val copied = makeView()
    prev.next = copied

    val next = this.next ?: return head

    return next.copyAll(head, copied)
}

internal tailrec fun BufferView.findTail(): BufferView {
    val next = this.next ?: return this
    return next.findTail()
}

/**
 * Summarize remainings of all elements of the chain
 */
internal fun BufferView.remainingAll(): Long = remainingAll(0L)

private tailrec fun BufferView.remainingAll(n: Long): Long {
    val rem = readRemaining.toLong() + n
    val next = this.next ?: return rem

    return next.remainingAll(rem)
}

internal tailrec fun BufferView.isEmpty(): Boolean {
    if (readRemaining > 0) return false
    val next = this.next ?: return true
    return next.isEmpty()
}

class BufferLimitExceededException(message: String) : Exception(message)