@file:Suppress("RedundantModalityModifier")

package kotlinx.io.core

import kotlinx.io.core.internal.*
import kotlinx.io.internal.jvm.*
import kotlinx.io.pool.*
import kotlinx.io.utils.*
import java.nio.*
import java.nio.charset.*
import java.util.concurrent.atomic.*
import kotlin.jvm.*

/**
 * A read-write facade to actual buffer of fixed size. Multiple views could share the same actual buffer.
 */
actual class IoBuffer private constructor(
        private var content: ByteBuffer,
        internal actual val origin: IoBuffer?) : Input, Output {

    constructor(external: ByteBuffer) : this(external, null)

    @PublishedApi
    @JvmField
    internal var readBuffer: ByteBuffer = if (content === EmptyBuffer) EmptyBuffer else content.slice()

    @PublishedApi
    @JvmField
    internal var writeBuffer: ByteBuffer = if (content === EmptyBuffer) EmptyBuffer else content.slice()

    @Volatile
    private var refCount = 1L

    private inline var readPosition: Int
        get() = readBuffer.position()
        set(value) {
            readBuffer.position(value)
        }

    @PublishedApi
    internal inline var writePosition: Int
        get() = writeBuffer.position()
        set(value) {
            writeBuffer.position(value)
            readBuffer.limit(value)
        }

    init {
        require(origin !== this) { "origin shouldn't point to itself" }
        readBuffer.limit(0)
    }

    /**
     * Mutable reference to next buffer view. Useful to chain multiple views
     */
    actual var next: IoBuffer? = null

    /**
     * User data: could be a session, connection or anything useful
     */
    @ExperimentalIoApi
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

    override val endOfInput: Boolean
        get() = !canRead()

    /**
     * Number of bytes available for read
     */
    actual val readRemaining: Int get() {
        return readBuffer.remaining()
    }

    /**
     * Number of free bytes useful for writing. Doesn't include gaps.
     */
    actual val writeRemaining: Int get() = writeBuffer.remaining()

    /**
     * read and write operations byte-order (endianness)
     */
    actual final override var byteOrder: ByteOrder
        get() = ByteOrder.of(readBuffer.order())
        set(value) {
            readBuffer.order(value.nioOrder)
            writeBuffer.order(value.nioOrder)
        }

    /**
     * Reserves [n] bytes at the beginning. Could be invoked only once and only before writing.
     */
    actual fun reserveStartGap(n: Int) {
        require(n >= 0) { "n shouldn't be negative: $n" }
        require(n <= writeBuffer.capacity()) { "Not enough space to reserve $n bytes" }

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
        require(n >= 0) { "n shouldn't be negative: $n" }
        val writeBufferLimit = writeBuffer.limit()

        if (writeBufferLimit != writeBuffer.capacity()) throw IllegalStateException("Can't reserve $n bytes gap: there is already a reserved gap (${writeBuffer.capacity() - writeBufferLimit} bytes)")
        val newLimit = writeBufferLimit - n

        if (newLimit < writePosition) throw IllegalStateException("Can't reserve $n bytes gap: there are already bytes written at the end - not enough space to reserve")

        writeBuffer.limit(newLimit)
    }

    actual final override fun writeByte(v: Byte) {
        writeBuffer.put(v)
        afterWrite()
    }

    actual final override  fun writeShort(v: Short) {
        writeBuffer.putShort(v)
        afterWrite()
    }

    actual final override  fun writeInt(v: Int) {
        writeBuffer.putInt(v)
        afterWrite()
    }

    actual final override  fun writeLong(v: Long) {
        writeBuffer.putLong(v)
        afterWrite()
    }

    actual final override  fun writeFloat(v: Float) {
        writeBuffer.putFloat(v)
        afterWrite()
    }

    actual final override  fun writeDouble(v: Double) {
        writeBuffer.putDouble(v)
        afterWrite()
    }

    actual final override fun writeFully(src: ByteArray, offset: Int, length: Int) {
        writeBuffer.put(src, offset, length)
        afterWrite()
    }

    actual final override fun writeFully(src: ShortArray, offset: Int, length: Int) {
        val bb = writeBuffer
        for (i in offset until offset + length) {
            bb.putShort(src[i])
        }
        afterWrite()
    }

    actual final override fun writeFully(src: IntArray, offset: Int, length: Int) {
        val bb = writeBuffer
        for (i in offset until offset + length) {
            bb.putInt(src[i])
        }
        afterWrite()
    }

    actual final override fun writeFully(src: LongArray, offset: Int, length: Int) {
        val bb = writeBuffer
        for (i in offset until offset + length) {
            bb.putLong(src[i])
        }
        afterWrite()
    }

    actual final override fun writeFully(src: FloatArray, offset: Int, length: Int) {
        val bb = writeBuffer
        for (i in offset until offset + length) {
            bb.putFloat(src[i])
        }
        afterWrite()
    }

    actual final override fun writeFully(src: DoubleArray, offset: Int, length: Int) {
        val bb = writeBuffer
        for (i in offset until offset + length) {
            bb.putDouble(src[i])
        }
        afterWrite()
    }

    actual final override fun writeFully(src: IoBuffer, length: Int) {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(length <= src.readRemaining) { "length is bigger than src buffer size: $length > ${src.readRemaining}" }
        require(length <= writeRemaining) { "Not enough space to write $length bytes" }

        if (length == src.readRemaining) {
            writeBuffer.put(src.readBuffer)
        } else {
            val bb = src.readBuffer
            val limit = bb.limit()
            bb.limit(bb.position() + length)
            writeBuffer.put(bb)
            bb.limit(limit)
        }
        afterWrite()
    }

    override fun writeFully(bb: ByteBuffer) {
        require(bb.remaining() <= writeRemaining) { "Not enough space to write ${bb.remaining()} bytes" }

        writeBuffer.put(bb)
        afterWrite()
    }

    actual final override fun append(csq: CharSequence?, start: Int, end: Int): Appendable {
        val idx = appendChars(csq ?: "null", start, end)
        if (idx != end) throw IllegalStateException("Not enough free space to append char sequence")
        return this
    }

    actual final override fun append(csq: CharSequence?): Appendable {
        return if (csq == null) append("null") else append(csq, 0, csq.length)
    }

    actual final override fun append(csq: CharArray, start: Int, end: Int): Appendable {
        val idx = appendChars(csq, start, end)

        if (idx != end) throw IllegalStateException("Not enough free space to append char sequence")
        return this
    }

    actual override fun append(c: Char): Appendable {
        val s = writeBuffer.putUtf8Char(c.toInt())
        if (s == 0) notEnoughFreeSpace(c)
        afterWrite()
        return this
    }

    private fun notEnoughFreeSpace(c: Char): Nothing {
        throw IllegalStateException("Not Enough free space to append character '$c', remaining $writeRemaining bytes")
    }
    actual fun appendChars(csq: CharArray, start: Int, end: Int): Int {
        val buffer = writeBuffer
        var idx = start

        while (idx < end) {
            val ch = csq[idx++]
            val size = if (ch.isSurrogate()) buffer.putUtf8CharSurrogate(ch.toInt(), csq[idx++].toInt())
            else buffer.putUtf8Char(ch.toInt())

            if (size == 0) {
                return appendCharFailed(ch, idx)
            }
        }

        afterWrite()
        return end
    }

    actual fun appendChars(csq: CharSequence, start: Int, end: Int): Int {
        val buffer = writeBuffer
        if (!buffer.hasRemaining()) return start
        val idx = if (buffer.hasArray()) {
            appendASCII_array(buffer, csq, start, end)
        } else {
            appendASCII_buffer(buffer, csq, start, end)
        }

        if (!buffer.hasRemaining() || idx == end) {
            afterWrite()
            return idx
        }

        return appendUTF8(buffer, csq, idx, end)
    }

    private fun appendUTF8(writeBuffer: ByteBuffer, csq: CharSequence, start: Int, end: Int): Int {
        var idx = start
        while (idx < end) {
            val ch = csq[idx++]
            val size = if (ch.isSurrogate()) {
                writeBuffer.putUtf8CharSurrogate(ch.toInt(), csq[idx++].toInt())
            } else {
                writeBuffer.putUtf8Char(ch.toInt())
            }

            if (size == 0) {
                return appendCharFailed(ch, idx)
            }
        }

        afterWrite()
        return end
    }

    private fun appendCharFailed(ch: Char, idx: Int): Int {
        afterWrite()
        return if (ch.isSurrogate()) idx - 2 else idx - 1
    }

    private fun appendASCII_buffer(writeBuffer: ByteBuffer, csq: CharSequence, start: Int, end: Int): Int {
        val limitedEnd = minOf(end, start + writeBuffer.remaining())
        var rc = limitedEnd

        for (idx in start until limitedEnd) {
            val ch = csq[idx].toInt()
            if (ch > 0x7f) {
                rc = idx
                break
            }
            writeBuffer.put(ch.toByte())
        }

        return rc
    }

    private fun appendASCII_array(writeBuffer: ByteBuffer, csq: CharSequence, start: Int, end: Int): Int {
        val array = writeBuffer.array()!!
        var offset = writeBuffer.arrayOffset() + writeBuffer.position()
        val limitedEnd = minOf(end, start + writeBuffer.remaining())
        var rc = limitedEnd

        for (idx in start until limitedEnd) {
            val ch = csq[idx].toInt()
            if (ch > 0x7f || offset >= array.size) {
                rc = idx
                break
            }
            array[offset++] = ch.toByte()
        }

        writeBuffer.position(offset - writeBuffer.arrayOffset())
        return rc
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun ByteBuffer.putUtf8Char(v: Int): Int {
        return when {
            v in 1..0x7f -> {
                if (remaining() < 1) return 0
                put(v.toByte())
                1
            }
            v > 0xffff -> {
                if (remaining() < 4) return 0
                apply {
                    put((0xf0 or ((v shr 18) and 0x3f)).toByte())
                    put((0x80 or ((v shr 12) and 0x3f)).toByte())
                    put((0x80 or ((v shr  6) and 0x3f)).toByte())
                    put((0x80 or ( v         and 0x3f)).toByte())
                }
                4
            }
            v > 0x7ff -> {
                if (remaining() < 3) return 0
                apply {
                    put((0xe0 or ((v shr 12) and 0x0f)).toByte())
                    put((0x80 or ((v shr  6) and 0x3f)).toByte())
                    put((0x80 or ( v         and 0x3f)).toByte())
                }
                3
            }
            else -> {
                if (remaining() < 2) return 0
                apply {
                    put((0xc0 or ((v shr  6) and 0x1f)).toByte())
                    put((0x80 or ( v         and 0x3f)).toByte())
                }
                2
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private fun ByteBuffer.putUtf8CharSurrogate(high: Int, low: Int): Int {
        val highValue = (high and 0x7ff) shl 10
        val lowValue = (low and 0x3ff)
        val value = 0x010000 or (highValue or lowValue)

        return putUtf8Char(value)
    }

    actual final override fun fill(n: Long, v: Byte) {
        require(n <= writeRemaining) { "Not enough space to write $n bytes" }

        val bb = writeBuffer
        repeat(n.toInt()) {
            bb.put(v)
        }
        afterWrite()
    }

    /**
     * Writes exactly [length] bytes of [array] starting from [offset] position or fails if not enough free space
     */
    @Deprecated("Use writeFully instead", ReplaceWith("writeFully(array, offset, length)"))
    actual fun write(array: ByteArray, offset: Int, length: Int) {
        writeFully(array, offset, length)
    }

    @Deprecated("Use writeFully instead", ReplaceWith("writeFully(buffer)"))
    fun write(buffer: ByteBuffer) {
        writeFully(buffer)
    }

    inline fun readDirect(block: (ByteBuffer) -> Unit) {
        val bb = readBuffer
        val positionBefore = bb.position()
        val limit = bb.limit()
        block(bb)
        val delta = bb.position() - positionBefore
        if (delta < 0) negativeShiftError(delta)
        if (bb.limit() != limit) limitChangeError()
    }

    inline fun writeDirect(size: Int, block: (ByteBuffer) -> Unit): Int {
        val rem = writeRemaining
        require (size <= rem) { "size $size is greater than buffer's remaining capacity $rem" }
        val buffer = writeBuffer
        val positionBefore = buffer.position()
        block(buffer)
        val delta = buffer.position() - positionBefore
        if (delta < 0 || delta > rem) wrongBufferPositionChangeError(delta, size)

        afterWrite()
        return delta
    }

    /**
     * Writes [length] bytes of [src] buffer or fails if not enough free space available
     */
    @Deprecated("Use writeFully instead", ReplaceWith("writeFully(src, length)"))
    actual fun writeBuffer(src: IoBuffer, length: Int): Int {
        writeFully(src, length)
        return length
    }

    internal actual fun restoreStartGap(n: Int) {
        val rp = readPosition
        if (rp < n) {
            throw IllegalArgumentException("Can't restore start gap: $n bytes were not reserved before")
        }

        readPosition = rp - n
    }

    internal actual fun restoreEndGap(n: Int) {
        val limit = writeBuffer.limit()
        val newLimit = limit - n
        writeBuffer.limit(newLimit) // position will be bumped as well

        if (readBuffer.limit() > newLimit) {
            readBuffer.limit(newLimit) // position will be bumped as well
        }
    }

    internal actual fun writeBufferPrepend(other: IoBuffer) {
        val size = other.readRemaining
        val rp = readPosition

        if (size > rp) {
            throw IllegalArgumentException("Can't prepend buffer: not enough free space at the beginning")
        }

        val to = writeBuffer
        val pos = writePosition

        val limitBefore = to.limit()
        to.limit(rp)
        to.position(rp - size)
        to.put(other.readBuffer)
        to.limit(limitBefore)

        readPosition = rp - size
        writePosition = pos
    }

    internal actual fun writeBufferAppend(other: IoBuffer, maxSize: Int) {
        val rem = writeBuffer.remaining()
        val size = minOf(maxSize, other.readRemaining)

        if (rem < size) {
            val requiredGap = size - rem
            val gap = endGap

            if (requiredGap > gap) {
                throw IllegalArgumentException("Can't append buffer: not enough free space at the end")
            }
            writeBuffer.limit(writeBuffer.limit() + requiredGap)
        }

        writeFully(other, size)
    }

    actual final override fun readByte() = readBuffer.get()
    actual final override fun readShort() = readBuffer.getShort()
    actual final override fun readInt() = readBuffer.getInt()
    actual final override fun readLong() = readBuffer.getLong()
    actual final override fun readFloat() = readBuffer.getFloat()
    actual final override fun readDouble() = readBuffer.getDouble()

    @Deprecated("Use readFully instead", ReplaceWith("readFully(dst, offset, length)"))
    actual fun read(dst: ByteArray, offset: Int, length: Int) {
        readFully(dst, offset, length)
    }

    actual final override fun readFully(dst: ByteArray, offset: Int, length: Int) {
        readBuffer.get(dst, offset, length)
    }

    actual final override fun readAvailable(dst: ByteArray, offset: Int, length: Int): Int {
        val size = minOf(length, readBuffer.remaining())
        if (size == -1 && readBuffer.remaining() == 0) return -1

        readBuffer.get(dst, offset, size)
        return size
    }

    actual final override fun readFully(dst: IoBuffer, length: Int) {
        val readRemaining = readRemaining
        require(length <= dst.writeRemaining) { "Not enough space in the destination buffer to write $length bytes" }
        require(length <= readRemaining) { "Not enough bytes available to read $length bytes" }

        readFully(dst.writeBuffer, length)
        dst.afterWrite()
    }

    actual final override fun readAvailable(dst: IoBuffer, length: Int): Int {
        val readRemaining = readRemaining
        val size = minOf(length, readRemaining)
        if (readRemaining == 0) return -1

        readFully(dst.writeBuffer, size)
        dst.afterWrite()
        return size
    }

    actual final override fun readFully(dst: ShortArray, offset: Int, length: Int) {
        readBuffer.asShortBuffer().get(dst, offset, length)
        readPosition += length shl 1
    }

    actual final override fun readFully(dst: IntArray, offset: Int, length: Int) {
        readBuffer.asIntBuffer().get(dst, offset, length)
        readPosition += length shl 2
    }

    actual final override fun readFully(dst: LongArray, offset: Int, length: Int) {
        readBuffer.asLongBuffer().get(dst, offset, length)
        readPosition += length shl 3
    }

    actual final override fun readFully(dst: DoubleArray, offset: Int, length: Int) {
        readBuffer.asDoubleBuffer().get(dst, offset, length)
        readPosition += length shl 3
    }

    actual final override fun readFully(dst: FloatArray, offset: Int, length: Int) {
        readBuffer.asFloatBuffer().get(dst, offset, length)
        readPosition += length shl 2
    }

    actual final override fun readAvailable(dst: ShortArray, offset: Int, length: Int): Int {
        val readRemaining = readRemaining
        if (readRemaining == 0) return -1
        val size = minOf(readRemaining shr 1, length)
        readFully(dst, offset, size)
        return size
    }

    actual final override fun readAvailable(dst: IntArray, offset: Int, length: Int): Int {
        val readRemaining = readRemaining
        if (readRemaining == 0) return -1
        val size = minOf(readRemaining shr 2, length)
        readFully(dst, offset, size)
        return size
    }

    actual final override fun readAvailable(dst: LongArray, offset: Int, length: Int): Int {
        val readRemaining = readRemaining
        if (readRemaining == 0) return -1
        val size = minOf(readRemaining shr 3, length)
        readFully(dst, offset, size)
        return size
    }

    actual final override fun readAvailable(dst: FloatArray, offset: Int, length: Int): Int {
        val readRemaining = readRemaining
        if (readRemaining == 0) return -1
        val size = minOf(readRemaining shr 2, length)
        readFully(dst, offset, size)
        return size
    }

    actual final override fun readAvailable(dst: DoubleArray, offset: Int, length: Int): Int {
        val readRemaining = readRemaining
        if (readRemaining == 0) return -1
        val size = minOf(readRemaining shr 3, length)
        readFully(dst, offset, size)
        return size
    }

    @Deprecated("Use readFully instead", ReplaceWith("readFully(dst, length)"))
    fun read(dst: ByteBuffer, length: Int) {
        return readFully(dst, length)
    }

    final override fun readAvailable(dst: ByteBuffer, length: Int): Int {
        val remaining = readRemaining
        val size = minOf(remaining, length)
        if (remaining == 0) return -1

        readFully(dst, size)
        return size
    }

    final override fun readFully(dst: ByteBuffer, length: Int) {
        val bb = readBuffer
        val rem = bb.remaining()

        if (length == rem) {
            dst.put(bb)
        } else if (length > rem) {
            throw BufferUnderflowException()
        } else {
            val l = bb.limit()
            bb.limit(bb.position() + length)
            dst.put(bb)
            bb.limit(l)
        }
    }

    /*
     * Returns next byte (unsigned) or `-1` if no more bytes available
     */
    actual final override fun tryPeek(): Int {
        val bb = readBuffer
        return if (bb.hasRemaining()) bb.get(bb.position()).toInt() and 0xff else -1
    }

    actual final override fun peekTo(buffer: IoBuffer): Int {
        val readRemaining = readRemaining
        if (readRemaining == 0) return -1

        val size = minOf(readRemaining, buffer.writeRemaining)
        buffer.writeFully(this, size)

        return size
    }

    actual final override fun discard(n: Long): Long {
        require(n >= 0L) { "Negative discard quantity $n" }
        val size = minOf(readRemaining.toLong(), n).toInt()
        readPosition += size
        return size.toLong()
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
        // limit is checked inside of ByteBuffer.limit(I)
        writeBuffer.limit(limit)
        readPosition = 0
        writePosition = 0
    }

    /**
     * Reset read/write position to original's content pos/limit. May not work due to slicing.
     */
    @Deprecated("Unstable API. Could be changed or removed without notice.")
    fun resetFromContentToWrite(child: ByteBuffer) {
        writeBuffer.limit(child.limit())
        writeBuffer.position(child.position())
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
    @ExperimentalIoApi
    actual fun isExclusivelyOwned(): Boolean = refCount == 1L

    /**
     * Creates a new view to the same actual buffer with independant read and write positions and gaps
     */
    actual fun makeView(): IoBuffer {
        if (this === Empty) return this

        val newOrigin = origin ?: this
        newOrigin.acquire()

        val view = IoBuffer(content, newOrigin)
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
    actual fun release(pool: ObjectPool<IoBuffer>) {
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

    actual override fun close() {
        throw UnsupportedOperationException("close for buffer view is not supported")
    }

    fun readText(decoder: CharsetDecoder, out: Appendable, lastBuffer: Boolean, max: Int = Int.MAX_VALUE): Int {
        if (out is CharBuffer) {
            return readTextDirectlyToOut(decoder, out, lastBuffer, max)
        }

        return readTextImpl(decoder, out, lastBuffer, max)
    }

    actual final override fun flush() {
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
        if (this === Empty) {
            throw IllegalArgumentException("Attempted to release empty")
        }
        while (true) {
            val value = refCount
            val newValue = value - 1

            if (value == 0L) {
                throw IllegalStateException("Unable to release: already released")
            }
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

    private fun clearInstanceInternal() {
        next = null
        attachment = null
//        resetForWrite(capacity)
        writeBuffer.limit(writeBuffer.capacity())
        writeBuffer.position(0)
        readBuffer.limit(0) // position will be set to 0 as well

        if (!RefCount.compareAndSet(this, 0L, 1L)) {
            throw IllegalStateException("Unable to prepare buffer: refCount is not zero (used while parked in the pool?)")
        }
    }

    @PublishedApi
    @Suppress("NOTHING_TO_INLINE")
    internal inline fun afterWrite() {
        readBuffer.limit(writePosition)
    }

    override fun toString(): String =
        "Buffer[readable = $readRemaining, writable = $writeRemaining, startGap = $startGap, endGap = $endGap]"

    actual companion object {
        private val EmptyBuffer: ByteBuffer = ByteBuffer.allocateDirect(0)
        private val RefCount = AtomicLongFieldUpdater.newUpdater(IoBuffer::class.java, IoBuffer::refCount.name)!!

        private val DEFAULT_BUFFER_SIZE = getIOIntProperty("buffer.size", 4096)
        private val DEFAULT_BUFFER_POOL_SIZE = getIOIntProperty("buffer.pool.size", 100)
        private val DEFAULT_BUFFER_POOL_DIRECT = getIOIntProperty("buffer.pool.direct", 0)

        actual val Empty = IoBuffer(EmptyBuffer, null)
        actual val Pool: ObjectPool<IoBuffer> = object : DefaultPool<IoBuffer>(DEFAULT_BUFFER_POOL_SIZE) {
            override fun produceInstance(): IoBuffer {
                val buffer = when (DEFAULT_BUFFER_POOL_DIRECT) {
                    0 -> ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
                    else -> ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE)
                }
                return IoBuffer(buffer, null)
            }

            override fun disposeInstance(instance: IoBuffer) {
                instance.unlink()
            }

            override fun clearInstance(instance: IoBuffer): IoBuffer {
                return instance.apply {
                    clearInstanceInternal()
                }
            }

            override fun validateInstance(instance: IoBuffer) {
                require(instance.refCount == 0L) { "Buffer is not yet released but tried to recycle" }
                require(instance.origin == null) { "Unable to recycle buffer view, only origin buffers are applicable" }
            }
        }

        actual val NoPool: ObjectPool<IoBuffer> = object : NoPoolImpl<IoBuffer>() {
            override fun borrow(): IoBuffer {
                val buffer = when (DEFAULT_BUFFER_POOL_DIRECT) {
                    0 -> ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
                    else -> ByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE)
                }
                return IoBuffer(buffer, null)
            }
        }

        actual val EmptyPool: ObjectPool<IoBuffer> = EmptyBufferPoolImpl
    }
}
