package kotlinx.io.core

import kotlinx.io.core.IoBuffer.*
import kotlinx.io.core.internal.*
import kotlinx.io.errors.IOException
import kotlinx.io.pool.*

/**
 * Read-only immutable byte packet. Could be consumed only once however it does support [copy] that doesn't copy every byte
 * but creates a new view instead. Once packet created it should be either completely read (consumed) or released
 * via [release].
 */
@DangerousInternalIoApi
abstract class ByteReadPacketBase(@PublishedApi internal var head: IoBuffer,
                                  remaining: Long = head.remainingAll(),
                                  val pool: ObjectPool<IoBuffer>) : Input {

    init {
        head.setByteOrderForNonEmpty(ByteOrder.BIG_ENDIAN)
    }

    final override var byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
        set(newOrder) {
            field = newOrder
            head.setByteOrderForNonEmpty(newOrder)
        }

    /**
     * Number of bytes available for read
     */
    val remaining: Long get() = headRemaining.toLong() + tailRemaining

    @Deprecated("For compatibility purpose", level = DeprecationLevel.HIDDEN)
    fun getRemaining(): Int = remaining.coerceAtMostMaxInt()

    /**
     * @return `true` if there is at least one byte to read
     */
    fun canRead() = tailRemaining != 0L || head.canRead()

    /**
     * @return `true` if there are at least [n] bytes to read
     */
    fun hasBytes(n: Int) = headRemaining + tailRemaining >= n

    @PublishedApi
    internal var headRemaining = head.readRemaining

    private var tailRemaining: Long = remaining - headRemaining

    /**
     * `true` if no bytes available for read
     */
    val isEmpty: Boolean
        get() = headRemaining == 0 && tailRemaining == 0L && noMoreChunksAvailable

    val isNotEmpty: Boolean
        get() = headRemaining > 0 || tailRemaining > 0L || !noMoreChunksAvailable

    private var noMoreChunksAvailable = false
    override val endOfInput: Boolean
        get() = isEmpty && (noMoreChunksAvailable || doFill() == null)

    /**
     * Returns a copy of the packet. The original packet and the copy could be used concurrently. Both need to be
     * either completely consumed or released via [release]
     */
    fun copy(): ByteReadPacket = ByteReadPacket(head.copyAll(), remaining, pool)

    /**
     * Release packet. After this function invocation the packet becomes empty. If it has been copied via [copy]
     * then the copy should be released as well.
     */
    fun release() {
        val head = head
        val empty = IoBuffer.Empty

        if (head !== empty) {
            this.head = empty
            headRemaining = 0
            tailRemaining = 0
            head.releaseAll(pool)
        }
    }

    override fun close() {
        release()
        if (!noMoreChunksAvailable) {
            noMoreChunksAvailable = true
        }
        closeSource()
    }

    internal fun stealAll(): IoBuffer? {
        val head = head
        val empty = IoBuffer.Empty

        if (head === empty) return null
        this.head = empty
        headRemaining = 0
        tailRemaining = 0
        return head
    }

    internal fun steal(): IoBuffer? {
        val head = head
        val next = head.next
        val empty = IoBuffer.Empty
        if (head === empty) return null

        val nextRemaining = next?.readRemaining ?: 0

        this.head = next ?: empty
        this.headRemaining = nextRemaining
        this.tailRemaining -= nextRemaining
        head.next = null

        return head
    }

    internal fun append(chain: IoBuffer) {
        if (chain === IoBuffer.Empty) return

        val size = chain.remainingAll()
        if (head === IoBuffer.Empty) {
            head = chain
            headRemaining = chain.readRemaining
            tailRemaining = size - headRemaining
        } else {
            head.findTail().next = chain
            tailRemaining += size
        }
        chain.byteOrder = byteOrder
    }

    internal fun tryWriteAppend(chain: IoBuffer): Boolean {
        val tail = head.findTail()
        val size = chain.readRemaining

        if (size == 0 || tail.writeRemaining < size) return false
        tail.writeBufferAppend(chain, size)

        if (head === tail) {
            headRemaining += size
        } else {
            tailRemaining += size
        }

        return true
    }

    final override fun readByte(): Byte {
        val headRemaining = headRemaining
        if (headRemaining > 1) {
            this.headRemaining = headRemaining - 1
            return head.readByte()
        }

        return readByteSlow2()
    }

    private fun readByteSlow2(): Byte {
        val head = head
        val headRemaining = headRemaining

        if (headRemaining == 1) {
            this.headRemaining = headRemaining - 1
            return head.readByte().also { ensureNext(head) }
        } else {
            return readByteSlow(head)
        }
    }

    private fun readByteSlow(head: IoBuffer): Byte {
        ensureNext(head) ?: throw EOFException("One more byte required but reached end of input")
        return readByte()
    }

    final override fun readShort() = readN(2) { readShort() }
    final override fun readFloat() = readN(4) { readFloat() }
    final override fun readDouble() = readN(8) { readDouble() }

    final override fun readInt(): Int {
        val headRemaining = headRemaining
        if (headRemaining > 4) {
            this.headRemaining = headRemaining - 4
            return head.readInt()
        }

        return readIntSlow()
    }

    private fun readIntSlow(): Int = readN(4) { readInt() }

    final override fun readLong(): Long {
        val headRemaining = headRemaining
        if (headRemaining > 8) {
            this.headRemaining = headRemaining - 8
            return head.readLong()
        }

        return readLongSlow()
    }

    private fun readLongSlow(): Long = readN(8) { readLong() }

    /**
     * Read as much bytes as possible to [dst] array
     * @return number of bytes copied
     */
    fun readAvailable(dst: ByteArray): Int = readAvailable(dst, 0, dst.size)

    /**
     * Read at most [length] bytes to [dst] array and write them at [offset]
     * @return number of bytes copied to the array
     */
    final override fun readAvailable(dst: ByteArray, offset: Int, length: Int): Int {
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        return readAsMuchAsPossible(dst, offset, length, 0)
    }

    /**
     * Read exactly [length] bytes to [dst] array at specified [offset]
     */
    final override fun readFully(dst: ByteArray, offset: Int, length: Int) {
        val rc = readAvailable(dst, offset, length)
        if (rc != length) throw EOFException("Not enough data in packet to fill buffer: ${length - rc} more bytes required")
    }

    /**
     * Discards at most [n] bytes
     * @return number of bytes has been discarded
     */
    fun discard(n: Int) = discardAsMuchAsPossible(n, 0)

    /**
     * Discards exactly [n] bytes or fails with [EOFException]
     */
    fun discardExact(n: Int) {
        if (discard(n) != n) throw EOFException("Unable to discard $n bytes due to end of packet")
    }

    @PublishedApi
    internal inline fun read(block: (IoBuffer) -> Unit) {
        read(1, block)
    }

    @PublishedApi
    internal inline fun read(n: Int, block: (IoBuffer) -> Unit) {
        val head = head
        var before = head.readRemaining
        val buffer = if (before < n) {
            prepareRead(n, head).also { before = it?.readRemaining ?: 0 }
        } else {
            head
        }

        if (buffer != null) {
            block(buffer)
            val after = buffer.readRemaining
            val delta = before - after
            if (delta > 0) {
                headRemaining -= delta
            }
            if (after == 0) {
                ensureNext(buffer)
            }
        }
    }

    final override fun readFully(dst: ShortArray, offset: Int, length: Int) {
        var copied = 0
        takeWhile { buffer ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc == -1) throw EOFException("Unexpected EOF while reading $length bytes")
            copied += rc
            copied < length
        }

        if (copied != length) {
            throw EOFException("Not enough bytes available to read $length short integers, $copied were copied")
        }
    }

    final override fun readAvailable(dst: ShortArray, offset: Int, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) {
            if (doFill() == null) return -1
            return readAvailable(dst, offset, length)
        }
        val size = minOf(remaining, length.toLong()).toInt()
        readFully(dst, offset, size)
        return size
    }

    final override fun readFully(dst: IntArray, offset: Int, length: Int) {
        var copied = 0
        takeWhile { buffer ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc == -1) throw EOFException("Unexpected EOF while read $length short integers")
            copied += rc
            copied < length
        }

        if (copied != length) {
            throw EOFException("Not enough bytes available to read $length integers, $copied were copied")
        }
    }

    final override fun readAvailable(dst: IntArray, offset: Int, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) {
            if (doFill() == null) return -1
            return readAvailable(dst, offset, length)
        }
        val size = minOf(remaining, length.toLong()).toInt()
        readFully(dst, offset, size)
        return size
    }

    final override fun readFully(dst: LongArray, offset: Int, length: Int) {
        var copied = 0
        takeWhile { buffer ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc == -1) throw EOFException("Unexpected EOF while reading $length long integers")
            copied += rc
            copied < length
        }

        if (copied != length) {
            throw EOFException("Not enough bytes available to read $length long integers, $copied were copied")
        }
    }

    final override fun readAvailable(dst: LongArray, offset: Int, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) {
            if (doFill() == null) return -1
            return readAvailable(dst, offset, length)
        }
        val size = minOf(remaining, length.toLong()).toInt()
        readFully(dst, offset, size)
        return size
    }

    final override fun readFully(dst: FloatArray, offset: Int, length: Int) {
        var copied = 0
        takeWhile { buffer ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc == -1) throw EOFException("Unexpected EOF while read $length float number")
            copied += rc
            copied < length
        }

        if (copied != length) {
            throw EOFException("Not enough bytes available to read $length float numbers, $copied were copied")
        }
    }

    final override fun readAvailable(dst: FloatArray, offset: Int, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) {
            if (doFill() == null) return -1
            return readAvailable(dst, offset, length)
        }
        val size = minOf(remaining, length.toLong()).toInt()
        readFully(dst, offset, size)
        return size
    }

    final override fun readFully(dst: DoubleArray, offset: Int, length: Int) {
        var copied = 0
        takeWhile { buffer ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc == -1) throw EOFException("Unexpected EOF while reading $length double float numbers")
            copied += rc
            copied < length
        }

        if (copied != length) {
            throw EOFException("Not enough bytes available to read $length double numbers, $copied were copied")
        }
    }

    final override fun readAvailable(dst: DoubleArray, offset: Int, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) {
            if (doFill() == null) return -1
            return readAvailable(dst, offset, length)
        }
        val size = minOf(remaining, length.toLong()).toInt()
        readFully(dst, offset, size)
        return size
    }

    final override fun readFully(dst: IoBuffer, length: Int) {
        require(length <= dst.writeRemaining) { "Not enough free space in destination buffer to write $length bytes" }

        var copied = 0
        takeWhile { buffer ->
            val rc = buffer.readAvailable(dst, length - copied)
            if (rc > 0) copied += rc
            copied < length
        }

        if (copied != length) {
            throw EOFException("Not enough bytes available to read $length bytes, $copied were copied")
        }
    }

    final override fun readAvailable(dst: IoBuffer, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) {
            if (doFill() == null) return -1
            return readAvailable(dst, length)
        }
        val size = minOf(remaining, length.toLong(), dst.writeRemaining.toLong()).toInt()
        readFully(dst, size)
        return size
    }

    /*
     * Returns next byte (unsigned) or `-1` if no more bytes available
     */
    final override fun tryPeek(): Int {
        val head = head
        if (headRemaining > 0) {
            return head.tryPeek()
        }

        if (tailRemaining == 0L && noMoreChunksAvailable) return -1

        return prepareRead(1, head)?.tryPeek() ?: -1
    }

    final override fun peekTo(buffer: IoBuffer): Int {
        val head = prepareReadHead(1) ?: return -1

        val size = minOf(buffer.writeRemaining, head.readRemaining)
        buffer.writeFully(head, size)

        return size
    }

    final override fun discard(n: Long): Long {
        return discardAsMuchAsPossible(minOf(Int.MAX_VALUE.toLong(), n).toInt(), 0).toLong()
    }

    internal fun readCbuf(cbuf: CharArray, off: Int, len: Int): Int {
        if (isEmpty) return -1

        val out = object : Appendable {
            private var idx = off

            override fun append(c: Char): Appendable {
                cbuf[idx++] = c
                return this
            }

            override fun append(csq: CharSequence?): Appendable {
                if (csq is String) {
                    csq.getCharsInternal(cbuf, idx)
                    idx += csq.length
                }
                else if (csq != null) {
                    for (i in 0 until csq.length) {
                        cbuf[idx++] = csq[i]
                    }
                }

                return this
            }

            override fun append(csq: CharSequence?, start: Int, end: Int): Appendable {
                throw UnsupportedOperationException()
            }
        }

        return readText(out, 0, len)
    }

    /**
     * Read at least [min] and at most [max] characters and append them to [out]
     * @return number of characters appended
     */
    fun readText(out: Appendable, min: Int = 0, max: Int = Int.MAX_VALUE): Int {
        if (max.toLong() >= remaining) {
            val s = readTextExactBytes(bytes = remaining.toInt())
            out.append(s)
            return s.length
        }
        return readASCII(out, min, max)
    }

    /**
     * Read exactly [exactCharacters] characters and append them to [out]
     */
    fun readTextExact(out: Appendable, exactCharacters: Int) {
        readText(out, exactCharacters, exactCharacters)
    }

    /**
     * Read a string at last [min] and at most [max] characters length
     */
    fun readText(min: Int = 0, max: Int = Int.MAX_VALUE): String {
        if (min == 0 && (max == 0 || isEmpty)) return ""
        val remaining = remaining
        if (remaining > 0 && max.toLong() >= remaining) return readTextExactBytes(bytes = remaining.toInt())

        return buildString(min.coerceAtLeast(16).coerceAtMost(max)) {
            readASCII(this, min, max)
        }
    }

    /**
     * Read a string exactly [exactCharacters] length
     */
    fun readTextExact(exactCharacters: Int): String {
        return readText(exactCharacters, exactCharacters)
    }

    private fun readASCII(out: Appendable, min: Int, max: Int): Int {
        when {
            max == 0 && min == 0 -> return 0
            isEmpty -> if (min == 0) return 0 else atLeastMinCharactersRequire(min)
            max < min -> minShouldBeLess(min, max)
        }

        var copied = 0
        var utf8 = false

        takeWhile { buffer ->
            val rc = buffer.decodeASCII {
                if (copied == max) false
                else {
                    out.append(it)
                    copied++
                    true
                }
            }

            when {
                rc -> true
                copied == max -> false
                else -> {
                    utf8 = true
                    false
                }
            }
        }

        if (utf8) {
            return copied + readUtf8(out, min - copied, max - copied)
        }
        if (copied < min) prematureEndOfStreamChars(min, copied)
        return copied
    }

    private fun atLeastMinCharactersRequire(min: Int): Nothing =
        throw EOFException("at least $min characters required but no bytes available")

    private fun minShouldBeLess(min: Int, max: Int): Nothing =
        throw IllegalArgumentException("min should be less or equal to max but min = $min, max = $max")

    private fun prematureEndOfStreamChars(min: Int, copied: Int): Nothing = throw MalformedUTF8InputException(
        "Premature end of stream: expected at least $min chars but had only $copied"
    )

    private fun prematureEndOfStream(size: Int): Nothing =
        throw MalformedUTF8InputException("Premature end of stream: expected $size bytes")

    private fun readUtf8(out: Appendable, min: Int, max: Int): Int {
        var copied = 0

        takeWhileSize { buffer ->
            val size = buffer.decodeUTF8 {
                if (copied == max) false
                else {
                    out.append(it)
                    copied++
                    true
                }
            }

            when {
                size == 0 -> 1
                size > 0 -> size
                else -> 0
            }
        }

        if (copied < min) prematureEndOfStreamChars(min, copied)

        return copied
    }

    private tailrec fun discardAsMuchAsPossible(n: Int, skipped: Int): Int {
        if (n == 0) return skipped
        val current = prepareRead(1) ?: return skipped
        val size = minOf(current.readRemaining, n)
        current.discardExact(size)
        headRemaining -= size
        afterRead()

        return discardAsMuchAsPossible(n - size, skipped + size)
    }

    private tailrec fun readAsMuchAsPossible(array: ByteArray, offset: Int, length: Int, copied: Int): Int {
        if (length == 0) return copied
        val current = prepareRead(1) ?: return copied
        val size = minOf(length, current.readRemaining)

        current.readFully(array, offset, size)
        headRemaining -= size

        return if (size != length || current.readRemaining == 0) {
            afterRead()
            readAsMuchAsPossible(array, offset + size, length - size, copied + size)
        } else {
            copied + size
        }
    }

    private inline fun <R> readN(n: Int, block: IoBuffer.() -> R): R {
        val bb = prepareRead(n) ?: notEnoughBytesAvailable(n)
        val rc = block(bb)

        val after = bb.readRemaining
        if (after == 0) {
            ensureNext(bb)
        } else {
            headRemaining = after
        }

        return rc
    }

    private fun notEnoughBytesAvailable(n: Int): Nothing {
        throw EOFException("Not enough data in packet ($remaining) to read $n byte(s)")
    }

    @DangerousInternalIoApi
    fun updateHeadRemaining(remaining: Int) {
        headRemaining = remaining
    }

    @DangerousInternalIoApi
    fun prepareReadHead(minSize: Int): IoBuffer? = prepareRead(minSize, head)

    @DangerousInternalIoApi
    fun ensureNextHead(current: IoBuffer): IoBuffer? = ensureNext(current)

    @PublishedApi
    internal fun ensureNext(current: IoBuffer) = ensureNext(current, IoBuffer.Empty)

    @DangerousInternalIoApi
    fun fixGapAfterRead(current: IoBuffer) {
        val next = current.next ?: return fixGapAfterReadFallback(current)

        val remaining = current.readRemaining
        val overrunSize = minOf(remaining, IoBuffer.ReservedSize - current.endGap)
        if (next.startGap < overrunSize) return fixGapAfterReadFallback(current)

        next.restoreStartGap(overrunSize)

        if (remaining > overrunSize) {
            current.restoreEndGap(overrunSize)

            this.headRemaining = remaining - overrunSize
            this.tailRemaining += overrunSize
        } else {
            this.head = next
            val nextSize = next.readRemaining
            this.headRemaining = nextSize
            this.tailRemaining -= nextSize - overrunSize

            current.release(pool)
        }
    }

    private fun fixGapAfterReadFallback(current: IoBuffer) {
        if (noMoreChunksAvailable) {
            this.headRemaining = current.readRemaining
            this.tailRemaining = 0
            return
        }

        val size = current.readRemaining
        val overrun = minOf(size, IoBuffer.ReservedSize - current.endGap)

        if (size > overrun) {
            fixGapAfterReadFallbackUnreserved(current, size, overrun)
        } else {
            val new = pool.borrow()
            new.reserveEndGap(IoBuffer.ReservedSize)
            new.next = current.next

            new.writeBufferAppend(current, size)
            this.head = new
            this.headRemaining = size
            this.tailRemaining = 0L
        }

        current.release(pool)
    }

    private fun fixGapAfterReadFallbackUnreserved(current: IoBuffer, size: Int, overrun: Int) {
        // if we have a chunk with no end reservation
        // we can split it into two to fix it

        val chunk1 = pool.borrow()
        val chunk2 = pool.borrow()

        chunk1.reserveEndGap(IoBuffer.ReservedSize)
        chunk2.reserveEndGap(IoBuffer.ReservedSize)
        chunk1.next = chunk2
        chunk2.next = current.next

        chunk1.writeBufferAppend(current, size - overrun)
        chunk2.writeBufferAppend(current, overrun)

        this.head = chunk1
        this.headRemaining = chunk1.readRemaining
        this.tailRemaining = chunk2.readRemaining.toLong()
    }

    private tailrec fun ensureNext(current: IoBuffer, empty: IoBuffer): IoBuffer? {
        if (current === empty) {
            return doFill()
        }

        val next = current.next
        current.release(pool)

        return when {
            next == null -> {
                this.headRemaining = 0
                this.tailRemaining = 0L
                this.head = empty
                ensureNext(empty, empty)
            }
            next.canRead() -> {
                head = next
                next.byteOrder = byteOrder
                val nextRemaining = next.readRemaining
                headRemaining = nextRemaining
                tailRemaining -= nextRemaining
                next
            }
            else -> ensureNext(next, empty)
        }
    }

    /**
     * Reads the next chunk suitable for reading or `null` if no more chunks available. It is also allowed
     * to return a chain of chunks linked through [IoBuffer.next]. The last chunk should have `null` next reference.
     * Could rethrow exceptions from the underlying source.
     */
    protected abstract fun fill(): IoBuffer?

    /**
     * Should close the underlying bytes source. Could do nothing or throw exceptions.
     */
    protected abstract fun closeSource()

    internal fun markNoMoreChunksAvailable() {
        if (!noMoreChunksAvailable) {
            noMoreChunksAvailable = true
        }
    }

    protected fun doFill(): IoBuffer? {
        if (noMoreChunksAvailable) return null
        val chunk = fill()
        if (chunk == null) {
            noMoreChunksAvailable = true
            return null
        }
        appendView(chunk)
        return chunk
    }

    internal fun appendView(chunk: IoBuffer) {
        val tail = head.findTail()
        if (tail === IoBuffer.Empty) {
            head = chunk
            chunk.byteOrder = byteOrder
            require(tailRemaining == 0L) { throw IllegalStateException("It should be no tail remaining bytes if current tail is EmptyBuffer") }
            headRemaining = chunk.readRemaining
            tailRemaining = chunk.next?.remainingAll() ?: 0L
        } else {
            tail.next = chunk
            tailRemaining += chunk.remainingAll()
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    internal inline fun prepareRead(minSize: Int): IoBuffer? = prepareRead(minSize, head)

    @PublishedApi
    internal tailrec fun prepareRead(minSize: Int, head: IoBuffer): IoBuffer? {
        val headSize = headRemaining
        if (headSize >= minSize) return head

        val next = head.next ?: doFill() ?: return null
        next.byteOrder = byteOrder

        if (headSize == 0) {
            if (head !== IoBuffer.Empty) {
                releaseHead(head)
            }

            return prepareRead(minSize, next)
        } else {
            val before = next.readRemaining
            head.writeBufferAppend(next, minSize - headSize)
            val after = next.readRemaining
            headRemaining = head.readRemaining
            tailRemaining -= before - after
            if (after == 0) {
                head.next = next.next
                next.release(pool)
            }
        }

        if (head.readRemaining >= minSize) return head
        if (minSize > IoBuffer.ReservedSize) minSizeIsTooBig(minSize)

        return prepareRead(minSize, head)
    }

    private fun minSizeIsTooBig(minSize: Int): Nothing {
        throw IllegalStateException("minSize of $minSize is too big (should be less than ${IoBuffer.ReservedSize}")
    }

    private fun afterRead() {
        val head = head
        if (head.readRemaining == 0) {
            releaseHead(head)
        }
    }

    internal fun releaseHead(head: IoBuffer): IoBuffer {
        val next = head.next ?: IoBuffer.Empty
        this.head = next
        val nextRemaining = next.readRemaining
        this.headRemaining = nextRemaining
        this.tailRemaining -= nextRemaining
        head.release(pool)

        return next
    }

    companion object {
        @Deprecated(
            "Use ByteReadPacket.Empty instead",
            ReplaceWith("ByteReadPacket.Empty"),
            level = DeprecationLevel.ERROR
        )
        val Empty: ByteReadPacket
            get() = ByteReadPacket.Empty

        @Deprecated(
            "Use IoBuffer.ReservedSize instead",
            replaceWith = ReplaceWith("IoBuffer.ReservedSize", "kotlinx.io.core.IoBuffer"),
            level = DeprecationLevel.ERROR
        )
        val ReservedSize: Int = IoBuffer.ReservedSize
    }
}

expect class EOFException(message: String) : IOException

private fun IoBuffer.setByteOrderForNonEmpty(newByteOrder: ByteOrder) {
    if (canRead()) {
        byteOrder = newByteOrder
    }
}
