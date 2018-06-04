package kotlinx.io.core

import kotlinx.io.charsets.*
import kotlinx.io.core.internal.*

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

    return decoded > 0 || !endOfInput
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
fun Input.readText(out: Appendable, decoder: CharsetDecoder, max: Int = Int.MAX_VALUE): Int {
    return decoder.decode(this, out, max)
}

/**
 * Reads at most [max] characters decoding bytes with specified [charset]. Extra character bytes will remain unconsumed
 * @return number of characters copied to [out]
 */
fun Input.readText(out: Appendable, charset: Charset = Charsets.UTF_8, max: Int = Int.MAX_VALUE): Int {
    return readText(out, charset.newDecoder(), max)
}

/**
 * Reads at most [max] characters decoding bytes with specified [decoder]. Extra character bytes will remain unconsumed
 * @return a decoded string
 */
fun Input.readText(decoder: CharsetDecoder, max: Int = Int.MAX_VALUE): String {
    return buildString(minOf(sizeEstimate(), max.toLong()).toInt()) {
        readText(this, decoder, max)
    }
}

/**
 * Reads at most [max] characters decoding bytes with specified [charset]. Extra character bytes will remain unconsumed
 * @return a decoded string
 */
fun Input.readText(charset: Charset = Charsets.UTF_8, max: Int = Int.MAX_VALUE): String {
    return readText(charset.newDecoder(), max)
}

fun Input.readTextExact(charset: Charset = Charsets.UTF_8, n: Int): String {
    val s = readText(charset.newDecoder(), n)
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
