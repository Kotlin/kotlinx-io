/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
@file:OptIn(UnsafeIoApi::class)

package kotlinx.io.unsafe

import kotlinx.io.Buffer
import kotlinx.io.UnsafeIoApi
import kotlinx.io.assertArrayEquals
import kotlinx.io.writeString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class UnsafeBufferOperationsForEachTest {

    @Test
    fun emptyBuffer() {
        UnsafeBufferOperations.forEachSegment(Buffer()) { _, head ->
            fail()
        }
    }

    @Test
    fun singleSegment() {
        var counter = 0
        UnsafeBufferOperations.forEachSegment(Buffer().also { it.writeByte(1) }) { ctx, segment ->
            ++counter
            assertEquals(1, segment.size)
        }
        assertEquals(1, counter)
    }

    @Test
    fun multipleSegments() {
        val buffer = Buffer()

        val expectedSegments = 10
        for (i in 0 ..< expectedSegments) {
            UnsafeBufferOperations.moveToTail(buffer, byteArrayOf(i.toByte()))
        }

        val storedBytes = ByteArray(expectedSegments)
        var idx = 0
        UnsafeBufferOperations.forEachSegment(buffer) { ctx, segment ->
            assertTrue(idx < expectedSegments)
            storedBytes[idx++] = ctx.getUnchecked(segment, 0)
        }

        assertArrayEquals(ByteArray(expectedSegments) { it.toByte() }, storedBytes)
    }

    @Test
    fun acquireDataDuringIteration() {
        val buffer = Buffer().also { it.writeString("hello buffer") }

        val expectedSize = buffer.size

        UnsafeBufferOperations.forEachSegment(buffer) { ctx, segment ->
            ctx.withData(segment) { data, startIndex, endIndex ->
                assertEquals("hello buffer", data.decodeToString(startIndex, endIndex))
            }
        }

        assertEquals(expectedSize, buffer.size)
    }
}