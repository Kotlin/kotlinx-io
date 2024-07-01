/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.*
import kotlin.test.*

@OptIn(UnsafeIoApi::class)
class UnsafeBufferOperationsWriteTest {
    private class TestException : RuntimeException()

    @Test
    fun bufferCapacity() {
        val buffer = Buffer()

        UnsafeBufferOperations.writeToTail(buffer, 1) { data, startIndex, endIndex ->
            // Unsafe check, head is not committed yet
            assertSame(buffer.head!!.dataAsByteArray(true), data)
            assertEquals(0, startIndex)
            assertEquals(buffer.head!!.remainingCapacity, endIndex)
            0
        }

        UnsafeBufferOperations.writeToTail(buffer, 1) { _, segment ->
            // Unsafe check, head is not committed yet
            assertSame(buffer.head, segment)
            0
        }
    }

    @Test
    fun writeByteByByte() {
        val buffer = Buffer()
        val data = "hello world".encodeToByteArray()

        for (idx in data.indices) {
            UnsafeBufferOperations.writeToTail(buffer, 1) { writeable, pos, _ ->
                writeable[pos] = data[idx]
                1
            }
            assertEquals(idx + 1, buffer.size.toInt())
        }
        assertEquals("hello world", buffer.readString())
    }

    @Test
    fun writeNothing() {
        val buffer = Buffer()

        UnsafeBufferOperations.writeToTail(buffer, 1) { _, _, _ -> 0 }
        assertTrue(buffer.exhausted())

        UnsafeBufferOperations.writeToTail(buffer, 1) { _, _ -> 0 }
        assertTrue(buffer.exhausted())

        buffer.writeInt(42)
        UnsafeBufferOperations.writeToTail(buffer, 1) { _, _, _ -> 0 }
        assertEquals(4, buffer.size)

        UnsafeBufferOperations.writeToTail(buffer, 1) { _, _ -> 0 }
        assertEquals(4, buffer.size)

        buffer.write(ByteArray(Segment.SIZE - 4))
        UnsafeBufferOperations.writeToTail(buffer, 1) { _, _, _ -> 0 }
        assertEquals(Segment.SIZE.toLong(), buffer.size)

        UnsafeBufferOperations.writeToTail(buffer, 1) { _, _ -> 0 }
        assertEquals(Segment.SIZE.toLong(), buffer.size)
    }

    @Test
    fun writeWholeBuffer() {
        val buffer = Buffer()
        UnsafeBufferOperations.writeToTail(buffer, 1) { data, from, to ->
            for (idx in from..<to) {
                data[idx] = 42
            }
            to - from
        }
        assertEquals(Segment.SIZE, buffer.size.toInt())
        assertArrayEquals(ByteArray(Segment.SIZE) { 42 }, buffer.readByteArray())
    }

    @Test
    fun writeWithCtx() {
        val buffer = Buffer()

        UnsafeBufferOperations.writeToTail(buffer, 1) { ctx, segment ->
            ctx.setUnchecked(segment, 0, 1)
            ctx.setUnchecked(segment, 1, 2)
            2
        }

        assertArrayEquals(byteArrayOf(1, 2), buffer.readByteArray())
    }

    @Test
    fun requireToManyBytes() {
        val buffer = Buffer()
        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferOperations.writeToTail(buffer, 100500) { _, _, _ -> 0 }
        }
        assertTrue(buffer.exhausted())

        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferOperations.writeToTail(buffer, 100500) { _, _ -> 0 }
        }
        assertTrue(buffer.exhausted())
    }

    @Test
    fun writeToTheEndOfABuffer() {
        val buffer = Buffer().apply { write(ByteArray(Segment.SIZE - 1)) }
        UnsafeBufferOperations.writeToTail(buffer, 1) { data, pos, limit ->
            assertEquals(1, limit - pos)
            data[pos] = 42
            1
        }
        assertEquals(Segment.SIZE, buffer.size.toInt())
        UnsafeBufferOperations.writeToTail(buffer, 1) { data, pos, _ ->
            data[pos] = 43
            1
        }
        assertEquals(Segment.SIZE + 1, buffer.size.toInt())

        buffer.skip(Segment.SIZE - 1L)
        assertArrayEquals(byteArrayOf(42, 43), buffer.readByteArray())
    }

    @Test
    fun writeToTheEndOfABufferUsingCtx() {
        val buffer = Buffer().apply { write(ByteArray(Segment.SIZE - 1)) }
        UnsafeBufferOperations.writeToTail(buffer, 1) { ctx, seg ->
            assertEquals(1, seg.remainingCapacity)
            ctx.setUnchecked(seg, 0, 42)
            1
        }
        assertEquals(Segment.SIZE, buffer.size.toInt())
        UnsafeBufferOperations.writeToTail(buffer, 1) { ctx, seg ->
            ctx.setUnchecked(seg, 0, 43)
            1
        }
        assertEquals(Segment.SIZE + 1, buffer.size.toInt())

        buffer.skip(Segment.SIZE - 1L)
        assertArrayEquals(byteArrayOf(42, 43), buffer.readByteArray())
    }

    @Test
    fun returnIllegalWriteCount() {
        val buffer = Buffer()
        assertFailsWith<IllegalStateException> {
            UnsafeBufferOperations.writeToTail(buffer, 1) { _, _, _ ->
                -1
            }
        }
        assertTrue(buffer.exhausted())

        assertFailsWith<IllegalStateException> {
            UnsafeBufferOperations.writeToTail(buffer, 1) { _, _, _ ->
                100500
            }
        }
        assertTrue(buffer.exhausted())
    }

    @Test
    fun writeMultipleBytes() {
        val buffer = Buffer()

        UnsafeBufferOperations.writeToTail(buffer, 10) { ctx, tail ->
            ctx.setUnchecked(tail, 0, 1)
            ctx.setUnchecked(tail, 1, 2, 3)
            ctx.setUnchecked(tail, 3, 4, 5, 6)
            ctx.setUnchecked(tail, 6, 7, 8, 9, 10)
            10
        }

        assertArrayEquals(ByteArray(10) { (it + 1).toByte() }, buffer.readByteArray())

        // check overlapping writes
        UnsafeBufferOperations.writeToTail(buffer, 4) { ctx, tail ->
            ctx.setUnchecked(tail, 0, 11, 11, 11, 11)
            ctx.setUnchecked(tail, 1, 10, 10, 10)
            ctx.setUnchecked(tail, 2, 9, 9)
            ctx.setUnchecked(tail, 3, 8)
            4
        }
        assertArrayEquals(byteArrayOf(11, 10, 9, 8), buffer.readByteArray())
    }

    @Test
    fun resetWriteOnException() {
        val buffer = Buffer()

        assertFailsWith<TestException> {
            UnsafeBufferOperations.writeToTail(buffer, 2) { _, _, _ ->
                throw TestException()
            }
        }

        assertTrue(buffer.exhausted())

        assertFailsWith<TestException> {
            UnsafeBufferOperations.writeToTail(buffer, 2) { _, _ ->
                throw TestException()
            }
        }

        assertTrue(buffer.exhausted())
    }

    @Test
    fun returnLessBytesThanItWasActuallyWritten() {
        val buffer = Buffer()

        UnsafeBufferOperations.writeToTail(buffer, 42) { data, pos, limit ->
            data.fill(0xab.toByte(), pos, limit)
            4
        }
        assertEquals(4, buffer.size)
        assertEquals(0xababababu, buffer.readUInt())
    }
}
