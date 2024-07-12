/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.*
import java.nio.ByteBuffer
import kotlin.test.*

@OptIn(UnsafeIoApi::class)
class UnsafeBufferOperationsJvmReadBulkTest {
    private class TestException : RuntimeException()

    @Test
    fun readAllFromEmptyBuffer() {
        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferOperations.readBulk(Buffer(), Array(1) { null }) { _, _ -> 0L }
        }
    }

    @Test
    fun readUsingEmptyArray() {
        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferOperations.readBulk(
                Buffer().apply { writeByte(0) },
                Array(0) { null }) { _, _ -> 0L }
        }
    }

    @Test
    fun readSingleSegment() {
        val buffer = Buffer().apply { writeString("hello world") }
        val array = Array<ByteBuffer?>(16) { null }

        UnsafeBufferOperations.readBulk(buffer, array) { arrayArg, iovecLen ->
            assertSame(array, arrayArg)
            assertEquals(1, iovecLen)

            val buf = arrayArg[0]
            assertNotNull(buf)
            assertEquals(11, buf.capacity())

            val str = ByteArray(11).let {
                buf.get(it)
                it.decodeToString()
            }
            assertEquals("hello world", str)

            11
        }
        assertTrue(buffer.exhausted())
    }

    @Test
    fun readSingleSegmentWithoutConsumingIt() {
        val buffer = Buffer().apply { writeString("hello world") }
        val array = Array<ByteBuffer?>(16) { null }

        UnsafeBufferOperations.readBulk(buffer, array) { arrayArg, iovecLen ->
            assertSame(array, arrayArg)
            assertEquals(1, iovecLen)

            val buf = arrayArg[0]
            assertNotNull(buf)
            assertEquals(11, buf.capacity())

            val str = ByteArray(11).let {
                buf.get(it)
                it.decodeToString()
            }
            assertEquals("hello world", str)

            0
        }
        assertEquals("hello world", buffer.readString())
    }

    @Test
    fun readMultipleSegments() {
        val buffer = Buffer().apply {
            write(ByteArray(Segment.SIZE) { 1 })
            write(ByteArray(Segment.SIZE) { 2 })
            write(ByteArray(Segment.SIZE + 1) { 3 })
        }
        val buffers = Array<ByteBuffer?>(16) { null }
        UnsafeBufferOperations.readBulk(buffer, buffers) { array, iovecLen ->
            assertSame(buffers, array)
            assertEquals(4, iovecLen)

            assertEquals(Segment.SIZE, array[0]!!.remaining())
            val tmpBuffer = ByteArray(Segment.SIZE)
            array[0]!!.get(tmpBuffer)
            assertContentEquals(ByteArray(Segment.SIZE) { 1 }, tmpBuffer)

            assertEquals(Segment.SIZE, array[1]!!.remaining())
            array[1]!!.get(tmpBuffer)
            assertContentEquals(ByteArray(Segment.SIZE) { 2 }, tmpBuffer)

            assertEquals(Segment.SIZE, array[2]!!.remaining())
            array[2]!!.get(tmpBuffer)
            assertContentEquals(ByteArray(Segment.SIZE) { 3 }, tmpBuffer)

            assertEquals(1, array[3]!!.remaining())
            assertEquals(3, array[3]!!.get())

            buffer.size
        }
        assertTrue(buffer.exhausted())
    }

    @Test
    fun readMultipleSegmentsWithoutConsumingIt() {
        val buffer = Buffer().apply {
            write(ByteArray(Segment.SIZE) { 1 })
            write(ByteArray(Segment.SIZE) { 2 })
            write(ByteArray(Segment.SIZE + 1) { 3 })
        }
        val buffers = Array<ByteBuffer?>(16) { null }
        UnsafeBufferOperations.readBulk(buffer, buffers) { array, iovecLen ->
            assertSame(buffers, array)
            assertEquals(4, iovecLen)

            assertEquals(Segment.SIZE, array[0]!!.remaining())
            val tmpBuffer = ByteArray(Segment.SIZE)
            array[0]!!.get(tmpBuffer)
            assertContentEquals(ByteArray(Segment.SIZE) { 1 }, tmpBuffer)

            assertEquals(Segment.SIZE, array[1]!!.remaining())
            array[1]!!.get(tmpBuffer)
            assertContentEquals(ByteArray(Segment.SIZE) { 2 }, tmpBuffer)

            assertEquals(Segment.SIZE, array[2]!!.remaining())
            array[2]!!.get(tmpBuffer)
            assertContentEquals(ByteArray(Segment.SIZE) { 3 }, tmpBuffer)

            assertEquals(1, array[3]!!.remaining())
            assertEquals(3, array[3]!!.get())

            0
        }
        assertEquals(Segment.SIZE * 3 + 1L, buffer.size)
    }

    @Test
    fun consumeBufferPartially() {
        val buffer = Buffer().apply {
            writeString("hello world")
        }
        UnsafeBufferOperations.readBulk(buffer, Array(1) { null }) { _, _ ->
            6
        }
        assertEquals("world", buffer.readString())
    }

    @Test
    fun consumeMultiSegmentBufferPartially() {
        val buffer = Buffer().apply {
            write(ByteArray(Segment.SIZE * 3))
        }
        UnsafeBufferOperations.readBulk(buffer, Array(3) { null }) { _, _ ->
            Segment.SIZE * 3 - 1111L
        }
        assertEquals(1111, buffer.size)
    }

    @Test
    fun passShortArray() {
        val buffer = Buffer().apply {
            write(ByteArray(Segment.SIZE * 2))
        }
        UnsafeBufferOperations.readBulk(buffer, Array(1) { null }) { array, _ ->
            array[0]!!.remaining().toLong()
        }
        assertEquals(Segment.SIZE.toLong(), buffer.size)
    }

    @Test
    fun returnIncorrectReadValue() {
        val buffer = Buffer().apply { write(ByteArray(Segment.SIZE + 1)) }
        val size = buffer.size

        assertFailsWith<IllegalStateException> {
            UnsafeBufferOperations.readBulk(buffer, Array(2) { null }) { _, _ -> -1L}
        }
        assertFailsWith<IllegalStateException> {
            UnsafeBufferOperations.readBulk(buffer, Array(2) { null }) { _, _ -> size + 1L }
        }
        assertFailsWith<IllegalStateException> {
            UnsafeBufferOperations.readBulk(buffer, Array(1) { null }) { _, _  -> size }
        }
    }

    @Test
    fun resetReadOnException() {
        val buffer = Buffer().apply { writeString("hello world") }

        val sizeBeforeRead = buffer.size
        assertFailsWith<TestException> {
            UnsafeBufferOperations.readBulk(buffer, Array(1) { null }) { _, _ ->
                throw TestException()
            }
        }
        assertEquals(buffer.size, sizeBeforeRead)
    }
}
