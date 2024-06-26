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
            assertSame(buffer.head!!.data, data)

            assertEquals(0, startIndex)
            assertEquals(buffer.head!!.data.size, endIndex)
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

        buffer.writeInt(42)
        UnsafeBufferOperations.writeToTail(buffer, 1) { _, _, _ -> 0 }
        assertEquals(4, buffer.size)

        buffer.write(ByteArray(Segment.SIZE - 4))
        UnsafeBufferOperations.writeToTail(buffer, 1) { _, _, _ -> 0 }
        assertEquals(Segment.SIZE.toLong(), buffer.size)
    }

    @Test
    fun writeWholeBuffer() {
        val buffer = Buffer()
        UnsafeBufferOperations.writeToTail(buffer, 1) { data, from, to ->
            for (idx in from ..< to) {
                data[idx] = 42
            }
            to - from
        }
        assertEquals(Segment.SIZE, buffer.size.toInt())
        assertArrayEquals(ByteArray(Segment.SIZE) { 42 }, buffer.readByteArray())
    }

    @Test
    fun requireToManyBytes() {
        val buffer = Buffer()
        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferOperations.writeToTail(buffer, 100500) { _, _, _ -> 0 }
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
    fun resetWriteOnException() {
        val buffer = Buffer()

        assertFailsWith<TestException> {
            UnsafeBufferOperations.writeToTail(buffer, 2) { _, _, _ ->
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
