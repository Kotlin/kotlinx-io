/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.*
import kotlin.test.*

@OptIn(UnsafeIoApi::class)
class UnsafeBufferApiWriteTest {
    @Test
    fun testBufferCapacity() {
        val buffer = Buffer()

        UnsafeBufferAccessors.writeToTail(buffer, 1) { data, startIndex, endIndex ->
            // Unsafe check, head is not committed yet
            assertSame(buffer.head!!.data, data)
            assertEquals(buffer.head!!.data.size, (endIndex - startIndex))
            assertEquals(0, startIndex)
            assertEquals(buffer.head!!.data.size, endIndex)
            0
        }
    }

    @Test
    fun testWriteByteByByte() {
        val buffer = Buffer()
        val data = "hello world".encodeToByteArray()

        for (idx in data.indices) {
            UnsafeBufferAccessors.writeToTail(buffer, 1) { writeable, pos, _ ->
                writeable[pos] = data[idx]
                1
            }
            assertEquals(idx + 1, buffer.size.toInt())
        }
        assertEquals("hello world", buffer.readString())
    }

    @Test
    fun testWriteNothing() {
        val buffer = Buffer()
        UnsafeBufferAccessors.writeToTail(buffer, 1) { _, _, _ -> 0 }
        assertTrue(buffer.exhausted())
    }

    @Test
    fun testWriteWholeBuffer() {
        val buffer = Buffer()
        UnsafeBufferAccessors.writeToTail(buffer, 1) { data, from, to ->
            for (idx in from ..< to) {
                data[idx] = 42
            }
            to - from
        }
        assertEquals(Segment.SIZE, buffer.size.toInt())
        assertArrayEquals(ByteArray(Segment.SIZE) { 42 }, buffer.readByteArray())
    }

    @Test
    fun testRequireToManyBytes() {
        val buffer = Buffer()
        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferAccessors.writeToTail(buffer, 100500) { _, _, _ -> 0 }
        }
        assertTrue(buffer.exhausted())
    }

    @Test
    fun testWriteToTheEndOfABuffer() {
        val buffer = Buffer().apply { write(ByteArray(Segment.SIZE - 1)) }
        UnsafeBufferAccessors.writeToTail(buffer, 1) { data, pos, limit ->
            assertEquals(1, limit - pos)
            data[pos] = 42
            1
        }
        assertEquals(Segment.SIZE, buffer.size.toInt())
        UnsafeBufferAccessors.writeToTail(buffer, 1) { data, pos, _ ->
            data[pos] = 43
            1
        }
        assertEquals(Segment.SIZE + 1, buffer.size.toInt())

        buffer.skip(Segment.SIZE - 1L)
        assertArrayEquals(byteArrayOf(42, 43), buffer.readByteArray())
    }

    @Test
    fun testReturnIllegalWriteCount() {
        val buffer = Buffer()
        assertFailsWith<IllegalStateException> {
            UnsafeBufferAccessors.writeToTail(buffer, 1) { _, _, _ ->
                -1
            }
        }
        assertTrue(buffer.exhausted())

        assertFailsWith<IllegalStateException> {
            UnsafeBufferAccessors.writeToTail(buffer, 1) { _, _, _ ->
                100500
            }
        }
        assertTrue(buffer.exhausted())
    }
}
