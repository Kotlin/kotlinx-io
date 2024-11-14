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

class BufferIntArrayWriteTest : AbstractIntArrayWriteTest(SinkFactory.BUFFER)
class SinkIntArrayWriteTest : AbstractIntArrayWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class BufferShortArrayWriteTest : AbstractShortArrayWriteTest(SinkFactory.BUFFER)
class SinkShortArrayWriteTest : AbstractShortArrayWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class BufferLongArrayWriteTest : AbstractLongArrayWriteTest(SinkFactory.BUFFER)
class SinkLongArrayWriteTest : AbstractLongArrayWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class BufferFloatArrayWriteTest : AbstractFloatArrayWriteTest(SinkFactory.BUFFER)
class SinkFloatArrayWriteTest : AbstractFloatArrayWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class BufferDoubleArrayWriteTest : AbstractDoubleArrayWriteTest(SinkFactory.BUFFER)
class SinkDoubleArrayWriteTest : AbstractDoubleArrayWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class BufferIntArrayLeWriteTest : AbstractIntArrayLeWriteTest(SinkFactory.BUFFER)
class SinkIntArrayLeWriteTest : AbstractIntArrayLeWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class BufferShortArrayLeWriteTest : AbstractShortArrayLeWriteTest(SinkFactory.BUFFER)
class SinkShortArrayLeWriteTest : AbstractShortArrayLeWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class BufferLongArrayLeWriteTest : AbstractLongArrayLeWriteTest(SinkFactory.BUFFER)
class SinkLongArrayLeWriteTest : AbstractLongArrayLeWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class BufferFloatArrayLeWriteTest : AbstractFloatArrayLeWriteTest(SinkFactory.BUFFER)
class SinkFloatArrayLeWriteTest : AbstractFloatArrayLeWriteTest(SinkFactory.REAL_BUFFERED_SINK)

class BufferDoubleArrayLeWriteTest : AbstractDoubleArrayLeWriteTest(SinkFactory.BUFFER)
class SinkDoubleArrayLeWriteTest : AbstractDoubleArrayLeWriteTest(SinkFactory.REAL_BUFFERED_SINK)


abstract class AbstractIntArrayWriteTest internal constructor(factory: SinkFactory) :
    GenericArrayWriteTest<Int, IntArray>(
        factory,
        typeSizeInBytes = 4,
        arrayFactory = { sz -> IntArray(sz) { it } },
        elementReader = Buffer::readInt,
        getter = IntArray::get,
        functionUnderTest = Sink::write
    )

abstract class AbstractLongArrayWriteTest internal constructor(factory: SinkFactory) :
    GenericArrayWriteTest<Long, LongArray>(
        factory,
        typeSizeInBytes = 8,
        arrayFactory = { sz -> LongArray(sz) { it.toLong() } },
        elementReader = Buffer::readLong,
        getter = LongArray::get,
        functionUnderTest = Sink::write
    )

abstract class AbstractShortArrayWriteTest internal constructor(factory: SinkFactory) :
    GenericArrayWriteTest<Short, ShortArray>(
        factory,
        typeSizeInBytes = 2,
        arrayFactory = { sz -> ShortArray(sz) { it.toShort() } },
        elementReader = Buffer::readShort,
        getter = ShortArray::get,
        functionUnderTest = Sink::write
    )

abstract class AbstractFloatArrayWriteTest internal constructor(factory: SinkFactory) :
    GenericArrayWriteTest<Float, FloatArray>(
        factory,
        typeSizeInBytes = 4,
        arrayFactory = { sz -> FloatArray(sz) { it.toFloat() } },
        elementReader = Buffer::readFloat,
        getter = FloatArray::get,
        functionUnderTest = Sink::write
    )

abstract class AbstractDoubleArrayWriteTest internal constructor(factory: SinkFactory) :
    GenericArrayWriteTest<Double, DoubleArray>(
        factory,
        typeSizeInBytes = 8,
        arrayFactory = { sz -> DoubleArray(sz) { it.toDouble() } },
        elementReader = Buffer::readDouble,
        getter = DoubleArray::get,
        functionUnderTest = Sink::write
    )

abstract class AbstractIntArrayLeWriteTest internal constructor(factory: SinkFactory) :
    GenericArrayWriteTest<Int, IntArray>(
        factory,
        typeSizeInBytes = 4,
        arrayFactory = { sz -> IntArray(sz) { it } },
        elementReader = Buffer::readIntLe,
        getter = IntArray::get,
        functionUnderTest = Sink::writeLe
    )

abstract class AbstractLongArrayLeWriteTest internal constructor(factory: SinkFactory) :
    GenericArrayWriteTest<Long, LongArray>(
        factory,
        typeSizeInBytes = 8,
        arrayFactory = { sz -> LongArray(sz) { it.toLong() } },
        elementReader = Buffer::readLongLe,
        getter = LongArray::get,
        functionUnderTest = Sink::writeLe
    )

abstract class AbstractShortArrayLeWriteTest internal constructor(factory: SinkFactory) :
    GenericArrayWriteTest<Short, ShortArray>(
        factory,
        typeSizeInBytes = 2,
        arrayFactory = { sz -> ShortArray(sz) { it.toShort() } },
        elementReader = Buffer::readShortLe,
        getter = ShortArray::get,
        functionUnderTest = Sink::writeLe
    )

abstract class AbstractFloatArrayLeWriteTest internal constructor(factory: SinkFactory) :
    GenericArrayWriteTest<Float, FloatArray>(
        factory,
        typeSizeInBytes = 4,
        arrayFactory = { sz -> FloatArray(sz) { it.toFloat() } },
        elementReader = Buffer::readFloatLe,
        getter = FloatArray::get,
        functionUnderTest = Sink::writeLe
    )

abstract class AbstractDoubleArrayLeWriteTest internal constructor(factory: SinkFactory) :
    GenericArrayWriteTest<Double, DoubleArray>(
        factory,
        typeSizeInBytes = 8,
        arrayFactory = { sz -> DoubleArray(sz) { it.toDouble() } },
        elementReader = Buffer::readDoubleLe,
        getter = DoubleArray::get,
        functionUnderTest = Sink::writeLe
    )

abstract class GenericArrayWriteTest<ElementType, ArrayType>
internal constructor(
    factory: SinkFactory,
    val typeSizeInBytes: Int,
    val arrayFactory: (Int) -> ArrayType,
    val elementReader: Buffer.() -> ElementType,
    val getter: (ArrayType, Int) -> ElementType,
    val functionUnderTest: Sink.(ArrayType, Int, Int) -> Unit
) {
    val data = Buffer()
    val sink = factory.create(data)

    @Test
    fun writeEmptyArray() {
        functionUnderTest(sink, arrayFactory(0), 0, 0)
        sink.flush()
        assertTrue(data.exhausted())
    }

    @Test
    fun writeArrayWithInvalidIndices() {
        val array = arrayFactory(6)

        assertFailsWith<IndexOutOfBoundsException> {
            functionUnderTest(sink, array, -1, 1)
        }

        assertFailsWith<IndexOutOfBoundsException> {
            functionUnderTest(sink, array, 0, 7)
        }

        assertFailsWith<IllegalArgumentException> {
            functionUnderTest(sink, array, 2, 1)
        }

        assertFailsWith<IndexOutOfBoundsException> {
            functionUnderTest(sink, array, 6, 10)
        }

        assertFailsWith<IndexOutOfBoundsException> {
            functionUnderTest(sink, array, -2, 0)
        }
    }

    @OptIn(UnsafeIoApi::class)
    @Test
    fun writeArray() {
        writeCheck(arrayFactory(10), 0, 0, 0)
        writeCheck(arrayFactory(10), 0, 10, 0)
        writeCheck(arrayFactory(10), 2, 5, 0)

        val segmentSize = UnsafeBufferOperations.maxSafeWriteCapacity
        val segmentLength = segmentSize / typeSizeInBytes

        val threeSegments = segmentLength * 3
        val segmentAndHalf = threeSegments / 2

        // write segment and a half
        writeCheck(arrayFactory(segmentAndHalf), 0, segmentAndHalf, 0)

        // write into an almost full segment
        for (offsetFromTail in 1..typeSizeInBytes) {
            writeCheck(arrayFactory(segmentAndHalf), 0, segmentAndHalf, segmentSize - offsetFromTail)
        }

        // write multiple segments
        writeCheck(arrayFactory(threeSegments), 0, threeSegments, segmentSize / 2)
    }

    fun writeCheck(array: ArrayType, from: Int, to: Int, padBytes: Int) {
        val padding = ByteArray(padBytes) { -1 }

        sink.write(padding)
        functionUnderTest(sink, array, from, to)
        sink.flush()

        assertEquals(padBytes + (to - from) * typeSizeInBytes.toLong(), data.size)

        assertArrayEquals(padding, data.readByteArray(padBytes))

        for (i in from..<to) {
            assertEquals(getter(array, i), elementReader(data), "Wrong value corresponding to array[$i]")
        }
    }
}
