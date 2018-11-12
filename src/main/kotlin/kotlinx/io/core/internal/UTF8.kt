package kotlinx.io.core.internal

import kotlinx.io.core.*

internal inline fun IoBuffer.decodeASCII(consumer: (Char) -> Boolean): Boolean {
    for (i in 0 until readRemaining) {
        val v = readByte().toInt() and 0xff
        if (v and 0x80 != 0 || !consumer(v.toChar())) {
            pushBack(1)
            return false
        }
    }

    return true
}

@DangerousInternalIoApi
suspend fun decodeUTF8LineLoopSuspend(
    out: Appendable,
    limit: Int,
    nextChunk: suspend (Int) -> ByteReadPacketBase?
): Boolean {
    var decoded = 0
    var size = 1
    var cr = false
    var end = false

    while (!end && size != 0) {
        val chunk = nextChunk(size) ?: break
        chunk.takeWhileSize { buffer ->
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

            size = if (end) 0 else size.coerceAtLeast(1)

            size
        }
    }

    if (size > 1) prematureEndOfStreamUtf(size)
    if (cr) {
        end = true
    }

    return decoded > 0 || end
}

private fun prematureEndOfStreamUtf(size: Int): Nothing =
    throw EOFException("Premature end of stream: expected $size bytes to decode UTF-8 char")

/**
 * Decodes all the bytes to utf8 applying every character on [consumer] until or consumer return `false`.
 * If a consumer returned false then a character will be pushed back (including all surrogates will be pushed back as well)
 * and [decodeUTF8] returns -1
 * @return number of bytes required to decode incomplete utf8 character or 0 if all bytes were processed
 * or -1 if consumer rejected loop
 */
@DangerousInternalIoApi
inline fun IoBuffer.decodeUTF8(consumer: (Char) -> Boolean): Int {
    var byteCount = 0
    var value = 0
    var lastByteCount = 0

    while (canRead()) {
        val v = readByte().toInt() and 0xff
        when {
            v and 0x80 == 0 -> {
                if (byteCount != 0) malformedByteCount(byteCount)
                if (!consumer(v.toChar())) {
                    pushBack(1)
                    return -1
                }
            }
            byteCount == 0 -> {
                // first unicode byte

                var mask = 0x80
                value = v

                for (i in 1..6) { // TODO do we support 6 bytes unicode?
                    if (value and mask != 0) {
                        value = value and mask.inv()
                        mask = mask shr 1
                        byteCount++
                    } else {
                        break
                    }
                }

                lastByteCount = byteCount
                byteCount--

                if (byteCount > readRemaining) {
                    pushBack(1) // return one byte back
                    return lastByteCount
                }
            }
            else -> {
                // trailing unicode byte
                value = (value shl 6) or (v and 0x7f)
                byteCount--

                if (byteCount == 0) {
                    if (isBmpCodePoint(value)) {
                        if (!consumer(value.toChar())) {
                            pushBack(lastByteCount)
                            return -1
                        }
                    } else if (!isValidCodePoint(value)) {
                        malformedCodePoint(value)
                    } else {
                        if (!consumer(highSurrogate(value).toChar()) ||
                                !consumer(lowSurrogate(value).toChar())) {
                            pushBack(lastByteCount)
                            return -1
                        }
                    }

                    value = 0
                }
            }
        }
    }

    return 0
}

@PublishedApi
internal fun malformedByteCount(byteCount: Int): Nothing =
    throw MalformedUTF8InputException("Expected $byteCount more character bytes")

@PublishedApi
internal fun malformedCodePoint(value: Int): Nothing =
    throw IllegalArgumentException("Malformed code-point $value found")

private const val MaxCodePoint = 0x10ffff
private const val MinLowSurrogate = 0xdc00
private const val MinHighSurrogate = 0xd800
private const val MinSupplementary = 0x10000
private const val HighSurrogateMagic = MinHighSurrogate - (MinSupplementary ushr 10)

@PublishedApi
internal fun isBmpCodePoint(cp: Int) = cp ushr 16 == 0

@PublishedApi
internal fun isValidCodePoint(codePoint: Int) = codePoint <= MaxCodePoint

@PublishedApi
internal fun lowSurrogate(cp: Int) = (cp and 0x3ff) + MinLowSurrogate

@PublishedApi
internal fun highSurrogate(cp: Int) = (cp ushr 10) + HighSurrogateMagic

class MalformedUTF8InputException(message: String) : Exception(message)
