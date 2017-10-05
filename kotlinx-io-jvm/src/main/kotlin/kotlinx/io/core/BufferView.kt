package kotlinx.io.core

import kotlinx.io.pool.*
import java.nio.*
import java.util.concurrent.atomic.*

actual class BufferView private constructor(
        private var content: ByteBuffer,
        private actual val origin: BufferView?) {

    private var readBuffer: ByteBuffer = if (content === EmptyBuffer) EmptyBuffer else content.slice()
    private var writeBuffer: ByteBuffer = if (content === EmptyBuffer) EmptyBuffer else content.slice()

    @Volatile
    private var refCount = 1L

    private inline var readPosition: Int
        get() = readBuffer.position()
        set(value) {
            readBuffer.position(value)
        }

    private inline var writePosition: Int
        get() = writeBuffer.position()
        set(value) {
            writeBuffer.position(value)
            readBuffer.limit(value)
        }

    init {
        readBuffer.limit(0)
    }

    actual var next: BufferView? = null
    actual var attachment: Any? = null

    actual val startGap: Int get() = readPosition
    actual val endGap: Int get() = writeBuffer.limit() - writeBuffer.capacity()

    actual fun canRead(): Boolean = readBuffer.hasRemaining()
    actual fun canWrite(): Boolean = writeBuffer.hasRemaining()

    actual val readRemaining: Int get() {
        if (refCount == 0L) throw IllegalStateException("Using released object")

        return readBuffer.remaining()
    }
    actual val writeRemaining: Int get() = writeBuffer.remaining()

    actual var byteOrder: ByteOrder
        get() = ByteOrder.of(readBuffer.order())
        set(value) {
            readBuffer.order(value.nioOrder)
            writeBuffer.order(value.nioOrder)
        }

    actual fun reserveStartGap(n: Int) {
        require(n >= 0)
        require(n <= writeBuffer.capacity())

        val rp = readPosition
        if (rp != 0) throw IllegalStateException("Can't reserve $n bytes gap: there is already a reserved gap ($rp bytes)")
        val wp = writePosition
        if (wp != 0 || rp != wp) throw IllegalStateException("Can't reserve $n bytes gap: there are already bytes written at the beginning")

        writePosition = wp + n
        readPosition = rp + n
    }

    actual fun reserveEndGap(n: Int) {
        require(n >= 0)
        val writeBufferLimit = writeBuffer.limit()

        if (writeBufferLimit != writeBuffer.capacity()) throw IllegalStateException("Can't reserve $n bytes gap: there is already a reserved gap (${writeBuffer.capacity() - writeBufferLimit} bytes)")
        val newLimit = writeBufferLimit - n

        if (newLimit < writePosition) throw IllegalStateException("Can't reserve $n bytes gap: there are already bytes written at the end - not enough space to reserve")

        writeBuffer.limit(newLimit)
    }

    actual fun writeByte(v: Byte) {
        writeBuffer.put(v)
        afterWrite()
    }

    actual fun writeShort(v: Short) {
        writeBuffer.putShort(v)
        afterWrite()
    }

    actual fun writeInt(v: Int) {
        writeBuffer.putInt(v)
        afterWrite()
    }

    actual fun writeLong(v: Long) {
        writeBuffer.putLong(v)
        afterWrite()
    }

    actual fun writeFloat(v: Float) {
        writeBuffer.putFloat(v)
        afterWrite()
    }

    actual fun writeDouble(v: Double) {
        writeBuffer.putDouble(v)
        afterWrite()
    }

    actual fun write(array: ByteArray, offset: Int, length: Int) {
        writeBuffer.put(array, offset, length)
        afterWrite()
    }

    fun write(buffer: ByteBuffer) {
        writeBuffer.put(buffer)
        afterWrite()
    }

    internal inline fun readDirect(block: (ByteBuffer) -> Unit) {
        val bb = readBuffer
        val positionBefore = bb.position()
        val limit = bb.limit()
        block(bb)
        val delta = bb.position() - positionBefore
        if (delta < 0) throw IllegalStateException("Wrong buffer position change: negative shift $delta")
        if (bb.limit() != limit) throw IllegalStateException("Limit change is now allowed")
    }

    internal inline fun writeDirect(size: Int, block: (ByteBuffer) -> Unit): Int {
        val rem = writeRemaining
        require (size <= rem) { "size $size is greater than buffer's remaining capacity $rem" }
        val buffer = writeBuffer
        val positionBefore = buffer.position()
        block(buffer)
        val delta = buffer.position() - positionBefore
        if (delta < 0 || delta > rem) throw IllegalStateException("Wrong buffer position change: $delta (position should be moved forward only by at most size bytes (size =  $size)")

        readBuffer.limit(buffer.position())
        return delta
    }

    actual fun writeBuffer(other: BufferView, length: Int): Int {
        val otherSize = other.readBuffer.remaining()
        return when {
            otherSize <= length -> {
                writeBuffer.put(other.readBuffer)
                afterWrite()
                otherSize
            }
            length > writeBuffer.remaining() -> throw BufferOverflowException()
            else -> {
                val l = other.readBuffer.limit()
                other.readBuffer.limit(other.readBuffer.position() + length)
                writeBuffer.put(other.readBuffer)
                other.readBuffer.limit(l)
                afterWrite()
                length
            }
        }
    }

    internal actual fun writeBufferPrepend(other: BufferView) {
        val size = other.readRemaining
        val rp = readPosition

        if (size > rp) throw IllegalArgumentException("Can't prepend buffer: not enough free space in the beginning")

        val to = writeBuffer
        val pos = writePosition

        to.limit(rp)
        to.position(rp - size)
        to.put(other.readBuffer)

        readPosition = rp - size
        writePosition = pos
    }

    internal actual fun writeBufferAppend(other: BufferView, maxSize: Int) {
        val rem = writeBuffer.remaining()
        val size = minOf(maxSize, other.readRemaining)

        if (rem < size) {
            val requiredGap = size - rem
            val gap = endGap

            if (requiredGap > gap) throw IllegalArgumentException("Can't append buffer: not enough free space in the end")
            writeBuffer.limit(writeBuffer.limit() + requiredGap)
        }

        writeBuffer(other, size)
    }

    actual fun readByte() = readBuffer.get()
    actual fun readShort() = readBuffer.getShort()
    actual fun readInt() = readBuffer.getInt()
    actual fun readLong() = readBuffer.getLong()
    actual fun readFloat() = readBuffer.getFloat()
    actual fun readDouble() = readBuffer.getDouble()

    actual fun read(dst: ByteArray, offset: Int, length: Int) {
        readBuffer.get(dst, offset, length)
    }

    fun read(dst: ByteBuffer, length: Int) {
        val bb = readBuffer
        if (length == bb.remaining()) {
            dst.put(bb)
        } else {
            val l = bb.limit()
            bb.limit(bb.position() + length)
            dst.put(bb)
            bb.limit(l)
        }
    }

    actual fun discardExact(n: Int) {
        readBuffer.position(readBuffer.position() + n)
    }

    actual fun pushBack(n: Int) {
        readBuffer.position(readBuffer.position() - n)
    }

    actual fun resetForWrite() {
        writeBuffer.limit(writeBuffer.capacity())
        readPosition = 0
        writePosition = 0
    }

    actual fun resetForRead() {
        readPosition = 0
        writePosition = writeBuffer.limit()
    }

    actual fun isExclusivelyOwned(): Boolean = refCount == 1L

    actual fun makeView(): BufferView {
        val newOrigin = origin ?: this
        newOrigin.acquire()

        val view = BufferView(content, newOrigin)
        view.attachment = attachment
        view.writePosition = writePosition
        view.writeBuffer.limit(writeBuffer.limit())
        view.readPosition = readPosition

        return view
    }

    actual fun release(pool: ObjectPool<BufferView>) {
        if (releaseRefCount()) {
            resetForWrite()

            if (origin != null) {
                unlink()
                origin.release(pool)
            } else {
                pool.recycle(this)
            }
        }
    }

    private fun unlink(): ByteBuffer? {
        val empty = EmptyBuffer
        val buffer = content

        if (buffer === empty) return null

        content = empty
        readBuffer = empty
        writeBuffer = empty

        return buffer
    }

    private fun releaseRefCount(): Boolean {
        if (this === Empty) throw IllegalArgumentException("Attempted to release empty")
        while (true) {
            val value = refCount
            val newValue = value - 1

            if (value == 0L) throw IllegalStateException("Unable to release: already released")
            if (RefCount.compareAndSet(this, value, newValue)) {
                return newValue == 0L
            }
        }
    }

    private fun acquire() {
        while (true) {
            val value = refCount
            if (value == 0L) throw IllegalStateException("Unable to acquire: already released")
            if (RefCount.compareAndSet(this, value, value + 1)) break
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun afterWrite() {
        readBuffer.limit(writePosition)
    }

    actual companion object {
        val EmptyBuffer: ByteBuffer = ByteBuffer.allocateDirect(0)
        private val RefCount = AtomicLongFieldUpdater.newUpdater(BufferView::class.java, BufferView::refCount.name)!!

        actual val Empty = BufferView(EmptyBuffer, null)
        actual val Pool: ObjectPool<BufferView> = object : DefaultPool<BufferView>(100) {
            override fun produceInstance(): BufferView {
                return BufferView(ByteBuffer.allocateDirect(4096), null)
            }

            override fun disposeInstance(instance: BufferView) {
                instance.unlink()
            }

            override fun clearInstance(instance: BufferView): BufferView {
                return instance.apply {
                    next = null
                    attachment = null
                    resetForWrite()
                    if (!RefCount.compareAndSet(this, 0L, 1L)) {
                        throw IllegalStateException("Unable to prepare buffer: refCount is not zero (used while parked in the pool?)")
                    }
                }
            }

            override fun validateInstance(instance: BufferView) {
                require(instance.refCount == 0L) { "Buffer is not yet released but tried to recycle" }
                require(instance.origin == null) { "Unable to recycle buffer view, only origin buffers are applicable" }
            }
        }
    }
}
