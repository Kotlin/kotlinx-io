package kotlinx.io.core

import kotlinx.io.charsets.*

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
 * Read exactly [n] bytes (consumes all remaining if [n] is not specified). Does fail if not enough bytes remaining
 */
fun ByteReadPacket.readBytes(n: Int = remaining.coerceAtMostMaxInt()): ByteArray = ByteArray(n).also { readFully(it, 0, n) }

/**
 * Reads at most [max] characters decoding bytes with specified [decoder]. Extra character bytes will remain unconsumed
 * @return number of characters copied to [out]
 */
fun ByteReadPacket.readText(out: Appendable, decoder: CharsetDecoder, max: Int = Int.MAX_VALUE): Int {
    return decoder.decode(this, out, max)
}

/**
 * Reads at most [max] characters decoding bytes with specified [charset]. Extra character bytes will remain unconsumed
 * @return number of characters copied to [out]
 */
fun ByteReadPacket.readText(out: Appendable, charset: Charset = Charsets.UTF_8, max: Int = Int.MAX_VALUE): Int {
    return readText(out, charset.newDecoder(), max)
}

/**
 * Reads at most [max] characters decoding bytes with specified [decoder]. Extra character bytes will remain unconsumed
 * @return a decoded string
 */
fun ByteReadPacket.readText(decoder: CharsetDecoder, max: Int = Int.MAX_VALUE): String {
    return buildString(minOf(remaining, max.toLong()).toInt()) {
        readText(this, decoder, max)
    }
}

/**
 * Reads at most [max] characters decoding bytes with specified [charset]. Extra character bytes will remain unconsumed
 * @return a decoded string
 */
fun ByteReadPacket.readText(charset: Charset = Charsets.UTF_8, max: Int = Int.MAX_VALUE): String {
    return readText(charset.newDecoder(), max)
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
