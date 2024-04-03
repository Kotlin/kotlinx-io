/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

/**
 * Removes two bytes from this source and returns a short integer composed of it according to the little-endian order.
 *
 * @throws EOFException when there are not enough data to read a short value.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readShortLe
 */
public fun Source.readShortLe(): Short {
    return readShort().reverseBytes()
}

/**
 * Removes four bytes from this source and returns an integer composed of it according to the little-endian order.
 *
 * @throws EOFException when there are not enough data to read an int value.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readIntLe
 */
public fun Source.readIntLe(): Int {
    return readInt().reverseBytes()
}

/**
 * Removes eight bytes from this source and returns a long integer composed of it according to the little-endian order.
 *
 * @throws EOFException when there are not enough data to read a long value.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readLongLe
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
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readDecimalLong
 */
@OptIn(InternalIoApi::class)
public fun Source.readDecimalLong(): Long {
    require(1L)

    var negative = false
    var value = 0L
    var overflowDigit = OVERFLOW_DIGIT_START
    when (val b = buffer[0]) {
        '-'.code.toByte() -> {
            negative = true
            overflowDigit--
            require(2)
            if (buffer[1] !in '0'.code .. '9'.code) {
                throw NumberFormatException("Expected a digit but was 0x${buffer[1].toHexString()}")
            }
        }
        in '0'.code..'9'.code -> {
            value = ('0'.code - b).toLong()
        }
        else -> {
            throw NumberFormatException("Expected a digit or '-' but was 0x${b.toHexString()}")
        }
    }

    var bufferOffset = 1L
    while (request(bufferOffset + 1)) {
        val finished = buffer.seek(bufferOffset) { seg, offset ->
            seg!!
            var currIdx = (bufferOffset - offset).toInt()
            val size = seg.size
            while (currIdx < size) {
                val b = seg.getUnchecked(currIdx)
                if (b in '0'.code..'9'.code) {
                    val digit = '0'.code - b

                    // Detect when the digit would cause an overflow.
                    if (value < OVERFLOW_ZONE || value == OVERFLOW_ZONE && digit < overflowDigit) {
                        with(Buffer()) {
                            writeDecimalLong(value)
                            writeByte(b)

                            if (!negative) readByte() // Skip negative sign.
                            throw NumberFormatException("Number too large: ${readString()}")
                        }
                    }
                    value = value * 10L + digit
                    currIdx++
                    bufferOffset++
                } else {
                    return@seek true
                }
            }
            false
        }
        if (finished) break
    }
    skip(bufferOffset)

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
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readHexLong
 */
@OptIn(InternalIoApi::class)
public fun Source.readHexadecimalUnsignedLong(): Long {
    require(1)

    var result = when (val b = buffer[0]) {
        in '0'.code..'9'.code -> b - '0'.code
        in 'a'.code..'f'.code -> b - 'a'.code + 10
        in 'A'.code..'F'.code -> b - 'A'.code + 10
        else -> throw NumberFormatException("Expected leading [0-9a-fA-F] character but was 0x${b.toHexString()}")
    }.toLong()

    var bytesRead = 1L
    while (request(bytesRead + 1L)) {
        val stop = buffer.seek(bytesRead) { seg, offset ->
            seg!!
            val startIndex = (bytesRead - offset).toInt()
            for (localOffset in startIndex until seg.size) {
                val b = seg.getUnchecked(localOffset)
                val bDigit = when (b) {
                    in '0'.code..'9'.code -> b - '0'.code
                    in 'a'.code..'f'.code -> b - 'a'.code + 10
                    in 'A'.code..'F'.code -> b - 'A'.code + 10
                    else -> return@seek true
                }
                if (result and -0x1000000000000000L != 0L) {
                    with(Buffer()) {
                        writeHexadecimalUnsignedLong(result)
                        writeByte(b)
                        throw NumberFormatException("Number too large: " + readString())
                    }
                }
                result = result.shl(4) + bDigit
                bytesRead++
            }
            false
        }
        if (stop) break
    }
    skip(bytesRead)
    return result
}

/**
 * Returns an index of [byte] first occurrence in the range of [startIndex] to [endIndex],
 * or `-1` when the range doesn't contain [byte].
 *
 * The scan terminates at either [endIndex] or source's exhaustion, whichever comes first. The
 * maximum number of bytes scanned is `toIndex-fromIndex`.
 * If [byte] not found in buffered data, [endIndex] is yet to be reached and the underlying source is not yet exhausted
 * then new data will be read from the underlying source into the buffer.
 *
 * @param byte the value to find.
 * @param startIndex the start of the range (inclusive) to find [byte], `0` by default.
 * @param endIndex the end of the range (exclusive) to find [byte], [Long.MAX_VALUE] by default.
 *
 * @throws IllegalStateException when the source is closed.
 * @throws IllegalArgumentException when `startIndex > endIndex` or either of indices is negative.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.indexOfByteSample
 */
@OptIn(InternalIoApi::class)
public fun Source.indexOf(byte: Byte, startIndex: Long = 0L, endIndex: Long = Long.MAX_VALUE): Long {
    require(startIndex in 0..endIndex) {
        if (endIndex < 0) {
            "startIndex ($startIndex) and endIndex ($endIndex) should be non negative"
        } else {
            "startIndex ($startIndex) is not within the range [0..endIndex($endIndex))"
        }
    }
    if (startIndex == endIndex) return -1L

    var offset = startIndex
    while (offset < endIndex && request(offset + 1)) {
        val idx = buffer.indexOf(byte, offset, minOf(endIndex, buffer.size))
        if (idx != -1L) {
            return idx
        }
        offset = buffer.size
    }
    return -1L
}

/**
 * Removes all bytes from this source and returns them as a byte array.
 *
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readToArraySample
 */
public fun Source.readByteArray(): ByteArray {
    return readByteArrayImpl(-1)
}

/**
 * Removes [byteCount] bytes from this source and returns them as a byte array.
 *
 * @param byteCount the number of bytes that should be read from the source.
 *
 * @throws IllegalArgumentException when [byteCount] is negative.
 * @throws EOFException when the underlying source is exhausted before [byteCount] bytes of data could be read.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readToArraySample
 */
public fun Source.readByteArray(byteCount: Int): ByteArray {
    checkByteCount(byteCount.toLong())
    return readByteArrayImpl(byteCount)
}

@OptIn(InternalIoApi::class)
private fun Source.readByteArrayImpl(size: Int): ByteArray {
    var arraySize = size
    if (size == -1) {
        var fetchSize = Int.MAX_VALUE.toLong()
        while (buffer.size < Int.MAX_VALUE && request(fetchSize)) {
            fetchSize *= 2
        }
        check(buffer.size < Int.MAX_VALUE) { "Can't create an array of size ${buffer.size}" }
        arraySize = buffer.size.toInt()
    } else {
        require(size.toLong())
    }
    val array = ByteArray(arraySize)
    buffer.readTo(array)
    return array
}


/**
 * Removes exactly `endIndex - startIndex` bytes from this source and copies them into [sink] subrange starting at
 * [startIndex] and ending at [endIndex].
 *
 * @param sink the array to write data to
 * @param startIndex the startIndex (inclusive) of the [sink] subrange to read data into, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [sink] subrange to read data into, `sink.size` by default.
 *
 * @throws EOFException when the requested number of bytes cannot be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [sink] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readToArraySample
 */
public fun Source.readTo(sink: ByteArray, startIndex: Int = 0, endIndex: Int = sink.size) {
    checkBounds(sink.size, startIndex, endIndex)
    var offset = startIndex
    while (offset < endIndex) {
        val bytesRead = readAtMostTo(sink, offset, endIndex)
        if (bytesRead == -1) {
            throw EOFException(
                "Source exhausted before reading ${endIndex - startIndex} bytes. " +
                        "Only $bytesRead bytes were read."
            )
        }
        offset += bytesRead
    }
}

/**
 * Removes an unsigned byte from this source and returns it.
 *
 * @throws EOFException when there are no more bytes to read.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readUByte
 */
public fun Source.readUByte(): UByte = readByte().toUByte()

/**
 * Removes two bytes from this source and returns an unsigned short integer composed of it
 * according to the big-endian order.
 *
 * @throws EOFException when there are not enough data to read an unsigned short value.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readUShort
 */
public fun Source.readUShort(): UShort = readShort().toUShort()

/**
 * Removes four bytes from this source and returns an unsigned integer composed of it
 * according to the big-endian order.
 *
 * @throws EOFException when there are not enough data to read an unsigned int value.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readUInt
 */
public fun Source.readUInt(): UInt = readInt().toUInt()

/**
 * Removes eight bytes from this source and returns an unsigned long integer composed of it
 * according to the big-endian order.
 *
 * @throws EOFException when there are not enough data to read an unsigned long value.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readULong
 */
public fun Source.readULong(): ULong = readLong().toULong()

/**
 * Removes two bytes from this source and returns an unsigned short integer composed of it
 * according to the little-endian order.
 *
 * @throws EOFException when there are not enough data to read an unsigned short value.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readUShortLe
 */
public fun Source.readUShortLe(): UShort = readShortLe().toUShort()

/**
 * Removes four bytes from this source and returns an unsigned integer composed of it
 * according to the little-endian order.
 *
 * @throws EOFException when there are not enough data to read an unsigned int value.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readUIntLe
 */
public fun Source.readUIntLe(): UInt = readIntLe().toUInt()

/**
 * Removes eight bytes from this source and returns an unsigned long integer composed of it
 * according to the little-endian order.
 *
 * @throws EOFException when there are not enough data to read an unsigned long value.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readULongLe
 */
public fun Source.readULongLe(): ULong = readLongLe().toULong()

/**
 * Removes four bytes from this source and returns a floating point number with type [Float] composed of it
 * according to the big-endian order.
 *
 * The [Float.Companion.fromBits] function is used for decoding bytes into [Float].
 *
 * @throws EOFException when there are not enough data to read an unsigned int value.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readFloat
 */
public fun Source.readFloat(): Float = Float.fromBits(readInt())

/**
 * Removes eight bytes from this source and returns a floating point number with type [Double] composed of it
 * according to the big-endian order.
 *
 * The [Double.Companion.fromBits] function is used for decoding bytes into [Double].
 *
 * @throws EOFException when there are not enough data to read an unsigned int value.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readDouble
 */
public fun Source.readDouble(): Double = Double.fromBits(readLong())

/**
 * Removes four bytes from this source and returns a floating point number with type [Float] composed of it
 * according to the little-endian order.
 *
 * The [Float.Companion.fromBits] function is used for decoding bytes into [Float].
 *
 * @throws EOFException when there are not enough data to read an unsigned int value.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readFloatLe
 */
public fun Source.readFloatLe(): Float = Float.fromBits(readIntLe())

/**
 * Removes eight bytes from this source and returns a floating point number with type [Double] composed of it
 * according to the little-endian order.
 *
 * The [Double.Companion.fromBits] function is used for decoding bytes into [Double].
 *
 * @throws EOFException when there are not enough data to read an unsigned int value.
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readDoubleLe
 */
public fun Source.readDoubleLe(): Double = Double.fromBits(readLongLe())

/**
 * Return `true` if the next byte to be consumed from this source is equal to [byte].
 * Otherwise, return `false` as well as when the source is exhausted.
 *
 * If there is no buffered data, this call will result in a fetch from the underlying source.
 *
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.startsWithSample
 */
@OptIn(InternalIoApi::class)
public fun Source.startsWith(byte: Byte): Boolean = request(1) && buffer[0] == byte
