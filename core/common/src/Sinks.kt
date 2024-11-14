/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.math.min

private val HEX_DIGIT_BYTES = ByteArray(16) {
    ((if (it < 10) '0'.code else ('a'.code - 10)) + it).toByte()
}

/**
 * Writes two bytes containing [short], in the little-endian order, to this sink.
 *
 * @param short the short integer to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeDecimalLong
 */
@OptIn(DelicateIoApi::class, UnsafeIoApi::class)
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
        UnsafeBufferOperations.writeToTail(buffer, width) { ctx, segment ->
            for (pos in width - 1 downTo if (negative) 1 else 0) {
                val digit = (v % 10).toByte()
                ctx.setUnchecked(segment, pos, HEX_DIGIT_BYTES[digit.toInt()])
                v /= 10
            }
            if (negative) {
                ctx.setUnchecked(segment, 0, '-'.code.toByte())
            }
            width
        }
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
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeHexLong
 */
@OptIn(DelicateIoApi::class, UnsafeIoApi::class)
public fun Sink.writeHexadecimalUnsignedLong(long: Long) {
    var v = long
    if (v == 0L) {
        // Both a shortcut and required since the following code can't handle zero.
        writeByte('0'.code.toByte())
        return
    }

    val width = hexNumberLength(v)

    writeToInternalBuffer { buffer ->
        UnsafeBufferOperations.writeToTail(buffer, width) { ctx, segment ->
            for (pos in width - 1 downTo 0) {
                ctx.setUnchecked(segment, pos, HEX_DIGIT_BYTES[v.toInt().and(0xF)])
                v = v ushr 4
            }
            width
        }
    }
}

/**
 * Writes am unsigned byte to this sink.
 *
 * @param byte the byte to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
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
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeULongLe
 */
public fun Sink.writeULongLe(long: ULong) {
    writeLongLe(long.toLong())
}

/**
 * Writes four bytes of a bit representation of [float], in the big-endian order, to this sink.
 * Bit representation of the [float] corresponds to the IEEE 754 floating-point "single format" bit layout.
 *
 * To obtain a bit representation, the [Float.toBits] function is used.
 *
 * Should be used with care when working with special values (like `NaN`) as bit patterns obtained for [Float.NaN] may vary depending on a platform.
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Float] value to a [Sink] using
 * [Sink.writeFloat] and then reading it back using [Source.readFloat] may not be equal to the original value.
 * Please refer to [Float.toBits] documentation for details.
 *
 * @param float the floating point number to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeFloat
 */
public fun Sink.writeFloat(float: Float) {
    writeInt(float.toBits())
}

/**
 * Writes eight bytes of a bit representation of [double], in the big-endian order, to this sink.
 * Bit representation of the [double] corresponds to the IEEE 754 floating-point "double format" bit layout.
 *
 * To obtain a bit representation, the [Double.toBits] function is used.
 *
 * Should be used with care when working with special values (like `NaN`) as bit patterns obtained for [Double.NaN] may vary depending on a platform.
 *
 * @param double the floating point number to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeDouble
 */
public fun Sink.writeDouble(double: Double) {
    writeLong(double.toBits())
}

/**
 * Writes four bytes of a bit representation of [float], in the little-endian order, to this sink.
 * Bit representation of the [float] corresponds to the IEEE 754 floating-point "single format" bit layout.
 *
 * To obtain a bit representation, the [Float.toBits] function is used.
 *
 * Should be used with care when working with special values (like `NaN`) as bit patterns obtained for [Float.NaN] may vary depending on a platform.
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Float] value to a [Sink] using
 * [Sink.writeFloatLe] and then reading it back using [Source.readFloatLe] may not be equal to the original value.
 * Please refer to [Float.toBits] documentation for details.
 *
 * @param float the floating point number to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeFloatLe
 */
public fun Sink.writeFloatLe(float: Float) {
    writeIntLe(float.toBits())
}

/**
 * Writes eight bytes of a bit representation of [double], in the little-endian order, to this sink.
 * Bit representation of the [double] corresponds to the IEEE 754 floating-point "double format" bit layout.
 *
 * To obtain a bit representation, the [Double.toBits] function is used.
 *
 * Should be used with care when working with special values (like `NaN`) as bit patterns obtained for [Double.NaN] may vary depending on a platform.
 *
 * @param double the floating point number to be written.
 *
 * @throws IllegalStateException when the sink is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeDoubleLe
 */
public fun Sink.writeDoubleLe(double: Double) {
    writeLongLe(double.toBits())
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
 * @throws IOException when some I/O error occurs.
 */
@DelicateIoApi
@OptIn(InternalIoApi::class, ExperimentalContracts::class)
public inline fun Sink.writeToInternalBuffer(lambda: (Buffer) -> Unit) {
    contract {
        callsInPlace(lambda, EXACTLY_ONCE)
    }
    lambda(this.buffer)
    this.hintEmit()
}

/**
 * Writes [Short] values from [source] array or its subrange to this sink.
 * Each short value is written in big-endian byte order, the same way [Sink.writeShort] writes it.
 *
 * @param source the array from which value will be written into this sink.
 * @param startIndex the start index (inclusive) of the [source] subrange to be written, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [source] subrange to be written, size of the [source] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalStateException when the sink is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeShortArrayToSink
 */
public fun Sink.write(source: ShortArray, startIndex: Int = 0, endIndex: Int = source.size) {
    checkBounds(source.size, startIndex, endIndex)
    writeArrayImpl(source, startIndex, endIndex, Short.SIZE_BYTES, ShortArray::get, ByteArray::uncheckedStoreShortAt)
}

/**
 * Writes [Int] values from [source] array or its subrange to this sink.
 * Each int value is written in big-endian byte order, the same way [Sink.writeInt] writes it.
 *
 * @param source the array from which value will be written into this sink.
 * @param startIndex the start index (inclusive) of the [source] subrange to be written, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [source] subrange to be written, size of the [source] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalStateException when the sink is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeIntArrayToSink
 */
public fun Sink.write(source: IntArray, startIndex: Int = 0, endIndex: Int = source.size) {
    checkBounds(source.size, startIndex, endIndex)
    writeArrayImpl(source, startIndex, endIndex, Int.SIZE_BYTES, IntArray::get, ByteArray::uncheckedStoreIntAt)
}

/**
 * Writes [Long] values from [source] array or its subrange to this sink.
 * Each long value is written in big-endian byte order, the same way [Sink.writeLong] writes it.
 *
 * @param source the array from which value will be written into this sink.
 * @param startIndex the start index (inclusive) of the [source] subrange to be written, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [source] subrange to be written, size of the [source] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalStateException when the sink is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeLongArrayToSink
 */
public fun Sink.write(source: LongArray, startIndex: Int = 0, endIndex: Int = source.size) {
    checkBounds(source.size, startIndex, endIndex)
    writeArrayImpl(source, startIndex, endIndex, Long.SIZE_BYTES, LongArray::get, ByteArray::uncheckedStoreLongAt)
}

/**
 * Writes [Float] values from [source] array or its subrange to this sink.
 * Each float value is written in big-endian byte order, the same way [Sink.writeFloat] writes it.
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Float] value to a [Sink] using
 * [Sink.writeFloat] and then reading it back using [Source.readFloat] may not be equal to the original value.
 * The same limitation applies to this function.
 * Please refer to [Float.toBits] documentation for details.
 *
 * @param source the array from which value will be written into this sink.
 * @param startIndex the start index (inclusive) of the [source] subrange to be written, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [source] subrange to be written, size of the [source] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalStateException when the sink is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeFloatArrayToSink
 */
public fun Sink.write(source: FloatArray, startIndex: Int = 0, endIndex: Int = source.size) {
    checkBounds(source.size, startIndex, endIndex)
    writeArrayImpl(source, startIndex, endIndex, Float.SIZE_BYTES, FloatArray::get, ByteArray::uncheckedStoreFloatAt)
}

/**
 * Writes [Double] values from [source] array or its subrange to this sink.
 * Each double value is written in big-endian byte order, the same way [Sink.writeDouble] writes it.
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Double] value to a [Sink] using
 * [Sink.writeDouble] and then reading it back using [Source.readDouble] may not be equal to the original value.
 * The same limitation applies to this function.
 * Please refer to [Double.toBits] documentation for details.
 *
 * @param source the array from which value will be written into this sink.
 * @param startIndex the start index (inclusive) of the [source] subrange to be written, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [source] subrange to be written, size of the [source] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalStateException when the sink is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeDoubleArrayToSink
 */
public fun Sink.write(source: DoubleArray, startIndex: Int = 0, endIndex: Int = source.size) {
    checkBounds(source.size, startIndex, endIndex)
    writeArrayImpl(source, startIndex, endIndex, Double.SIZE_BYTES, DoubleArray::get, ByteArray::uncheckedStoreDoubleAt)
}

/**
 * Writes [Short] values from [source] array or its subrange to this sink.
 * Each short value is written in little-endian byte order, the same way [Sink.writeShortLe] writes it.
 *
 * @param source the array from which value will be written into this sink.
 * @param startIndex the start index (inclusive) of the [source] subrange to be written, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [source] subrange to be written, size of the [source] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalStateException when the sink is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeShortArrayToSink
 */
public fun Sink.writeLe(source: ShortArray, startIndex: Int = 0, endIndex: Int = source.size) {
    checkBounds(source.size, startIndex, endIndex)
    writeArrayImpl(source, startIndex, endIndex, Short.SIZE_BYTES, ShortArray::get, ByteArray::uncheckedStoreShortLeAt)
}

/**
 * Writes [Int] values from [source] array or its subrange to this sink.
 * Each int value is written in little-endian byte order, the same way [Sink.writeIntLe] writes it.
 *
 * @param source the array from which value will be written into this sink.
 * @param startIndex the start index (inclusive) of the [source] subrange to be written, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [source] subrange to be written, size of the [source] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalStateException when the sink is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeIntArrayToSink
 */
public fun Sink.writeLe(source: IntArray, startIndex: Int = 0, endIndex: Int = source.size) {
    checkBounds(source.size, startIndex, endIndex)
    writeArrayImpl(source, startIndex, endIndex, Int.SIZE_BYTES, IntArray::get, ByteArray::uncheckedStoreIntLeAt)
}

/**
 * Writes [Long] values from [source] array or its subrange to this sink.
 * Each long value is written in little-endian byte order, the same way [Sink.writeLongLe] writes it.
 *
 * @param source the array from which value will be written into this sink.
 * @param startIndex the start index (inclusive) of the [source] subrange to be written, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [source] subrange to be written, size of the [source] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalStateException when the sink is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeLongArrayToSink
 */
public fun Sink.writeLe(source: LongArray, startIndex: Int = 0, endIndex: Int = source.size) {
    checkBounds(source.size, startIndex, endIndex)
    writeArrayImpl(source, startIndex, endIndex, Long.SIZE_BYTES, LongArray::get, ByteArray::uncheckedStoreLongLeAt)
}

/**
 * Writes [Float] values from [source] array or its subrange to this sink.
 * Each float value is written in little-endian byte order, the same way [Sink.writeFloatLe] writes it.
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Float] value to a [Sink] using
 * [Sink.writeFloatLe] and then reading it back using [Source.readFloatLe] may not be equal to the original value.
 * The same limitation applies to this function.
 * Please refer to [Float.toBits] documentation for details.
 *
 * @param source the array from which value will be written into this sink.
 * @param startIndex the start index (inclusive) of the [source] subrange to be written, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [source] subrange to be written, size of the [source] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalStateException when the sink is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeFloatArrayToSink
 */
public fun Sink.writeLe(source: FloatArray, startIndex: Int = 0, endIndex: Int = source.size) {
    checkBounds(source.size, startIndex, endIndex)
    writeArrayImpl(source, startIndex, endIndex, Float.SIZE_BYTES, FloatArray::get, ByteArray::uncheckedStoreFloatLeAt)
}

/**
 * Writes [Double] values from [source] array or its subrange to this sink.
 * Each double value is written in little-endian byte order, the same way [Sink.writeDoubleLe] writes it.
 *
 * Note that in Kotlin/JS a value obtained by writing an original [Double] value to a [Sink] using
 * [Sink.writeDoubleLe] and then reading it back using [Source.readDoubleLe] may not be equal to the original value.
 * The same limitation applies to this function.
 * Please refer to [Double.toBits] documentation for details.
 *
 * @param source the array from which value will be written into this sink.
 * @param startIndex the start index (inclusive) of the [source] subrange to be written, 0 by default.
 * @param endIndex the endIndex (exclusive) of the [source] subrange to be written, size of the [source] by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalStateException when the sink is closed.
 * @throws IOException when some I/O error occurs.
 *
 * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeDoubleArrayToSink
 */
public fun Sink.writeLe(source: DoubleArray, startIndex: Int = 0, endIndex: Int = source.size) {
    checkBounds(source.size, startIndex, endIndex)
    writeArrayImpl(
        source,
        startIndex,
        endIndex,
        Double.SIZE_BYTES,
        DoubleArray::get,
        ByteArray::uncheckedStoreDoubleLeAt
    )
}

@OptIn(InternalIoApi::class, UnsafeIoApi::class)
internal inline fun <ArrayT, T> Sink.writeArrayImpl(
    source: ArrayT, startIndex: Int, endIndex: Int, typeSizeInBytes: Int,
    getter: ArrayT.(Int) -> T, setter: ByteArray.(Int, T) -> Unit
) {
    var idx = startIndex
    while (idx < endIndex) {
        UnsafeBufferOperations.writeToTail(buffer, typeSizeInBytes) { arr, from, to ->
            val cap = min(to - from, (endIndex - idx) * typeSizeInBytes)
            val len = cap and (typeSizeInBytes - 1).inv()
            for (i in from until from + len step typeSizeInBytes) {
                setter(arr, i, getter(source, idx++))
            }
            len
        }
    }
}

internal expect inline fun ByteArray.uncheckedStoreShortAt(idx: Int, value: Short)
internal expect inline fun ByteArray.uncheckedStoreShortLeAt(idx: Int, value: Short)
internal expect inline fun ByteArray.uncheckedStoreIntAt(idx: Int, value: Int)
internal expect inline fun ByteArray.uncheckedStoreIntLeAt(idx: Int, value: Int)
internal expect inline fun ByteArray.uncheckedStoreLongAt(idx: Int, value: Long)
internal expect inline fun ByteArray.uncheckedStoreLongLeAt(idx: Int, value: Long)
internal expect inline fun ByteArray.uncheckedStoreFloatAt(idx: Int, value: Float)
internal expect inline fun ByteArray.uncheckedStoreFloatLeAt(idx: Int, value: Float)
internal expect inline fun ByteArray.uncheckedStoreDoubleAt(idx: Int, value: Double)
internal expect inline fun ByteArray.uncheckedStoreDoubleLeAt(idx: Int, value: Double)

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedStoreShortAtCommon(idx: Int, value: Short) {
    this[idx] = (value.toInt() ushr 8 and 0xff).toByte()
    this[idx + 1] = (value.toInt() and 0xff).toByte()
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedStoreShortLeAtCommon(idx: Int, value: Short) {
    this[idx] = (value.toInt() and 0xff).toByte()
    this[idx + 1] = (value.toInt() ushr 8 and 0xff).toByte()
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedStoreIntAtCommon(idx: Int, value: Int) {
    this[idx] = (value ushr 24 and 0xff).toByte()
    this[idx + 1] = (value ushr 16 and 0xff).toByte()
    this[idx + 2] = (value ushr 8 and 0xff).toByte()
    this[idx + 3] = (value and 0xff).toByte()
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedStoreIntLeAtCommon(idx: Int, value: Int) {
    this[idx] = (value and 0xff).toByte()
    this[idx + 1] = (value ushr 8 and 0xff).toByte()
    this[idx + 2] = (value ushr 16 and 0xff).toByte()
    this[idx + 3] = (value ushr 24 and 0xff).toByte()
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedStoreLongAtCommon(idx: Int, value: Long) {
    this[idx] = (value ushr 56 and 0xffL).toByte()
    this[idx + 1] = (value ushr 48 and 0xffL).toByte()
    this[idx + 2] = (value ushr 40 and 0xffL).toByte()
    this[idx + 3] = (value ushr 32 and 0xffL).toByte()
    this[idx + 4] = (value ushr 24 and 0xffL).toByte()
    this[idx + 5] = (value ushr 16 and 0xffL).toByte()
    this[idx + 6] = (value ushr 8 and 0xffL).toByte()
    this[idx + 7] = (value and 0xffL).toByte()
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedStoreLongLeAtCommon(idx: Int, value: Long) {
    this[idx] = (value and 0xffL).toByte()
    this[idx + 1] = (value ushr 8 and 0xffL).toByte()
    this[idx + 2] = (value ushr 16 and 0xffL).toByte()
    this[idx + 3] = (value ushr 24 and 0xffL).toByte()
    this[idx + 4] = (value ushr 32 and 0xffL).toByte()
    this[idx + 5] = (value ushr 40 and 0xffL).toByte()
    this[idx + 6] = (value ushr 48 and 0xffL).toByte()
    this[idx + 7] = (value ushr 56 and 0xffL).toByte()
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedStoreFloatAtCommon(idx: Int, value: Float) =
    uncheckedStoreIntAt(idx, value.toBits())

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedStoreFloatLeAtCommon(idx: Int, value: Float) =
    uncheckedStoreIntLeAt(idx, value.toBits())

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedStoreDoubleAtCommon(idx: Int, value: Double) =
    uncheckedStoreLongAt(idx, value.toBits())

@Suppress("NOTHING_TO_INLINE")
internal inline fun ByteArray.uncheckedStoreDoubleLeAtCommon(idx: Int, value: Double) =
    uncheckedStoreLongLeAt(idx, value.toBits())
