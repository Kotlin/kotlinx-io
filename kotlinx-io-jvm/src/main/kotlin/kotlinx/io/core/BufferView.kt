package kotlinx.io.core

import kotlinx.io.pool.*
import kotlinx.io.utils.*
import java.nio.*
import java.nio.charset.*
import java.util.concurrent.atomic.*

/**
 * A read-write facade to actual buffer of fixed size. Multiple views could share the same actual buffer.
 */
actual class BufferView private constructor(
        private var content: ByteBuffer,
        internal actual val origin: BufferView?) {

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

    /**
     * Mutable reference to next buffer view. Useful to chain multiple views
     */
    actual var next: BufferView? = null

    /**
     * User data: could be a session, connection or anything useful
     */
    actual var attachment: Any? = null

    /**
     * Amount of reserved bytes at the beginning
     */
    actual val startGap: Int get() = readPosition

    /**
     * Amount of reserved bytes at the end
     */
    actual val endGap: Int get() = writeBuffer.capacity() - writeBuffer.limit()

    /**
     * @return `true` if there are available bytes to be read
     */
    actual fun canRead(): Boolean = readBuffer.hasRemaining()

    /**
     * @return `true` if there is free room to for write
     */
    actual fun canWrite(): Boolean = writeBuffer.hasRemaining()

    /**
     * Backing buffer capacity. Value for released buffer is unspecified
     */
    actual val capacity: Int get() = writeBuffer.capacity()

    /**
     * Number of bytes available for read
     */
    actual val readRemaining: Int get() {
        if (refCount == 0L) throw IllegalStateException("Using released object")

        return readBuffer.remaining()
    }

    /**
     * Number of free bytes useful for writing. Doesn't include gaps.
     */
    actual val writeRemaining: Int get() = writeBuffer.remaining()

    /**
     * read and write operations byte-order (endianness)
     */
    actual var byteOrder: ByteOrder
        get() = ByteOrder.of(readBuffer.order())
        set(value) {
            readBuffer.order(value.nioOrder)
            writeBuffer.order(value.nioOrder)
        }

    /**
     * Reserves [n] bytes at the beginning. Could be invoked only once and only before writing.
     */
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

    /**
     * Reserves [n] bytes at the end of buffer. Could be invoked only once and only if there are at least [n] bytes free
     */
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

    /**
     * Writes exactly [length] bytes of [array] starting from [offset] position or fails if not enough free space
     */
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

    /**
     * Writes [length] bytes of [src] buffer or fails if not enough free space available
     */
    actual fun writeBuffer(src: BufferView, length: Int): Int {
        val otherSize = src.readBuffer.remaining()
        return when {
            otherSize <= length -> {
                writeBuffer.put(src.readBuffer)
                afterWrite()
                otherSize
            }
            length > writeBuffer.remaining() -> throw BufferOverflowException()
            else -> {
                val l = src.readBuffer.limit()
                src.readBuffer.limit(src.readBuffer.position() + length)
                writeBuffer.put(src.readBuffer)
                src.readBuffer.limit(l)
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

    /**
     * Push back [n] bytes: only possible if there were at least [n] bytes read before this operation.
     */
    actual fun pushBack(n: Int) {
        readBuffer.position(readBuffer.position() - n)
    }

    /**
     * Marks the whole buffer available for write and no bytes for read.
     */
    actual fun resetForWrite() {
        resetForWrite(writeBuffer.capacity())
    }

    actual fun resetForWrite(limit: Int) {
        require(limit <= writeBuffer.capacity())
        writeBuffer.limit(limit)
        readPosition = 0
        writePosition = 0
    }

    /**
     * Marks the whole buffer available for read and no for write
     */
    actual fun resetForRead() {
        readPosition = 0
        writePosition = writeBuffer.limit()
    }

    /**
     * @return `true` if and only if the are no buffer views that share the same actual buffer. This actually does
     * refcount and only work guaranteed if other views created/not created via [makeView] function.
     * One can instantiate multiple buffers with the same buffer and this function will return `true` in spite of
     * the fact that the buffer is actually shared.
     */
    actual fun isExclusivelyOwned(): Boolean = refCount == 1L

    /**
     * Creates a new view to the same actual buffer with independant read and write positions and gaps
     */
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

    /**
     * releases buffer view and returns it to the [pool] if there are no more usages. Based on simple ref-counting so
     * it is very fragile.
     */
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

    fun readText(decoder: CharsetDecoder, out: Appendable, lastBuffer: Boolean, max: Int = Int.MAX_VALUE): Int {
        if (out is CharBuffer) {
            return readTextDirectlyToOut(decoder, out, lastBuffer, max)
        }

        return readTextImpl(decoder, out, lastBuffer, max)
    }

    private fun readTextImpl(decoder: CharsetDecoder, out: Appendable, lastBuffer: Boolean, max: Int = Int.MAX_VALUE): Int {
        if (max == 0 || !readBuffer.hasRemaining()) return 0

        val buffer = Pool.borrow()
        val cb = buffer.content.asCharBuffer()
        var decoded = 0
        var maxRemaining = max

        while (decoded < max) {
            cb.clear()
            if (maxRemaining < cb.remaining()) {
                cb.limit(maxRemaining)
            }

            val cr = decoder.decode(readBuffer, cb, lastBuffer)
            if (cr.isError) {
                buffer.release(Pool)
                cr.throwException()
            }

            cb.flip()
            val decodedPart = cb.remaining()
            out.append(cb)
            decoded += decodedPart
            maxRemaining -= decodedPart

            if (decodedPart == 0 && cr.isUnderflow) {
                break
            }
        }

        buffer.release(Pool)

        return decoded
    }

    private fun readTextDirectlyToOut(decoder: CharsetDecoder, out: CharBuffer, lastBuffer: Boolean, max: Int = Int.MAX_VALUE): Int {
        if (!readBuffer.hasRemaining()) return 0
        var decoded = 0

        val outLimit = out.limit()
        if (max < out.remaining()) {
            out.limit(out.position() + max)
        }

        while (true) {
            val before = out.position()
            val cr = decoder.decode(readBuffer, out, lastBuffer)
            if (cr.isError) {
                cr.throwException()
            }

            val decodedPart = out.position() - before
            decoded += decodedPart

            if (cr.isOverflow) break
            else if (cr.isUnderflow && !readBuffer.hasRemaining()) break
        }

        out.limit(outLimit)

        return decoded
    }

    private fun unlink(): ByteBuffer? {
        if (refCount != 0L) throw IllegalStateException("Unable to unlink buffer view: refCount is $refCount != 0")

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

        private val DEFAULT_BUFFER_SIZE = getIOIntProperty("buffer.size", 4096)
        private val DEFAULT_BUFFER_POOL_SIZE = getIOIntProperty("buffer.pool.size", 100)

        actual val Empty = BufferView(EmptyBuffer, null)
        actual val Pool: ObjectPool<BufferView> = object : DefaultPool<BufferView>(DEFAULT_BUFFER_POOL_SIZE) {
            override fun produceInstance(): BufferView {
                return BufferView(ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE), null)
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

        actual val NoPool: ObjectPool<BufferView> = object : NoPoolImpl<BufferView>() {
            override fun borrow(): BufferView {
                return BufferView(ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE), null)
            }
        }
    }
}
