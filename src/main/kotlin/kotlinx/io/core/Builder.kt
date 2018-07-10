package kotlinx.io.core

import kotlinx.io.pool.*
import kotlinx.io.core.internal.require

expect val PACKET_MAX_COPY_SIZE: Int

//fun BytePacketBuilder() = BytePacketBuilder(0)

/**
 * Build a byte packet in [block] lambda. Creates a temporary builder and releases it in case of failure
 */
inline fun buildPacket(headerSizeHint: Int = 0, block: BytePacketBuilder.() -> Unit): ByteReadPacket {
    val builder = BytePacketBuilder(headerSizeHint)
    try {
        block(builder)
        return builder.build()
    } catch (t: Throwable) {
        builder.release()
        throw t
    }
}

expect fun BytePacketBuilder(headerSizeHint: Int = 0): BytePacketBuilder

/**
 * A builder that provides ability to build byte packets with no knowledge of it's size.
 * Unlike Java's ByteArrayOutputStream it doesn't copy the whole content every time it's internal buffer overflows
 * but chunks buffers instead. Packet building via [build] function is O(1) operation and only does instantiate
 * a new [ByteReadPacket]. Once a byte packet has been built via [build] function call, the builder could be
 * reused again. You also can discard all written bytes via [reset] or [release]. Please note that an instance of
 * builder need to be terminated either via [build] function invocation or via [release] call otherwise it will
 * cause byte buffer leak so that may have performance impact.
 *
 * Byte packet builder is also an [Appendable] so it does append UTF-8 characters to a packet
 *
 * ```
 * buildPacket {
 *     listOf(1,2,3).joinTo(this, separator = ",")
 * }
 * ```
 */
class BytePacketBuilder(private var headerSizeHint: Int, pool: ObjectPool<IoBuffer>): BytePacketBuilderPlatformBase(pool) {
    init {
        require(headerSizeHint >= 0) { "shouldn't be negative: headerSizeHint = $headerSizeHint" }
    }

    /**
     * Number of bytes written to the builder
     */
    val size: Int get() {
        val size = _size
        if (size == -1) {
            _size = head.remainingAll().toInt()
            return _size
        }
        return size
    }

    val isEmpty: Boolean get() = head === IoBuffer.Empty || _size == 0 || (_size == -1 && size == 0)
    val isNotEmpty: Boolean get() = head !== IoBuffer.Empty || _size > 0 || (size == -1 && (head.canRead() || size > 0))

    private var head: IoBuffer = IoBuffer.Empty

    override fun append(c: Char): BytePacketBuilder {
        return super.append(c) as BytePacketBuilder
    }

    override fun append(csq: CharSequence?): BytePacketBuilder {
        return super.append(csq) as BytePacketBuilder
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): BytePacketBuilder {
        return super.append(csq, start, end) as BytePacketBuilder
    }

    /**
     * Release any resources that the builder holds. Builder shouldn't be used after release
     */
    override fun release() {
        val head = this.head
        val empty = IoBuffer.Empty

        if (head !== empty) {
            this.head = empty
            this.tail = empty
            head.releaseAll(pool)
            _size = 0
        }
    }

    override fun flush() {
    }

    override fun close() {
        release()
    }

    /**
     * Creates a temporary packet view of the packet being build without discarding any bytes from the builder.
     * This is similar to `build().copy()` except that the builder keeps already written bytes untouched.
     * A temporary view packet is passed as argument to [block] function and it shouldn't leak outside of this block
     * otherwise an unexpected behaviour may occur.
     */
    fun <R> preview(block: (tmp: ByteReadPacket) -> R): R {
        val head = head.copyAll()
        val pool = if (head === IoBuffer.Empty) IoBuffer.EmptyPool else pool
        val packet = ByteReadPacket(head, pool)

        return try {
            block(packet)
        } finally {
            packet.release()
        }
    }

    /**
     * Builds byte packet instance and resets builder's state to be able to build another one packet if needed
     */
    fun build(): ByteReadPacket {
        val size = size
        val head = stealAll()

        return when (head) {
            null -> ByteReadPacket.Empty
            else -> ByteReadPacket(head, size.toLong(), pool)
        }
    }

    /**
     * Detach all chunks and cleanup all internal state so builder could be reusable again
     * @return a chain of buffer views or `null` of it is empty
     */
    internal fun stealAll(): IoBuffer? {
        val head = this.head

        this.head = IoBuffer.Empty
        this.tail = IoBuffer.Empty
        this._size = 0

        return if (head === IoBuffer.Empty) null else head
    }

    /**
     * Writes another packet to the end. Please note that the instance [p] gets consumed so you don't need to release it
     */
    override fun writePacket(p: ByteReadPacket) {
        val foreignStolen = p.stealAll()
        if (foreignStolen == null) {
            p.release()
            return
        }

        val tail = tail
        if (tail === IoBuffer.Empty) {
            head = foreignStolen
            this.tail = foreignStolen.findTail()
            _size = foreignStolen.remainingAll().toInt()
            return
        }

        val lastSize = tail.readRemaining
        val nextSize = foreignStolen.readRemaining

        val maxCopySize = PACKET_MAX_COPY_SIZE
        val appendSize = if (nextSize < maxCopySize && nextSize <= (tail.endGap + tail.writeRemaining)) {
            nextSize
        } else -1

        val prependSize = if (lastSize < maxCopySize && lastSize <= foreignStolen.startGap && foreignStolen.isExclusivelyOwned()) {
            lastSize
        } else -1

        if (appendSize == -1 && prependSize == -1) {
            // simply enqueue
            tail.next = foreignStolen
            this.tail = foreignStolen.findTail()
            _size = head.remainingAll().toInt()
        } else if (prependSize == -1 || appendSize <= prependSize) {
            // do append
            tail.writeBufferAppend(foreignStolen, tail.writeRemaining + tail.endGap)
            tail.next = foreignStolen.next
            this.tail = foreignStolen.findTail().takeUnless { it === foreignStolen } ?: tail
            foreignStolen.release(p.pool)
            _size = head.remainingAll().toInt()
        } else if (appendSize == -1 || prependSize < appendSize) {
            // do prepend
            foreignStolen.writeBufferPrepend(tail)

            if (head === tail) {
                head = foreignStolen
            } else {
                var pre = head
                while (true) {
                    val next = pre.next!!
                    if (next === tail) break
                    pre = next
                }

                pre.next = foreignStolen
            }
            tail.release(pool)

            this.tail = foreignStolen.findTail()
            _size = head.remainingAll().toInt()
        } else {
            throw IllegalStateException("prep = $prependSize, app = $appendSize")
        }
    }

    override fun last(buffer: IoBuffer) {
        if (head === IoBuffer.Empty) {
            if (buffer.isEmpty()) { // headerSize is just a hint so we shouldn't force to reserve space
                buffer.reserveStartGap(headerSizeHint) // it will always fail for non-empty buffer
            }
            tail = buffer
            head = buffer
            _size = buffer.remainingAll().toInt()
        } else {
            tail.next = buffer
            tail = buffer
            _size = -1
        }
    }
}

expect abstract class BytePacketBuilderPlatformBase
    internal constructor(pool: ObjectPool<IoBuffer>) : BytePacketBuilderBase

abstract class BytePacketBuilderBase internal constructor(protected val pool: ObjectPool<IoBuffer>) : Appendable, Output {

    /**
     * Number of bytes currently buffered or -1 if not known (need to be recomputed)
     */
    protected var _size: Int = 0

    /**
     * Byte order (Endianness) to be used by future write functions calls on this builder instance. Doesn't affect any
     * previously written values. Note that [reset] doesn't change this value back to the default byte order.
     * @default [ByteOrder.BIG_ENDIAN]
     */
    final override var byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
        set(value) {
            field = value
            tail.byteOrder = value
        }

    @PublishedApi
    internal var tail: IoBuffer = IoBuffer.Empty

    final override fun writeFully(src: ByteArray, offset: Int, length: Int) {
        if (length == 0) return

        var copied = 0

        writeLoop(1, { copied < length }) { buffer, bufferRemaining ->
            val size = minOf(bufferRemaining, length - copied)
            buffer.writeFully(src, offset + copied, size)
            copied += size
            size
        }
    }

    final override fun writeLong(v: Long) {
        write(8) { it.writeLong(v); 8 }
    }

    final override fun writeInt(v: Int) {
        write(4) { it.writeInt(v); 4 }
    }

    final override fun writeShort(v: Short) {
        write(2) { it.writeShort(v); 2 }
    }

    final override fun writeByte(v: Byte) {
        write(1) { it.writeByte(v); 1 }
    }

    final override fun writeDouble(v: Double) {
        write(8) { it.writeDouble(v); 8 }
    }

    final override fun writeFloat(v: Float) {
        write(4) { it.writeFloat(v); 4 }
    }

    override fun writeFully(src: ShortArray, offset: Int, length: Int) {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset + length < src.lastIndex) { "offset ($offset) + length ($length) >= src.lastIndex (${src.lastIndex})" }

        if (length == 0) return

        var start = offset
        var remaining = length

        writeLoop(2, { remaining > 0 }) { buffer, chunkRemaining ->
            val qty = minOf(chunkRemaining shr 1, remaining)
            buffer.writeFully(src, start, qty)
            start += qty
            remaining -= qty
            qty * 2
        }
    }

    override fun writeFully(src: IntArray, offset: Int, length: Int) {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset + length < src.lastIndex) { "offset ($offset) + length ($length) >= src.lastIndex (${src.lastIndex})" }

        var start = offset
        var remaining = length

        writeLoop(4, { remaining > 0 }) { buffer, chunkRemaining ->
            val qty = minOf(chunkRemaining shr 2, remaining)
            buffer.writeFully(src, start, qty)
            start += qty
            remaining -= qty
            qty * 4
        }
    }

    override fun writeFully(src: LongArray, offset: Int, length: Int) {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset + length < src.lastIndex) { "offset ($offset) + length ($length) >= src.lastIndex (${src.lastIndex})" }

        var start = offset
        var remaining = length

        writeLoop(8, { remaining > 0 }) { buffer, chunkRemaining ->
            val qty = minOf(chunkRemaining shr 3, remaining)
            buffer.writeFully(src, start, qty)
            start += qty
            remaining -= qty
            qty * 8
        }
    }

    override fun writeFully(src: FloatArray, offset: Int, length: Int) {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset + length < src.lastIndex) { "offset ($offset) + length ($length) >= src.lastIndex (${src.lastIndex})" }

        var start = offset
        var remaining = length

        writeLoop(4, { remaining > 0 }) { buffer, chunkRemaining ->
            val qty = minOf(chunkRemaining shr 2, remaining)
            buffer.writeFully(src, start, qty)
            start += qty
            remaining -= qty
            qty * 4
        }
    }

    override fun writeFully(src: DoubleArray, offset: Int, length: Int) {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset + length < src.lastIndex) { "offset ($offset) + length ($length) >= src.lastIndex (${src.lastIndex})" }

        var start = offset
        var remaining = length

        writeLoop(8, { remaining > 0 }) { buffer, chunkRemaining ->
            val qty = minOf(chunkRemaining shr 3, remaining)
            buffer.writeFully(src, start, qty)
            start += qty
            remaining -= qty
            qty * 8
        }
    }

    override fun writeFully(src: IoBuffer, length: Int) {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(length <= src.readRemaining) { "Not enough bytes available in src buffer to read $length bytes" }

        val totalSize = minOf(src.readRemaining, length)
        if (totalSize == 0) return
        var remaining = totalSize

        var tail = tail
        if (!tail.canWrite()) {
            tail = appendNewBuffer()
        }

        do {
            val size = minOf(tail.writeRemaining, remaining)
            tail.writeFully(src, size)
            remaining -= size

            if (remaining == 0) break
            tail = appendNewBuffer()
        } while (true)

        addSize(totalSize)
    }

    override fun fill(n: Long, v: Byte) {
        require(n >= 0L) { "n shouldn't be negative: $n" }

        var rem = n
        writeLoop(1, { rem > 0L }) { buffer, chunkRemaining ->
            val size = minOf(chunkRemaining.toLong(), n).toInt()
            buffer.fill(size.toLong(), v)
            rem -= size
            size
        }
    }

    /**
     * Append single UTF-8 character
     */
    override fun append(c: Char): BytePacketBuilderBase {
        write(3) {
            it.putUtf8Char(c.toInt())
        }
        return this
    }

    override fun append(csq: CharSequence?): BytePacketBuilderBase {
        if (csq == null) {
            appendChars("null", 0, 4)
        } else {
            appendChars(csq, 0, csq.length)
        }
        return this
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): BytePacketBuilderBase {
        if (csq == null) {
            return append("null", start, end)
        }

        appendChars(csq, start, end)

        return this
    }

    open fun writePacket(p: ByteReadPacket) {
        while (true) {
            val buffer = p.steal() ?: break
            last(buffer)
        }
    }

    /**
     * Write exact [n] bytes from packet to the builder
     */
    fun writePacket(p: ByteReadPacket, n: Int) {
        var remaining = n

        while (remaining > 0) {
            val headRemaining = p.headRemaining
            if (headRemaining <= remaining) {
                remaining -= headRemaining
                last(p.steal() ?: throw EOFException("Unexpected end of packet"))
            } else {
                p.read { view ->
                    writeFully(view, remaining)
                }
                break
            }
        }
    }

    /**
     * Write exact [n] bytes from packet to the builder
     */
    fun writePacket(p: ByteReadPacket, n: Long) {
        var remaining = n

        while (remaining > 0L) {
            val headRemaining = p.headRemaining.toLong()
            if (headRemaining <= remaining) {
                remaining -= headRemaining
                last(p.steal() ?: throw EOFException("Unexpected end of packet"))
            } else {
                p.read { view ->
                    writeFully(view, remaining.toInt())
                }
                break
            }
        }
    }

    override fun append(csq: CharArray, start: Int, end: Int): Appendable {
        appendChars(csq, start, end)
        return this
    }

    private fun appendChars(csq: CharSequence, start: Int, end: Int): Int {
        var idx = start
        if (idx >= end) return idx
        idx = tail.appendChars(csq, idx, end)

        while (idx < end) {
            idx = appendNewBuffer().appendChars(csq, idx, end)
        }

        this._size = -1
        return idx
    }

    private fun appendChars(csq: CharArray, start: Int, end: Int): Int {
        var idx = start
        if (idx >= end) return idx
        idx = tail.appendChars(csq, idx, end)

        while (idx < end) {
            idx = appendNewBuffer().appendChars(csq, idx, end)
        }

        this._size = -1
        return idx
    }

    fun writeStringUtf8(s: String) {
        append(s, 0, s.length)
    }

    fun writeStringUtf8(cs: CharSequence) {
        append(cs, 0, cs.length)
    }

//    fun writeStringUtf8(cb: CharBuffer) {
//        append(cb, 0, cb.remaining())
//    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun IoBuffer.putUtf8Char(v: Int) = when {
        v in 1..0x7f -> {
            writeByte(v.toByte())
            1
        }
        v > 0x7ff -> {
            writeByte((0xe0 or ((v shr 12) and 0x0f)).toByte())
            writeByte((0x80 or ((v shr  6) and 0x3f)).toByte())
            writeByte((0x80 or ( v         and 0x3f)).toByte())
            3
        }
        else -> {
            writeByte((0xc0 or ((v shr  6) and 0x1f)).toByte())
            writeByte((0x80 or ( v         and 0x3f)).toByte())
            2
        }
    }

    /**
     * Release any resources that the builder holds. Builder shouldn't be used after release
     */
    abstract fun release()

    override fun `$prepareWrite$`(n: Int): IoBuffer {
        if (tail.writeRemaining >= n) return tail
        return appendNewBuffer()
    }

    override fun `$afterWrite$`() {
        _size = -1
    }

    /**
     * Discard all written bytes and prepare to build another packet.
     */
    fun reset() {
        release()
    }

    @PublishedApi
    internal inline fun write(size: Int, block: (IoBuffer) -> Int) {
        var buffer = tail
        if (buffer.writeRemaining < size) {
            buffer = appendNewBuffer()
        }

        addSize(block(buffer))
    }

    private inline fun writeLoop(size: Int, predicate: () -> Boolean, block: (IoBuffer, Int) -> Int) {
        if (!predicate()) return
        var written = 0
        var buffer = tail
        var rem = buffer.writeRemaining

        do {
            if (rem < size) {
                buffer = appendNewBuffer()
                rem = buffer.writeRemaining
            }

            val result = block(buffer, rem)
            written += result
            rem -= result
        } while (predicate())

        addSize(written)
    }

    @PublishedApi
    internal fun addSize(n: Int) {
        val size = _size
        if (size != -1) {
            _size = size + n
        }
    }

    internal abstract fun last(buffer: IoBuffer)

    @PublishedApi
    internal fun appendNewBuffer(): IoBuffer {
        val new = pool.borrow()
        new.reserveEndGap(ByteReadPacket.ReservedSize)
        new.byteOrder = byteOrder

        last(new)

        return new
    }
}

private inline fun <T> T.takeUnless(predicate: (T) -> Boolean): T? {
    return if (!predicate(this)) this else null
}


