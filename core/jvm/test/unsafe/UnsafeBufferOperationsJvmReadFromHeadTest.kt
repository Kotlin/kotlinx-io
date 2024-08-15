/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.Buffer
import kotlinx.io.UnsafeIoApi
import kotlinx.io.assertArrayEquals
import kotlinx.io.writeString
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(UnsafeIoApi::class)
class UnsafeBufferOperationsJvmReadFromHeadTest {
    private class TestException : RuntimeException()

    @Test
    fun callsInPlaceContract() {
        val buffer = Buffer().apply { writeString("hello world") }

        val called: Boolean
        UnsafeBufferOperations.readFromHead(buffer) { _ ->
            called = true
        }
        assertTrue(called)
    }

    @Test
    fun bufferCapacity() {
        val buffer = Buffer().apply { writeString("hello world") }

        val head = buffer.head!!
        UnsafeBufferOperations.readFromHead(buffer) { bb: ByteBuffer ->
            assertEquals(head.size, bb.remaining())
            assertEquals(0, bb.position())
            assertEquals(head.size, bb.limit())
        }
    }

    @Test
    fun consumeByteByByte() {
        val expectedData = "hello world".encodeToByteArray()
        val actualData = ByteArray(expectedData.size)

        val buffer = Buffer().apply { write(expectedData) }
        for (idx in actualData.indices) {
            val read = UnsafeBufferOperations.readFromHead(buffer) { bb ->
                actualData[idx] = bb.get()
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
        val read = UnsafeBufferOperations.readFromHead(buffer) { _ -> /* do nothing */ }
        assertEquals(0, read)
        assertEquals(42, buffer.readInt())
    }

    @Test
    fun readEverything() {
        val buffer = Buffer().apply { writeString("hello world") }
        val read = UnsafeBufferOperations.readFromHead(buffer) { bb ->
            bb.position(bb.limit())
        }
        assertEquals(11, read)
        assertTrue(buffer.exhausted())
    }

    @Test
    fun writeIntoReadOnlyBuffer() {
        val buffer = Buffer().apply { writeInt(42) }
        UnsafeBufferOperations.readFromHead(buffer) { bb ->
            assertFailsWith<UnsupportedOperationException> {
                bb.put(42)
            }
        }
        assertEquals(42, buffer.readInt())
    }

    @Test
    fun readFromEmptyBuffer() {
        val buffer = Buffer()
        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferOperations.readFromHead(buffer) { _ -> fail() }
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

        UnsafeBufferOperations.readFromHead(buffer) { bb ->
            assertEquals(segmentSize - bytesToSkip, bb.remaining())
            bb.getShort()
        }

        assertEquals(extraBytesCount, buffer.size.toInt())
    }

    @Test
    fun changeLimit() {
        val buffer = Buffer().apply { writeString("hello world") }
        val read = UnsafeBufferOperations.readFromHead(buffer) { bb ->
            // read a single byte only
            bb.position(1)
            bb.limit(2)
        }
        assertEquals(1, read)
        assertEquals(10, buffer.size)
    }

    @Test
    fun resetReadOnException() {
        val buffer = Buffer().apply { writeString("hello world") }

        val sizeBeforeRead = buffer.size
        assertFailsWith<TestException> {
            UnsafeBufferOperations.readFromHead(buffer) { bb ->
                bb.get()
                throw TestException()
            }
        }
        assertEquals(buffer.size, sizeBeforeRead)
    }
}
