package kotlinx.io

import kotlinx.io.buffer.*
import kotlinx.io.text.*

// Based on https://bjoern.hoehrmann.de/utf-8/decoder/dfa/
// 364 ints

private val utf8StateMachine = intArrayOf(
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9, 9,
    7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7,
    8, 8, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
    10, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 3, 3, 11, 6, 6, 6, 5, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
    0, 12, 24, 36, 60, 96, 84, 12, 12, 12, 48, 72, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
    12, 0, 12, 12, 12, 12, 12, 0, 12, 0, 12, 12, 12, 24, 12, 12, 12, 12, 12, 24, 12, 24, 12, 12,
    12, 12, 12, 12, 12, 12, 12, 24, 12, 12, 12, 12, 12, 24, 12, 12, 12, 12, 12, 12, 12, 24, 12, 12,
    12, 12, 12, 12, 12, 12, 12, 36, 12, 36, 12, 12, 12, 36, 12, 12, 12, 12, 12, 36, 12, 36, 12, 12,
    12, 36, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12
)

private const val UTF8_STATE_ACCEPT = 0
private const val UTF8_STATE_REJECT = 1

private inline fun IntArray.decodeUTF8Byte(
    byte: Int,
    state: Int,
    code: Int,
    update: (newState: Int, newCode: Int) -> Unit
) {
    val type = this[byte]
    val newCode = if (state == UTF8_STATE_ACCEPT)
        (0xff ushr type) and byte
    else
        (byte and 0x3f) or (code shl 6)

    val newState = this[256 + state + type]
    update(newState, newCode)
}

fun Buffer.decodeUTF8(offset: Int, size: Int, consumer: (Char) -> Boolean): Int {
    val stateMachine = utf8StateMachine
    var state = UTF8_STATE_ACCEPT
    var codePoint = 0

    for (index in offset until offset + size) {
        val byte = loadByteAt(index).toInt() and 0xff
        stateMachine.decodeUTF8Byte(byte, state, codePoint) { s, c ->
            state = s
            codePoint = c
        }

        when (state) {
            UTF8_STATE_REJECT -> {
                // TODO: Attempt to recover
                malformedInput(codePoint)
            }
            UTF8_STATE_ACCEPT -> when {
                codePoint ushr 16 == 0 -> {
                    val ok = consumer(codePoint.toChar())
                    if (!ok)
                        return index - offset

                }
                codePoint <= MaxCodePoint -> {
                    val high = highSurrogate(codePoint).toChar()
                    val low = lowSurrogate(codePoint).toChar()
                    val ok = consumer(high) && consumer(low)
                    if (!ok)
                        return index - offset
                }
                else -> {
                    malformedInput(codePoint)
                }
            }
        }

    }
    return size
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
