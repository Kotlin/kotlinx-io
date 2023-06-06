/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

/**
 * Removes two bytes from this source and returns a short integer composed of it according to the little-endian order.
 *
 * @throws EOFException when there are not enough data to read a short value.
 */
public fun Source.readShortLe(): Short {
    return readShort().reverseBytes()
}

/**
 * Removes four bytes from this source and returns an integer composed of it according to the little-endian order.
 *
 * @throws EOFException when there are not enough data to read an int value.
 */
public fun Source.readIntLe(): Int {
    return readInt().reverseBytes()
}

/**
 * Removes eight bytes from this source and returns a long integer composed of it according to the little-endian order.
 *
 * @throws EOFException when there are not enough data to read a long value.
 */
public fun Source.readLongLe(): Long {
    return readLong().reverseBytes()
}

internal const val OVERFLOW_ZONE = Long.MIN_VALUE / 10L
internal const val OVERFLOW_DIGIT_START = Long.MIN_VALUE % 10L + 1

/**
 * Reads a long from this source in signed decimal form (i.e., as a string in base 10 with
 * optional leading `-`).
 *
 * Source data will be consumed until the source is exhausted, the first occurrence of non-digit byte,
 * or overflow happened during resulting value construction.
 *
 * @throws NumberFormatException if the found digits do not fit into a `long` or a decimal
 * number was not present.
 * @throws EOFException if the source is exhausted before a call of this method.
 */
public fun Source.readDecimalLong(): Long {
    require(1)
    var b = readByte()
    var negative = false
    var value = 0L
    var seen = 0
    var overflowDigit = OVERFLOW_DIGIT_START
    when (b) {
        '-'.code.toByte() -> {
            negative = true
            overflowDigit--
        }
        in '0'.code..'9'.code -> {
            value = ('0'.code - b).toLong()
            seen = 1
        }
        else -> {
            throw NumberFormatException("Expected a digit or '-' but was 0x${b.toHexString()}")
        }
    }

    while (request(1)) {
        b = buffer[0]
        if (b in '0'.code..'9'.code) {
            val digit = '0'.code - b
            readByte() // consume byte

            // Detect when the digit would cause an overflow.
            if (value < OVERFLOW_ZONE || value == OVERFLOW_ZONE && digit < overflowDigit) {
                val buffer = Buffer().writeDecimalLong(value).writeByte(b.toInt())
                if (!negative) buffer.readByte() // Skip negative sign.
                throw NumberFormatException("Number too large: ${buffer.readUtf8()}")
            }
            value = value * 10L + digit
            seen++
        } else {
            break
        }
    }

    if (seen < 1) {
        if (!request(1)) throw EOFException()
        val expected = if (negative) "Expected a digit" else "Expected a digit or '-'"
        throw NumberFormatException("$expected but was 0x${buffer[0].toHexString()}")
    }

    return if (negative) value else -value
}

/**
 * Reads a long form this source in hexadecimal form (i.e., as a string in base 16).
 *
 * Source data will be consumed until the source is exhausted, the first occurrence of non-digit byte,
 * or overflow happened during resulting value construction.
 *
 * @throws NumberFormatException if the found hexadecimal does not fit into a `long` or
 * hexadecimal was not found.
 * @throws EOFException if the source is exhausted before a call of this method.
 */
public fun Source.readHexadecimalUnsignedLong(): Long {
    require(1)
    var b = readByte()
    var result = when (b) {
        in '0'.code..'9'.code -> b - '0'.code
        in 'a'.code..'f'.code -> b - 'a'.code + 10
        in 'A'.code..'F'.code -> b - 'A'.code + 10
        else -> throw NumberFormatException("Expected leading [0-9a-fA-F] character but was 0x${b.toHexString()}")
    }.toLong()

    while (request(1)) {
        b = buffer[0]
        val bDigit = when (b) {
            in '0'.code..'9'.code -> b - '0'.code
            in 'a'.code..'f'.code -> b - 'a'.code + 10
            in 'A'.code..'F'.code -> b - 'A'.code + 10
            else -> break
        }
        if (result and -0x1000000000000000L != 0L) {
            val buffer = Buffer().writeHexadecimalUnsignedLong(result).writeByte(b.toInt())
            throw NumberFormatException("Number too large: " + buffer.readUtf8())
        }
        readByte() // consume byte
        result = result.shl(4) + bDigit
    }
    return result
}

/**
 * Returns an index of [b] first occurrence in the range of [fromIndex] inclusive to [toIndex]
 * exclusive, or `-1` when the range doesn't contain [b].
 *
 * The scan terminates at either [toIndex] or source's exhaustion, whichever comes first. The
 * maximum number of bytes scanned is `toIndex-fromIndex`.
 * If [b] not found in buffered data, [toIndex] is yet to be reached and the underlying source is not yet exhausted
 * then new data will be read from the underlying source into the buffer.
 *
 * @param b the value to find.
 * @param fromIndex the start of the range to find [b], inclusive.
 * @param toIndex the end of the range to find [b], exclusive.
 */
public fun Source.indexOf(b: Byte, fromIndex: Long = 0L, toIndex: Long = Long.MAX_VALUE): Long {
    require(fromIndex in 0..toIndex)
    if (fromIndex == toIndex) return -1L

    var offset = fromIndex
    val peekSource = peek()

    if (!peekSource.request(offset)) {
        return -1L
    }
    peekSource.skip(offset)
    while (offset < toIndex && peekSource.request(1)) {
        if (peekSource.readByte() == b) return offset
        offset++
    }
    return -1L
}