/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlin.test.*

class SegmentIteratorTest {
    @Test
    fun testEmptyBuffer() {
        val iter = Buffer().segments()
        assertFalse(iter.hasNext())
        assertFailsWith<NoSuchElementException> {
            iter.next()
        }
    }

    @Test
    fun singleSegmentBuffer() {
        val iter = Buffer().apply { writeByte(42) }.segments()

        assertTrue(iter.hasNext())
        val segment = iter.next()
        assertEquals(1, segment.size)
        assertFalse(iter.hasNext())
    }

    @Test
    fun multiSegmentBuffer() {
        val iter = Buffer().apply { write(ByteArray(Segment.SIZE * 2 + 1)) }.segments()
        assertTrue(iter.hasNext())
        assertEquals(Segment.SIZE, iter.next().size)
        assertTrue(iter.hasNext())
        assertEquals(Segment.SIZE, iter.next().size)
        assertTrue(iter.hasNext())
        assertEquals(1, iter.next().size)
        assertFalse(iter.hasNext())
    }
}
