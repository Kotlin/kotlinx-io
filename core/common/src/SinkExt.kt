/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

/**
 * Writes a little-endian short to this sink using two bytes.
 * ```
 * Buffer buffer = new Buffer();
 * buffer.writeShortLe(32767);
 * buffer.writeShortLe(15);
 *
 * assertEquals(4, buffer.size());
 * assertEquals((byte) 0xff, buffer.readByte());
 * assertEquals((byte) 0x7f, buffer.readByte());
 * assertEquals((byte) 0x0f, buffer.readByte());
 * assertEquals((byte) 0x00, buffer.readByte());
 * assertEquals(0, buffer.size());
 * ```
 */
fun <T: Sink> T.writeShortLe(s: Int): T {
    this.writeShort(s.toShort().reverseBytes().toInt())
    return this
}

/**
 * Writes a little-endian int to this sink using four bytes.
 * ```
 * Buffer buffer = new Buffer();
 * buffer.writeIntLe(2147483647);
 * buffer.writeIntLe(15);
 *
 * assertEquals(8, buffer.size());
 * assertEquals((byte) 0xff, buffer.readByte());
 * assertEquals((byte) 0xff, buffer.readByte());
 * assertEquals((byte) 0xff, buffer.readByte());
 * assertEquals((byte) 0x7f, buffer.readByte());
 * assertEquals((byte) 0x0f, buffer.readByte());
 * assertEquals((byte) 0x00, buffer.readByte());
 * assertEquals((byte) 0x00, buffer.readByte());
 * assertEquals((byte) 0x00, buffer.readByte());
 * assertEquals(0, buffer.size());
 * ```
 */
fun <T: Sink> T.writeIntLe(i: Int): T {
    this.writeInt(i.reverseBytes())
    return this
}

/**
 * Writes a little-endian long to this sink using eight bytes.
 * ```
 * Buffer buffer = new Buffer();
 * buffer.writeLongLe(9223372036854775807L);
 * buffer.writeLongLe(15);
 *
 * assertEquals(16, buffer.size());
 * assertEquals((byte) 0xff, buffer.readByte());
 * assertEquals((byte) 0xff, buffer.readByte());
 * assertEquals((byte) 0xff, buffer.readByte());
 * assertEquals((byte) 0xff, buffer.readByte());
 * assertEquals((byte) 0xff, buffer.readByte());
 * assertEquals((byte) 0xff, buffer.readByte());
 * assertEquals((byte) 0xff, buffer.readByte());
 * assertEquals((byte) 0x7f, buffer.readByte());
 * assertEquals((byte) 0x0f, buffer.readByte());
 * assertEquals((byte) 0x00, buffer.readByte());
 * assertEquals((byte) 0x00, buffer.readByte());
 * assertEquals((byte) 0x00, buffer.readByte());
 * assertEquals((byte) 0x00, buffer.readByte());
 * assertEquals((byte) 0x00, buffer.readByte());
 * assertEquals((byte) 0x00, buffer.readByte());
 * assertEquals((byte) 0x00, buffer.readByte());
 * assertEquals(0, buffer.size());
 * ```
 */
fun <T: Sink> T.writeLongLe(v: Long): T {
    this.writeLong(v.reverseBytes())
    return this
}

/**
 * Writes a long to this sink in signed decimal form (i.e., as a string in base 10).
 * ```
 * Buffer buffer = new Buffer();
 * buffer.writeDecimalLong(8675309L);
 * buffer.writeByte(' ');
 * buffer.writeDecimalLong(-123L);
 * buffer.writeByte(' ');
 * buffer.writeDecimalLong(1L);
 *
 * assertEquals("8675309 -123 1", buffer.readUtf8());
 * ```
 */
fun <T: Sink> T.writeDecimalLong(v: Long): T {
    // TODO: optimize
    writeUtf8(v.toString())
    return this
}

/**
 * Writes a long to this sink in hexadecimal form (i.e., as a string in base 16).
 * ```
 * Buffer buffer = new Buffer();
 * buffer.writeHexadecimalUnsignedLong(65535L);
 * buffer.writeByte(' ');
 * buffer.writeHexadecimalUnsignedLong(0xcafebabeL);
 * buffer.writeByte(' ');
 * buffer.writeHexadecimalUnsignedLong(0x10L);
 *
 * assertEquals("ffff cafebabe 10", buffer.readUtf8());
 * ```
 */
fun <T: Sink> T.writeHexadecimalUnsignedLong(v: Long): T {
    if (v == 0L) {
        writeByte('0'.code)
    } else {
        // TODO: optimize
        writeUtf8(v.toHexString())
    }
    return this
}