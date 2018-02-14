package kotlinx.io.core

import kotlinx.io.pool.*

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

expect fun BytePacketBuilder(headerSizeHint: Int): BytePacketBuilder

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
class BytePacketBuilder(private var headerSizeHint: Int, private val pool: ObjectPool<BufferView>) : Appendable {
    init {
        require(headerSizeHint >= 0) { "shouldn't be negative: headerSizeHint = $headerSizeHint" }
    }

    /**
     * Number of bytes currently buffered
     */
    var size: Int = 0
        private set

    /**
     * Byte order (Endianness) to be used by future write functions calls on this builder instance. Doesn't affect any
     * previously written values. Note that [reset] doesn't change this value back to the default byte order.
     * @default [ByteOrder.BIG_ENDIAN]
     */
    var byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
        set(value) {
            field = value
            tail.byteOrder = value
        }

    private var head: BufferView = BufferView.Empty
    private var tail: BufferView = head

    fun writeFully(src: ByteArray) {
        writeFully(src, 0, src.size)
    }

    fun writeFully(src: ByteArray, offset: Int, length: Int) {
        var copied = 0

        while (copied < length) {
            write(1) { buffer ->
                val size = minOf(buffer.writeRemaining, length - copied)
                buffer.write(src, offset + copied, size)
                copied += size
                size
            }
        }
    }

    fun writeLong(l: Long) {
        write(8) { it.writeLong(l); 8 }
    }

    fun writeInt(i: Int) {
        write(4) { it.writeInt(i); 4 }
    }

    fun writeShort(s: Short) {
        write(2) { it.writeShort(s); 2 }
    }

    fun writeByte(b: Byte) {
        write(1) { it.writeByte(b); 1 }
    }

    fun writeDouble(d: Double) {
        write(8) { it.writeDouble(d); 8 }
    }

    fun writeFloat(f: Float) {
        write(4) { it.writeFloat(f); 4 }
    }

    /**
     * Append single UTF-8 character
     */
    override fun append(c: Char): BytePacketBuilder {
        write(3) {
            it.putUtf8Char(c.toInt() and 0xffff)
        }
        return this
    }

    override fun append(csq: CharSequence?): BytePacketBuilder {
        if (csq == null) {
            append("null")
        } else {
            append(csq, 0, csq.length)
        }
        return this
    }

    override fun append(csq: CharSequence?, start: Int, end: Int): BytePacketBuilder {
        if (csq == null) {
            return append("null", start, end)
        }

        appendASCII(csq, start, end)
        return this
    }

    /**
     * Writes another packet to the end. Please note that the instance [p] gets consumed so you don't need to release it
     */
    fun writePacket(p: ByteReadPacket) {
        val foreignStolen = p.stealAll()
        if (foreignStolen == null) {
            p.release()
            return
        }

        val tail = tail
        if (tail === BufferView.Empty) {
            head = foreignStolen
            this.tail = foreignStolen.findTail()
            size = foreignStolen.remainingAll().toInt()
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
            size = head.remainingAll().toInt()
        } else if (prependSize == -1 || appendSize <= prependSize) {
            // do append
            tail.writeBufferAppend(foreignStolen, tail.writeRemaining + tail.endGap)
            tail.next = foreignStolen.next
            this.tail = foreignStolen.findTail().takeUnless { it === foreignStolen } ?: tail
            foreignStolen.release(p.pool)
            size = head.remainingAll().toInt()
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
            size = head.remainingAll().toInt()
        } else {
            throw IllegalStateException("prep = $prependSize, app = $appendSize")
        }
    }

    private tailrec fun appendASCII(csq: CharSequence, start: Int, end: Int) {
        val bb = ensure()
        val limitedEnd = minOf(end, start + bb.writeRemaining)

        for (i in start until limitedEnd) {
            val chi = csq[i].toInt() and 0xffff
            if (chi >= 0x80) {
                appendUTF8(csq, i, end, bb)
                return
            }

            bb.writeByte(chi.toByte())
            size++
        }

        if (limitedEnd < end) {
            return appendASCII(csq, limitedEnd, end)
        }
    }

    // expects at least one byte remaining in [bb]
    private tailrec fun appendUTF8(csq: CharSequence, start: Int, end: Int, bb: BufferView) {
        var rem = bb.writeRemaining
        val limitedEnd = minOf(end, start + rem)

        for (i in start until limitedEnd) {
            val chi = csq[i].toInt() and 0xffff
            val requiredSize = when {
                chi <= 0x7f -> 1
                chi > 0x7ff -> 3
                else -> 2
            }

            if (rem < requiredSize) {
                return appendUTF8(csq, i, end, appendNewBuffer())
            }

            val chSize = bb.putUtf8Char(chi)
            rem -= chSize
            size += chSize
        }

        if (limitedEnd < end) {
            return appendUTF8(csq, limitedEnd, end, appendNewBuffer())
        }
    }

    internal fun appendChars(ca: CharArray, start: Int, end: Int) {
        return appendASCII(ca, start, end)
    }

    private tailrec fun appendASCII(csq: CharArray, start: Int, end: Int) {
        val bb = ensure()
        val limitedEnd = minOf(end, start + bb.writeRemaining)

        for (i in start until limitedEnd) {
            val chi = csq[i].toInt() and 0xffff
            if (chi >= 0x80) {
                appendUTF8(csq, i, end, bb)
                return
            }

            bb.writeByte(chi.toByte())
            size++
        }

        if (limitedEnd < end) {
            return appendASCII(csq, limitedEnd, end)
        }
    }

    // expects at least one byte remaining in [bb]
    private tailrec fun appendUTF8(csq: CharArray, start: Int, end: Int, bb: BufferView) {
        val limitedEnd = minOf(end, start + bb.writeRemaining)
        for (i in start until limitedEnd) {
            val chi = csq[i].toInt() and 0xffff
            val requiredSize = when {
                chi <= 0x7f -> 1
                chi > 0x7ff -> 3
                else -> 2
            }

            if (bb.writeRemaining < requiredSize) {
                return appendUTF8(csq, i, end, appendNewBuffer())
            }

            size += bb.putUtf8Char(chi)
        }

        if (limitedEnd < end) {
            return appendUTF8(csq, limitedEnd, end, appendNewBuffer())
        }
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
    private inline fun BufferView.putUtf8Char(v: Int) = when {
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
     * Creates a temporary packet view of the packet being build without discarding any bytes from the builder.
     * This is similar to `build().copy()` except that the builder keeps already written bytes untouched.
     * A temporary view packet is passed as argument to [block] function and it shouldn't leak outside of this block
     * otherwise an unexpected behaviour may occur.
     */
    fun <R> preview(block: (tmp: ByteReadPacket) -> R): R {
        val head = head.copyAll()
        val pool = if (head === BufferView.Empty) EmptyBufferViewPool else pool
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
        val head = this.head

        this.head = BufferView.Empty
        this.tail = BufferView.Empty
        this.size = 0

        if (head === BufferView.Empty) return ByteReadPacket(head, EmptyBufferViewPool)
        return ByteReadPacket(head, pool)
    }

    /**
     * Release any resources that the builder holds. Builder shouldn't be used after release
     */
    fun release() {
        val head = this.head
        val empty = BufferView.Empty

        if (head !== empty) {
            this.head = empty
            this.tail = empty
            head.releaseAll(pool)
            size = 0
        }
    }

    /**
     * Discard all written bytes and prepare to build another packet.
     */
    fun reset() {
        release()
    }

    internal inline fun write(size: Int, block: (BufferView) -> Int) {
        val buffer = last()?.takeIf { it.writeRemaining >= size }

        this.size += if (buffer == null) {
            block(appendNewBuffer())
        } else {
            block(buffer)
        }
    }

    private fun ensure(): BufferView = last()?.takeIf { it.writeRemaining > 0 } ?: appendNewBuffer()

    private fun appendNewBuffer(): BufferView {
        val new = pool.borrow()
        if (head === BufferView.Empty) {
            new.reserveStartGap(headerSizeHint)
        }
        new.reserveEndGap(ByteReadPacket.ReservedSize)
        new.byteOrder = byteOrder
        last(new)
        return new
    }

    private fun last(): BufferView? = tail.takeIf { it !== BufferView.Empty }

    private fun last(new: BufferView) {
        if (head === BufferView.Empty) {
            tail = new
            head = new
        } else {
            tail.next = new
            tail = new
        }
    }
}

