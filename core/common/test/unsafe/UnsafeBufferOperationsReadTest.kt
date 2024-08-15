/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.Buffer
import kotlinx.io.UnsafeIoApi
import kotlinx.io.assertArrayEquals
import kotlinx.io.writeString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(UnsafeIoApi::class)
class UnsafeBufferOperationsReadTest {
    private class TestException : RuntimeException()

    @Test
    fun callsInPlaceContract() {
        val buffer = Buffer().apply { writeString("hello world") }

        val bytesCalled: Boolean
        UnsafeBufferOperations.readFromHead(buffer) { _, _, _ ->
            bytesCalled = true
            0
        }
        assertTrue(bytesCalled)

        val segmentsCalled: Boolean
        UnsafeBufferOperations.readFromHead(buffer) { _, _ ->
            segmentsCalled = true
            0
        }
        assertTrue(segmentsCalled)
    }

    @Test
    fun bufferCapacity() {
        val buffer = Buffer().apply { writeString("hello world") }

        val head = buffer.head!!
        UnsafeBufferOperations.readFromHead(buffer) { data, startIndex, endIndex ->
            assertTrue(endIndex <= data.size)
            assertEquals(0, startIndex)
            assertEquals(head.size, endIndex)
            0
        }
    }

    @Test
    fun consumeByteByByte() {
        val expectedData = "hello world".encodeToByteArray()
        val actualData = ByteArray(expectedData.size)

        val buffer = Buffer().apply { write(expectedData) }
        for (idx in actualData.indices) {
            val read = UnsafeBufferOperations.readFromHead(buffer) { data, startIndex, _ ->
                actualData[idx] = data[startIndex]
                1
            }
            assertEquals(1, read)
            assertEquals(actualData.size - idx - 1, buffer.size.toInt())
        }
        assertTrue(buffer.exhausted())
        assertArrayEquals(expectedData, actualData)
    }

    @Test
    fun readNothing() {
        val buffer = Buffer().apply { writeInt(42) }
        val read1 = UnsafeBufferOperations.readFromHead(buffer) { _, _, _ -> 0 }
        assertEquals(0, read1)
        assertEquals(42, buffer.readInt())

        buffer.writeInt(42)
        val read2 = UnsafeBufferOperations.readFromHead(buffer) { _, _ -> 0 }
        assertEquals(0, read2)
        assertEquals(42, buffer.readInt())
    }

    @Test
    fun readEverything() {
        val buffer = Buffer().apply { writeString("hello world") }
        val read1 = UnsafeBufferOperations.readFromHead(buffer) { _, startIndex, endIndex ->
            endIndex - startIndex
        }
        assertEquals(11, read1)
        assertTrue(buffer.exhausted())

        buffer.writeString("hello world")
        val read2 = UnsafeBufferOperations.readFromHead(buffer) { _, seg -> seg.size }
        assertEquals(11, read2)
        assertTrue(buffer.exhausted())
    }

    @Test
    fun readFromEmptyBuffer() {
        val buffer = Buffer()
        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferOperations.readFromHead(buffer) { _, _, _ -> fail() }
        }

        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferOperations.readFromHead(buffer) { _, _ -> fail() }
        }
    }

    @Test
    fun readFromTheSegmentEnd() {
        val segmentSize = UnsafeBufferOperations.maxSafeWriteCapacity
        val extraBytesCount = 128
        val bytesToSkip = segmentSize - 2

        val buffer = Buffer().apply { write(ByteArray(segmentSize + extraBytesCount) { 0xff.toByte() }) }
        buffer.skip(bytesToSkip.toLong())
        val head = buffer.head!!
        assertEquals(bytesToSkip, head.pos)

        UnsafeBufferOperations.readFromHead(buffer) { _, startIndex, endIndex ->
            assertEquals(2, endIndex - startIndex)
            2
        }

        assertEquals(extraBytesCount, buffer.size.toInt())
    }

    @Test
    fun readFromTheSegmentEndUsingCtx() {
        val segmentSize = UnsafeBufferOperations.maxSafeWriteCapacity
        val extraBytesCount = 128
        val bytesToSkip = segmentSize - 2

        val buffer = Buffer().apply { write(ByteArray(segmentSize + extraBytesCount) { 0xff.toByte() }) }
        buffer.skip(bytesToSkip.toLong())
        val head = buffer.head!!
        assertEquals(bytesToSkip, head.pos)

        UnsafeBufferOperations.readFromHead(buffer) { _, seg ->
            assertEquals(2, seg.size)
            2
        }

        assertEquals(extraBytesCount, buffer.size.toInt())
    }

    @Test
    fun returnIllegalReadCount() {
        val buffer = Buffer().apply { writeInt(0) }

        assertFailsWith<IllegalStateException> {
            UnsafeBufferOperations.readFromHead(buffer) { _, _, _ -> -1 }
        }
        assertEquals(4L, buffer.size)

        assertFailsWith<IllegalStateException> {
            UnsafeBufferOperations.readFromHead(buffer) { _, _ -> -1 }
        }
        assertEquals(4L, buffer.size)

        assertFailsWith<IllegalStateException> {
            UnsafeBufferOperations.readFromHead(buffer) { _, f, t -> (t - f + 1) }
        }
        assertEquals(4L, buffer.size)

        assertFailsWith<IllegalStateException> {
            UnsafeBufferOperations.readFromHead(buffer) { _, seg -> seg.remainingCapacity + 1 }
        }
        assertEquals(4L, buffer.size)
    }

    @Test
    fun resetReadOnException() {
        val buffer = Buffer().apply { writeString("hello world") }

        val sizeBeforeRead = buffer.size
        assertFailsWith<TestException> {
            UnsafeBufferOperations.readFromHead(buffer) { _, _, _ ->
                throw TestException()
            }
        }
        assertEquals(buffer.size, sizeBeforeRead)

        assertFailsWith<TestException> {
            UnsafeBufferOperations.readFromHead(buffer) { _, _ ->
                throw TestException()
            }
        }
        assertEquals(buffer.size, sizeBeforeRead)
    }
}
