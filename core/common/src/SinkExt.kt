/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

internal val HEX_DIGIT_BYTES = "0123456789abcdef".asUtf8ToByteArray()

/**
 * Writes two bytes containing [short], in the little-endian order, to this sink.
 *
 * @param short the short integer to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeShortLe
 */
public fun Sink.writeShortLe(short: Short) {
    this.writeShort(short.reverseBytes())
}

/**
 * Writes four bytes containing [int], in the little-endian order, to this sink.
 *
 * @param int the integer to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeIntLe
 */
public fun Sink.writeIntLe(int: Int) {
    this.writeInt(int.reverseBytes())
}

/**
 * Writes eight bytes containing [long], in the little-endian order, to this sink.
 *
 * @param long the long integer to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeLongLe
 */
public fun Sink.writeLongLe(long: Long) {
    this.writeLong(long.reverseBytes())
}

/**
 * Writes [long] to this sink in signed decimal form (i.e., as a string in base 10).
 *
 * Resulting string will not contain leading zeros, except the `0` value itself.
 *
 * @param long the long to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeDecimalLong
 */
@OptIn(DelicateIoApi::class)
public fun Sink.writeDecimalLong(long: Long) {
    var v = long
    if (v == 0L) {
        // Both a shortcut and required since the following code can't handle zero.
        writeByte('0'.code.toByte())
        return
    }

    var negative = false
    if (v < 0L) {
        v = -v
        if (v < 0L) { // Only true for Long.MIN_VALUE.
            writeString("-9223372036854775808")
            return
        }
        negative = true
    }

    // Binary search for character width which favors matching lower numbers.
    var width =
        if (v < 100000000L)
            if (v < 10000L)
                if (v < 100L)
                    if (v < 10L) 1
                    else 2
                else if (v < 1000L) 3
                else 4
            else if (v < 1000000L)
                if (v < 100000L) 5
                else 6
            else if (v < 10000000L) 7
            else 8
        else if (v < 1000000000000L)
            if (v < 10000000000L)
                if (v < 1000000000L) 9
                else 10
            else if (v < 100000000000L) 11
            else 12
        else if (v < 1000000000000000L)
            if (v < 10000000000000L) 13
            else if (v < 100000000000000L) 14
            else 15
        else if (v < 100000000000000000L)
            if (v < 10000000000000000L) 16
            else 17
        else if (v < 1000000000000000000L) 18
        else 19
    if (negative) {
        ++width
    }

    writeToInternalBuffer { buffer ->
        val tail = buffer.writableSegment(width)
        val data = tail.data
        var pos = tail.limit + width // We write backwards from right to left.
        while (v != 0L) {
            val digit = (v % 10).toInt()
            data[--pos] = HEX_DIGIT_BYTES[digit]
            v /= 10
        }
        if (negative) {
            data[--pos] = '-'.code.toByte()
        }

        tail.limit += width
        buffer.size += width.toLong()
    }
}

/**
 * Writes [long] to this sink in hexadecimal form (i.e., as a string in base 16).
 *
 * Resulting string will not contain leading zeros, except the `0` value itself.
 *
 * @param long the long to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeHexLong
 */
@OptIn(DelicateIoApi::class)
public fun Sink.writeHexadecimalUnsignedLong(long: Long) {
    var v = long
    if (v == 0L) {
        // Both a shortcut and required since the following code can't handle zero.
        writeByte('0'.code.toByte())
        return
    }

    // Mask every bit below the most significant bit to a 1
    // http://aggregate.org/MAGIC/#Most%20Significant%201%20Bit
    var x = v
    x = x or (x ushr 1)
    x = x or (x ushr 2)
    x = x or (x ushr 4)
    x = x or (x ushr 8)
    x = x or (x ushr 16)
    x = x or (x ushr 32)

    // Count the number of 1s
    // http://aggregate.org/MAGIC/#Population%20Count%20(Ones%20Count)
    x -= x ushr 1 and 0x5555555555555555
    x = (x ushr 2 and 0x3333333333333333) + (x and 0x3333333333333333)
    x = (x ushr 4) + x and 0x0f0f0f0f0f0f0f0f
    x += x ushr 8
    x += x ushr 16
    x = (x and 0x3f) + ((x ushr 32) and 0x3f)

    // Round up to the nearest full byte
    val width = ((x + 3) / 4).toInt()

    writeToInternalBuffer { buffer ->
        val tail = buffer.writableSegment(width)
        val data = tail.data
        var pos = tail.limit + width - 1
        val start = tail.limit
        while (pos >= start) {
            data[pos] = HEX_DIGIT_BYTES[(v and 0xF).toInt()]
            v = v ushr 4
            pos--
        }
        tail.limit += width
        buffer.size += width.toLong()
    }
}

/**
 * Writes am unsigned byte to this sink.
 *
 * @param byte the byte to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeUByte
 */
public fun Sink.writeUByte(byte: UByte) {
    writeByte(byte.toByte())
}

/**
 * Writes two bytes containing [short], in the big-endian order, to this sink.
 *
 * @param short the unsigned short integer to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeUShort
 */
public fun Sink.writeUShort(short: UShort) {
    writeShort(short.toShort())
}

/**
 * Writes four bytes containing [int], in the big-endian order, to this sink.
 *
 * @param int the unsigned integer to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeUInt
 */
public fun Sink.writeUInt(int: UInt) {
    writeInt(int.toInt())
}

/**
 * Writes eight bytes containing [long], in the big-endian order, to this sink.
 *
 * @param long the unsigned long integer to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeULong
 */
public fun Sink.writeULong(long: ULong) {
    writeLong(long.toLong())
}

/**
 * Writes two bytes containing [short], in the little-endian order, to this sink.
 *
 * @param short the unsigned short integer to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeUShortLe
 */
public fun Sink.writeUShortLe(short: UShort) {
    writeShortLe(short.toShort())
}

/**
 * Writes four bytes containing [int], in the little-endian order, to this sink.
 *
 * @param int the unsigned integer to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeUIntLe
 */
public fun Sink.writeUIntLe(int: UInt) {
    writeIntLe(int.toInt())
}

/**
 * Writes eight bytes containing [long], in the little-endian order, to this sink.
 *
 * @param long the unsigned long integer to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeULongLe
 */
public fun Sink.writeULongLe(long: ULong) {
    writeLongLe(long.toLong())
}

/**
 * Provides direct access to the sink's internal buffer and hints its emit before exit.
 *
 * The internal buffer is passed into [lambda],
 * and it may be partially emitted to the underlying sink before returning from this method.
 *
 * Use this method with care as the data within the buffer is not yet emitted to the underlying sink
 * and consumption of data from the buffer will cause its loss.
 *
 * @param lambda the callback accessing internal buffer.
 *
 * @throws IllegalStateException when the sink is closed.
 */
@DelicateIoApi
@OptIn(InternalIoApi::class)
public inline fun Sink.writeToInternalBuffer(lambda: (Buffer) -> Unit) {
    lambda(this.buffer)
    this.hintEmit()
}