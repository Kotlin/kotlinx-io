package kotlinx.io

import kotlinx.io.text.*

// Based on https://bjoern.hoehrmann.de/utf-8/decoder/dfa/
// 364 ints

private val utf8StateMachine = intArrayOf(
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
    12, 36, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,

    // filler to 512, unused
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
)

private const val UTF8_STATE_ACCEPT = 0
private const val UTF8_STATE_REJECT = 1

private inline fun decodeUTF8Byte(
    stateMachine: IntArray,
    byte: Int,
    state: Int,
    code: Int,
    update: (newState: Int, newCode: Int) -> Unit
) {
    val type = stateMachine[byte]
    val newCode = if (state == UTF8_STATE_ACCEPT)
        (0xff ushr type) and byte
    else
        (byte and 0x3f) or (code shl 6)

    val newState = stateMachine[256 + state + type]
    update(newState, newCode)
}

private inline fun Input.decodeUTF8(consumer: (Int) -> Boolean) {
    val stateMachine = utf8StateMachine
    var state = UTF8_STATE_ACCEPT
    var codePoint = 0

    while (state >= 0) {
        readBuffer2 { buffer, startOffset, endOffset ->
            for (index in startOffset until endOffset) {
                val byte = buffer.loadByteAt(index).toInt() and 0xff

                decodeUTF8Byte(stateMachine, byte, state, codePoint) { s, c -> state = s; codePoint = c }

                // TODO: Attempt to recover from bad states
                when (state) {
                    UTF8_STATE_ACCEPT -> when {
                        codePoint <= MaxCodePoint -> {
                            if (!consumer(codePoint)) {
                                state = -1 // signal to exit loop
                                // must return consumed bytes for Input positions to be updated in readBuffer
                                return@readBuffer2 index - startOffset
                            }
                        }
                        else -> malformedInput(codePoint)
                    }
                    UTF8_STATE_REJECT -> malformedInput(codePoint)
                    else -> {
                        /* need more bytes to read the code point */
                    }
                }
            }
            endOffset - startOffset
        }
    }
}

private inline fun Input.decodeUTF8Chars(consumer: (Char) -> Boolean) {
    decodeUTF8 { codePoint ->
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

fun Input.readUTF8StringUntilDelimiterTo(stringBuilder: StringBuilder, delimiter: Char) =
    decodeUTF8Chars {
        stringBuilder.append(it)
        it != delimiter
    }

fun Input.readUTF8StringUntilDelimiterTo(out: Appendable, delimiter: Char) =
    decodeUTF8Chars {
        out.append(it)
        it != delimiter
    }

fun Input.readUTF8StringTo(out: StringBuilder, length: Int) {
    var consumed = 0
    decodeUTF8Chars {
        out.append(it)
        ++consumed < length
    }
}

fun Input.readUTF8StringTo(out: Appendable, length: Int) {
    var consumed = 0
    decodeUTF8Chars {
        out.append(it)
        ++consumed < length
    }
}

fun Input.readUTF8String(length: Int): String = buildString {
    readUTF8StringTo(this, length)
}

fun Input.readUTF8StringUntilDelimiter(delimiter: Char) = buildString {
    readUTF8StringUntilDelimiterTo(this, delimiter)
}

/**
 * Inline depth optimisation
 */
private fun malformedInput(codePoint: Int): Nothing {
    throw MalformedInputException("Malformed UTF8 input, current code point $codePoint")
}

@Suppress("NOTHING_TO_INLINE")
private inline fun lowSurrogate(codePoint: Int) = (codePoint and 0x3ff) + MinLowSurrogate

@Suppress("NOTHING_TO_INLINE")
private inline fun highSurrogate(codePoint: Int) = (codePoint ushr 10) + HighSurrogateMagic

private const val MaxCodePoint = 0x10ffff
private const val MinLowSurrogate = 0xdc00
private const val MinHighSurrogate = 0xd800
private const val MinSupplementary = 0x10000
private const val HighSurrogateMagic = MinHighSurrogate - (MinSupplementary ushr 10)

internal fun codePoint(high: Char, low: Char): Int {
    check(high.isHighSurrogate())
    check(low.isLowSurrogate())

    val highValue = high.toInt() - HighSurrogateMagic
    val lowValue = low.toInt() - MinLowSurrogate

    return highValue shl 10 or lowValue
}
