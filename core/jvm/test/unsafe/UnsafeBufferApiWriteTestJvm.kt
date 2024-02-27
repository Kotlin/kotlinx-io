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
class UnsafeBufferApiWriteTestJvm {
    @Test
    fun testBufferCapacity() {
        val buffer = Buffer()

        UnsafeBufferAccessors.writeToTail(buffer, 1) { bb ->
            // Unsafe check, head is not committed yet
            assertEquals(buffer.head!!.data.size, bb.remaining())
            assertEquals(0, bb.position())
            assertEquals(buffer.head!!.data.size, bb.limit())
        }
    }

    @Test
    fun testWriteByteByByte() {
        val buffer = Buffer()
        val data = "hello world".encodeToByteArray()

        for (idx in data.indices) {
            UnsafeBufferAccessors.writeToTail(buffer, 1) { bb ->
                bb.put(data[idx])
            }
            assertEquals(idx + 1, buffer.size.toInt())
        }
        assertEquals("hello world", buffer.readString())
    }

    @Test
    fun testWriteNothing() {
        val buffer = Buffer()
        UnsafeBufferAccessors.writeToTail(buffer, 1) { _ -> }
        assertTrue(buffer.exhausted())
    }

    @Test
    fun testWriteWholeBuffer() {
        val buffer = Buffer()
        UnsafeBufferAccessors.writeToTail(buffer, 1) { bb ->
            bb.position(bb.limit())
        }
        assertEquals(Segment.SIZE, buffer.size.toInt())
    }

    @Test
    fun testRequireToManyBytes() {
        val buffer = Buffer()
        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferAccessors.writeToTail(buffer, 100500) { _ -> }
        }
        assertTrue(buffer.exhausted())
    }

    @Test
    fun testWriteToTheEndOfABuffer() {
        val buffer = Buffer().apply { write(ByteArray(Segment.SIZE - 1)) }
        UnsafeBufferAccessors.writeToTail(buffer, 1) { bb ->
            assertEquals(1, bb.remaining())
            bb.put(42)
        }
        assertEquals(Segment.SIZE, buffer.size.toInt())
        UnsafeBufferAccessors.writeToTail(buffer, 1) { bb ->
            bb.put(43)
        }
        assertEquals(Segment.SIZE + 1, buffer.size.toInt())

        buffer.skip(Segment.SIZE - 1L)
        assertArrayEquals(byteArrayOf(42, 43), buffer.readByteArray())
    }

    @Test
    fun testChangeLimit() {
        val buffer = Buffer()

        UnsafeBufferAccessors.writeToTail(buffer, 8) { bb ->
            // only two bytes written
            bb.position(2)
            bb.limit(4)
        }

        assertEquals(2, buffer.size)
    }
}
