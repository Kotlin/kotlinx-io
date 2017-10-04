package kotlinx.io.core

import kotlinx.io.pool.*

expect enum class ByteOrder {
    BIG_ENDIAN, LITTLE_ENDIAN;

    companion object {
        fun nativeOrder(): ByteOrder
    }
}

expect class BufferView {
    private val origin: BufferView?

    var next: BufferView?
    var attachment: Any?
    val startGap: Int
    val endGap: Int

    fun canRead(): Boolean
    fun canWrite(): Boolean

    val readRemaining: Int
    val writeRemaining: Int

    fun reserveStartGap(n: Int)
    fun reserveEndGap(n: Int)

    var byteOrder: ByteOrder

    fun readByte(): Byte
    fun readShort(): Short
    fun readInt(): Int
    fun readLong(): Long
    fun readFloat(): Float
    fun readDouble(): Double
    fun read(dst: ByteArray, offset: Int, length: Int)

    fun discardExact(n: Int)

    fun writeByte(v: Byte)
    fun writeShort(v: Short)
    fun writeInt(v: Int)
    fun writeLong(v: Long)
    fun writeFloat(v: Float)
    fun writeDouble(v: Double)
    fun write(array: ByteArray, offset: Int, length: Int)

    fun writeBuffer(other: BufferView, length: Int): Int
    internal fun writeBufferPrepend(other: BufferView)
    internal fun writeBufferAppend(other: BufferView, maxSize: Int)

    fun pushBack(n: Int)
    fun resetForWrite()
    fun resetForRead()
    fun isExclusivelyOwned(): Boolean

    fun makeView(): BufferView
    fun release(pool: ObjectPool<BufferView>)

    companion object {
        val Empty: BufferView
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

internal tailrec fun BufferView.tail(): BufferView {
    val next = this.next ?: return this
    return next.tail()
}

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