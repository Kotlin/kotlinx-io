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

@OptIn(UnsafeIoApi::class, SnapshotApi::class)
class UnsafeBufferApiReadTest {
    @Test
    fun testBufferCapacity() {
        val buffer = Buffer().apply { writeString("hello world") }

        val head = buffer.head!!
        UnsafeBufferAccessors.readFromHead(buffer) { data, startIndex, endIndex ->
            assertTrue(endIndex <= data.size)
            assertEquals(head.size, endIndex - startIndex)
            assertEquals(0, startIndex)
            assertEquals(head.size, endIndex)
            0
        }
    }

    @Test
    fun testConsumeByteByByte() {
        val expectedData = "hello world".encodeToByteArray()
        val actualData = ByteArray(expectedData.size)

        val buffer = Buffer().apply { write(expectedData) }
        for (idx in actualData.indices) {
            UnsafeBufferAccessors.readFromHead(buffer) { data, startIndex, _ ->
                actualData[idx] = data[startIndex]
                1
            }
            assertEquals(actualData.size - idx - 1, buffer.size.toInt())
        }
        assertTrue(buffer.exhausted())
        assertArrayEquals(expectedData, actualData)
    }

    @Test
    fun testReadNothing() {
        val buffer = Buffer().apply { writeInt(42) }
        UnsafeBufferAccessors.readFromHead(buffer) { _, _, _ -> 0}
        assertEquals(42, buffer.readInt())
    }

    @Test
    fun testReadEverything() {
        val buffer = Buffer().apply { writeString("hello world") }
        UnsafeBufferAccessors.readFromHead(buffer) { _, startIndex, endIndex ->
            endIndex - startIndex
        }
        assertTrue(buffer.exhausted())
    }

    @Test
    fun testReadFromEmptyBuffer() {
        val buffer = Buffer()
        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferAccessors.readFromHead(buffer) { _, _, _ -> 0 }
        }
    }

    @Test
    fun testReadFromTheSegmentEnd() {
        val buffer = Buffer().apply { write(ByteArray(9000) { 0xff.toByte() }) }
        buffer.skip(8190)
        val head = buffer.head!!
        assertEquals(8190, head.pos)

        UnsafeBufferAccessors.readFromHead(buffer) { _, startIndex, endIndex ->
            assertEquals(2, endIndex - startIndex)
            2
        }

        assertEquals(9000 - 8192, buffer.size.toInt())
    }

    @Test
    fun testReturnIllegalReadCount() {
        val buffer = Buffer().apply { writeInt(0) }

        assertFailsWith<IllegalStateException> {
            UnsafeBufferAccessors.readFromHead(buffer) { _, _, _ -> -1 }
        }
        assertEquals(4L, buffer.size)

        assertFailsWith<IllegalStateException> {
            UnsafeBufferAccessors.readFromHead(buffer) { _, f, t -> (t - f + 1) }
        }
        assertEquals(4L, buffer.size)
    }
}
