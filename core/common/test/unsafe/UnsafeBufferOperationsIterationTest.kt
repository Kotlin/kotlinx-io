/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.unsafe

import kotlinx.io.Buffer
import kotlinx.io.UnsafeIoApi
import kotlinx.io.assertArrayEquals
import kotlinx.io.writeString
import kotlin.test.*

@OptIn(UnsafeIoApi::class)

class UnsafeBufferOperationsIterationTest {
    @Test
    fun emptyBuffer() {
        UnsafeBufferOperations.iterate(Buffer()) { _, head ->
            assertNull(head)
        }
    }

    @Test
    fun singleSegment() {
        UnsafeBufferOperations.iterate(Buffer().also { it.writeByte(1) }) { ctx, head ->
            assertNotNull(head)
            assertEquals(1, head.size)

            assertNull(ctx.next(head))
        }
    }

    @Test
    fun multipleSegments() {
        val buffer = Buffer()

        val expectedSegments = 10
        for (i in 0 ..< expectedSegments) {
            UnsafeBufferOperations.moveToTail(buffer, byteArrayOf(i.toByte()))
        }

        val storedBytes = ByteArray(expectedSegments)

        UnsafeBufferOperations.iterate(buffer) { ctx, head ->
            var idx = 0
            var seg = head

            do {
                seg!!

                assertTrue(idx < expectedSegments)
                storedBytes[idx++] = ctx.getUnchecked(seg, 0)
                seg = ctx.next(seg)
            } while (seg != null)
        }

        assertArrayEquals(ByteArray(expectedSegments) { it.toByte() }, storedBytes)
    }

    @Test
    fun acquireDataDuringIteration() {
        val buffer = Buffer().also { it.writeString("hello buffer") }

        UnsafeBufferOperations.iterate(buffer) { ctx, head ->
            assertNotNull(head)

            ctx.withData(head) { data, startIndex, endIndex ->
                assertEquals("hello buffer", data.decodeToString(startIndex, endIndex))
            }
        }
    }

    @Test
    fun seekStartOffsets() {
        val firstSegmentData = "hello buffer".encodeToByteArray()
        val buffer = Buffer().also { it.write(firstSegmentData) }

        UnsafeBufferOperations.iterate(buffer, offset = 6) { _, segment, offset ->
            assertNotNull(segment)
            assertEquals(0, offset)
        }

        val secondSegmentData = "; that's a second segment".encodeToByteArray()
        UnsafeBufferOperations.moveToTail(buffer, secondSegmentData)
        val thirdSegmentData = "; that's a third segment".encodeToByteArray()
        UnsafeBufferOperations.moveToTail(buffer, thirdSegmentData)

        val startOfSegmentOffsets = longArrayOf(
            0,
            firstSegmentData.size.toLong(),
            (firstSegmentData.size + secondSegmentData.size).toLong(),
            (firstSegmentData.size + secondSegmentData.size + thirdSegmentData.size).toLong()
        )

        val segmentsData = arrayOf(firstSegmentData, secondSegmentData, thirdSegmentData)

        for (limitIdx in 1 ..< startOfSegmentOffsets.size) {
            val startOffset = startOfSegmentOffsets[limitIdx - 1]
            val limitOffset = startOfSegmentOffsets[limitIdx]

            for (offset in startOffset ..< limitOffset) {
                UnsafeBufferOperations.iterate(buffer, offset) { ctx, seg, o ->
                    assertNotNull(seg)
                    assertEquals(startOffset, o)

                    ctx.withData(seg) { data, startIndex, endIndex ->
                        val slice = data.sliceArray(startIndex ..< endIndex)
                        assertArrayEquals(segmentsData[limitIdx - 1], slice)
                    }
                }
            }
        }
    }

    @Test
    fun seekOutOfBounds() {
        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferOperations.iterate(Buffer(), offset = -1) { _, _, _ -> fail() }
        }
        assertFailsWith<IndexOutOfBoundsException> {
            UnsafeBufferOperations.iterate(Buffer(), offset = 0) { _, _, _ -> fail() }
        }
        assertFailsWith<IndexOutOfBoundsException> {
            UnsafeBufferOperations.iterate(Buffer(), offset = 9001) { _, _, _ -> fail() }
        }

        val buffer = Buffer().also { it.writeInt(42) }
        assertFailsWith<IllegalArgumentException> {
            UnsafeBufferOperations.iterate(buffer, offset = -1) { _, _, _ -> fail() }
        }
        assertFailsWith<IndexOutOfBoundsException> {
            UnsafeBufferOperations.iterate(buffer, offset = 4) { _, _, _ -> fail() }
        }
    }
}
