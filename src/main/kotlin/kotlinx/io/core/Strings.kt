package kotlinx.io.core

import kotlinx.io.charsets.*
import kotlinx.io.core.internal.*

@Suppress("NOTHING_TO_INLINE")
inline fun String.toByteArray(charset: Charset = Charsets.UTF_8): ByteArray = charset.newEncoder().encodeToByteArray(this, 0, length)

expect fun String(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size, charset: Charset = Charsets.UTF_8): String

/**
 * Read a string line considering optionally specified [estimate] but up to optional [limit] characters length
 * (does fail once limit exceeded) or return `null` if the packet is empty
 */
fun ByteReadPacket.readUTF8Line(estimate: Int = 16, limit: Int = Int.MAX_VALUE): String? {
    if (isEmpty) return null
    val sb = StringBuilder(estimate)
    return if (readUTF8LineTo(sb, limit)) sb.toString() else null
}

/**
 * Read a string line considering optionally specified [estimate] but up to optional [limit] characters length
 * (does fail once limit exceeded) or return `null` if the packet is empty
 */
fun Input.readUTF8Line(estimate: Int = 16, limit: Int = Int.MAX_VALUE): String? {
    val sb = StringBuilder(estimate)
    return if (readUTF8LineTo(sb, limit)) sb.toString() else null
}

/**
 * Read UTF-8 line and append all line characters to [out] except line endings. Does support CR, LF and CR+LF
 * @return `true` if some characters were appended or line ending reached (empty line) or `false` if packet
 * if empty
 */
fun Input.readUTF8LineTo(out: Appendable, limit: Int): Boolean {
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

                    if (decoded == limit) bufferLimitExceeded(limit)
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

    return decoded > 0 || !endOfInput
}

/**
 * Reads UTF-8 characters until one of the specified [delimiters] found, [limit] exceeded or end of stream encountered
 *
 * @throws BufferLimitExceededException
 * @returns a string of characters read before delimiter
 */
fun Input.readUTF8UntilDelimiter(delimiters: String, limit: Int = Int.MAX_VALUE): String {
    return buildString {
        readUTF8UntilDelimiterTo(this, delimiters, limit)
    }
}

/**
 * Reads UTF-8 characters to [out] buffer until one of the specified [delimiters] found, [limit] exceeded
 * or end of stream encountered
 *
 * @throws BufferLimitExceededException
 * @returns number of characters copied (possibly zero)
 */
fun Input.readUTF8UntilDelimiterTo(out: Appendable, delimiters: String, limit: Int = Int.MAX_VALUE): Int {
    var decoded = 0
    var delimiter = false

    takeWhile { buffer ->
        buffer.decodeASCII { ch ->
            if (ch in delimiters) {
                delimiter = true
                false
            } else {
                if (decoded == limit) bufferLimitExceeded(limit)
                decoded++
                out.append(ch)
                true
            }
        }
    }

    if (!delimiter) {
        decoded = readUTF8UntilDelimiterToSlow(out, delimiters, limit, decoded)
    }

    return decoded
}

private fun Input.readUTF8UntilDelimiterToSlow(out: Appendable, delimiters: String, limit: Int, decoded0: Int): Int {
    var decoded = decoded0
    var size = 1

    takeWhileSize { buffer ->
        size = buffer.decodeUTF8 { ch ->
            if (ch in delimiters) {
                false
            } else {
                if (decoded == limit) {
                    bufferLimitExceeded(limit)
                }
                decoded++
                out.append(ch)
                true
            }
        }

        size = if (size == -1) 0 else size.coerceAtLeast(1)
        size
    }

    if (size > 1) prematureEndOfStream(size)

    return decoded
}

/**
 * Reads UTF-8 characters to [out] buffer until one of the specified [delimiters] found, [limit] exceeded
 * or end of stream encountered
 *
 * @throws BufferLimitExceededException
 * @returns number of characters copied (possibly zero)
 */
fun Input.readUTF8UntilDelimiterTo(out: BytePacketBuilderBase, delimiters: String, limit: Int = Int.MAX_VALUE): Int {
    var decoded = 0
    var delimiter = false

    takeWhile { buffer ->
        val before = buffer.readRemaining

        val rc = buffer.decodeASCII { ch ->
            if (ch in delimiters) {
                delimiter = true
                false
            } else {
                if (decoded == limit) bufferLimitExceeded(limit)
                decoded++
                true
            }
        }

        val delta = before - buffer.readRemaining
        if (delta > 0) {
            buffer.pushBack(delta)
            out.writeFully(buffer, delta)
        }

        rc
    }

    if (!delimiter && !endOfInput) {
        decoded = readUTF8UntilDelimiterToSlow(out, delimiters, limit, decoded)
    }

    return decoded
}

private fun Input.readUTF8UntilDelimiterToSlow(out: BytePacketBuilderBase, delimiters: String, limit: Int, decoded0: Int): Int {
    var decoded = decoded0
    var size = 1

    takeWhileSize { buffer ->
        val before = buffer.readRemaining

        size = buffer.decodeUTF8 { ch ->
            if (ch in delimiters) {
                false
            } else {
                if (decoded == limit) {
                    bufferLimitExceeded(limit)
                }
                decoded++
                true
            }
        }

        val delta = before - buffer.readRemaining
        if (delta > 0) {
            buffer.pushBack(delta)
            out.writeFully(buffer, delta)
        }

        size = if (size == -1) 0 else size.coerceAtLeast(1)
        size
    }

    if (size > 1) prematureEndOfStream(size)

    return decoded
}


private fun bufferLimitExceeded(limit: Int): Nothing {
    throw BufferLimitExceededException("Too many characters before delimiter: limit $limit exceeded")
}

private fun prematureEndOfStream(size: Int): Nothing = throw MalformedUTF8InputException("Premature end of stream: expected $size bytes")

/**
 * Read exactly [n] bytes (consumes all remaining if [n] is not specified). Does fail if not enough bytes remaining
 */
fun ByteReadPacket.readBytes(n: Int = remaining.coerceAtMostMaxInt()): ByteArray = ByteArray(n).also { readFully(it, 0, n) }

/**
 * Reads exactly [n] bytes from the input or fails if not enough bytes available.
 */
fun Input.readBytes(n: Int): ByteArray = readBytesOf(n, n)

/**
 * Reads all remaining bytes from the input
 */
fun Input.readBytes(): ByteArray = readBytesOf()

/**
 * Reads at least [min] but no more than [max] bytes from the input to a new byte array
 * @throws EOFException if not enough bytes available to get [min] bytes
 */
fun Input.readBytesOf(min: Int = 0, max: Int = Int.MAX_VALUE): ByteArray = if (min == max) {
    ByteArray(min).also { readFully(it, 0, min) }
} else {
    var array = ByteArray(max.toLong().coerceAtMost(sizeEstimate()).coerceAtLeast(min.toLong()).toInt())
    var size = 0

    while (size < max) {
        val partSize = minOf(max, array.size) - size
        val rc = readAvailable(array, size, partSize)
        if (rc <= 0) break
        size += rc
        if (array.size == size) {
            array = array.copyOf(size * 2)
        }
    }

    if (size < min) throw EOFException("Not enough bytes available to read $min bytes: ${min - size} more required")

    if (size == array.size) array else array.copyOf(size)
}

/**
 * Reads at most [max] characters decoding bytes with specified [decoder]. Extra character bytes will remain unconsumed
 * @return number of characters copied to [out]
 */
@Deprecated("Use CharsetDecoder.decode instead",
        ReplaceWith("decoder.decode(this, out, max)", "kotlinx.io.charsets.decode"))
fun Input.readText(out: Appendable, decoder: CharsetDecoder, max: Int = Int.MAX_VALUE): Int {
    return decoder.decode(this, out, max)
}

/**
 * Reads at most [max] characters decoding bytes with specified [charset]. Extra character bytes will remain unconsumed
 * @return number of characters copied to [out]
 */
fun Input.readText(out: Appendable, charset: Charset = Charsets.UTF_8, max: Int = Int.MAX_VALUE): Int {
    return charset.newDecoder().decode(this, out, max)
}

/**
 * Reads at most [max] characters decoding bytes with specified [decoder]. Extra character bytes will remain unconsumed
 * @return a decoded string
 */
@Deprecated("Use CharetDecoder.decode instead",
        ReplaceWith("decoder.decode(this, max)", "kotlinx.io.charsets.decode"))
fun Input.readText(decoder: CharsetDecoder, max: Int = Int.MAX_VALUE): String {
    return decoder.decode(this, max)
}

/**
 * Reads at most [max] characters decoding bytes with specified [charset]. Extra character bytes will remain unconsumed
 * @return a decoded string
 */
fun Input.readText(charset: Charset = Charsets.UTF_8, max: Int = Int.MAX_VALUE): String {
    return charset.newDecoder().decode(this, max)
}

fun Input.readTextExact(charset: Charset = Charsets.UTF_8, n: Int): String {
    val s = readText(charset, n)
    if (s.length < n) throw EOFException("Not enough data available to read $n characters")
    return s
}

/**
 * Writes [text] characters in range \[[fromIndex] .. [toIndex]) with the specified [encoder]
 */
fun BytePacketBuilder.writeText(text: CharSequence, fromIndex: Int = 0, toIndex: Int = text.length, encoder: CharsetEncoder) {
    encoder.encode(text, fromIndex, toIndex, this)
}

/**
 * Writes [text] characters in range \[[fromIndex] .. [toIndex]) with the specified [charset]
 */
fun BytePacketBuilder.writeText(text: CharSequence, fromIndex: Int = 0, toIndex: Int = text.length, charset: Charset = Charsets.UTF_8) {
    writeText(text, fromIndex, toIndex, charset.newEncoder())
}
