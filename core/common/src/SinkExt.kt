/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

/**
 * Writes two bytes containing [short], in the little-endian order, to this sink.
 *
 * @param short the short integer to be written.
 */
public fun <T: Sink> T.writeShortLe(short: Short): T {
    this.writeShort(short.reverseBytes())
    return this
}

/**
 * Writes four bytes containing [int], in the little-endian order, to this sink.
 *
 * @param int the integer to be written.
 */
public fun <T: Sink> T.writeIntLe(int: Int): T {
    this.writeInt(int.reverseBytes())
    return this
}

/**
 * Writes eight bytes containing [long], in the little-endian order, to this sink.
 *
 * @param long the long integer to be written.
 */
public fun <T: Sink> T.writeLongLe(long: Long): T {
    this.writeLong(long.reverseBytes())
    return this
}

/**
 * Writes [long] to this sink in signed decimal form (i.e., as a string in base 10).
 *
 * Resulting string will not contain leading zeros, except the `0` value itself.
 *
 * @param long the long to be written.
 */
public fun <T: Sink> T.writeDecimalLong(long: Long): T {
    // TODO: optimize
    writeUtf8(long.toString())
    return this
}

/**
 * Writes [long] to this sink in hexadecimal form (i.e., as a string in base 16).
 *
 * Resulting string will not contain leading zeros, except the `0` value itself.
 *
 * @param long the long to be written.
 */
public fun <T: Sink> T.writeHexadecimalUnsignedLong(long: Long): T {
    if (long == 0L) {
        writeByte('0'.code.toByte())
    } else {
        // TODO: optimize
        writeUtf8(long.toHexString())
    }
    return this
}

/**
 * Writes am unsigned byte to this sink.
 *
 * @param byte the byte to be written.
 */
public fun <T: Sink> T.writeByte(byte: UByte): T {
    writeByte(byte.toByte())
    return this
}

/**
 * Writes two bytes containing [short], in the big-endian order, to this sink.
 *
 * @param short the unsigned short integer to be written.
 */
public fun <T: Sink> T.writeShort(short: UShort): T {
    writeShort(short.toShort())
    return this
}

/**
 * Writes four bytes containing [int], in the big-endian order, to this sink.
 *
 * @param int the unsigned integer to be written.
 */
public fun <T: Sink> T.writeInt(int: UInt): T {
    writeInt(int.toInt())
    return this
}

/**
 * Writes eight bytes containing [long], in the big-endian order, to this sink.
 *
 * @param long the unsigned long integer to be written.
 */
public fun <T: Sink> T.writeLong(long: ULong): T {
    writeLong(long.toLong())
    return this
}

/**
 * Writes two bytes containing [short], in the little-endian order, to this sink.
 *
 * @param short the unsigned short integer to be written.
 */
public fun <T: Sink> T.writeShortLe(short: UShort): T {
    writeShortLe(short.toShort())
    return this
}

/**
 * Writes four bytes containing [int], in the little-endian order, to this sink.
 *
 * @param int the unsugned integer to be written.
 */
public fun <T: Sink> T.writeIntLe(int: UInt): T {
    writeIntLe(int.toInt())
    return this
}

/**
 * Writes eight bytes containing [long], in the little-endian order, to this sink.
 *
 * @param long the unsigned long integer to be written.
 */
public fun <T: Sink> T.writeLongLe(long: ULong): T {
    writeLongLe(long.toLong())
    return this
}
