/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(UnsafeIoApi::class)
class UnsafeBufferOperationsJvmWriteToTailTest {
    private class TestException : RuntimeException()

    @Test
    fun bufferCapacity() {
        val buffer = Buffer()

        UnsafeBufferOperations.writeToTail(buffer, 1) { bb ->
            // Unsafe check, head is not committed yet
            assertEquals(buffer.head!!.data.size, bb.remaining())
            assertEquals(0, bb.position())
            assertEquals(buffer.head!!.data.size, bb.limit())
        }
    }

    @Test
    fun writeByteByByte() {
        val buffer = Buffer()
        val data = "hello world".encodeToByteArray()

        for (idx in data.indices) {
            UnsafeBufferOperations.writeToTail(buffer, 1) { bb ->
                bb.put(data[idx])
            }
            assertEquals(idx + 1, buffer.size.toInt())
        }
        assertEquals("hello world", buffer.readString())
    }

    @Test
    fun writeNothing() {
        val buffer = Buffer()
        UnsafeBufferOperations.writeToTail(buffer, 1) { _ -> }
        assertTrue(buffer.exhausted())
    }

    @Test
    fun writeWholeBuffer() {
        val buffer = Buffer()
        UnsafeBufferOperations.writeToTail(buffer, 1) { bb ->
            bb.position(bb.limit())
        }
        assertEquals(Segment.SIZE, buffer.size.toInt())
    }

    @Test
    fun requireToManyBytes() {
        val buffer = Buffer()
        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferOperations.writeToTail(buffer, 100500) { _ -> }
        }
        assertTrue(buffer.exhausted())
    }

    @Test
    fun writeToTheEndOfABuffer() {
        val buffer = Buffer().apply { write(ByteArray(Segment.SIZE - 1)) }
        UnsafeBufferOperations.writeToTail(buffer, 1) { bb ->
            assertEquals(1, bb.remaining())
            bb.put(42)
        }
        assertEquals(Segment.SIZE, buffer.size.toInt())
        UnsafeBufferOperations.writeToTail(buffer, 1) { bb ->
            bb.put(43)
        }
        assertEquals(Segment.SIZE + 1, buffer.size.toInt())

        buffer.skip(Segment.SIZE - 1L)
        assertArrayEquals(byteArrayOf(42, 43), buffer.readByteArray())
    }

    @Test
    fun changeLimit() {
        val buffer = Buffer()

        UnsafeBufferOperations.writeToTail(buffer, 8) { bb ->
            // only two bytes written
            bb.position(2)
            bb.limit(4)
        }

        assertEquals(2, buffer.size)
    }

    @Test
    fun resetWriteOnException() {
        val buffer = Buffer()

        assertFailsWith<TestException> {
            UnsafeBufferOperations.writeToTail(buffer, 2) { bb ->
                bb.put(42)
                throw TestException()
            }
        }

        assertTrue(buffer.exhausted())
    }
}
