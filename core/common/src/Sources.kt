/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlin.math.min

/**
 * Removes two bytes from this source and returns a short integer composed of it according to the little-endian order.
 *
 * @throws EOFException when there are not enough data to read a short value.
 * @throws IllegalStateException when the source is closed.
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
            if (buffer[1] !in '0'.code..'9'.code) {
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * Note that in Kotlin/JS a value obtained by writing an original [Float] value to a [Sink] using
 * [Sink.writeFloat] and then reading it back using [Source.readFloat] may not be equal to the original value.
 * Please refer to [Float.Companion.fromBits] documentation for details.
 *
 * @throws EOFException when there are not enough data to read an unsigned int value.
 * @throws IllegalStateException when the source is closed.
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * Note that in Kotlin/JS a value obtained by writing an original [Float] value to a [Sink] using
 * [Sink.writeFloatLe] and then reading it back using [Source.readFloatLe] may not be equal to the original value.
 * Please refer to [Float.Companion.fromBits] documentation for details.
 *
 * @throws EOFException when there are not enough data to read an unsigned int value.
 * @throws IllegalStateException when the source is closed.
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.startsWithSample
 */
@OptIn(InternalIoApi::class)
public fun Source.startsWith(byte: Byte): Boolean = request(1) && buffer[0] == byte

/**
 * Removes exactly `endIndex - startIndex` [Short] values from this source
 * and copies them into [sink] subrange starting at [startIndex] and ending at [endIndex].
 * Each short value is read in big-endian byte order, the same way [Source.readShort] reads values.
 *
 * @param sink the array to write data to
 * @param startIndex the startIndex (inclusive) of the [sink] subrange to read data into, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [sink] subrange to read data into, `sink.size` by default.
 *
 * @throws EOFException when the requested number of bytes cannot be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [sink] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readToShortArraySample
 */
public fun Source.readTo(sink: ShortArray, startIndex: Int = 0, endIndex: Int = sink.size) {
    checkBounds(sink.size, startIndex, endIndex)
    readArrayImpl(
        sink,
        startIndex,
        endIndex,
        Short.SIZE_BYTES,
        ShortArray::set,
        ByteArray::uncheckedLoadShortAt,
        Buffer::readShort
    )
}

/**
 * Removes exactly `endIndex - startIndex` [Int] values from this source
 * and copies them into [sink] subrange starting at [startIndex] and ending at [endIndex].
 * Each int value is read in big-endian byte order, the same way [Source.readInt] reads values.
 *
 * @param sink the array to write data to
 * @param startIndex the startIndex (inclusive) of the [sink] subrange to read data into, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [sink] subrange to read data into, `sink.size` by default.
 *
 * @throws EOFException when the requested number of bytes cannot be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [sink] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readToIntArraySample
 */
public fun Source.readTo(sink: IntArray, startIndex: Int = 0, endIndex: Int = sink.size) {
    checkBounds(sink.size, startIndex, endIndex)
    readArrayImpl(
        sink,
        startIndex,
        endIndex,
        Int.SIZE_BYTES,
        IntArray::set,
        ByteArray::uncheckedLoadIntAt,
        Buffer::readInt
    )
}

/**
 * Removes exactly `endIndex - startIndex` [Long] values from this source
 * and copies them into [sink] subrange starting at [startIndex] and ending at [endIndex].
 * Each long value is read in big-endian byte order, the same way [Source.readLong] reads values.
 *
 * @param sink the array to write data to
 * @param startIndex the startIndex (inclusive) of the [sink] subrange to read data into, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [sink] subrange to read data into, `sink.size` by default.
 *
 * @throws EOFException when the requested number of bytes cannot be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [sink] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readToLongArraySample
 */
public fun Source.readTo(sink: LongArray, startIndex: Int = 0, endIndex: Int = sink.size) {
    checkBounds(sink.size, startIndex, endIndex)
    readArrayImpl(
        sink,
        startIndex,
        endIndex,
        Long.SIZE_BYTES,
        LongArray::set,
        ByteArray::uncheckedLoadLongAt,
        Buffer::readLong
    )
}

/**
 * Removes exactly `endIndex - startIndex` [Float] values from this source
 * and copies them into [sink] subrange starting at [startIndex] and ending at [endIndex].
 * Each float value is read in big-endian byte order, the same way [Source.readFloat] reads values.
 *
 * The [Float.Companion.fromBits] function is used for decoding bytes into [Float].
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Float] value to a [Sink] using
 * [Sink.writeFloat] and then reading it back using [Source.readFloat] may not be equal to the original value.
 * The same limitation is applicable to this function.
 * Please refer to [Float.Companion.fromBits] documentation for details.
 *
 * @param sink the array to write data to
 * @param startIndex the startIndex (inclusive) of the [sink] subrange to read data into, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [sink] subrange to read data into, `sink.size` by default.
 *
 * @throws EOFException when the requested number of bytes cannot be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [sink] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readToFloatArraySample
 */
public fun Source.readTo(sink: FloatArray, startIndex: Int = 0, endIndex: Int = sink.size) {
    checkBounds(sink.size, startIndex, endIndex)
    readArrayImpl(
        sink,
        startIndex,
        endIndex,
        Float.SIZE_BYTES,
        FloatArray::set,
        ByteArray::uncheckedLoadFloatAt,
        Buffer::readFloat
    )
}

/**
 * Removes exactly `endIndex - startIndex` [Double] values from this source
 * and copies them into [sink] subrange starting at [startIndex] and ending at [endIndex].
 * Each double value is read in big-endian byte order, the same way [Source.readDouble] reads values.
 *
 * The [Double.Companion.fromBits] function is used for decoding bytes into [Double].
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Double] value to a [Sink] using
 * [Sink.writeDouble] and then reading it back using [Source.readDouble] may not be equal to the original value.
 * The same limitation is applicable to this function.
 * Please refer to [Double.Companion.fromBits] documentation for details.
 *
 * @param sink the array to write data to
 * @param startIndex the startIndex (inclusive) of the [sink] subrange to read data into, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [sink] subrange to read data into, `sink.size` by default.
 *
 * @throws EOFException when the requested number of bytes cannot be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [sink] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readToDoubleArraySample
 */
public fun Source.readTo(sink: DoubleArray, startIndex: Int = 0, endIndex: Int = sink.size) {
    checkBounds(sink.size, startIndex, endIndex)
    readArrayImpl(
        sink,
        startIndex,
        endIndex,
        Double.SIZE_BYTES,
        DoubleArray::set,
        ByteArray::uncheckedLoadDoubleAt,
        Buffer::readDouble
    )
}

/**
 * Removes exactly `endIndex - startIndex` [Short] values from this source
 * and copies them into [sink] subrange starting at [startIndex] and ending at [endIndex].
 * Each short value is read in little-endian byte order, the same way [Source.readShortLe] reads values.
 *
 * @param sink the array to write data to
 * @param startIndex the startIndex (inclusive) of the [sink] subrange to read data into, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [sink] subrange to read data into, `sink.size` by default.
 *
 * @throws EOFException when the requested number of bytes cannot be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [sink] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readToShortArraySample
 */
public fun Source.readLeTo(sink: ShortArray, startIndex: Int = 0, endIndex: Int = sink.size) {
    checkBounds(sink.size, startIndex, endIndex)
    readArrayImpl(
        sink,
        startIndex,
        endIndex,
        Short.SIZE_BYTES,
        ShortArray::set,
        ByteArray::uncheckedLoadShortLeAt,
        Buffer::readShortLe
    )
}

/**
 * Removes exactly `endIndex - startIndex` [Int] values from this source
 * and copies them into [sink] subrange starting at [startIndex] and ending at [endIndex].
 * Each int value is read in little-endian byte order, the same way [Source.readIntLe] reads values.
 *
 * @param sink the array to write data to
 * @param startIndex the startIndex (inclusive) of the [sink] subrange to read data into, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [sink] subrange to read data into, `sink.size` by default.
 *
 * @throws EOFException when the requested number of bytes cannot be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [sink] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readToIntArraySample
 */
public fun Source.readLeTo(sink: IntArray, startIndex: Int = 0, endIndex: Int = sink.size) {
    checkBounds(sink.size, startIndex, endIndex)
    readArrayImpl(
        sink,
        startIndex,
        endIndex,
        Int.SIZE_BYTES,
        IntArray::set,
        ByteArray::uncheckedLoadIntLeAt,
        Buffer::readIntLe
    )
}

/**
 * Removes exactly `endIndex - startIndex` [Long] values from this source
 * and copies them into [sink] subrange starting at [startIndex] and ending at [endIndex].
 * Each long value is read in little-endian byte order, the same way [Source.readLongLe] reads values.
 *
 * @param sink the array to write data to
 * @param startIndex the startIndex (inclusive) of the [sink] subrange to read data into, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [sink] subrange to read data into, `sink.size` by default.
 *
 * @throws EOFException when the requested number of bytes cannot be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [sink] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readToLongArraySample
 */
public fun Source.readLeTo(sink: LongArray, startIndex: Int = 0, endIndex: Int = sink.size) {
    checkBounds(sink.size, startIndex, endIndex)
    readArrayImpl(
        sink,
        startIndex,
        endIndex,
        Long.SIZE_BYTES,
        LongArray::set,
        ByteArray::uncheckedLoadLongLeAt,
        Buffer::readLongLe
    )
}

/**
 * Removes exactly `endIndex - startIndex` [Float] values from this source
 * and copies them into [sink] subrange starting at [startIndex] and ending at [endIndex].
 * Each float value is read in little-endian byte order, the same way [Source.readFloatLe] reads values.
 *
 * The [Float.Companion.fromBits] function is used for decoding bytes into [Float].
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Float] value to a [Sink] using
 * [Sink.writeFloatLe] and then reading it back using [Source.readFloatLe] may not be equal to the original value.
 * The same limitation is applicable to this function.
 * Please refer to [Float.Companion.fromBits] documentation for details.
 *
 * @param sink the array to write data to
 * @param startIndex the startIndex (inclusive) of the [sink] subrange to read data into, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [sink] subrange to read data into, `sink.size` by default.
 *
 * @throws EOFException when the requested number of bytes cannot be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [sink] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readToFloatArraySample
 */
public fun Source.readLeTo(sink: FloatArray, startIndex: Int = 0, endIndex: Int = sink.size) {
    checkBounds(sink.size, startIndex, endIndex)
    readArrayImpl(
        sink,
        startIndex,
        endIndex,
        Float.SIZE_BYTES,
        FloatArray::set,
        ByteArray::uncheckedLoadFloatLeAt,
        Buffer::readFloatLe
    )
}

/**
 * Removes exactly `endIndex - startIndex` [Double] values from this source
 * and copies them into [sink] subrange starting at [startIndex] and ending at [endIndex].
 * Each double value is read in little-endian byte order, the same way [Source.readDoubleLe] reads values.
 *
 * The [Double.Companion.fromBits] function is used for decoding bytes into [Double].
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Double] value to a [Sink] using
 * [Sink.writeDoubleLe] and then reading it back using [Source.readDoubleLe] may not be equal to the original value.
 * The same limitation is applicable to this function.
 * Please refer to [Double.Companion.fromBits] documentation for details.
 *
 * @param sink the array to write data to
 * @param startIndex the startIndex (inclusive) of the [sink] subrange to read data into, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [sink] subrange to read data into, `sink.size` by default.
 *
 * @throws EOFException when the requested number of bytes cannot be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [sink] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readToDoubleArraySample
 */
public fun Source.readLeTo(sink: DoubleArray, startIndex: Int = 0, endIndex: Int = sink.size) {
    checkBounds(sink.size, startIndex, endIndex)
    readArrayImpl(
        sink,
        startIndex,
        endIndex,
        Double.SIZE_BYTES,
        DoubleArray::set,
        ByteArray::uncheckedLoadDoubleLeAt,
        Buffer::readDoubleLe
    )
}

/**
 * Reads [size] [Short] values from this source and returns them as a new array.
 * Each short value is read in big-endian byte order, the same way [Source.readShort] reads values.
 *
 * @param size the number of values that should be read from the source.
 *
 * @throws IllegalArgumentException when [size] is negative.
 * @throws EOFException when the underlying source is exhausted before [size] values could be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readShortArraySample
 */
public fun Source.readShortArray(size: Int): ShortArray {
    checkSize(size)
    val array = ShortArray(prefetchArrayData(size, Short.SIZE_BYTES))
    readTo(array, 0, array.size)
    return array
}

/**
 * Reads [Short] values from this source until the source is exhausted and returns them as a new array.
 *
 * Each short value is read in big-endian byte order, the same way [Source.readShort] reads values.
 *
 * @throws IllegalStateException when the source is closed.
 * @throws IllegalStateException when the source contains more than [Int.MAX_VALUE] values.
 * @throws IllegalStateException when a number of bytes contained in the source is not a multiple of [Short.SIZE_BYTES].
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readShortArraySample
 */
public fun Source.readShortArray(): ShortArray {
    val array = ShortArray(prefetchArrayData(-1, Short.SIZE_BYTES))
    readTo(array, 0, array.size)
    return array
}

/**
 * Reads [size] [Int] values from this source and returns them as a new array.
 * Each int value is read in big-endian byte order, the same way [Source.readInt] reads values.
 *
 * @param size the number of values that should be read from the source.
 *
 * @throws IllegalArgumentException when [size] is negative.
 * @throws EOFException when the underlying source is exhausted before [size] values could be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readIntArraySample
 */
public fun Source.readIntArray(size: Int): IntArray {
    checkSize(size)
    val array = IntArray(prefetchArrayData(size, Int.SIZE_BYTES))
    readTo(array, 0, array.size)
    return array
}

/**
 * Reads [Int] values from this source until the source is exhausted and returns them as a new array.
 *
 * Each int value is read in big-endian byte order, the same way [Source.readInt] reads values.
 *
 * @throws IllegalStateException when the source is closed.
 * @throws IllegalStateException when the source contains more than [Int.MAX_VALUE] values.
 * @throws IllegalStateException when a number of bytes contained in the source is not a multiple of [Int.SIZE_BYTES].
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readIntArraySample
 */
public fun Source.readIntArray(): IntArray {
    val array = IntArray(prefetchArrayData(-1, Int.SIZE_BYTES))
    readTo(array, 0, array.size)
    return array
}

/**
 * Reads [Long] values from this source until the source is exhausted and returns them as a new array.
 *
 * Each long value is read in big-endian byte order, the same way [Source.readLong] reads values.
 *
 * @throws IllegalStateException when the source is closed.
 * @throws IllegalStateException when the source contains more than [Int.MAX_VALUE] values.
 * @throws IllegalStateException when a number of bytes contained in the source is not a multiple of [Long.SIZE_BYTES].
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readLongArraySample
 */
public fun Source.readLongArray(): LongArray {
    val array = LongArray(prefetchArrayData(-1, Long.SIZE_BYTES))
    readTo(array, 0, array.size)
    return array
}

/**
 * Reads [size] [Long] values from this source and returns them as a new array.
 * Each long value is read in big-endian byte order, the same way [Source.readLong] reads values.
 *
 * @param size the number of values that should be read from the source.
 *
 * @throws IllegalArgumentException when [size] is negative.
 * @throws EOFException when the underlying source is exhausted before [size] values could be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readLongArraySample
 */
public fun Source.readLongArray(size: Int): LongArray {
    checkSize(size)
    val array = LongArray(prefetchArrayData(size, Long.SIZE_BYTES))
    readTo(array, 0, array.size)
    return array
}

/**
 * Reads [Float] values from this source until the source is exhausted and returns them as a new array.
 *
 * Each float value is read in big-endian byte order, the same way [Source.readFloat] reads values.
 *
 * The [Float.Companion.fromBits] function is used for decoding bytes into [Float].
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Float] value to a [Sink] using
 * [Sink.writeFloat] and then reading it back using [Source.readFloat] may not be equal to the original value.
 * The same limitation is applicable to this function.
 * Please refer to [Float.Companion.fromBits] documentation for details.
 *
 * @throws IllegalStateException when the source is closed.
 * @throws IllegalStateException when the source contains more than [Int.MAX_VALUE] values.
 * @throws IllegalStateException when a number of bytes contained in the source is not a multiple of [Float.SIZE_BYTES].
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readFloatArraySample
 */
public fun Source.readFloatArray(): FloatArray {
    val array = FloatArray(prefetchArrayData(-1, Float.SIZE_BYTES))
    readTo(array, 0, array.size)
    return array
}

/**
 * Reads [size] [Float] values from this source and returns them as a new array.
 * Each float value is read in big-endian byte order, the same way [Source.readFloat] reads values.
 *
 * The [Float.Companion.fromBits] function is used for decoding bytes into [Float].
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Float] value to a [Sink] using
 * [Sink.writeFloat] and then reading it back using [Source.readFloat] may not be equal to the original value.
 * The same limitation is applicable to this function.
 * Please refer to [Float.Companion.fromBits] documentation for details.
 *
 * @param size the number of values that should be read from the source.
 *
 * @throws IllegalArgumentException when [size] is negative.
 * @throws EOFException when the underlying source is exhausted before [size] values could be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readFloatArraySample
 */
public fun Source.readFloatArray(size: Int): FloatArray {
    checkSize(size)
    val array = FloatArray(prefetchArrayData(size, Float.SIZE_BYTES))
    readTo(array, 0, array.size)
    return array
}

/**
 * Reads [Double] values from this source until the source is exhausted and returns them as a new array.
 *
 * Each double value is read in big-endian byte order, the same way [Source.readDouble] reads values.
 *
 * The [Double.Companion.fromBits] function is used for decoding bytes into [Double].
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Double] value to a [Sink] using
 * [Sink.writeDouble] and then reading it back using [Source.readDouble] may not be equal to the original value.
 * The same limitation is applicable to this function.
 * Please refer to [Double.Companion.fromBits] documentation for details.
 *
 * @throws IllegalStateException when the source is closed.
 * @throws IllegalStateException when the source contains more than [Int.MAX_VALUE] values.
 * @throws IllegalStateException when a number of bytes contained in the source is not a multiple of [Double.SIZE_BYTES].
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readDoubleArraySample
 */
public fun Source.readDoubleArray(): DoubleArray {
    val array = DoubleArray(prefetchArrayData(-1, Double.SIZE_BYTES))
    readTo(array, 0, array.size)
    return array
}

/**
 * Reads [size] [Double] values from this source and returns them as a new array.
 * Each double value is read in big-endian byte order, the same way [Source.readDouble] reads values.
 *
 * The [Double.Companion.fromBits] function is used for decoding bytes into [Double].
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Double] value to a [Sink] using
 * [Sink.writeDouble] and then reading it back using [Source.readDouble] may not be equal to the original value.
 * The same limitation is applicable to this function.
 * Please refer to [Double.Companion.fromBits] documentation for details.
 *
 * @param size the number of values that should be read from the source.
 *
 * @throws IllegalArgumentException when [size] is negative.
 * @throws EOFException when the underlying source is exhausted before [size] values could be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readDoubleArraySample
 */
public fun Source.readDoubleArray(size: Int): DoubleArray {
    checkSize(size)
    val array = DoubleArray(prefetchArrayData(size, Double.SIZE_BYTES))
    readTo(array, 0, array.size)
    return array
}

/**
 * Reads [Short] values from this source until the source is exhausted and returns them as a new array.
 *
 * Each short value is read in little-endian byte order, the same way [Source.readShortLe] reads values.
 *
 * @throws IllegalStateException when the source is closed.
 * @throws IllegalStateException when the source contains more than [Int.MAX_VALUE] values.
 * @throws IllegalStateException when a number of bytes contained in the source is not a multiple of [Short.SIZE_BYTES].
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readShortArraySample
 */
public fun Source.readShortLeArray(size: Int): ShortArray {
    checkSize(size)
    val array = ShortArray(prefetchArrayData(size, Short.SIZE_BYTES))
    readLeTo(array, 0, array.size)
    return array
}

/**
 * Reads [size] [Short] values from this source and returns them as a new array.
 * Each short value is read in little-endian byte order, the same way [Source.readShortLe] reads values.
 *
 * @param size the number of values that should be read from the source.
 *
 * @throws IllegalArgumentException when [size] is negative.
 * @throws EOFException when the underlying source is exhausted before [size] values could be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readShortArraySample
 */
public fun Source.readShortLeArray(): ShortArray {
    val array = ShortArray(prefetchArrayData(-1, Short.SIZE_BYTES))
    readLeTo(array, 0, array.size)
    return array
}

/**
 * Reads [size] [Int] values from this source and returns them as a new array.
 * Each int value is read in little-endian byte order, the same way [Source.readIntLe] reads values.
 *
 * @param size the number of values that should be read from the source.
 *
 * @throws IllegalArgumentException when [size] is negative.
 * @throws EOFException when the underlying source is exhausted before [size] values could be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readIntArraySample
 */
public fun Source.readIntLeArray(size: Int): IntArray {
    checkSize(size)
    val array = IntArray(prefetchArrayData(size, Int.SIZE_BYTES))
    readLeTo(array, 0, array.size)
    return array
}

/**
 * Reads [Int] values from this source until the source is exhausted and returns them as a new array.
 *
 * Each int value is read in little-endian byte order, the same way [Source.readIntLe] reads values.
 *
 * @throws IllegalStateException when the source is closed.
 * @throws IllegalStateException when the source contains more than [Int.MAX_VALUE] values.
 * @throws IllegalStateException when a number of bytes contained in the source is not a multiple of [Int.SIZE_BYTES].
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readIntArraySample
 */
public fun Source.readIntLeArray(): IntArray {
    val array = IntArray(prefetchArrayData(-1, Int.SIZE_BYTES))
    readLeTo(array, 0, array.size)
    return array
}

/**
 * Reads [Long] values from this source until the source is exhausted and returns them as a new array.
 *
 * Each long value is read in little-endian byte order, the same way [Source.readLongLe] reads values.
 *
 * @throws IllegalStateException when the source is closed.
 * @throws IllegalStateException when the source contains more than [Int.MAX_VALUE] values.
 * @throws IllegalStateException when a number of bytes contained in the source is not a multiple of [Long.SIZE_BYTES].
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readLongArraySample
 */
public fun Source.readLongLeArray(): LongArray {
    val array = LongArray(prefetchArrayData(-1, Long.SIZE_BYTES))
    readLeTo(array, 0, array.size)
    return array
}

/**
 * Reads [size] [Long] values from this source and returns them as a new array.
 * Each long value is read in little-endian byte order, the same way [Source.readLongLe] reads values.
 *
 * @param size the number of values that should be read from the source.
 *
 * @throws IllegalArgumentException when [size] is negative.
 * @throws EOFException when the underlying source is exhausted before [size] values could be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readLongArraySample
 */
public fun Source.readLongLeArray(size: Int): LongArray {
    checkSize(size)
    val array = LongArray(prefetchArrayData(size, Long.SIZE_BYTES))
    readLeTo(array, 0, array.size)
    return array
}

/**
 * Reads [Float] values from this source until the source is exhausted and returns them as a new array.
 *
 * Each float value is read in little-endian byte order, the same way [Source.readFloatLe] reads values.
 *
 * The [Float.Companion.fromBits] function is used for decoding bytes into [Float].
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Float] value to a [Sink] using
 * [Sink.writeFloatLe] and then reading it back using [Source.readFloatLe] may not be equal to the original value.
 * The same limitation is applicable to this function.
 * Please refer to [Float.Companion.fromBits] documentation for details.
 *
 * @throws IllegalStateException when the source is closed.
 * @throws IllegalStateException when the source contains more than [Int.MAX_VALUE] values.
 * @throws IllegalStateException when a number of bytes contained in the source is not a multiple of [Float.SIZE_BYTES].
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readFloatArraySample
 */
public fun Source.readFloatLeArray(): FloatArray {
    val array = FloatArray(prefetchArrayData(-1, Float.SIZE_BYTES))
    readLeTo(array, 0, array.size)
    return array
}

/**
 * Reads [size] [Float] values from this source and returns them as a new array.
 * Each float value is read in little-endian byte order, the same way [Source.readFloatLe] reads values.
 *
 * The [Float.Companion.fromBits] function is used for decoding bytes into [Float].
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Float] value to a [Sink] using
 * [Sink.writeFloatLe] and then reading it back using [Source.readFloatLe] may not be equal to the original value.
 * The same limitation is applicable to this function.
 * Please refer to [Float.Companion.fromBits] documentation for details.
 *
 * @param size the number of values that should be read from the source.
 *
 * @throws IllegalArgumentException when [size] is negative.
 * @throws EOFException when the underlying source is exhausted before [size] values could be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readFloatArraySample
 */
public fun Source.readFloatLeArray(size: Int): FloatArray {
    checkSize(size)
    val array = FloatArray(prefetchArrayData(size, Float.SIZE_BYTES))
    readLeTo(array, 0, array.size)
    return array
}

/**
 * Reads [Double] values from this source until the source is exhausted and returns them as a new array.
 *
 * Each double value is read in little-endian byte order, the same way [Source.readDoubleLe] reads values.
 *
 * The [Double.Companion.fromBits] function is used for decoding bytes into [Double].
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Double] value to a [Sink] using
 * [Sink.writeDoubleLe] and then reading it back using [Source.readDoubleLe] may not be equal to the original value.
 * The same limitation is applicable to this function.
 * Please refer to [Double.Companion.fromBits] documentation for details.
 *
 * @throws IllegalStateException when the source is closed.
 * @throws IllegalStateException when the source contains more than [Int.MAX_VALUE] values.
 * @throws IllegalStateException when a number of bytes contained in the source is not a multiple of [Double.SIZE_BYTES].
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readDoubleArraySample
 */
public fun Source.readDoubleLeArray(): DoubleArray {
    val array = DoubleArray(prefetchArrayData(-1, Double.SIZE_BYTES))
    readLeTo(array, 0, array.size)
    return array
}

/**
 * Reads [size] [Double] values from this source and returns them as a new array.
 * Each double value is read in little-endian byte order, the same way [Source.readDoubleLe] reads values.
 *
 * The [Double.Companion.fromBits] function is used for decoding bytes into [Double].
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Double] value to a [Sink] using
 * [Sink.writeDoubleLe] and then reading it back using [Source.readDoubleLe] may not be equal to the original value.
 * The same limitation is applicable to this function.
 * Please refer to [Double.Companion.fromBits] documentation for details.
 *
 * @param size the number of values that should be read from the source.
 *
 * @throws IllegalArgumentException when [size] is negative.
 * @throws EOFException when the underlying source is exhausted before [size] values could be read.
 * @throws IllegalStateException when the source is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.readDoubleArraySample
 */
public fun Source.readDoubleLeArray(size: Int): DoubleArray {
    checkSize(size)
    val array = DoubleArray(prefetchArrayData(size, Double.SIZE_BYTES))
    readLeTo(array, 0, array.size)
    return array
}

@OptIn(InternalIoApi::class)
private fun Source.prefetchArrayData(arraySize: Int, elementSize: Int): Int {
    if (arraySize >= 0) {
        require(arraySize * elementSize.toLong())
        return arraySize
    }

    val maxFetchSize = Int.MAX_VALUE.toLong() * elementSize
    var fetchSize = maxFetchSize
    while (buffer.size < maxFetchSize && request(fetchSize)) {
        fetchSize = fetchSize * 2
    }
    check(buffer.size < maxFetchSize) { "Can't create an array of size ${buffer.size / elementSize}" }
    check(buffer.size % elementSize == 0L) {
        "Can't read the source in full as the number of available bytes (${buffer.size}) is not a multiple of an" +
                "array element type ($elementSize)"
    }
    return (buffer.size / elementSize).toInt()
}

@OptIn(InternalIoApi::class, UnsafeIoApi::class)
internal inline fun <Type, ArrayType> Source.readArrayImpl(
    sink: ArrayType,
    startIndex: Int,
    endIndex: Int,
    typeSizeInBytes: Int,
    setter: ArrayType.(Int, Type) -> Unit,
    reader: ByteArray.(Int) -> Type,
    partialReader: Buffer.() -> Type
) {
    var idx = startIndex
    while (idx < endIndex) {
        // The source has to contain at least a single element.
        if (!request(typeSizeInBytes.toLong())) {
            // According to `readTo` contract, the input will be consumed on error
            buffer.clear()
            throw EOFException(
                "Source exhausted before reading ${endIndex - startIndex} bytes. " +
                        "Only ${idx - startIndex} bytes were read."
            )
        }
        var hasValueSplitAmongSegments = false
        UnsafeBufferOperations.readFromHead(buffer) { arr, from, to ->
            val remaining = endIndex - idx
            val cap = min(remaining * typeSizeInBytes, to - from)
            val len = cap and (typeSizeInBytes - 1).inv()

            for (i in from until from + len step typeSizeInBytes) {
                setter(sink, idx++, reader(arr, i))
            }

            hasValueSplitAmongSegments = len != cap && idx < endIndex

            len
        }
        // hasValueSplitAmongSegments == true if a segment had less than typeSizeInBytes.
        // partialReader should perform all capacity checks on its own.
        if (hasValueSplitAmongSegments && request(typeSizeInBytes.toLong())) {
            setter(sink, idx++, partialReader(buffer))
        }
    }
}

internal expect inline fun ByteArray.uncheckedLoadShortAt(offset: Int): Short
internal expect inline fun ByteArray.uncheckedLoadShortLeAt(offset: Int): Short
internal expect inline fun ByteArray.uncheckedLoadIntAt(offset: Int): Int
internal expect inline fun ByteArray.uncheckedLoadIntLeAt(offset: Int): Int
internal expect inline fun ByteArray.uncheckedLoadLongAt(offset: Int): Long
internal expect inline fun ByteArray.uncheckedLoadLongLeAt(offset: Int): Long
internal expect inline fun ByteArray.uncheckedLoadFloatAt(offset: Int): Float
internal expect inline fun ByteArray.uncheckedLoadFloatLeAt(offset: Int): Float
internal expect inline fun ByteArray.uncheckedLoadDoubleAt(offset: Int): Double
internal expect inline fun ByteArray.uncheckedLoadDoubleLeAt(offset: Int): Double

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedLoadShortAtCommon(offset: Int): Short {
    return ((this[offset] and 0xff shl 8).or(this[offset + 1] and 0xff)).toShort()
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedLoadShortLeAtCommon(offset: Int): Short {
    return ((this[offset] and 0xff).or(this[offset + 1] and 0xff shl 8)).toShort()
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedLoadIntAtCommon(offset: Int): Int {
    return (this[offset] and 0xff shl 24)
        .or(this[offset + 1] and 0xff shl 16)
        .or(this[offset + 2] and 0xff shl 8)
        .or(this[offset + 3] and 0xff)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedLoadIntLeAtCommon(offset: Int): Int {
    return (this[offset] and 0xff)
        .or(this[offset + 1] and 0xff shl 8)
        .or(this[offset + 2] and 0xff shl 16)
        .or(this[offset + 3] and 0xff shl 24)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedLoadLongAtCommon(offset: Int): Long {
    return (this[offset] and 0xffL shl 56)
        .or(this[offset + 1] and 0xffL shl 48)
        .or(this[offset + 2] and 0xffL shl 40)
        .or(this[offset + 3] and 0xffL shl 32)
        .or(this[offset + 4] and 0xffL shl 24)
        .or(this[offset + 5] and 0xffL shl 16)
        .or(this[offset + 6] and 0xffL shl 8)
        .or(this[offset + 7] and 0xffL)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedLoadLongLeAtCommon(offset: Int): Long {
    return (this[offset] and 0xffL)
        .or(this[offset + 1] and 0xffL shl 8)
        .or(this[offset + 2] and 0xffL shl 16)
        .or(this[offset + 3] and 0xffL shl 24)
        .or(this[offset + 4] and 0xffL shl 32)
        .or(this[offset + 5] and 0xffL shl 40)
        .or(this[offset + 6] and 0xffL shl 48)
        .or(this[offset + 7] and 0xffL shl 56)
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedLoadFloatAtCommon(offset: Int): Float =
    Float.fromBits(uncheckedLoadIntAt(offset))

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedLoadFloatLeAtCommon(offset: Int): Float =
    Float.fromBits(uncheckedLoadIntLeAt(offset))

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedLoadDoubleAtCommon(offset: Int): Double =
    Double.fromBits(uncheckedLoadLongAt(offset))

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedLoadDoubleLeAtCommon(offset: Int): Double =
    Double.fromBits(uncheckedLoadLongLeAt(offset))

@Suppress("NOTHING_TO_INLINE")
internal inline fun checkSize(size: Int) {
    require(size >= 0) { "size ($size) < 0" }
}
