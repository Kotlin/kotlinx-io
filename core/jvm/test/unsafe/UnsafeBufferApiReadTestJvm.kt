/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.*
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(UnsafeIoApi::class, SnapshotApi::class)
class UnsafeBufferApiReadTestJvm {
    @Test
    fun testBufferCapacity() {
        val buffer = Buffer().apply { writeString("hello world") }

        val head = buffer.head!!
        UnsafeBufferAccessors.readFromHead(buffer) { bb: ByteBuffer ->
            assertEquals(head.size, bb.remaining())
            assertEquals(0, bb.position())
            assertEquals(head.size, bb.limit())
        }
    }

    @Test
    fun testConsumeByteByByte() {
        val expectedData = "hello world".encodeToByteArray()
        val actualData = ByteArray(expectedData.size)

        val buffer = Buffer().apply { write(expectedData) }
        for (idx in actualData.indices) {
            UnsafeBufferAccessors.readFromHead(buffer) { bb ->
                actualData[idx] = bb.get()
            }
            assertEquals(actualData.size - idx - 1, buffer.size.toInt())
        }
        assertTrue(buffer.exhausted())
        assertArrayEquals(expectedData, actualData)
    }

    @Test
    fun testReadNothing() {
        val buffer = Buffer().apply { writeInt(42) }
        UnsafeBufferAccessors.readFromHead(buffer) { _ -> /* do nothing */ }
        assertEquals(42, buffer.readInt())
    }

    @Test
    fun testReadEverything() {
        val buffer = Buffer().apply { writeString("hello world") }
        UnsafeBufferAccessors.readFromHead(buffer) { bb ->
            bb.position(bb.limit())
        }
        assertTrue(buffer.exhausted())
    }

    @Test
    fun testWriteIntoReadOnlyBuffer() {
        val buffer = Buffer().apply { writeInt(42) }
        UnsafeBufferAccessors.readFromHead(buffer) { bb ->
            assertFailsWith<UnsupportedOperationException> {
                bb.put(42)
            }
        }
        assertEquals(42, buffer.readInt())
    }

    @Test
    fun testReadFromEmptyBuffer() {
        val buffer = Buffer()
        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferAccessors.readFromHead(buffer) { _ -> }
        }
    }

    @Test
    fun testReadFromTheSegmentEnd() {
        val buffer = Buffer().apply { write(ByteArray(9000) { 0xff.toByte() }) }
        buffer.skip(8190)
        val head = buffer.head!!
        assertEquals(8190, head.pos)

        UnsafeBufferAccessors.readFromHead(buffer) { bb ->
            assertEquals(2, bb.remaining())
            bb.getShort()
        }

        assertEquals(9000 - 8192, buffer.size.toInt())
    }

    @Test
    fun testChangeLimit() {
        val buffer = Buffer().apply { writeString("hello world") }
        UnsafeBufferAccessors.readFromHead(buffer) { bb ->
            // read a single byte only
            bb.position(1)
            bb.limit(2)
        }
        assertEquals(10, buffer.size)
    }
}
