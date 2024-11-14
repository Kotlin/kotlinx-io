/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BufferSourceShortArrayReadTest : AbstractShortArrayReadTest(SourceFactory.BUFFER)
class RealBufferedSourceShortArrayReadTest : AbstractShortArrayReadTest(SourceFactory.REAL_BUFFERED_SOURCE)
class OneByteAtATimeBufferedSourceShortArrayReadTest :
    AbstractShortArrayReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE)

class OneByteAtATimeBufferShortArrayReadTest : AbstractShortArrayReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFER)
class PeekBufferShortArrayReadTest : AbstractShortArrayReadTest(SourceFactory.PEEK_BUFFER)
class PeekBufferedSourceShortArrayReadTest : AbstractShortArrayReadTest(SourceFactory.PEEK_BUFFERED_SOURCE)


class BufferSourceIntArrayReadTest : AbstractIntArrayReadTest(SourceFactory.BUFFER)
class RealBufferedSourceIntArrayReadTest : AbstractIntArrayReadTest(SourceFactory.REAL_BUFFERED_SOURCE)
class OneByteAtATimeBufferedSourceIntArrayReadTest :
    AbstractIntArrayReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE)

class OneByteAtATimeBufferIntArrayReadTest : AbstractIntArrayReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFER)
class PeekBufferIntArrayReadTest : AbstractIntArrayReadTest(SourceFactory.PEEK_BUFFER)
class PeekBufferedSourceIntArrayReadTest : AbstractIntArrayReadTest(SourceFactory.PEEK_BUFFERED_SOURCE)

class BufferSourceLongArrayReadTest : AbstractLongArrayReadTest(SourceFactory.BUFFER)
class RealBufferedSourceLongArrayReadTest : AbstractLongArrayReadTest(SourceFactory.REAL_BUFFERED_SOURCE)
class OneByteAtATimeBufferedSourceLongArrayReadTest :
    AbstractLongArrayReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE)

class OneByteAtATimeBufferLongArrayReadTest : AbstractLongArrayReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFER)
class PeekBufferLongArrayReadTest : AbstractLongArrayReadTest(SourceFactory.PEEK_BUFFER)
class PeekBufferedSourceLongArrayReadTest : AbstractLongArrayReadTest(SourceFactory.PEEK_BUFFERED_SOURCE)

class BufferSourceFloatArrayReadTest : AbstractFloatArrayReadTest(SourceFactory.BUFFER)
class RealBufferedSourceFloatArrayReadTest : AbstractFloatArrayReadTest(SourceFactory.REAL_BUFFERED_SOURCE)
class OneByteAtATimeBufferedSourceFloatArrayReadTest :
    AbstractFloatArrayReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE)

class OneByteAtATimeBufferFloatArrayReadTest : AbstractFloatArrayReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFER)
class PeekBufferFloatArrayReadTest : AbstractFloatArrayReadTest(SourceFactory.PEEK_BUFFER)
class PeekBufferedSourceFloatArrayReadTest : AbstractFloatArrayReadTest(SourceFactory.PEEK_BUFFERED_SOURCE)

class BufferSourceDoubleArrayReadTest : AbstractDoubleArrayReadTest(SourceFactory.BUFFER)
class RealBufferedSourceDoubleArrayReadTest : AbstractDoubleArrayReadTest(SourceFactory.REAL_BUFFERED_SOURCE)
class OneByteAtATimeBufferedSourceDoubleArrayReadTest :
    AbstractDoubleArrayReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE)

class OneByteAtATimeBufferDoubleArrayReadTest : AbstractDoubleArrayReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFER)
class PeekBufferDoubleArrayReadTest : AbstractDoubleArrayReadTest(SourceFactory.PEEK_BUFFER)
class PeekBufferedSourceDoubleArrayReadTest : AbstractDoubleArrayReadTest(SourceFactory.PEEK_BUFFERED_SOURCE)

class BufferSourceShortArrayLeReadTest : AbstractShortArrayLeReadTest(SourceFactory.BUFFER)
class RealBufferedSourceShortArrayLeReadTest : AbstractShortArrayLeReadTest(SourceFactory.REAL_BUFFERED_SOURCE)
class OneByteAtATimeBufferedSourceShortArrayLeReadTest :
    AbstractShortArrayLeReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE)

class OneByteAtATimeBufferShortArrayLeReadTest : AbstractShortArrayLeReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFER)
class PeekBufferShortArrayLeReadTest : AbstractShortArrayLeReadTest(SourceFactory.PEEK_BUFFER)
class PeekBufferedSourceShortArrayLeReadTest : AbstractShortArrayLeReadTest(SourceFactory.PEEK_BUFFERED_SOURCE)


class BufferSourceIntArrayLeReadTest : AbstractIntArrayLeReadTest(SourceFactory.BUFFER)
class RealBufferedSourceIntArrayLeReadTest : AbstractIntArrayLeReadTest(SourceFactory.REAL_BUFFERED_SOURCE)
class OneByteAtATimeBufferedSourceIntArrayLeReadTest :
    AbstractIntArrayLeReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE)

class OneByteAtATimeBufferIntArrayLeReadTest : AbstractIntArrayLeReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFER)
class PeekBufferIntArrayLeReadTest : AbstractIntArrayLeReadTest(SourceFactory.PEEK_BUFFER)
class PeekBufferedSourceIntArrayLeReadTest : AbstractIntArrayLeReadTest(SourceFactory.PEEK_BUFFERED_SOURCE)

class BufferSourceLongArrayLeReadTest : AbstractLongArrayLeReadTest(SourceFactory.BUFFER)
class RealBufferedSourceLongArrayLeReadTest : AbstractLongArrayLeReadTest(SourceFactory.REAL_BUFFERED_SOURCE)
class OneByteAtATimeBufferedSourceLongArrayLeReadTest :
    AbstractLongArrayLeReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE)

class OneByteAtATimeBufferLongArrayLeReadTest : AbstractLongArrayLeReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFER)
class PeekBufferLongArrayLeReadTest : AbstractLongArrayLeReadTest(SourceFactory.PEEK_BUFFER)
class PeekBufferedSourceLongArrayLeReadTest : AbstractLongArrayLeReadTest(SourceFactory.PEEK_BUFFERED_SOURCE)

class BufferSourceFloatArrayLeReadTest : AbstractFloatArrayLeReadTest(SourceFactory.BUFFER)
class RealBufferedSourceFloatArrayLeReadTest : AbstractFloatArrayLeReadTest(SourceFactory.REAL_BUFFERED_SOURCE)
class OneByteAtATimeBufferedSourceFloatArrayLeReadTest :
    AbstractFloatArrayLeReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE)

class OneByteAtATimeBufferFloatArrayLeReadTest : AbstractFloatArrayLeReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFER)
class PeekBufferFloatArrayLeReadTest : AbstractFloatArrayLeReadTest(SourceFactory.PEEK_BUFFER)
class PeekBufferedSourceFloatArrayLeReadTest : AbstractFloatArrayLeReadTest(SourceFactory.PEEK_BUFFERED_SOURCE)

class BufferSourceDoubleArrayLeReadTest : AbstractDoubleArrayLeReadTest(SourceFactory.BUFFER)
class RealBufferedSourceDoubleArrayLeReadTest : AbstractDoubleArrayLeReadTest(SourceFactory.REAL_BUFFERED_SOURCE)
class OneByteAtATimeBufferedSourceDoubleArrayLeReadTest :
    AbstractDoubleArrayLeReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE)

class OneByteAtATimeBufferDoubleArrayLeReadTest : AbstractDoubleArrayLeReadTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFER)
class PeekBufferDoubleArrayLeReadTest : AbstractDoubleArrayLeReadTest(SourceFactory.PEEK_BUFFER)
class PeekBufferedSourceDoubleArrayLeReadTest : AbstractDoubleArrayLeReadTest(SourceFactory.PEEK_BUFFERED_SOURCE)


abstract class AbstractShortArrayReadTest internal constructor(factory: SourceFactory) :
    GenericArrayReadTest<Short, ShortArray>(
        factory,
        typeSizeInBytes = Short.SIZE_BYTES,
        arrayFactory = { ShortArray(it) { it.toShort() } },
        resultingArrayFactory = { ShortArray(it) { -1 } },
        defaultValue = -1,
        elementWriter = Sink::writeShort,
        getter = ShortArray::get,
        length = ShortArray::size,
        readToFunction = Source::readTo,
        readNewArrayFunction = Source::readShortArray,
        readNewSizedArrayFunction = Source::readShortArray
    )

abstract class AbstractIntArrayReadTest internal constructor(factory: SourceFactory) :
    GenericArrayReadTest<Int, IntArray>(
        factory,
        typeSizeInBytes = Int.SIZE_BYTES,
        arrayFactory = { IntArray(it) { it } },
        resultingArrayFactory = { IntArray(it) { -1 } },
        defaultValue = -1,
        elementWriter = Sink::writeInt,
        getter = IntArray::get,
        length = IntArray::size,
        readToFunction = Source::readTo,
        readNewArrayFunction = Source::readIntArray,
        readNewSizedArrayFunction = Source::readIntArray
    )

abstract class AbstractLongArrayReadTest internal constructor(factory: SourceFactory) :
    GenericArrayReadTest<Long, LongArray>(
        factory,
        typeSizeInBytes = Long.SIZE_BYTES,
        arrayFactory = { LongArray(it) { it.toLong() } },
        resultingArrayFactory = { LongArray(it) { -1 } },
        defaultValue = -1,
        elementWriter = Sink::writeLong,
        getter = LongArray::get,
        length = LongArray::size,
        readToFunction = Source::readTo,
        readNewArrayFunction = Source::readLongArray,
        readNewSizedArrayFunction = Source::readLongArray
    )

abstract class AbstractFloatArrayReadTest internal constructor(factory: SourceFactory) :
    GenericArrayReadTest<Float, FloatArray>(
        factory,
        typeSizeInBytes = Float.SIZE_BYTES,
        arrayFactory = { FloatArray(it) { it.toFloat() } },
        resultingArrayFactory = { FloatArray(it) { Float.NaN } },
        defaultValue = Float.NaN,
        elementWriter = Sink::writeFloat,
        getter = FloatArray::get,
        length = FloatArray::size,
        readToFunction = Source::readTo,
        readNewArrayFunction = Source::readFloatArray,
        readNewSizedArrayFunction = Source::readFloatArray
    )

abstract class AbstractDoubleArrayReadTest internal constructor(factory: SourceFactory) :
    GenericArrayReadTest<Double, DoubleArray>(
        factory,
        typeSizeInBytes = Double.SIZE_BYTES,
        arrayFactory = { DoubleArray(it) { it.toDouble() } },
        resultingArrayFactory = { DoubleArray(it) { Double.NaN } },
        defaultValue = Double.NaN,
        elementWriter = Sink::writeDouble,
        getter = DoubleArray::get,
        length = DoubleArray::size,
        readToFunction = Source::readTo,
        readNewArrayFunction = Source::readDoubleArray,
        readNewSizedArrayFunction = Source::readDoubleArray
    )

abstract class AbstractShortArrayLeReadTest internal constructor(factory: SourceFactory) :
    GenericArrayReadTest<Short, ShortArray>(
        factory,
        typeSizeInBytes = Short.SIZE_BYTES,
        arrayFactory = { ShortArray(it) { it.toShort() } },
        resultingArrayFactory = { ShortArray(it) { -1 } },
        defaultValue = -1,
        elementWriter = Sink::writeShortLe,
        getter = ShortArray::get,
        length = ShortArray::size,
        readToFunction = Source::readLeTo,
        readNewArrayFunction = Source::readShortLeArray,
        readNewSizedArrayFunction = Source::readShortLeArray
    )

abstract class AbstractIntArrayLeReadTest internal constructor(factory: SourceFactory) :
    GenericArrayReadTest<Int, IntArray>(
        factory,
        typeSizeInBytes = Int.SIZE_BYTES,
        arrayFactory = { IntArray(it) { it } },
        resultingArrayFactory = { IntArray(it) { -1 } },
        defaultValue = -1,
        elementWriter = Sink::writeIntLe,
        getter = IntArray::get,
        length = IntArray::size,
        readToFunction = Source::readLeTo,
        readNewArrayFunction = Source::readIntLeArray,
        readNewSizedArrayFunction = Source::readIntLeArray
    )

abstract class AbstractLongArrayLeReadTest internal constructor(factory: SourceFactory) :
    GenericArrayReadTest<Long, LongArray>(
        factory,
        typeSizeInBytes = Long.SIZE_BYTES,
        arrayFactory = { LongArray(it) { it.toLong() } },
        resultingArrayFactory = { LongArray(it) { -1 } },
        defaultValue = -1,
        elementWriter = Sink::writeLongLe,
        getter = LongArray::get,
        length = LongArray::size,
        readToFunction = Source::readLeTo,
        readNewArrayFunction = Source::readLongLeArray,
        readNewSizedArrayFunction = Source::readLongLeArray
    )

abstract class AbstractFloatArrayLeReadTest internal constructor(factory: SourceFactory) :
    GenericArrayReadTest<Float, FloatArray>(
        factory,
        typeSizeInBytes = Float.SIZE_BYTES,
        arrayFactory = { FloatArray(it) { it.toFloat() } },
        resultingArrayFactory = { FloatArray(it) { Float.NaN } },
        defaultValue = Float.NaN,
        elementWriter = Sink::writeFloatLe,
        getter = FloatArray::get,
        length = FloatArray::size,
        readToFunction = Source::readLeTo,
        readNewArrayFunction = Source::readFloatLeArray,
        readNewSizedArrayFunction = Source::readFloatLeArray
    )

abstract class AbstractDoubleArrayLeReadTest internal constructor(factory: SourceFactory) :
    GenericArrayReadTest<Double, DoubleArray>(
        factory,
        typeSizeInBytes = Double.SIZE_BYTES,
        arrayFactory = { DoubleArray(it) { it.toDouble() } },
        resultingArrayFactory = { DoubleArray(it) { Double.NaN } },
        defaultValue = Double.NaN,
        elementWriter = Sink::writeDoubleLe,
        getter = DoubleArray::get,
        length = DoubleArray::size,
        readToFunction = Source::readLeTo,
        readNewArrayFunction = Source::readDoubleLeArray,
        readNewSizedArrayFunction = Source::readDoubleLeArray
    )

abstract class GenericArrayReadTest<ElementType, ArrayType>
internal constructor(
    factory: SourceFactory,
    val typeSizeInBytes: Int,
    val arrayFactory: (Int) -> ArrayType,
    val resultingArrayFactory: (Int) -> ArrayType,
    val defaultValue: ElementType,
    val elementWriter: Sink.(ElementType) -> Unit,
    val getter: (ArrayType, Int) -> ElementType,
    val length: ArrayType.() -> Int,
    val readToFunction: Source.(ArrayType, Int, Int) -> Unit,
    val readNewArrayFunction: Source.() -> ArrayType,
    val readNewSizedArrayFunction: Source.(Int) -> ArrayType,
) {
    private val sink: Sink
    private val source: Source

    init {
        val pipe = factory.pipe()
        sink = pipe.sink
        source = pipe.source
    }

    private fun writeArrayToSink(array: ArrayType) {
        for (idx in 0 ..< length(array)) {
            elementWriter(sink, getter(array, idx))
        }
        sink.flush()
    }

    private fun assertArrayEquals(expectedArray: ArrayType, actualArray: ArrayType) {
        assertEquals(length(expectedArray), length(actualArray))
        for (idx in 0 ..< length(actualArray)) {
            assertEquals(getter(expectedArray, idx), getter(actualArray, idx), "Unexpected value at index $idx")
        }
    }

    @Test
    fun readIntoEmptyArray() {
        val array = arrayFactory(0)
        sink.writeInt(42)
        sink.flush()

        readToFunction(source, array, 0, 0)
        assertTrue(source.request(4))
    }

    @Test
    fun readIntoArrayWithInvalidIndices() {
        val array = arrayFactory(6)

        assertFailsWith<IndexOutOfBoundsException> {
            readToFunction(source, array, -1, 1)
        }

        assertFailsWith<IndexOutOfBoundsException> {
            readToFunction(source, array, 0, 7)
        }

        assertFailsWith<IllegalArgumentException> {
            readToFunction(source, array, 2, 1)
        }

        assertFailsWith<IndexOutOfBoundsException> {
            readToFunction(source, array, 6, 10)
        }

        assertFailsWith<IndexOutOfBoundsException> {
            readToFunction(source, array, -2, 0)
        }
    }

    @Test
    fun readFromEmptySource() {
        val requiredArraySize = 10
        val targetArray = arrayFactory(10)

        assertFailsWith<EOFException> {
            readToFunction(source, targetArray, 0, requiredArraySize)
        }
    }

    @OptIn(UnsafeIoApi::class)
    @Test
    fun readFromPrematurelyExhaustedSource() {
        val requiredArraySize = 10
        val targetArray = arrayFactory(10)

        // source will be missing a single element
        sink.write(ByteArray((requiredArraySize - 1) * typeSizeInBytes))

        assertFailsWith<EOFException> {
            readToFunction(source, targetArray, 0, requiredArraySize)
        }
        assertTrue(source.readByteArray().isEmpty())

        // source will be missing a single byte
        sink.write(ByteArray(requiredArraySize * typeSizeInBytes - 1))
        assertFailsWith<EOFException> {
            readToFunction(source, targetArray, 0, requiredArraySize)
        }
        assertTrue(source.readByteArray().isEmpty())

        // Write a single segment, then skip some padding so that
        // the number of remaining bytes will be less than then number of bytes
        // required to fill the array. In that case underflow will happen on segments
        // boundary.
        val segmentSize = UnsafeBufferOperations.maxSafeWriteCapacity
        val paddingSize = segmentSize - requiredArraySize * typeSizeInBytes + 1L
        sink.write(ByteArray(segmentSize))
        source.skip(paddingSize)
        assertFailsWith<EOFException> {
            readToFunction(source, targetArray, 0, requiredArraySize)
        }
        assertTrue(source.readByteArray().isEmpty())
    }

    @OptIn(UnsafeIoApi::class)
    @Test
    fun readIntoArray() {
        readCheck(42, 0, 42)
        readCheck(42, 10, 42)
        readCheck(42, 0, 10)
        readCheck(42, 11, 23)

        val segmentSize = UnsafeBufferOperations.maxSafeWriteCapacity

        readCheck(42, 0, 42, segmentSize - 1)

        val threeSegments = segmentSize / typeSizeInBytes * 3
        val segmentAndHalf = threeSegments / 2

        readCheck(segmentAndHalf, 0, segmentAndHalf)

        for (pad in 1..< typeSizeInBytes) {
            readCheck(
                segmentAndHalf,
                fromIndex = 0,
                toIndex = segmentAndHalf,
                prefixBytes = segmentSize - pad
            )
        }

        readCheck(threeSegments, 0, threeSegments)
    }

    fun readCheck(arrayLength: Int, fromIndex: Int, toIndex: Int, prefixBytes: Int = 0) {
        val array = arrayFactory(arrayLength)

        sink.write(ByteArray(prefixBytes) { -1 })
        for (idx in fromIndex until toIndex) {
            elementWriter(sink, getter(array, idx))
        }
        sink.flush()

        source.skip(prefixBytes.toLong())

        val resultingArray = resultingArrayFactory(arrayLength)
        readToFunction(source, resultingArray, fromIndex, toIndex)

        var idx = 0
        // check that array's prefix was not overridden
        while (idx < fromIndex) {
            assertEquals(defaultValue, getter(resultingArray, idx), "Value at index $idx was overwritten")
            idx++
        }
        // check that values were correctly read
        while (idx < toIndex) {
            assertEquals(getter(array, idx), getter(resultingArray, idx), "Unexpected value at index $idx")
            idx++
        }
        // check that array's suffix was not overridden
        while (idx < arrayLength) {
            assertEquals(defaultValue, getter(resultingArray, idx), "Value at index $idx was overwritten")
            idx++
        }

        // there should be no more elements in the array
        assertTrue(source.exhausted())
    }

    @Test
    fun readNewArrayFromEmptySource() {
        assertEquals(0, length(readNewArrayFunction(source)))
        assertEquals(0, length(readNewSizedArrayFunction(source, 0)))

        // TODO
        assertFailsWith<EOFException> {
            readNewSizedArrayFunction(source, 1)
        }
    }

    @Test
    fun readNewArray() {
        val arraySize = 10

        val sourceArray = arrayFactory(arraySize)
        writeArrayToSink(sourceArray)

        assertArrayEquals(sourceArray, readNewArrayFunction(source))
        assertTrue(source.exhausted())

        // pad
        sink.write(ByteArray(3))
        writeArrayToSink(sourceArray)
        // read padding
        source.skip(3)

        assertArrayEquals(sourceArray, readNewArrayFunction(source))
        assertTrue(source.exhausted())
    }

    @Test
    fun readNewArrayWhenNotMultipleNumberOfBytesAvailable() {
        sink.write(ByteArray(10 * typeSizeInBytes + 1))
        sink.flush()

        assertFailsWith<IllegalStateException> {
            readNewArrayFunction(source)
        }
    }

    @Test
    fun readNewSizeArray() {
        val sourceArray = arrayFactory(10)
        writeArrayToSink(sourceArray)

        assertArrayEquals(sourceArray, readNewSizedArrayFunction(source, 10))
        assertTrue(source.exhausted())

        // pad
        sink.write(ByteArray(3))
        writeArrayToSink(sourceArray)
        // read padding
        source.skip(3)

        assertArrayEquals(sourceArray, readNewSizedArrayFunction(source, 10))
        assertTrue(source.exhausted())

        writeArrayToSink(sourceArray)
        val shorterArray = readNewSizedArrayFunction(source, 7)
        assertEquals(7, length(shorterArray))
        source.skip(3L * typeSizeInBytes)
        assertTrue(source.exhausted())

        for (idx in 0 ..< 7) {
            assertEquals(getter(shorterArray, idx), getter(shorterArray, idx), "Unexpected value at index $idx")
        }

        writeArrayToSink(sourceArray)
        assertFailsWith<EOFException> {
            readNewSizedArrayFunction(source, 11)
        }
        // Data should not be consumed on error
        source.skip(10L * typeSizeInBytes)
    }

    @Test
    fun readNewSizedArrayWithInvalidSize() {
        assertFailsWith<IllegalArgumentException> {
            readNewSizedArrayFunction(source, -1)
        }
    }

    @OptIn(InternalIoApi::class)
    @Test
    fun readNewArrayFromTooLargeSource() {
        // That's a hack, don't repeat it at home
        source.buffer.sizeMut = (Int.MAX_VALUE.toLong() + 1) * typeSizeInBytes
        try {
            assertFailsWith<IllegalStateException> {
                readNewArrayFunction(source)
            }
        } finally {
            source.buffer.sizeMut = 0L
        }
    }
}
