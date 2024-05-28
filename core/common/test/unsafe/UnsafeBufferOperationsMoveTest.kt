/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.*
import kotlin.test.*

@OptIn(UnsafeIoApi::class)
class UnsafeBufferOperationsMoveTest {
    @Test
    fun moveArrayFully() {
        val buffer = Buffer()
        val data = "hello unsafe new world!".encodeToByteArray()

        UnsafeBufferOperations.moveToTail(buffer, data)
        assertFalse(buffer.exhausted())
        assertEquals(data.size.toLong(), buffer.size)

        assertEquals("hello unsafe new world!", buffer.readString())
    }

    @Test
    fun moveArraySlice() {
        val buffer = Buffer()
        val data = "hello unsafe new world!".encodeToByteArray()

        UnsafeBufferOperations.moveToTail(buffer, data, 6, 12)
        assertEquals(6L, buffer.size)
        assertEquals("unsafe", buffer.readString())
    }

    @Test
    fun movedArrayIsReadOnly() {
        val firstBuffer = Buffer().also { it.writeString("this is ") }
        val secondBuffer = Buffer().also { it.writeString("this is ") }

        val data = "first second third".encodeToByteArray()

        UnsafeBufferOperations.moveToTail(firstBuffer, data, 0, 5)
        firstBuffer.writeString(" buffer")

        UnsafeBufferOperations.moveToTail(secondBuffer, data, 6, 12)
        secondBuffer.writeString(" buffer")

        assertArrayEquals("first second third".encodeToByteArray(), data)

        assertEquals("this is first buffer", firstBuffer.readString())
        assertEquals("this is second buffer", secondBuffer.readString())
    }

    @Test
    fun moveEmptySlice() {
        val buffer = Buffer()

        UnsafeBufferOperations.moveToTail(buffer, ByteArray(0))
        assertTrue(buffer.exhausted())

        UnsafeBufferOperations.moveToTail(buffer, ByteArray(10), 5, 5)
        assertTrue(buffer.exhausted())
    }

    @Test
    fun illegalArgumentsHandling() {
        assertFailsWith<IndexOutOfBoundsException> {
            UnsafeBufferOperations.moveToTail(Buffer(), ByteArray(1), -1)
        }

        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferOperations.moveToTail(Buffer(), ByteArray(10), 2, 0)
        }

        assertFailsWith<IndexOutOfBoundsException> {
            UnsafeBufferOperations.moveToTail(Buffer(), ByteArray(10), 11, 12)
        }
    }
}
