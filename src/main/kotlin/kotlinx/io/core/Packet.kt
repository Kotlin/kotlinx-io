package kotlinx.io.core

import kotlinx.io.pool.*

/**
 * Read-only immutable byte packet. Could be consumed only once however it does support [copy] that doesn't copy every byte
 * but creates a new view instead. Once packet created it should be either completely read (consumed) or released
 * via [release].
 */
class ByteReadPacket(private var head: BufferView,
                              val pool: ObjectPool<BufferView>) {

    /**
     * Number of bytes available for read
     */
    val remaining: Int
        get() = head.remainingAll().toInt() // TODO Long or Int?

    /**
     * `true` if no bytes available for read
     */
    val isEmpty: Boolean
        get() = head.isEmpty()

    /**
     * Returns a copy of the packet. The original packet and the copy could be used concurrently. Both need to be
     * either completely consumed or released via [release]
     */
    fun copy(): ByteReadPacket = ByteReadPacket(head.copyAll(), pool)

    /**
     * Release packet. After this function invocation the packet becomes empty. If it has been copied via [copy]
     * then the copy should be released as well.
     */
    fun release() {
        val head = head
        val empty = BufferView.Empty

        if (head !== empty) {
            this.head = empty
            head.releaseAll(pool)
        }
    }

    internal fun stealAll(): BufferView? {
        val head = head
        val empty = BufferView.Empty

        if (head === empty) return null
        this.head = empty
        return head
    }

    internal fun steal(): BufferView? {
        val head = head
        val next = head.next
        val empty = BufferView.Empty
        if (head === empty) return null

        this.head = next ?: empty

        return head
    }

    fun readByte() = readN(1) { readByte() }
    fun readShort() = readN(2) { readShort() }
    fun readInt() = readN(4) { readInt() }
    fun readLong() = readN(8) { readLong() }
    fun readFloat() = readN(4) { readFloat() }
    fun readDouble() = readN(8) { readDouble() }

    /**
     * Read as much bytes as possible to [dst] array
     * @return number of bytes copied
     */
    fun readAvailable(dst: ByteArray): Int = readAvailable(dst, 0, dst.size)

    /**
     * Read at most [length] bytes to [dst] array and write them at [offset]
     * @return number of bytes copied to the array
     */
    fun readAvailable(dst: ByteArray, offset: Int = 0, length: Int = dst.size): Int {
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset + length <= dst.size) { throw IllegalArgumentException() }

        return readAsMuchAsPossible(dst, offset, length, 0)
    }

    /**
     * Read exactly [length] bytes to [dst] array at specified [offset]
     */
    fun readFully(dst: ByteArray, offset: Int = 0, length: Int = dst.size) {
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

        while (!end) {
            val buffer = prepareRead(size)
            if (buffer == null) {
                if (size == 1) break
                throw MalformedUTF8InputException("Premature end of stream: expected $size bytes")
            }
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
                        afterRead()
                        return true
                    }
                    else -> {
                        if (cr) {
                            end = true
                            return@decodeUTF8 false
                        }

                        if (decoded == limit) {
                            afterRead()
                            throw BufferLimitExceededException("Too many characters in line: limit $limit exceeded")
                        }
                        decoded++
                        out.append(ch)
                        true
                    }
                }
            }

            if (size == 0 || end) {
                afterRead()
                size = 1
            }
        }

        return decoded > 0 || !isEmpty
    }

    internal inline fun readDirect(block: (BufferView) -> Unit) {
        val current = head

        if (current !== BufferView.Empty) {
            block(current)
            if (!current.canRead()) {
                releaseHead(current)
            }
        }
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

        while (copied < max) {
            val buffer = prepareRead(1)
            if (buffer == null) {
                if (copied >= min) break
                throw MalformedUTF8InputException("Premature end of stream: expected at least $min chars but had only $copied")
            }

            val rc = buffer.decodeASCII {
                if (copied == max) false
                else {
                    out.append(it)
                    copied++
                    true
                }
            }

            if (rc) {
                afterRead()
            } else if (copied == max) {
                break
            } else {
                // it is safe here to have negative min - copied
                // and it will be never negative max - copied
                return copied + readUtf8(out, min - copied, max - copied)
            }
        }

        return when {
            copied > 0 -> copied
            head.isEmpty() -> -1
            else -> 0
        }
    }

    private fun readUtf8(out: Appendable, min: Int, max: Int): Int {
        var size = 1
        var copied = 0

        while (copied < max) {
            val buffer = prepareRead(size)
            if (buffer == null) {
                if (copied >= min) break
                throw MalformedUTF8InputException("Premature end of stream: expected $size bytes")
            }

            size = buffer.decodeUTF8 {
                if (copied == max) false
                else {
                    out.append(it)
                    copied++
                    true
                }
            }

            if (size == 0) {
                afterRead()
                size = 1
            }
        }

        return copied
    }

    private tailrec fun discardAsMuchAsPossible(n: Int, skipped: Int): Int {
        if (n == 0) return skipped
        val current = prepareRead(1) ?: return skipped
        val size = minOf(current.readRemaining, n)
        current.discardExact(size)
        afterRead()

        return discardAsMuchAsPossible(n - size, skipped + size)
    }

    private tailrec fun readAsMuchAsPossible(array: ByteArray, offset: Int, length: Int, copied: Int): Int {
        if (length == 0) return copied
        val current = prepareRead(1) ?: return copied
        val size = minOf(length, current.readRemaining)

        current.read(array, offset, size)
        return if (size != length || current.readRemaining == 0) {
            afterRead()
            readAsMuchAsPossible(array, offset + size, length - size, copied + size)
        } else {
            copied + size
        }
    }

    private inline fun <R> readN(n: Int, block: BufferView.() -> R): R {
        val bb = prepareRead(n) ?: throw EOFException("Not enough data in packet to read $n byte(s)")
        val rc = block(bb)
        afterRead()
        return rc
    }

    internal tailrec fun prepareRead(minSize: Int): BufferView? {
        val head = head

        val headSize = head.readRemaining
        if (headSize >= minSize) return head
        val next = head.next ?: return null

        head.writeBufferAppend(next, minSize - headSize)
        if (next.readRemaining == 0) {
            head.next = next.next
            next.release(pool)
        }

        if (head.readRemaining >= minSize) return head
        if (minSize > ReservedSize) throw IllegalStateException("minSize of $minSize is too big (should be less than $ReservedSize")

        return prepareRead(minSize)
    }

    private fun afterRead() {
        val head = head
        if (head.readRemaining == 0) {
            releaseHead(head)
        }
    }

    internal fun releaseHead(head: BufferView) {
        val next = head.next
        this.head = next ?: BufferView.Empty
        head.release(pool)
    }

    companion object {
        val Empty = ByteReadPacket(BufferView.Empty, object: NoPoolImpl<BufferView>() {
            override fun borrow() = BufferView.Empty
        })

        val ReservedSize = 8
    }
}

expect class EOFException(message: String) : Exception
