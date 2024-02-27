/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.samples.unsafe

import kotlinx.io.*
import kotlinx.io.unsafe.UnsafeBufferAccessors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

public class ReadWriteUnsafe {
    @OptIn(SnapshotApi::class, UnsafeIoApi::class)
    @Test
    fun bulkConsumption() {
        val initialData = ByteArray(1024 * 1024 /* 1 MB */) { idx -> idx.toByte() }
        val expectedSum = initialData.asSequence().map { it.toLong() }.sum()

        val buffer = Buffer().also {
            it.write(initialData)
        }
        var sum = 0L
        while (!buffer.exhausted()) {
            UnsafeBufferAccessors.readFromHead(buffer) { data, startIndex, endIndex ->
                for (idx in startIndex ..< endIndex) {
                    sum += data[idx]
                }
                // consume all the data
                endIndex - startIndex
            }
        }
        assertEquals(expectedSum, sum)
    }

    @OptIn(SnapshotApi::class, UnsafeIoApi::class)
    @Test
    fun basicReadProperties() {
        val buffer = Buffer().apply { writeString("hello world") }
        assertEquals(11, buffer.size)

        UnsafeBufferAccessors.readFromHead(buffer) { data, startIndex, endIndex ->
            assertTrue(endIndex > startIndex)
            assertEquals('h', data[startIndex].toInt().toChar())
            0 /* don't consume any data */
        }
        assertEquals(11, buffer.size)

        UnsafeBufferAccessors.readFromHead(buffer) { data, startIndex, _ ->
            assertEquals('h', data[startIndex].toInt().toChar())
            1 /* consume the first byte */
        }
        assertEquals(10, buffer.size)
        assertEquals("ello world", buffer.readString())
    }

    @OptIn(UnsafeIoApi::class, SnapshotApi::class)
    @Test
    fun basicWriteProperties() {
        val buffer = Buffer()

        UnsafeBufferAccessors.writeToTail(buffer, 1) { data, startIndex, _ ->
            data[startIndex] = 42
            1
        }
        assertEquals(1, buffer.size)
        assertEquals(42, buffer.readByte())

        UnsafeBufferAccessors.writeToTail(buffer, 1) { data, startIndex, _ ->
            data[startIndex] = 42
            0
        }
        assertTrue(buffer.exhausted())
        assertFailsWith<IOException> { buffer.readByte() }
    }
}
