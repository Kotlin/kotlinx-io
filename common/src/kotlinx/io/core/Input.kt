package kotlinx.io.core

import kotlinx.io.core.internal.*

/**
 * Shouldn't be implemented directly. Inherit [AbstractInput] instead.
 */
expect interface Input : Closeable {
    var byteOrder: ByteOrder
    val endOfInput: Boolean

    fun readByte(): Byte
    fun readShort(): Short
    fun readInt(): Int
    fun readLong(): Long
    fun readFloat(): Float
    fun readDouble(): Double

    fun readFully(dst: ByteArray, offset: Int, length: Int)
    fun readFully(dst: ShortArray, offset: Int, length: Int)
    fun readFully(dst: IntArray, offset: Int, length: Int)
    fun readFully(dst: LongArray, offset: Int, length: Int)
    fun readFully(dst: FloatArray, offset: Int, length: Int)
    fun readFully(dst: DoubleArray, offset: Int, length: Int)
    fun readFully(dst: IoBuffer, length: Int = dst.writeRemaining)

    fun readAvailable(dst: ByteArray, offset: Int, length: Int): Int
    fun readAvailable(dst: ShortArray, offset: Int, length: Int): Int
    fun readAvailable(dst: IntArray, offset: Int, length: Int): Int
    fun readAvailable(dst: LongArray, offset: Int, length: Int): Int
    fun readAvailable(dst: FloatArray, offset: Int, length: Int): Int
    fun readAvailable(dst: DoubleArray, offset: Int, length: Int): Int
    fun readAvailable(dst: IoBuffer, length: Int): Int

    /*
     * Returns next byte (unsigned) or `-1` if no more bytes available
     */
    fun tryPeek(): Int

    /**
     * Copy available bytes to the specified [buffer] but keep them available.
     * If the underlying implementation could trigger
     * bytes population from the underlying source and block until any bytes available
     *
     * Very similar to [readAvailable] but don't discard copied bytes.
     *
     * @return number of bytes were copied
     */
    fun peekTo(buffer: IoBuffer): Int

    fun discard(n: Long): Long

    override fun close()
}


@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: ByteArray, offset: Int = 0, length: Int = dst.size - offset) {
    return readFully(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: ShortArray, offset: Int = 0, length: Int = dst.size - offset) {
    return readFully(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: IntArray, offset: Int = 0, length: Int = dst.size - offset) {
    return readFully(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: LongArray, offset: Int = 0, length: Int = dst.size - offset) {
    return readFully(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: FloatArray, offset: Int = 0, length: Int = dst.size - offset) {
    return readFully(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: DoubleArray, offset: Int = 0, length: Int = dst.size - offset) {
    return readFully(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: IoBuffer, length: Int = dst.writeRemaining) {
    return readFully(dst, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: ByteArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    return readAvailable(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: ShortArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    return readAvailable(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: IntArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    return readAvailable(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: LongArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    return readAvailable(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: FloatArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    return readAvailable(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: DoubleArray, offset: Int = 0, length: Int = dst.size - offset): Int {
    return readAvailable(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: IoBuffer, length: Int = dst.writeRemaining): Int {
    return readAvailable(dst, length)
}

fun Input.discard(): Long {
    return discard(Long.MAX_VALUE)
}

fun Input.discardExact(n: Long) {
    val discarded = discard(n)
    if (discarded != n) {
        throw IllegalStateException("Only $discarded bytes were discarded of $n requested")
    }
}

fun Input.discardExact(n: Int) {
    discardExact(n.toLong())
}

/**
 * Invoke [block] function for every chunk until end of input or [block] function return `false`
 * [block] function returns `true` to request more chunks or `false` to stop loop
 *
 * It is not guaranteed that every chunk will have fixed size but it will be never empty.
 * [block] function should never release provided buffer and should not write to it otherwise an undefined behaviour
 * could be observed
 */
inline fun Input.takeWhile(block: (IoBuffer) -> Boolean) {
    var release = true
    var current = prepareReadFirstHead(1) ?: return

    try {
        do {
            if (!block(current)) {
                break
            }
            release = false
            val next = prepareReadNextHead(current) ?: break
            current = next
            release = true
        } while (true)
    } finally {
        if (release) {
            completeReadHead(current)
        }
    }
}

/**
 * Invoke [block] function for every chunk until end of input or [block] function return zero
 * [block] function returns number of bytes required to read next primitive and shouldn't require too many bytes at once
 * otherwise it could fail with an exception.
 * It is not guaranteed that every chunk will have fixed size but it will be always at least requested bytes length.
 * [block] function should never release provided buffer and should not write to it otherwise an undefined behaviour
 * could be observed
 */
inline fun Input.takeWhileSize(initialSize: Int = 1, block: (IoBuffer) -> Int) {
    var release = true
    var current = prepareReadFirstHead(initialSize) ?: return
    var size = initialSize

    try {
        do {
            val before = current.readRemaining
            val after: Int

            if (before >= size) {
                try {
                    size = block(current)
                } finally {
                    after = current.readRemaining
                }
            } else {
                after = before
            }

            release = false

            val next = when {
                after == 0 -> prepareReadNextHead(current)
                after < size || current.endGap < IoBuffer.ReservedSize -> {
                    completeReadHead(current)
                    prepareReadFirstHead(size)
                }
                else -> current
            }

            if (next == null) {
                release = false
                break
            }

            current = next
            release = true
        } while (size > 0)
    } finally {
        if (release) {
            completeReadHead(current)
        }
    }
}

@ExperimentalIoApi
fun Input.peekCharUtf8(): Char {
    val rc = tryPeek()
    if (rc and 0x80 == 0) return rc.toChar()
    if (rc == -1) throw EOFException("Failed to peek a char: end of input")

    return peekCharUtf8Impl(rc)
}

private fun Input.peekCharUtf8Impl(first: Int): Char {
    var rc = '?'
    var found = false

    takeWhileSize(byteCountUtf8(first)) {
        it.decodeUTF8 { ch ->
            found = true
            rc = ch
            false
        }
    }

    if (!found) {
        throw MalformedUTF8InputException("No UTF-8 character found")
    }

    return rc
}
