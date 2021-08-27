package kotlinx.io.text

import kotlinx.io.*
import kotlinx.io.buffer.*

private const val lastASCII = 0x7F.toChar()

/**
 * Write [length] bytes in [text] starting from offset [index] to output.
 *
 * @throws MalformedInputException if encoding is invalid.
 */
public fun Output.writeUtf8String(text: CharSequence, index: Int = 0, length: Int = text.length - index) {
    var textIndex = index // index in text
    val textEndIndex = index + length // index of char after last
    var bytes = 0 // current left bytes to write (in inverted order)

    while (textIndex < textEndIndex || bytes != 0) {
        writeBuffer { buffer, startIndex, endIndex ->
            var offset = startIndex

            while (offset < endIndex) {
                if (bytes != 0) {
                    // write remaining bytes of multibyte sequence
                    buffer[offset++] = (bytes and 0xFF).toByte()
                    bytes = bytes shr 8
                    continue
                }

                if (textIndex == textEndIndex) {
                    return@writeBuffer offset
                }

                // get next character
                val character = text[textIndex++]
                if (character <= lastASCII) {
                    // write simple ascii (fast path)
                    buffer[offset++] = character.code.toByte()
                    continue
                }

                // fetch next code
                val code = when {
                    character.isHighSurrogate() -> {
                        if (textIndex == textEndIndex) {
                            splittedSurogate()
                        }
                        codePoint(character, text[textIndex++])
                    }
                    else -> character.code
                }

                // write Utf8 bytes to buffer or queue them for write in `bytes` if not enough space
                when {
                    code <= 0x7ff -> {
                        buffer[offset++] = (0xc0 or ((code shr 6) and 0x1f)).toByte()
                        val byte1 = (code and 0x3f) or 0x80
                        if (offset < buffer.size) {
                            buffer[offset++] = byte1.toByte()
                        } else {
                            bytes = byte1
                        }
                    }
                    code <= 0xffff -> {
                        buffer[offset++] = ((code shr 12) and 0x0f or 0xe0).toByte()
                        val byte1 = ((code shr 6) and 0x3f) or 0x80
                        val byte2 = (code and 0x3f) or 0x80
                        if (offset + 1 < buffer.size) {
                            buffer[offset++] = byte1.toByte()
                            buffer[offset++] = byte2.toByte()
                        } else {
                            bytes = (byte2 shl 8) or byte1 // order is reversed for writes
                        }
                    }
                    code <= 0x10ffff -> {
                        buffer[offset++] = ((code shr 18) and 0x07 or 0xf0).toByte()
                        val byte1 = ((code shr 12) and 0x3f) or 0x80
                        val byte2 = ((code shr 6) and 0x3f) or 0x80
                        val byte3 = (code and 0x3f) or 0x80
                        if (offset + 2 < buffer.size) {
                            buffer[offset++] = byte1.toByte()
                            buffer[offset++] = byte2.toByte()
                            buffer[offset++] = byte3.toByte()
                        } else {
                            bytes = (byte3 shl 16) or (byte2 shl 8) or byte1 // order is reversed for faster writes
                        }
                    }
                    else -> malformedCodePoint(code)
                }
            }
            offset
        }
    }
    flush()
}

/**
 * Write single UTF-8 [character] to [Output].
 *
 * @return count of written bytes
 * @throws MalformedInputException if [character] is splitted surrogate or invalid.
 */
public fun Output.writeUtf8Char(character: Char): Int {
    // ASCII character
    if (character <= lastASCII) {
        writeByte(character.code.toByte())
        return 1
    }

    if (character.isHighSurrogate()) {
        throw MalformedInputException("Splitted surrogate character: $character")
    }

    val code = character.code

    return when {
        code <= 0x7ff -> {
            val byte0 = (0xc0 or ((code shr 6) and 0x1f))
            val byte1 = (code and 0x3f) or 0x80
            writeShort(((byte0 shl 8) or byte1).toShort())
            2
        }
        code <= 0xffff -> {
            val byte0 = ((code shr 12) and 0x0f or 0xe0).toByte()
            val byte1 = ((code shr 6) and 0x3f) or 0x80
            val byte2 = (code and 0x3f) or 0x80
            writeByte(byte0)
            writeShort(((byte1 shl 8) or byte2).toShort())
            3
        }
        code <= 0x10ffff -> {
            val byte0 = ((code shr 18) and 0x07 or 0xf0)
            val byte1 = ((code shr 12) and 0x3f) or 0x80
            val byte2 = ((code shr 6) and 0x3f) or 0x80
            val byte3 = (code and 0x3f) or 0x80
            writeInt((byte0 shl 24) or (byte1 shl 16) or (byte2 shl 8) or byte3)
            4
        }
        else -> malformedCodePoint(code)
    }
}

internal fun codePoint(high: Char, low: Char): Int {
    check(high.isHighSurrogate())
    check(low.isLowSurrogate())

    val highValue = high.code - HighSurrogateMagic
    val lowValue = low.code - MinLowSurrogate

    return highValue shl 10 or lowValue
}

private fun malformedCodePoint(codePoint: Int): Nothing {
    // TODO: revise exceptions
    throw MalformedInputException("Malformed Utf8 code point $codePoint")
}

internal fun splittedSurogate(): Nothing {
    throw MalformedInputException("Splitted surrogate character")
}
