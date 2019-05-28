package kotlinx.io

import kotlinx.io.buffer.*
import kotlinx.io.text.*

private const val lastASCII = 0x7F.toChar()

fun Output.writeUTF8String(text: CharSequence, index: Int = 0, length: Int = text.length - index) {
    var textIndex = index // index in text
    val textEndIndex = index + length // index of char after last
    var code = 0 // current left bytes to write (in inverted order)

    while (textIndex < textEndIndex) {
        writeBufferRange { buffer, startOffset, endOffset ->
            var offset = startOffset

            while (offset <= endOffset && textIndex < textEndIndex) {
                if (code != 0) {
                    // write remaining bytes of multibyte sequence
                    // 6-bit blocks are reversed by initiation code below
                    buffer[offset++] = (code and 0xFF).toByte()
                    code = code shr 8
                    continue
                }

                // get next character
                val character = text[textIndex++]
                if (character <= lastASCII) {
                    // write simple ascii (fast path)
                    buffer[offset++] = character.toByte()
                    continue
                }

                // fetch next code
                code = when {
                    character.isHighSurrogate() -> {
                        if (textIndex == textEndIndex - 1) {
                            throw MalformedInputException("Splitted surrogate character")
                        }
                        codePoint(character, text[textIndex++])
                    }
                    else -> character.toInt()
                }

                when {
                    code < 0x7ff -> {
                        val s1 = 0xc0 or ((code shr 6) and 0x1f)
                        val s2 = (code and 0x3f) or 0x80
                        buffer[offset++] = s1.toByte()
                        code = s2
                    }
                    code < 0xffff -> {
                        val s1 = 0xe0 or ((code shr 12) and 0x0f)
                        val s2 = ((code shr 6) and 0x3f) or 0x80
                        val s3 = (code and 0x3f) or 0x80
                        buffer[offset++] = s1.toByte()
                        code = (s3 shl 8) or s2 // order is reversed for faster writes
                    }
                    code < 0x10ffff -> {
                        val s1 = 0xf0 or ((code shr 18) and 0x07)
                        val s2 = ((code shr 12) and 0x3f) or 0x80
                        val s3 = ((code shr 6) and 0x3f) or 0x80
                        val s4 = (code and 0x3f) or 0x80
                        buffer[offset++] = s1.toByte()
                        code = (s4 shl 16) or (s3 shl 8) or s2 // order is reversed for faster writes
                    }
                    else -> malformedCodePoint(code)
                }
            }
            offset - startOffset
        }
    }
}

internal fun codePoint(high: Char, low: Char): Int {
    check(high.isHighSurrogate())
    check(low.isLowSurrogate())

    val highValue = high.toInt() - HighSurrogateMagic
    val lowValue = low.toInt() - MinLowSurrogate

    return highValue shl 10 or lowValue
}

private fun malformedCodePoint(codePoint: Int): Nothing {
    throw MalformedInputException("Malformed UTF8 code point $codePoint")
}
