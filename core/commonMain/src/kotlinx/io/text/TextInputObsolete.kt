package kotlinx.io.text

import kotlinx.io.*


/**
 * Read UTF-8 string until [delimiter] to [output].
 *
 * @throws MalformedInputException if decoder fail to recognize charset.
 */
public fun Input.readUtf8StringUntilDelimiterTo(output: Appendable, delimiter: Char): Int = decodeUtf8Chars {
    if (it == delimiter) {
        return@decodeUtf8Chars false
    }
    output.append(it)
    true
}

/**
 * Read UTF-8 string until any of the [delimiters] to [output]. The delimiter will be consumed from [Input]
 * @throws MalformedInputException if decoder fail to recognize charset.
 */
public fun Input.readUtf8StringUntilDelimitersTo(output: Appendable, delimiters: String): Int = decodeUtf8Chars {
    if (it in delimiters) {
        return@decodeUtf8Chars false
    }

    output.append(it)
    true
}

/**
 * Read fixed [length] UTF-8 string to [output].
 *
 * @throws MalformedInputException if decoder fail to recognize charset.
 */
public fun Input.readUtf8StringTo(output: Appendable, length: Int): Int {
    var remaining = length
    return decodeUtf8Chars {
        output.append(it)
        --remaining > 0
    }
}

/**
 * Read UTF-8 string to [output] until CRLF("\r\n") chars.
 *
 * @throws MalformedInputException if decoder fail to recognize charset.
 */
public fun Input.readUtf8LineTo(output: Appendable, limit: Int = Int.MAX_VALUE) {
    var remaining = limit
    // TODO: consumes char after lonely CR
    var seenCR = false
    decodeUtf8Chars {
        if (it == '\r') {
            seenCR = true
            return@decodeUtf8Chars true // continue & skip
        }

        if (it == '\n') {
            return@decodeUtf8Chars false // stop & skip
        } else if (seenCR) {
            return@decodeUtf8Chars false
        } // lonely CR, stop & skip

        output.append(it)
        remaining--
        remaining > 0
    }
}

/**
 * Read fixed [length] UTF-8 string.
 *
 * @throws MalformedInputException if decoder fail to recognize charset.
 */
public fun Input.readUtf8String(length: Int): String = buildString(length) {
    readUtf8StringTo(this, length)
}

/**
 * Read UTF-8 string until [delimiter].
 *
 * @throws MalformedInputException if decoder fail to recognize charset.
 */
public fun Input.readUtf8StringUntilDelimiter(delimiter: Char): String = buildString {
    readUtf8StringUntilDelimiterTo(this, delimiter)
}

/**
 * Read UTF-8 string until any of the [delimiters].
 *
 * @throws MalformedInputException if decoder fail to recognize charset.
 */
public fun Input.readUtf8StringUntilDelimiters(delimiters: String): String = buildString {
    readUtf8StringUntilDelimitersTo(this, delimiters)
}

/**
 * Feed [consumer] with chars from [Input]. [consumer] returns should we continue to decode.
 *
 * @throws MalformedInputException if decoder fail to recognize charset.
 */
private inline fun Input.decodeUtf8Chars(consumer: (Char) -> Boolean): Int {
    var byteCount = 0
    var value = 0
    var state = STATE_UTF_8
    var count = 0

    while (state != STATE_FINISH && !eof()) {
        readBufferRange { buffer, startOffset, endOffset ->
            for (offset in startOffset until endOffset) {
                val byte = buffer.loadByteAt(offset).toInt() and 0xff
                when {
                    byte and 0x80 == 0 -> { // ASCII
                        if (byteCount != 0)
                            malformedInput(value)
                        if (!consumer(byte.toChar())) {
                            state = STATE_FINISH
                            return@readBufferRange offset + 1
                        }
                        count++
                    }
                    byteCount == 0 -> {
                        // first unicode byte
                        when {
                            byte < 0x80 -> {
                                if (!consumer(byte.toChar())) {
                                    state = STATE_FINISH
                                    return@readBufferRange offset + 1
                                }
                                count++
                            }
                            byte < 0xC0 -> {
                                byteCount = 0
                                value = byte and 0x7F
                            }
                            byte < 0xE0 -> {
                                byteCount = 1
                                value = byte and 0x3F
                            }
                            byte < 0xF0 -> {
                                byteCount = 2
                                value = byte and 0x1F
                            }
                            byte < 0xF8 -> {
                                byteCount = 3
                                value = byte and 0xF
                            }
                            byte < 0xFC -> {
                                byteCount = 4
                                value = byte and 0x7
                            }
                            byte < 0xFE -> {
                                byteCount = 5
                                value = byte and 0x3
                            }
                        }

                    }
                    else -> {
                        // trailing unicode byte
                        value = (value shl 6) or (byte and 0x7f)
                        byteCount--

                        if (byteCount == 0) {
                            val more = when {
                                value ushr 16 == 0 -> {
                                    if (consumer(value.toChar())) {
                                        count++
                                        true
                                    } else false
                                }
                                else -> {
                                    if (value > MaxCodePoint)
                                        malformedInput(value)

                                    val high = highSurrogate(value).toChar()
                                    val low = lowSurrogate(value).toChar()
                                    if (consumer(high)) {
                                        count++
                                        if (consumer(low)) {
                                            count++
                                            true
                                        } else false
                                    } else false
                                }
                            }
                            if (!more) {
                                state = STATE_FINISH
                                return@readBufferRange offset + 1
                            }

                            value = 0
                        }
                    }
                }
            }
            endOffset
        }
    }
    return count
}

/**
 * Inline depth optimisation
 */
private fun malformedInput(codePoint: Int): Nothing {
    throw MalformedInputException("Malformed Utf8 input, current code point $codePoint")
}

@Suppress("NOTHING_TO_INLINE")
private inline fun lowSurrogate(codePoint: Int): Int = (codePoint and 0x3ff) + MinLowSurrogate

@Suppress("NOTHING_TO_INLINE")
private inline fun highSurrogate(codePoint: Int): Int = (codePoint ushr 10) + HighSurrogateMagic

private const val MaxCodePoint = 0x10ffff
internal const val MinLowSurrogate = 0xdc00
private const val MinHighSurrogate = 0xd800
private const val MinSupplementary = 0x10000
internal const val HighSurrogateMagic = MinHighSurrogate - (MinSupplementary ushr 10)

// Alternative implementation, slower x1.5
// Based on https://bjoern.hoehrmann.de/utf-8/decoder/dfa/
// 364 ints

private val Utf8StateMachine = intArrayOf(
    // types
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
    8, 8, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
    10, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 3, 3, 11, 6, 6, 6, 5, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,

    // Transitions
    0, 12, 24, 36, 60, 96, 84, 12, 12, 12, 48, 72, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
    12, 0, 12, 12, 12, 12, 12, 0, 12, 0, 12, 12, 12, 24, 12, 12, 12, 12, 12, 24, 12, 24, 12, 12,
    12, 12, 12, 12, 12, 12, 12, 24, 12, 12, 12, 12, 12, 24, 12, 12, 12, 12, 12, 12, 12, 24, 12, 12,
    12, 12, 12, 12, 12, 12, 12, 36, 12, 36, 12, 12, 12, 36, 12, 12, 12, 12, 12, 36, 12, 36, 12, 12,
    12, 36, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12

/*
    // filler to 512, unused
    , 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
*/
)

internal const val STATE_FINISH = -2
//private const val Utf8_STATE_ASCII = -1
internal const val STATE_UTF_8 = 0
internal const val STATE_REJECT = 1

private inline fun Input.decodeUtf8(consumer: (Int) -> Boolean) {
    val stateMachine = Utf8StateMachine
    var state = STATE_UTF_8
    var codePoint = 0

    while (state != STATE_FINISH) {
        readBufferRange { buffer, startOffset, endOffset ->
            for (index in startOffset until endOffset) {
                val byte = buffer.loadByteAt(index).toInt() and 0xff

                val type = stateMachine[byte]
                codePoint = if (state == STATE_UTF_8)
                    (0xff ushr type) and byte
                else
                    (byte and 0x3f) or (codePoint shl 6)
                state = stateMachine[256 + state + type]

                // TODO: Attempt to recover from bad states
                when (state) {
                    STATE_UTF_8 -> when {
                        codePoint <= MaxCodePoint -> {
                            if (!consumer(codePoint)) {
                                state = STATE_FINISH // signal to exit loop
                                // must return consumed bytes for Input positions to be updated in readBuffer
                                return@readBufferRange index - startOffset
                            }
                        }
                        else -> malformedInput(codePoint)
                    }
                    STATE_REJECT -> malformedInput(codePoint)
                    else -> {
                        /* need more bytes to read the code point */
                    }
                }
            }
            endOffset - startOffset
        }
    }
}

private inline fun Input.decodeUtf8CharsAlt(consumer: (Char) -> Boolean) {
    decodeUtf8 { codePoint ->
        when {
            codePoint ushr 16 == 0 -> consumer(codePoint.toChar())
            else -> {
                val high = highSurrogate(codePoint).toChar()
                val low = lowSurrogate(codePoint).toChar()
                consumer(high) && consumer(low)
            }
        }
    }
}