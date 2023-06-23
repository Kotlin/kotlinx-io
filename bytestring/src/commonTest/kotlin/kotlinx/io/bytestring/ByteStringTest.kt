/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.bytestring

import kotlin.test.*

class ByteStringTest {
    @Test
    fun get() {
        val actual = ByteString("abc".encodeToByteArray())
        assertEquals(3, actual.size)
        assertEquals(actual[0], 'a'.code.toByte())
        assertEquals(actual[1], 'b'.code.toByte())
        assertEquals(actual[2], 'c'.code.toByte())
    }

    @Test
    fun getWithInvalidIndex() {
        val str = ByteString(0, 1, 2)
        assertFailsWith<IndexOutOfBoundsException> { str[-1] }
        assertFailsWith<IndexOutOfBoundsException> { str[3] }
    }

    @Test
    fun equalsAndHashCode() {
        with(ByteString(1, 2, 3)) { checkEqualsAndHashCodeAreSame(this, this) }
        checkEqualsAndHashCodeAreSame(ByteString.EMPTY, ByteString(byteArrayOf()))
        checkEqualsAndHashCodeAreSame(ByteString(1, 2, 3), ByteString(1, 2, 3))

        assertNotEquals(ByteString(1, 2, 3), ByteString(3, 2, 1))
        assertNotEquals(ByteString(1, 2, 3).hashCode(), ByteString(3, 2, 1).hashCode())

        assertNotEquals(ByteString(1, 2, 3, 4), ByteString(1, 2, 3))

        val str1 = ByteString(1, 2, 3)
        val str2 = ByteString(2, 3, 4)
        // force hashCode computation
        assertNotEquals(str1.hashCode(), str2.hashCode())
        assertNotEquals(str1, str2)

        assertFalse(ByteString().equals(null))
        assertFalse(ByteString().equals(byteArrayOf(1, 2, 3)))
    }

    private fun checkEqualsAndHashCodeAreSame(first: ByteString, second: ByteString) {
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun toByteArray() {
        val str = ByteString(1, 2, 3, 4, 5, 6)
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5, 6), str.toByteArray())
        assertContentEquals(byteArrayOf(), str.toByteArray(0, 0))
        assertContentEquals(byteArrayOf(1, 2, 3), str.toByteArray(endIndex = 3))
        assertContentEquals(byteArrayOf(4, 5, 6), str.toByteArray(startIndex = 3))
        assertContentEquals(byteArrayOf(2, 3, 4), str.toByteArray(startIndex = 1, endIndex = 4))
    }

    @Test
    fun toByteArrayWithInvalidIndex() {
        val str = ByteString(1, 2, 3)
        assertFailsWith<IndexOutOfBoundsException> { str.toByteArray(-1, 1) }
        assertFailsWith<IndexOutOfBoundsException> { str.toByteArray(1, 4) }
        assertFailsWith<IndexOutOfBoundsException> { str.toByteArray(-1, 4) }
        assertFailsWith<IllegalArgumentException> { str.toByteArray(2, 0) }
    }

    @Test
    fun copyTo() {
        val str = ByteString(1, 2, 3, 4, 5, 6)
        val dest = ByteArray(10)

        str.copyInto(dest)
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 0, 0, 0, 0), dest)

        dest.fill(0)
        str.copyInto(dest, 2)
        assertContentEquals(byteArrayOf(0, 0, 1, 2, 3, 4, 5, 6, 0, 0), dest)

        dest.fill(0)
        str.copyInto(dest, destinationOffset = 0, startIndex = 1)
        assertContentEquals(byteArrayOf(2, 3, 4, 5, 6, 0, 0, 0, 0, 0), dest)

        dest.fill(0)
        str.copyInto(dest, destinationOffset = 0, endIndex = 3)
        assertContentEquals(byteArrayOf(1, 2, 3, 0, 0, 0, 0, 0, 0, 0), dest)

        dest.fill(0)
        str.copyInto(dest, destinationOffset = 0, startIndex = 3, endIndex = 5)
        assertContentEquals(byteArrayOf(4, 5, 0, 0, 0, 0, 0, 0, 0, 0), dest)

        dest.fill(0)
        str.copyInto(dest, destinationOffset = 5, endIndex = 5)
        assertContentEquals(byteArrayOf(0, 0, 0, 0, 0, 1, 2, 3, 4, 5), dest)

        dest.fill(0)
        str.copyInto(dest, startIndex = 3, endIndex = 3)
        assertContentEquals(ByteArray(10), dest)
    }

    @Test
    fun copyToWithInvalidArguments() {
        val str = ByteString(1, 2, 3)
        val dest = ByteArray(10)

        assertFailsWith<IllegalArgumentException> { str.copyInto(dest, 0, startIndex = 1, endIndex = 0) }
        assertFailsWith<IndexOutOfBoundsException> { str.copyInto(dest, 9) }
        assertFailsWith<IndexOutOfBoundsException> { str.copyInto(dest, -1) }
        assertFailsWith<IndexOutOfBoundsException> { str.copyInto(dest, 0, startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { str.copyInto(dest, 0, endIndex = 5) }
    }

    @Test
    fun substring() {
        val str = ByteString(1, 2, 3, 4, 5)

        assertEquals(ByteString.EMPTY, str.substring(0, 0))
        assertEquals(ByteString(1, 2, 3), str.substring(startIndex = 0, endIndex = 3))
        assertEquals(ByteString(3, 4, 5), str.substring(startIndex = 2))
        assertEquals(ByteString(2, 3, 4), str.substring(startIndex = 1, endIndex = 4))
    }

    @Test
    fun substringWithInvalidArgs() {
        val str = ByteString(1, 2, 3)

        assertFailsWith<IllegalArgumentException> { str.substring(2, 1) }
        assertFailsWith<IndexOutOfBoundsException> { str.substring(-1) }
        assertFailsWith<IndexOutOfBoundsException> { str.substring(0, 10) }
        assertFailsWith<IndexOutOfBoundsException> { str.substring(-10, 10) }
    }

    @Test
    fun compareTo() {
        assertEquals(0, ByteString.EMPTY.compareTo(ByteString.EMPTY))
        assertEquals(0, ByteString(1, 2, 3).compareTo(ByteString(1, 2, 3)))
        assertEquals(-1, ByteString(1, 2).compareTo(ByteString(1, 2, 3)))
        assertEquals(-1, ByteString(0, 1, 2).compareTo(ByteString(0, 1, 3)))
        assertEquals(1, ByteString(1, 2, 3).compareTo(ByteString(1, 2)))
        assertEquals(1, ByteString(1, 2, 3).compareTo(ByteString(0, 1, 2)))
        assertEquals(1, ByteString(0xFF.toByte()).compareTo(ByteString(0)))
        assertEquals(-1, ByteString(1).compareTo(ByteString(0x81.toByte())))
    }

    @Test
    fun size() {
        assertEquals(0, ByteString.EMPTY.size)
        assertEquals(1, ByteString(0).size)
        assertEquals(12345, ByteString(ByteArray(12345)).size)
    }

    @Test
    fun indices() {
        assertEquals(0 until 10, ByteString(ByteArray(10)).indices())
        assertTrue(ByteString.EMPTY.indices().isEmpty())
    }

    @Test
    fun isEmpty() {
        assertTrue(ByteString.EMPTY.isEmpty())
        assertTrue(ByteString(byteArrayOf()).isEmpty())
        assertFalse(ByteString(byteArrayOf(0)).isEmpty())
    }

    @Test
    fun indexOfByte() {
        val str = ByteString(1, 2, 3, 4)
        for (idx in str.indices()) {
            assertEquals(idx, str.indexOf(str[idx]))
        }

        assertEquals(-1, str.indexOf(0))
        assertEquals(-1, str.indexOf(1, 1))
        assertEquals(-1, str.indexOf(4, 4))
        assertEquals(0, str.indexOf(1, -10))
        assertEquals(1, ByteString(0, 1, 1, 1).indexOf(1))

        assertEquals(-1, ByteString.EMPTY.indexOf(0))
        assertEquals(-1, ByteString.EMPTY.indexOf(0, 100500))
        assertEquals(-1, str.indexOf(1, 100500))
    }

    @Test
    fun indexOfByteArray() {
        val str = ByteString(1, 2, 3, 4, 5)

        assertEquals(0, str.indexOf(byteArrayOf(1, 2, 3, 4, 5)))
        assertEquals(0, str.indexOf(byteArrayOf(1, 2, 3)))
        assertEquals(0, str.indexOf(byteArrayOf(1)))

        assertEquals(2, str.indexOf(byteArrayOf(3, 4, 5)))
        assertEquals(-1, str.indexOf(byteArrayOf(3, 4, 5, 6)))
        assertEquals(0, str.indexOf(byteArrayOf()))
        assertEquals(-1, str.indexOf(byteArrayOf(-1)))

        assertEquals(-1, str.indexOf(byteArrayOf(1, 2, 3, 4, 5), 1))
        assertEquals(3, str.indexOf(byteArrayOf(4, 5), 3))

        assertEquals(0, str.indexOf(byteArrayOf(1, 2, 3), -1000))
        assertEquals(1, str.indexOf(byteArrayOf(2, 3), -1))

        assertEquals(1, ByteString(0, 1, 0, 1, 0, 1).indexOf(byteArrayOf(1, 0)))

        assertEquals(0, ByteString.EMPTY.indexOf(byteArrayOf()))
        assertEquals(0, ByteString.EMPTY.indexOf(byteArrayOf(), -100500))
        assertEquals(0, ByteString.EMPTY.indexOf(byteArrayOf(), 100500))
        assertEquals(-1, str.indexOf(byteArrayOf(1, 2, 3), 100500))
        assertEquals(-1, ByteString.EMPTY.indexOf(byteArrayOf(1, 2, 3, 4, 5)))
        assertEquals(-1, str.indexOf(byteArrayOf(2, 3, 5)))
    }

    @Test
    fun indexOfByteString() {
        val str = ByteString(1, 2, 3, 4, 5)

        assertEquals(0, str.indexOf(ByteString(1, 2, 3, 4, 5)))
        assertEquals(0, str.indexOf(ByteString(1, 2, 3)))
        assertEquals(0, str.indexOf(ByteString(1)))
        assertEquals(2, str.indexOf(ByteString(3, 4, 5)))
        assertEquals(-1, str.indexOf(ByteString(3, 4, 5, 6)))
        assertEquals(0, str.indexOf(ByteString.EMPTY))
        assertEquals(-1, str.indexOf(ByteString(-1)))
        assertEquals(-1, str.indexOf(ByteString(1, 2, 3, 4, 5), 1))
        assertEquals(3, str.indexOf(ByteString(4, 5), 3))
        assertEquals(0, str.indexOf(ByteString(1, 2, 3), -1000))
        assertEquals(1, str.indexOf(ByteString(2, 3), -1))
        assertEquals(1, ByteString(0, 1, 0, 1, 0, 1).indexOf(ByteString(1, 0)))
        assertEquals(0, ByteString.EMPTY.indexOf(ByteString.EMPTY))
        assertEquals(0, ByteString.EMPTY.indexOf(ByteString.EMPTY, -100500))
        assertEquals(0, ByteString.EMPTY.indexOf(ByteString.EMPTY, 100500))
        assertEquals(-1, str.indexOf(ByteString(1, 2, 3), 100500))
        assertEquals(-1, ByteString.EMPTY.indexOf(ByteString(1, 2, 3, 4, 5)))
        assertEquals(-1, str.indexOf(ByteString(2, 3, 5)))
    }

    @Test
    fun lastIndexOfByte() {
        val str = ByteString(1, 2, 3, 4)
        for (idx in str.indices()) {
            assertEquals(idx, str.lastIndexOf(str[idx]))
        }

        assertEquals(-1, str.lastIndexOf(0))
        assertEquals(-1, str.lastIndexOf(1, 1))
        assertEquals(-1, str.lastIndexOf(4, 4))
        assertEquals(0, str.lastIndexOf(1, -10))
        assertEquals(3, ByteString(0, 1, 1, 1, 0).lastIndexOf(1))

        assertEquals(-1, ByteString.EMPTY.lastIndexOf(0))
        assertEquals(-1, ByteString.EMPTY.lastIndexOf(0, 100500))
        assertEquals(-1, str.lastIndexOf(1, 1005000))
    }

    @Test
    fun lastIndexOfByteArray() {
        val str = ByteString(1, 2, 3, 4, 5)

        assertEquals(0, str.lastIndexOf(byteArrayOf(1, 2, 3, 4, 5)))
        assertEquals(0, str.lastIndexOf(byteArrayOf(1, 2, 3)))
        assertEquals(-1, str.lastIndexOf(byteArrayOf(0, 1, 2)))
        assertEquals(2, str.lastIndexOf(byteArrayOf(3, 4, 5)))
        assertEquals(-1, str.lastIndexOf(byteArrayOf(1, 2, 3), 1))
        assertEquals(1, str.lastIndexOf(byteArrayOf(2, 3, 4), 1))
        assertEquals(str.size, str.lastIndexOf(byteArrayOf()))
        assertEquals(str.size, str.lastIndexOf(byteArrayOf()))
        assertEquals(2, str.lastIndexOf(byteArrayOf(3, 4), -1000))
        assertEquals(0, str.lastIndexOf(byteArrayOf(1), -1))
        assertEquals(4, ByteString(1, 1, 1, 1, 1).lastIndexOf(byteArrayOf(1)))
        assertEquals(3, ByteString(0, 1, 0, 1, 0).lastIndexOf(byteArrayOf(1, 0)))
        assertEquals(0, ByteString.EMPTY.lastIndexOf(byteArrayOf()))
        assertEquals(0, ByteString.EMPTY.lastIndexOf(byteArrayOf(), -100500))
        assertEquals(0, ByteString.EMPTY.lastIndexOf(byteArrayOf(), 100500))
        assertEquals(-1, str.lastIndexOf(byteArrayOf(1, 2, 3), 100500))
        assertEquals(-1, ByteString.EMPTY.lastIndexOf(byteArrayOf(1, 2, 3)))
        assertEquals(-1, str.lastIndexOf(byteArrayOf(2, 3, 5)))
    }

    @Test
    fun lastIndexOfByteString() {
        val str = ByteString(1, 2, 3, 4, 5)

        assertEquals(0, str.lastIndexOf(ByteString(1, 2, 3, 4, 5)))
        assertEquals(0, str.lastIndexOf(ByteString(1, 2, 3)))
        assertEquals(-1, str.lastIndexOf(ByteString(0, 1, 2)))
        assertEquals(2, str.lastIndexOf(ByteString(3, 4, 5)))
        assertEquals(-1, str.lastIndexOf(ByteString(1, 2, 3), 1))
        assertEquals(1, str.lastIndexOf(ByteString(2, 3, 4), 1))
        assertEquals(str.size, str.lastIndexOf(ByteString.EMPTY))
        assertEquals(str.size, str.lastIndexOf(ByteString.EMPTY))
        assertEquals(2, str.lastIndexOf(ByteString(3, 4), -1000))
        assertEquals(0, str.lastIndexOf(ByteString(1), -1))
        assertEquals(4, ByteString(1, 1, 1, 1, 1).lastIndexOf(ByteString(1)))
        assertEquals(3, ByteString(0, 1, 0, 1, 0).lastIndexOf(ByteString(1, 0)))
        assertEquals(0, ByteString.EMPTY.lastIndexOf(ByteString.EMPTY))
        assertEquals(0, ByteString.EMPTY.lastIndexOf(ByteString.EMPTY, -100500))
        assertEquals(0, ByteString.EMPTY.lastIndexOf(ByteString.EMPTY, 100500))
        assertEquals(-1, str.lastIndexOf(ByteString(1, 2, 3), 100500))
        assertEquals(-1, ByteString.EMPTY.lastIndexOf(ByteString(1, 2, 3)))
        assertEquals(-1, str.lastIndexOf(ByteString(2, 3, 5)))
    }

    @Test
    fun startsWithByteArray() {
        val str = ByteString(1, 2, 3, 4, 5)

        assertTrue(str.startsWith(byteArrayOf(1, 2, 3, 4, 5)))
        assertTrue(str.startsWith(byteArrayOf(1, 2, 3)))

        assertTrue(str.startsWith(byteArrayOf()))

        assertFalse(str.startsWith(byteArrayOf(0, 1, 2, 3)))
        assertFalse(str.startsWith(byteArrayOf(2, 3, 4)))
        assertFalse(str.startsWith(byteArrayOf(1, 2, 3, 4, 5, 6)))

        assertTrue(ByteString.EMPTY.startsWith(byteArrayOf()))
    }

    @Test
    fun startWithByteString() {
        val str = ByteString(1, 2, 3, 4, 5)

        assertTrue(str.startsWith(ByteString(1, 2, 3, 4, 5)))
        assertTrue(str.startsWith(ByteString(1, 2, 3)))

        assertTrue(str.startsWith(ByteString.EMPTY))

        assertFalse(str.startsWith(ByteString(0, 1, 2, 3)))
        assertFalse(str.startsWith(ByteString(2, 3, 4)))
        assertFalse(str.startsWith(ByteString(1, 2, 3, 4, 5, 6)))

        assertTrue(ByteString.EMPTY.startsWith(ByteString.EMPTY))
    }

    @Test
    fun endsWithByteArray() {
        val str = ByteString(1, 2, 3, 4, 5)

        assertTrue(str.endsWith(byteArrayOf(1, 2, 3, 4, 5)))
        assertTrue(str.endsWith(byteArrayOf(3, 4, 5)))

        assertTrue(str.endsWith(byteArrayOf()))

        assertFalse(str.endsWith(byteArrayOf(3, 4, 5, 6)))
        assertFalse(str.endsWith(byteArrayOf(0, 1, 2, 3, 4, 5)))
        assertFalse(str.endsWith(byteArrayOf(2, 3, 4)))

        assertTrue(ByteString.EMPTY.endsWith(byteArrayOf()))
    }

    @Test
    fun endsWithByteString() {
        val str = ByteString(1, 2, 3, 4, 5)

        assertTrue(str.endsWith(ByteString(1, 2, 3, 4, 5)))
        assertTrue(str.endsWith(ByteString(3, 4, 5)))

        assertTrue(str.endsWith(ByteString.EMPTY))

        assertFalse(str.endsWith(ByteString(3, 4, 5, 6)))
        assertFalse(str.endsWith(ByteString(0, 1, 2, 3, 4, 5)))
        assertFalse(str.endsWith(ByteString(2, 3, 4)))

        assertTrue(ByteString.EMPTY.endsWith(ByteString.EMPTY))
    }

    @Test
    fun testToString() {
        assertEquals("ByteString(size=0)", ByteString.EMPTY.toString())
        assertEquals("ByteString(size=1 hex=00)", ByteString(0).toString())
        assertEquals(
            "ByteString(size=16 hex=000102030405060708090A0B0C0D0E0F)",
            ByteString(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15).toString()
        )
        assertEquals(
            "ByteString(size=64 hex=0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000000000000000000)",
            ByteString(ByteArray(64)).toString()
        )
    }

    private val bronzeHorseman = "На берегу пустынных волн"

    @Test
    fun utf8() {
        val byteString = ByteString.fromUtf8String(bronzeHorseman)
        assertEquals(byteString.toByteArray().toList(), bronzeHorseman.encodeToByteArray().toList())
        assertEquals(byteString, ByteString(*bronzeHorseman.encodeToByteArray()))
        assertEquals(byteString.toUtf8String(), bronzeHorseman)
    }
}