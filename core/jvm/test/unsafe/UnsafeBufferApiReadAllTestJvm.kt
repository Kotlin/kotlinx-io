/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.*
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import kotlin.test.*

@OptIn(SnapshotApi::class, UnsafeIoApi::class)
class UnsafeBufferApiReadAllTestJvm {
    @Test
    fun testReadAllFromEmptyBuffer() {
        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferAccessors.readFully(Buffer(), Array(1) { null }) { _, _, _ -> 0L }
        }
    }

    @Test
    fun testReadUsingEmptyArray() {
        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferAccessors.readFully(
                Buffer().apply { writeByte(0) },
                Array(0) { null }) { _, _, _ -> 0L }
        }
    }

    @Test
    fun testReadSingleSegment() {
        val buffer = Buffer().apply { writeString("hello world") }
        val array = Array<ByteBuffer?>(16) { null }

        UnsafeBufferAccessors.readFully(buffer, array) { arrayArg, startIndex, endIndex ->
            assertSame(array, arrayArg)
            assertEquals(0, startIndex)
            assertEquals(1, endIndex)

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
    fun testReadSingleSegmentWithoutConsumingIt() {
        val buffer = Buffer().apply { writeString("hello world") }
        val array = Array<ByteBuffer?>(16) { null }

        UnsafeBufferAccessors.readFully(buffer, array) { arrayArg, startIndex, endIndex ->
            assertSame(array, arrayArg)
            assertEquals(0, startIndex)
            assertEquals(1, endIndex)

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
    fun testReadMultipleSegments() {
        val buffer = Buffer().apply {
            write(ByteArray(Segment.SIZE) { 1 })
            write(ByteArray(Segment.SIZE) { 2 })
            write(ByteArray(Segment.SIZE + 1) { 3 })
        }
        val buffers = Array<ByteBuffer?>(16) { null }
        UnsafeBufferAccessors.readFully(buffer, buffers) { array, startIndex, endIndex ->
            assertSame(buffers, array)
            assertEquals(0, startIndex)
            assertEquals(4, endIndex)

            assertEquals(Segment.SIZE, array[0]!!.remaining())
            assertEquals(Segment.SIZE, array[1]!!.remaining())
            assertEquals(Segment.SIZE, array[2]!!.remaining())
            assertEquals(1, array[3]!!.remaining())

            buffer.size
        }
        assertTrue(buffer.exhausted())
    }

    @Test
    fun testReadMultipleSegmentsWithoutConsumingIt() {
        val buffer = Buffer().apply {
            write(ByteArray(Segment.SIZE) { 1 })
            write(ByteArray(Segment.SIZE) { 2 })
            write(ByteArray(Segment.SIZE + 1) { 3 })
        }
        val buffers = Array<ByteBuffer?>(16) { null }
        UnsafeBufferAccessors.readFully(buffer, buffers) { array, startIndex, endIndex ->
            assertSame(buffers, array)
            assertEquals(0, startIndex)
            assertEquals(4, endIndex)

            assertEquals(Segment.SIZE, array[0]!!.remaining())
            assertEquals(Segment.SIZE, array[1]!!.remaining())
            assertEquals(Segment.SIZE, array[2]!!.remaining())
            assertEquals(1, array[3]!!.remaining())

            0
        }
        assertEquals(Segment.SIZE * 3 + 1L, buffer.size)
    }

    @Test
    fun testConsumeBufferPartially() {
        val buffer = Buffer().apply {
            writeString("hello world")
        }
        UnsafeBufferAccessors.readFully(buffer, Array(1) { null }) { _, _, _ ->
            6
        }
        assertEquals("world", buffer.readString())
    }

    @Test
    fun testConsumeMultiSegmentBufferPartially() {
        val buffer = Buffer().apply {
            write(ByteArray(Segment.SIZE * 3))
        }
        UnsafeBufferAccessors.readFully(buffer, Array(3) { null }) { _, _, _ ->
            Segment.SIZE * 3 - 1111L
        }
        assertEquals(1111, buffer.size)
    }

    @Test
    fun testPassShortArray() {
        val buffer = Buffer().apply {
            write(ByteArray(Segment.SIZE * 2))
        }
        UnsafeBufferAccessors.readFully(buffer, Array(1) { null }) { array, _, _ ->
            array[0]!!.remaining().toLong()
        }
        assertEquals(Segment.SIZE.toLong(), buffer.size)
    }

    @Test
    fun testReturnIncorrectReadValue() {
        val buffer = Buffer().apply { write(ByteArray(Segment.SIZE + 1)) }
        val size = buffer.size

        assertFailsWith<IllegalStateException> {
            UnsafeBufferAccessors.readFully(buffer, Array(2) { null }) { _, _, _ -> -1L}
        }
        assertFailsWith<IllegalStateException> {
            UnsafeBufferAccessors.readFully(buffer, Array(2) { null }) { _, _, _ -> size + 1L }
        }
        assertFailsWith<IllegalStateException> {
            UnsafeBufferAccessors.readFully(buffer, Array(1) { null }) { _, _, _ -> size }
        }
    }
}
