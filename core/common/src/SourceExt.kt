/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

/**
 * Removes two bytes from this source and returns a little-endian short.
 * ```
 * Buffer buffer = new Buffer()
 *     .writeByte(0xff)
 *     .writeByte(0x7f)
 *     .writeByte(0x0f)
 *     .writeByte(0x00);
 * assertEquals(4, buffer.size());
 *
 * assertEquals(32767, buffer.readShortLe());
 * assertEquals(2, buffer.size());
 *
 * assertEquals(15, buffer.readShortLe());
 * assertEquals(0, buffer.size());
 * ```
 */
fun Source.readShortLe(): Short {
    return readShort().reverseBytes()
}

/**
 * Removes four bytes from this source and returns a little-endian int.
 * ```
 * Buffer buffer = new Buffer()
 *     .writeByte(0xff)
 *     .writeByte(0xff)
 *     .writeByte(0xff)
 *     .writeByte(0x7f)
 *     .writeByte(0x0f)
 *     .writeByte(0x00)
 *     .writeByte(0x00)
 *     .writeByte(0x00);
 * assertEquals(8, buffer.size());
 *
 * assertEquals(2147483647, buffer.readIntLe());
 * assertEquals(4, buffer.size());
 *
 * assertEquals(15, buffer.readIntLe());
 * assertEquals(0, buffer.size());
 * ```
 */
fun Source.readIntLe(): Int {
    return readInt().reverseBytes()
}

/**
 * Removes eight bytes from this source and returns a little-endian long.
 * ```
 * Buffer buffer = new Buffer()
 *     .writeByte(0xff)
 *     .writeByte(0xff)
 *     .writeByte(0xff)
 *     .writeByte(0xff)
 *     .writeByte(0xff)
 *     .writeByte(0xff)
 *     .writeByte(0xff)
 *     .writeByte(0x7f)
 *     .writeByte(0x0f)
 *     .writeByte(0x00)
 *     .writeByte(0x00)
 *     .writeByte(0x00)
 *     .writeByte(0x00)
 *     .writeByte(0x00)
 *     .writeByte(0x00)
 *     .writeByte(0x00);
 * assertEquals(16, buffer.size());
 *
 * assertEquals(9223372036854775807L, buffer.readLongLe());
 * assertEquals(8, buffer.size());
 *
 * assertEquals(15, buffer.readLongLe());
 * assertEquals(0, buffer.size());
 * ```
 */
fun Source.readLongLe(): Long {
    return readLong().reverseBytes()
}

internal const val OVERFLOW_ZONE = Long.MIN_VALUE / 10L
internal const val OVERFLOW_DIGIT_START = Long.MIN_VALUE % 10L + 1

/**
 * Reads a long from this source in signed decimal form (i.e., as a string in base 10 with
 * optional leading '-'). This will iterate until a non-digit character is found.
 * ```
 * Buffer buffer = new Buffer()
 *     .writeUtf8("8675309 -123 00001");
 *
 * assertEquals(8675309L, buffer.readDecimalLong());
 * assertEquals(' ', buffer.readByte());
 * assertEquals(-123L, buffer.readDecimalLong());
 * assertEquals(' ', buffer.readByte());
 * assertEquals(1L, buffer.readDecimalLong());
 * ```
 *
 * @throws NumberFormatException if the found digits do not fit into a `long` or a decimal
 * number was not present.
 */
// TODO: add tests, seems like it may throw exceptions with incorrect messages
// TODO: test overflow detection
fun Source.readDecimalLong(): Long {
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
 * Reads a long form this source in hexadecimal form (i.e., as a string in base 16). This will
 * iterate until a non-hexadecimal character is found.
 * ```
 * Buffer buffer = new Buffer()
 *     .writeUtf8("ffff CAFEBABE 10");
 *
 * assertEquals(65535L, buffer.readHexadecimalUnsignedLong());
 * assertEquals(' ', buffer.readByte());
 * assertEquals(0xcafebabeL, buffer.readHexadecimalUnsignedLong());
 * assertEquals(' ', buffer.readByte());
 * assertEquals(0x10L, buffer.readHexadecimalUnsignedLong());
 * ```
 *
 * @throws NumberFormatException if the found hexadecimal does not fit into a `long` or
 * hexadecimal was not found.
 */
fun Source.readHexadecimalUnsignedLong(): Long {
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
 * Removes and returns characters up to but not including the next line break. A line break is
 * either `"\n"` or `"\r\n"`; these characters are not included in the result.
 * ```
 * Buffer buffer = new Buffer()
 *     .writeUtf8("I'm a hacker!\n")
 *     .writeUtf8("That's what I said: you're a nerd.\n")
 *     .writeUtf8("I prefer to be called a hacker!\n");
 * assertEquals(81, buffer.size());
 *
 * assertEquals("I'm a hacker!", buffer.readUtf8Line());
 * assertEquals(67, buffer.size());
 *
 * assertEquals("That's what I said: you're a nerd.", buffer.readUtf8Line());
 * assertEquals(32, buffer.size());
 *
 * assertEquals("I prefer to be called a hacker!", buffer.readUtf8Line());
 * assertEquals(0, buffer.size());
 *
 * assertEquals(null, buffer.readUtf8Line());
 * assertEquals(0, buffer.size());
 * ```
 *
 * **On the end of the stream this method returns null,** just like [java.io.BufferedReader]. If
 * the source doesn't end with a line break then an implicit line break is assumed. Null is
 * returned once the source is exhausted. Use this for human-generated data, where a trailing
 * line break is optional.
 */
fun Source.readUtf8Line(): String? {
    if (!request(1)) return null

    val peekSource = peek()
    var offset = 0L
    var newlineSize = 0L
    while (peekSource.request(1)) {
        val b = peekSource.readByte().toInt()
        if (b == '\n'.code) {
            newlineSize = 1L
            break
        } else if (b == '\r'.code) {
            if (peekSource.request(1) && peekSource.buffer[0].toInt() == '\n'.code) {
                newlineSize = 2L
                break
            }
        }
        offset++
    }
    val line = readUtf8(offset)
    skip(newlineSize)
    return line
}

/**
 * Like [readUtf8LineStrict], except this allows the caller to specify the longest allowed match.
 * Use this to protect against streams that may not include `"\n"` or `"\r\n"`.
 *
 * The returned string will have at most `limit` UTF-8 bytes, and the maximum number of bytes
 * scanned is `limit + 2`. If `limit == 0` this will always throw an `EOFException` because no
 * bytes will be scanned.
 *
 * This method is safe. No bytes are discarded if the match fails, and the caller is free to try
 * another match:
 * ```
 * Buffer buffer = new Buffer();
 * buffer.writeUtf8("12345\r\n");
 *
 * // This will throw! There must be \r\n or \n at the limit or before it.
 * buffer.readUtf8LineStrict(4);
 *
 * // No bytes have been consumed so the caller can retry.
 * assertEquals("12345", buffer.readUtf8LineStrict(5));
 * ```
 */
fun Source.readUtf8LineStrict(limit: Long = Long.MAX_VALUE): String {
    if (!request(1)) throw EOFException()

    val peekSource = peek()
    var offset = 0L
    var newlineSize = 0L
    while (offset < limit && peekSource.request(1)) {
        val b = peekSource.readByte().toInt()
        if (b == '\n'.code) {
            newlineSize = 1L
            break
        } else if (b == '\r'.code) {
            if (peekSource.request(1) && peekSource.buffer[0].toInt() == '\n'.code) {
                newlineSize = 2L
                break
            }
        }
        offset++
    }
    if (offset == limit) {
        if (!peekSource.request(1)) throw EOFException()
        val nlCandidate = peekSource.readByte().toInt()
        if (nlCandidate == '\n'.code) {
            newlineSize = 1
        } else if (nlCandidate == '\r'.code && peekSource.request(1) && peekSource.readByte().toInt() == '\n'.code) {
            newlineSize = 2
        }
    }
    if (newlineSize == 0L) throw EOFException()
    val line = readUtf8(offset)
    skip(newlineSize)
    return line
}

/**
 * Returns the index of the first `b` in the buffer at or after `fromIndex`. This expands the
 * buffer as necessary until `b` is found. This reads an unbounded number of bytes into the
 * buffer. Returns -1 if the stream is exhausted before the requested byte is found.
 * ```
 * Buffer buffer = new Buffer();
 * buffer.writeUtf8("Don't move! He can't see us if we don't move.");
 *
 * byte m = 'm';
 * assertEquals(6,  buffer.indexOf(m));
 * assertEquals(40, buffer.indexOf(m, 12));
 * ```
 */
// fun Source.indexOf(b: Byte, fromIndex: Long = 0): Long {
//    return 0
//}

/**
 * Returns the index of `b` if it is found in the range of `fromIndex` inclusive to `toIndex`
 * exclusive. If `b` isn't found, or if `fromIndex == toIndex`, then -1 is returned.
 *
 * The scan terminates at either `toIndex` or the end of the buffer, whichever comes first. The
 * maximum number of bytes scanned is `toIndex-fromIndex`.
 */
fun Source.indexOf(b: Byte, fromIndex: Long = 0L, toIndex: Long = Long.MAX_VALUE): Long {
    require(fromIndex in 0..toIndex)
    if (fromIndex == toIndex) return -1L

    var offset = fromIndex
    var peekSource = peek()

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

//  /** Equivalent to [indexOf(bytes, 0)][indexOf]. */
//  fun indexOf(bytes: ByteString): Long

/**
 * Returns the index of the first match for `bytes` in the buffer at or after `fromIndex`. This
 * expands the buffer as necessary until `bytes` is found. This reads an unbounded number of
 * bytes into the buffer. Returns -1 if the stream is exhausted before the requested bytes are
 * found.
 * ```
 * ByteString MOVE = ByteString.encodeUtf8("move");
 *
 * Buffer buffer = new Buffer();
 * buffer.writeUtf8("Don't move! He can't see us if we don't move.");
 *
 * assertEquals(6,  buffer.indexOf(MOVE));
 * assertEquals(40, buffer.indexOf(MOVE, 12));
 * ```
 */
//  fun indexOf(bytes: ByteString, fromIndex: Long): Long

/** Equivalent to [indexOfElement(targetBytes, 0)][indexOfElement]. */
//  fun indexOfElement(targetBytes: ByteString): Long

/**
 * Returns the first index in this buffer that is at or after `fromIndex` and that contains any of
 * the bytes in `targetBytes`. This expands the buffer as necessary until a target byte is found.
 * This reads an unbounded number of bytes into the buffer. Returns -1 if the stream is exhausted
 * before the requested byte is found.
 * ```
 * ByteString ANY_VOWEL = ByteString.encodeUtf8("AEOIUaeoiu");
 *
 * Buffer buffer = new Buffer();
 * buffer.writeUtf8("Dr. Alan Grant");
 *
 * assertEquals(4,  buffer.indexOfElement(ANY_VOWEL));    // 'A' in 'Alan'.
 * assertEquals(11, buffer.indexOfElement(ANY_VOWEL, 9)); // 'a' in 'Grant'.
 * ```
 */
//  fun indexOfElement(targetBytes: ByteString, fromIndex: Long): Long

/**
 * Returns true if the bytes at `offset` in this source equal `bytes`. This expands the buffer as
 * necessary until a byte does not match, all bytes are matched, or if the stream is exhausted
 * before enough bytes could determine a match.
 * ```
 * ByteString simonSays = ByteString.encodeUtf8("Simon says:");
 *
 * Buffer standOnOneLeg = new Buffer().writeUtf8("Simon says: Stand on one leg.");
 * assertTrue(standOnOneLeg.rangeEquals(0, simonSays));
 *
 * Buffer payMeMoney = new Buffer().writeUtf8("Pay me $1,000,000.");
 * assertFalse(payMeMoney.rangeEquals(0, simonSays));
 * ```
 */
//  fun rangeEquals(offset: Long, bytes: ByteString): Boolean

/**
 * Returns true if `byteCount` bytes at `offset` in this source equal `bytes` at `bytesOffset`.
 * This expands the buffer as necessary until a byte does not match, all bytes are matched, or if
 * the stream is exhausted before enough bytes could determine a match.
 */
//  fun rangeEquals(offset: Long, bytes: ByteString, bytesOffset: Int, byteCount: Int): Boolean
