package kotlinx.io.core

import kotlinx.io.core.internal.*

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

    fun discard(n: Long): Long
    override fun close()

    @DangerousInternalIoApi
    fun `$updateRemaining$`(remaining: Int)

    @DangerousInternalIoApi
    fun `$ensureNext$`(current: IoBuffer): IoBuffer?

    @DangerousInternalIoApi
    fun `$prepareRead$`(minSize: Int): IoBuffer?
}


@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: ByteArray, offset: Int = 0, length: Int = dst.size) {
    return readFully(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: ShortArray, offset: Int = 0, length: Int = dst.size) {
    return readFully(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: IntArray, offset: Int = 0, length: Int = dst.size) {
    return readFully(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: LongArray, offset: Int = 0, length: Int = dst.size) {
    return readFully(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: FloatArray, offset: Int = 0, length: Int = dst.size) {
    return readFully(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: DoubleArray, offset: Int = 0, length: Int = dst.size) {
    return readFully(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readFully(dst: IoBuffer, length: Int = dst.writeRemaining) {
    return readFully(dst, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: ByteArray, offset: Int = 0, length: Int = dst.size): Int {
    return readAvailable(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: ShortArray, offset: Int = 0, length: Int = dst.size): Int {
    return readAvailable(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: IntArray, offset: Int = 0, length: Int = dst.size): Int {
    return readAvailable(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: LongArray, offset: Int = 0, length: Int = dst.size): Int {
    return readAvailable(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: FloatArray, offset: Int = 0, length: Int = dst.size): Int {
    return readAvailable(dst, offset, length)
}

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Input.readAvailable(dst: DoubleArray, offset: Int = 0, length: Int = dst.size): Int {
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
    var current = @Suppress("DEPRECATION_ERROR") `$prepareRead$`(1) ?: return

    do {
        val continueFlag = block(current)
        val after = current.readRemaining

        if (after == 0) {
            current = @Suppress("DEPRECATION_ERROR") `$ensureNext$`(current) ?: break
        } else if (!continueFlag) {
            @Suppress("DEPRECATION_ERROR") `$updateRemaining$`(after)
            break
        }
    } while (true)
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
    var current = @Suppress("DEPRECATION_ERROR") `$prepareRead$`(1) ?: return
    var size = initialSize

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

        if (after == 0) {
            current = @Suppress("DEPRECATION_ERROR") `$ensureNext$`(current) ?: break
        } else if (after < size) {
            @Suppress("DEPRECATION_ERROR") `$updateRemaining$`(after)
            current = @Suppress("DEPRECATION_ERROR") `$prepareRead$`(size) ?: break
        } else {
            @Suppress("DEPRECATION_ERROR") `$updateRemaining$`(after)
        }
    } while (size > 0)
}

