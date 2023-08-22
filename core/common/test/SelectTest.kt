/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.select.Options
import kotlinx.io.select.selectWithIter
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelectTest {
    @Test
    fun select() {
        val options = Options.of(
            "ROCK".encodeToByteString(),
            "SCISSORS".encodeToByteString(),
            "PAPER".encodeToByteString()
        )
        val buffer = Buffer()
        buffer.writeString("PAPER,SCISSORS,ROCK")

        assertEquals(2, buffer.selectWithIter(options))
        assertEquals(',', buffer.readByte().toInt().toChar())
        assertEquals(1, buffer.selectWithIter(options))
        assertEquals(',', buffer.readByte().toInt().toChar())
        assertEquals(0, buffer.selectWithIter(options))
        assertTrue(buffer.exhausted())
    }

    @Test
    fun selectSpanningMultipleSegments() {
        val commonPrefix: ByteString = ByteString(Random.Default.nextBytes(Segment.SIZE + 10))
        val a: ByteString = Buffer().let {
            it.write(commonPrefix)
            it.writeString("a")
            it.readByteString()
        }
        val bc: ByteString = Buffer().let {
            it.write(commonPrefix)
            it.writeString("bc")
            it.readByteString()
        }
        val bd: ByteString = Buffer().let {
            it.write(commonPrefix)
            it.writeString("bd")
            it.readByteString()
        }
        val options = Options.of(a, bc, bd)
        val source = Buffer()
        source.write(bd)
        source.write(a)
        source.write(bc)
        assertEquals(2, source.selectWithIter(options))
        assertEquals(0, source.selectWithIter(options))
        assertEquals(1, source.selectWithIter(options))
        assertTrue(source.exhausted())
    }

    @Test
    fun selectNotFound() {
        val options = Options.of(
            "ROCK".encodeToByteString(),
            "SCISSORS".encodeToByteString(),
            "PAPER".encodeToByteString()
        )
        val source = Buffer().apply { writeString("SPOCK") }
        assertEquals(-1, source.selectWithIter(options))
        assertEquals("SPOCK", source.readString())
    }

    @Test
    fun selectValuesHaveCommonPrefix() {
        val options = Options.of(
            "abcd".encodeToByteString(),
            "abce".encodeToByteString(),
            "abcc".encodeToByteString()
        )
        val source = Buffer().apply {
            writeString("abcc")
            writeString("abcd")
            writeString("abce")
        }
        assertEquals(2, source.selectWithIter(options))
        assertEquals(0, source.selectWithIter(options))
        assertEquals(1, source.selectWithIter(options))
    }

    @Test
    fun selectLongerThanSource() {
        val options = Options.of(
            "abcd".encodeToByteString(),
            "abce".encodeToByteString(),
            "abcc".encodeToByteString()
        )
        val source = Buffer().apply { writeString("abc") }
        assertEquals(-1, source.selectWithIter(options))
        assertEquals("abc", source.readString())
    }

    @Test
    @Throws(IOException::class)
    fun selectReturnsFirstByteStringThatMatches() {
        val options = Options.of(
            "abcd".encodeToByteString(),
            "abc".encodeToByteString(),
            "abcde".encodeToByteString()
        )
        val source = Buffer().apply { writeString("abcdef") }
        assertEquals(0, source.selectWithIter(options))
        assertEquals("ef", source.readString())
    }

    @Test
    fun selectFromEmptySource() {
        val options = Options.of(
            "abc".encodeToByteString(),
            "def".encodeToByteString()
        )
        assertEquals(-1, Buffer().selectWithIter(options))
    }

    @Test
    fun selectNoByteStringsFromEmptySource() {
        val options = Options.of()
        assertEquals(-1, Buffer().selectWithIter(options))
    }
}
