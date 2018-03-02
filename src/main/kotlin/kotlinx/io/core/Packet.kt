package kotlinx.io.core

import kotlinx.io.pool.*

/**
 * Read-only immutable byte packet. Could be consumed only once however it does support [copy] that doesn't copy every byte
 * but creates a new view instead. Once packet created it should be either completely read (consumed) or released
 * via [release].
 */
abstract class ByteReadPacketBase(private var head: BufferView,
                                  remaining: Long = head.remainingAll(),
                                  val pool: ObjectPool<BufferView>) : Input {

    final override var byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN

    /**
     * Number of bytes available for read
     */
    val remaining: Long get() = headRemaining.toLong() + tailRemaining

    private var headRemaining = head.readRemaining
    private var tailRemaining = remaining - headRemaining //head.next?.remainingAll() ?: 0

    /**
     * `true` if no bytes available for read
     */
    val isEmpty: Boolean
        get() = headRemaining == 0 && tailRemaining == 0L

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
        val empty = BufferView.Empty

        if (head !== empty) {
            this.head = empty
            headRemaining = 0
            tailRemaining = 0
            head.releaseAll(pool)
        }
    }

    internal fun stealAll(): BufferView? {
        val head = head
        val empty = BufferView.Empty

        if (head === empty) return null
        this.head = empty
        headRemaining = 0
        tailRemaining = 0
        return head
    }

    internal fun steal(): BufferView? {
        val head = head
        val next = head.next
        val empty = BufferView.Empty
        if (head === empty) return null

        this.head = next ?: empty
        this.headRemaining = next?.readRemaining ?: 0
        this.tailRemaining -= head.readRemaining

        return head
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

    private fun readByteSlow(head: BufferView): Byte {
        ensureNext(head)
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
        require(offset + length <= dst.size) { throw IllegalArgumentException() }

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

    /**
     * Read UTF-8 line and append all line characters to [out] except line endings. Does support CR, LF and CR+LF
     * @return `true` if some characters were appended or line ending reached (empty line) or `false` if packet
     * if empty
     */
    fun readUTF8LineTo(out: Appendable, limit: Int): Boolean {
        var decoded = 0
        var size = 1
        var cr = false
        var end = false

        takeWhileSize { buffer ->
            var skip = 0
            size = buffer.decodeUTF8 { ch ->
                when (ch) {
                    '\r' -> {
                        if (cr) {
                            end = true
                            return@decodeUTF8 false
                        }
                        cr = true
                        true
                    }
                    '\n' -> {
                        end = true
                        skip = 1
                        false
                    }
                    else -> {
                        if (cr) {
                            end = true
                            return@decodeUTF8 false
                        }

                        if (decoded == limit) {
                            throw BufferLimitExceededException("Too many characters in line: limit $limit exceeded")
                        }
                        decoded++
                        out.append(ch)
                        true
                    }
                }
            }

            if (skip > 0) {
                buffer.discardExact(skip)
            }

            if (end) 0 else size.coerceAtLeast(1)
        }

        if (size > 1) prematureEndOfStream(size)

        return decoded > 0 || !isEmpty
    }

    internal inline fun readDirect(block: (BufferView) -> Unit) {
        val head = head
        var before = head.readRemaining
        val buffer = if (before == 0) {
            ensureNext(head).also { before = it?.readRemaining ?: 0 }
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
        if (remaining < length * 2) throw IllegalArgumentException("Not enough bytes available ($remaining) to read $length short integers")

        var copied = 0
        takeWhile { buffer ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc == -1) throw EOFException("Unexpected EOF while reading $length bytes")
            copied += rc
            copied < length
        }
    }

    final override fun readAvailable(dst: ShortArray, offset: Int, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) return -1
        val size = minOf(remaining, length.toLong()).toInt()
        readFully(dst, offset, size)
        return size
    }

    final override fun readFully(dst: IntArray, offset: Int, length: Int) {
        if (remaining < length * 4) throw IllegalArgumentException("Not enough bytes available ($remaining) to read $length integers")

        var copied = 0
        takeWhile { buffer ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc == -1) throw EOFException("Unexpected EOF while read $length short integers")
            copied += rc
            copied < length
        }
    }

    final override fun readAvailable(dst: IntArray, offset: Int, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) return -1
        val size = minOf(remaining, length.toLong()).toInt()
        readFully(dst, offset, size)
        return size
    }

    final override fun readFully(dst: LongArray, offset: Int, length: Int) {
        if (remaining < length * 8) throw IllegalArgumentException("Not enough bytes available ($remaining) to read $length long integers")

        var copied = 0
        takeWhile { buffer ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc == -1) throw EOFException("Unexpected EOF while reading $length long integers")
            copied += rc
            copied < length
        }
    }

    final override fun readAvailable(dst: LongArray, offset: Int, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) return -1
        val size = minOf(remaining, length.toLong()).toInt()
        readFully(dst, offset, size)
        return size
    }

    final override fun readFully(dst: FloatArray, offset: Int, length: Int) {
        if (remaining < length * 4) throw IllegalArgumentException("Not enough bytes available ($remaining) to read $length float numbers")

        var copied = 0
        takeWhile { buffer ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc == -1) throw EOFException("Unexpected EOF while read $length float number")
            copied += rc
            copied < length
        }
    }

    final override fun readAvailable(dst: FloatArray, offset: Int, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) return -1
        val size = minOf(remaining, length.toLong()).toInt()
        readFully(dst, offset, size)
        return size
    }

    final override fun readFully(dst: DoubleArray, offset: Int, length: Int) {
        if (remaining < length.toLong() * 8) throw IllegalArgumentException("Not enough bytes available ($remaining) to read $length double float numbers")

        var copied = 0
        takeWhile { buffer ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc == -1) throw EOFException("Unexpected EOF while reading $length double float numbers")
            copied += rc
            copied < length
        }
    }

    final override fun readAvailable(dst: DoubleArray, offset: Int, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) return -1
        val size = minOf(remaining, length.toLong()).toInt()
        readFully(dst, offset, size)
        return size
    }

    final override fun readFully(dst: BufferView, length: Int) {
        if (remaining < length) throw IllegalArgumentException("Not enough bytes available ($remaining) to read $length bytes")
        require(length <= dst.writeRemaining) { "Not enough free space in destination buffer to write $length bytes" }

        var copied = 0
        takeWhile { buffer ->
            val rc = buffer.readAvailable(dst, length - copied)
            if (rc > 0) copied += rc
            copied < length
        }
    }

    final override fun readAvailable(dst: BufferView, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) return -1
        val size = minOf(remaining, length.toLong(), dst.writeRemaining.toLong()).toInt()
        readFully(dst, size)
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
                throw UnsupportedOperationException()
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
            isEmpty -> if (min == 0) return 0 else throw EOFException("at least $min characters required but no bytes available")
            max < min -> throw IllegalArgumentException("min should be less or equal to max but min = $min, max = $max")
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
                copied == max -> false
                rc -> true
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

    private fun prematureEndOfStreamChars(min: Int, copied: Int): Nothing = throw MalformedUTF8InputException("Premature end of stream: expected at least $min chars but had only $copied")
    private fun prematureEndOfStream(size: Int): Nothing = throw MalformedUTF8InputException("Premature end of stream: expected $size bytes")

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

            if (copied < max) size.coerceAtLeast(1) else 0
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

    private inline fun <R> readN(n: Int, block: BufferView.() -> R): R {
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

    @Deprecated("Non-public API. Do not use otherwise packet could be damaged", level = DeprecationLevel.ERROR)
    final override fun `$updateRemaining$`(remaining: Int) {
        headRemaining = remaining
    }

    @Deprecated("Non-public API", level = DeprecationLevel.ERROR)
    final override fun `$prepareRead$`(minSize: Int): BufferView? = prepareRead(minSize, head)

    @Deprecated("Non public API", level = DeprecationLevel.ERROR)
    final override fun `$ensureNext$`(current: BufferView): BufferView? = ensureNext(current)

    private fun ensureNext(current: BufferView) = ensureNext(current, BufferView.Empty)

    private tailrec fun ensureNext(current: BufferView, empty: BufferView): BufferView? {
        val next = if (current === empty) {
            doFill()
        } else {
            current.next.also { current.release(pool) } ?: doFill()
        }

        if (next == null) {
            head = empty
            headRemaining = 0
            tailRemaining = 0L
            return null
        }

        if (next.canRead()) {
            head = next
            next.byteOrder = byteOrder
            val nextRemaining = next.readRemaining
            headRemaining = nextRemaining
            tailRemaining -= nextRemaining
            return next
        } else {
            return ensureNext(next, empty)
        }
    }

    protected abstract fun fill(): BufferView?

    private fun doFill(): BufferView? {
        val chunk = fill() ?: return null
        val tail = head.findTail()
        if (tail === BufferView.Empty) {
            head = chunk
            chunk.byteOrder = byteOrder
            require(tailRemaining == 0L)
            headRemaining = chunk.readRemaining
            tailRemaining = chunk.next?.remainingAll() ?: 0L
        } else {
            tail.next = chunk
            tailRemaining += chunk.remainingAll()
        }

        return chunk
    }


    @Suppress("NOTHING_TO_INLINE")
    internal inline fun prepareRead(minSize: Int): BufferView? = prepareRead(minSize, head)

    internal tailrec fun prepareRead(minSize: Int, head: BufferView): BufferView? {
        val headSize = headRemaining
        if (headSize >= minSize) return head

        val next = head.next ?: doFill() ?: return null
        next.byteOrder = byteOrder

        if (headSize == 0) {
            if (head !== BufferView.Empty) {
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
        if (minSize > ReservedSize) minSizeIsTooBig(minSize)

        return prepareRead(minSize, head)
    }

    private fun minSizeIsTooBig(minSize: Int): Nothing {
        throw IllegalStateException("minSize of $minSize is too big (should be less than $ReservedSize")
    }

    private fun afterRead() {
        val head = head
        if (head.readRemaining == 0) {
            releaseHead(head)
        }
    }

    internal fun releaseHead(head: BufferView) {
        val next = head.next ?: BufferView.Empty
        this.head = next
        val nextRemaining = next.readRemaining
        this.headRemaining = nextRemaining
        this.tailRemaining -= nextRemaining
        head.release(pool)
    }

    companion object {
        val Empty: ByteReadPacket = ByteReadPacket(BufferView.Empty, object : NoPoolImpl<BufferView>() {
            override fun borrow() = BufferView.Empty
        })

        val ReservedSize = 8
    }
}

expect class EOFException(message: String) : Exception
