/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.bytestring

import kotlin.test.*

fun hexChar2Int(char: Char): Int = when (char) {
    in '0'..'9' -> char.code - '0'.code
    in 'a'..'f' -> char.code - 'a'.code + 10
    in 'A'..'F' -> char.code - 'A'.code + 10
    else -> throw IllegalArgumentException("Not a hex digit: $char")
}

fun String.decodeHex(): ByteString {
    val hex = this
    if (hex.isEmpty()) {
        return ByteString.EMPTY
    }
    val data = if (hex.length % 2 == 1) {
        ByteArray((hex.length + 1) / 2)
    } else {
        ByteArray(hex.length / 2)
    }

    var idx = 0
    var builderIdx = 0
    if (hex.length % 2 == 1) {
        data[builderIdx++] = hexChar2Int(hex[idx++]).toByte()
    }
    while (idx < hex.length) {
        var b = hexChar2Int(hex[idx++]) shl 4
        b = b or hexChar2Int(hex[idx++])
        data[builderIdx++] = b.toByte()
    }
    return ByteString(data)
}

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
        assertEquals("[size=0]", ByteString.EMPTY.toString())
        assertEquals("[size=1 hex=00]", ByteString(0).toString())
        assertEquals("[size=16 hex=000102030405060708090A0B0C0D0E0F]",
            ByteString(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15).toString())
        assertEquals("[size=64 hex=0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000]",
            ByteString(ByteArray(64)).toString())
        assertEquals("[size=65 hex=0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000…]",
            ByteString(ByteArray(65)).toString())
        assertEquals("[size=65 hex=0000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000000000000000000000]",
            ByteString(ByteArray(65)).toString(true))
    }

//    @Test
//    fun startsWithByteString() {
//        val byteString = "112233".decodeHex()
//        assertTrue(byteString.startsWith("".decodeHex()))
//        assertTrue(byteString.startsWith("11".decodeHex()))
//        assertTrue(byteString.startsWith("1122".decodeHex()))
//        assertTrue(byteString.startsWith("112233".decodeHex()))
//        assertFalse(byteString.startsWith("2233".decodeHex()))
//        assertFalse(byteString.startsWith("11223344".decodeHex()))
//        assertFalse(byteString.startsWith("112244".decodeHex()))
//    }
//
//    @Test
//    fun endsWithByteString() {
//        val byteString = "112233".decodeHex()
//        assertTrue(byteString.endsWith("".decodeHex()))
//        assertTrue(byteString.endsWith("33".decodeHex()))
//        assertTrue(byteString.endsWith("2233".decodeHex()))
//        assertTrue(byteString.endsWith("112233".decodeHex()))
//        assertFalse(byteString.endsWith("1122".decodeHex()))
//        assertFalse(byteString.endsWith("00112233".decodeHex()))
//        assertFalse(byteString.endsWith("002233".decodeHex()))
//    }
//
//    @Test
//    fun startsWithByteArray() {
//        val byteString = "112233".decodeHex()
//        assertTrue(byteString.startsWith("".decodeHex().toByteArray()))
//        assertTrue(byteString.startsWith("11".decodeHex().toByteArray()))
//        assertTrue(byteString.startsWith("1122".decodeHex().toByteArray()))
//        assertTrue(byteString.startsWith("112233".decodeHex().toByteArray()))
//        assertFalse(byteString.startsWith("2233".decodeHex().toByteArray()))
//        assertFalse(byteString.startsWith("11223344".decodeHex().toByteArray()))
//        assertFalse(byteString.startsWith("112244".decodeHex().toByteArray()))
//    }
//
//    @Test
//    fun endsWithByteArray() {
//        val byteString = "112233".decodeHex()
//        assertTrue(byteString.endsWith("".decodeHex().toByteArray()))
//        assertTrue(byteString.endsWith("33".decodeHex().toByteArray()))
//        assertTrue(byteString.endsWith("2233".decodeHex().toByteArray()))
//        assertTrue(byteString.endsWith("112233".decodeHex().toByteArray()))
//        assertFalse(byteString.endsWith("1122".decodeHex().toByteArray()))
//        assertFalse(byteString.endsWith("00112233".decodeHex().toByteArray()))
//        assertFalse(byteString.endsWith("002233".decodeHex().toByteArray()))
//    }
//
//    @Test
//    fun indexOfByteString() {
//        val byteString = "112233".decodeHex()
//        assertEquals(0, byteString.indexOf("112233".decodeHex()).toLong())
//        assertEquals(0, byteString.indexOf("1122".decodeHex()).toLong())
//        assertEquals(0, byteString.indexOf("11".decodeHex()).toLong())
//        assertEquals(0, byteString.indexOf("11".decodeHex(), 0).toLong())
//        assertEquals(0, byteString.indexOf("".decodeHex()).toLong())
//        assertEquals(0, byteString.indexOf("".decodeHex(), 0).toLong())
//        assertEquals(1, byteString.indexOf("2233".decodeHex()).toLong())
//        assertEquals(1, byteString.indexOf("22".decodeHex()).toLong())
//        assertEquals(1, byteString.indexOf("22".decodeHex(), 1).toLong())
//        assertEquals(1, byteString.indexOf("".decodeHex(), 1).toLong())
//        assertEquals(2, byteString.indexOf("33".decodeHex()).toLong())
//        assertEquals(2, byteString.indexOf("33".decodeHex(), 2).toLong())
//        assertEquals(2, byteString.indexOf("".decodeHex(), 2).toLong())
//        assertEquals(3, byteString.indexOf("".decodeHex(), 3).toLong())
//        assertEquals(-1, byteString.indexOf("112233".decodeHex(), 1).toLong())
//        assertEquals(-1, byteString.indexOf("44".decodeHex()).toLong())
//        assertEquals(-1, byteString.indexOf("11223344".decodeHex()).toLong())
//        assertEquals(-1, byteString.indexOf("112244".decodeHex()).toLong())
//        assertEquals(-1, byteString.indexOf("112233".decodeHex(), 1).toLong())
//        assertEquals(-1, byteString.indexOf("2233".decodeHex(), 2).toLong())
//        assertEquals(-1, byteString.indexOf("33".decodeHex(), 3).toLong())
//        assertEquals(3, byteString.indexOf("".decodeHex(), 4).toLong())
//    }
//
//    @Test
//    fun indexOfWithOffset() {
//        val byteString = "112233112233".decodeHex()
//        assertEquals(0, byteString.indexOf("112233".decodeHex(), -1).toLong())
//        assertEquals(0, byteString.indexOf("112233".decodeHex(), 0).toLong())
//        assertEquals(0, byteString.indexOf("112233".decodeHex()).toLong())
//        assertEquals(3, byteString.indexOf("112233".decodeHex(), 1).toLong())
//        assertEquals(3, byteString.indexOf("112233".decodeHex(), 2).toLong())
//        assertEquals(3, byteString.indexOf("112233".decodeHex(), 3).toLong())
//        assertEquals(-1, byteString.indexOf("112233".decodeHex(), 4).toLong())
//    }
//
//    @Test
//    fun indexOfByteArray() {
//        val byteString = "112233".decodeHex()
//        assertEquals(0, byteString.indexOf("112233".decodeHex().toByteArray()))
//        assertEquals(1, byteString.indexOf("2233".decodeHex().toByteArray()))
//        assertEquals(2, byteString.indexOf("33".decodeHex().toByteArray()))
//        assertEquals(-1, byteString.indexOf("112244".decodeHex().toByteArray()))
//    }
//
//    @Test
//    fun lastIndexOfByteString() {
//        val byteString = "112233".decodeHex()
//        assertEquals(0, byteString.lastIndexOf("112233".decodeHex()).toLong())
//        assertEquals(0, byteString.lastIndexOf("1122".decodeHex()).toLong())
//        assertEquals(0, byteString.lastIndexOf("11".decodeHex()).toLong())
//        assertEquals(0, byteString.lastIndexOf("11".decodeHex(), 3).toLong())
//        assertEquals(0, byteString.lastIndexOf("11".decodeHex(), 0).toLong())
//        assertEquals(0, byteString.lastIndexOf("".decodeHex(), 0).toLong())
//        assertEquals(1, byteString.lastIndexOf("2233".decodeHex()).toLong())
//        assertEquals(1, byteString.lastIndexOf("22".decodeHex()).toLong())
//        assertEquals(1, byteString.lastIndexOf("22".decodeHex(), 3).toLong())
//        assertEquals(1, byteString.lastIndexOf("22".decodeHex(), 1).toLong())
//        assertEquals(1, byteString.lastIndexOf("".decodeHex(), 1).toLong())
//        assertEquals(2, byteString.lastIndexOf("33".decodeHex()).toLong())
//        assertEquals(2, byteString.lastIndexOf("33".decodeHex(), 3).toLong())
//        assertEquals(2, byteString.lastIndexOf("33".decodeHex(), 2).toLong())
//        assertEquals(2, byteString.lastIndexOf("".decodeHex(), 2).toLong())
//        assertEquals(3, byteString.lastIndexOf("".decodeHex(), 3).toLong())
//        assertEquals(3, byteString.lastIndexOf("".decodeHex()).toLong())
//        assertEquals(-1, byteString.lastIndexOf("112233".decodeHex(), -1).toLong())
//        assertEquals(-1, byteString.lastIndexOf("112233".decodeHex(), -2).toLong())
//        assertEquals(-1, byteString.lastIndexOf("44".decodeHex()).toLong())
//        assertEquals(-1, byteString.lastIndexOf("11223344".decodeHex()).toLong())
//        assertEquals(-1, byteString.lastIndexOf("112244".decodeHex()).toLong())
//        assertEquals(-1, byteString.lastIndexOf("2233".decodeHex(), 0).toLong())
//        assertEquals(-1, byteString.lastIndexOf("33".decodeHex(), 1).toLong())
//        assertEquals(-1, byteString.lastIndexOf("".decodeHex(), -1).toLong())
//    }
//
//    @Test
//    fun lastIndexOfByteArray() {
//        val byteString = "112233".decodeHex()
//        assertEquals(0, byteString.lastIndexOf("112233".decodeHex().toByteArray()))
//        assertEquals(1, byteString.lastIndexOf("2233".decodeHex().toByteArray()))
//        assertEquals(2, byteString.lastIndexOf("33".decodeHex().toByteArray()))
//        assertEquals(3, byteString.lastIndexOf("".decodeHex().toByteArray()))
//    }
//
//    @Test
//    fun equalsTest() {
//        val byteString = "000102".decodeHex()
//        assertEquals(byteString, byteString)
//        assertEquals(byteString, "000102".decodeHex())
//        assertNotEquals(byteString, Any())
//        assertNotEquals(byteString, "000201".decodeHex())
//    }
//
//    @Ignore
//    @Test
//    fun equalsEmptyTest() {
//        // assertEquals("".decodeHex(), ByteString.EMPTY)
//        // assertEquals("".decodeHex(), ByteString.of())
//        // assertEquals(ByteString.EMPTY, factory.decodeHex(""))
//        // assertEquals(ByteString.of(), factory.decodeHex(""))
//    }
//
//    private val bronzeHorseman = "На берегу пустынных волн"
//
//    /*
//    @Test fun utf8() {
//        val byteString = factory.encodeUtf8(bronzeHorseman)
//        assertEquals(byteString.toByteArray().toList(), bronzeHorseman.commonAsUtf8ToByteArray().toList())
//        assertTrue(byteString == ByteString.of(*bronzeHorseman.commonAsUtf8ToByteArray()))
//        assertEquals(
//            byteString,
//            (
//                    "d09dd0b020d0b1d0b5d180d0b5d0b3d18320d0bfd183d181" +
//                            "d182d18bd0bdd0bdd18bd18520d0b2d0bed0bbd0bd"
//                    ).decodeHex(),
//        )
//        assertEquals(byteString.utf8(), bronzeHorseman)
//    }
//
//     */
//
//    @Test
//    fun testHashCode() {
//        val byteString = "0102".decodeHex()
//        assertEquals(byteString.hashCode().toLong(), byteString.hashCode().toLong())
//        assertEquals(byteString.hashCode().toLong(), "0102".decodeHex().hashCode().toLong())
//    }
//
//    /*
//    @Test fun toAsciiLowerCaseNoUppercase() {
//        val s = "a1_+".encodeUtf8()
//        assertEquals(s, s.toAsciiLowercase())
//        if (factory === ByteStringFactory.BYTE_STRING) {
//            assertSame(s, s.toAsciiLowercase())
//        }
//    }
//
//    @Test fun toAsciiAllUppercase() {
//        assertEquals("ab".encodeUtf8(), factory.encodeUtf8("AB").toAsciiLowercase())
//    }
//
//    @Test fun toAsciiStartsLowercaseEndsUppercase() {
//        assertEquals("abcd".encodeUtf8(), factory.encodeUtf8("abCD").toAsciiLowercase())
//    }
//
//    @Test fun toAsciiStartsUppercaseEndsLowercase() {
//        assertEquals("ABCD".encodeUtf8(), factory.encodeUtf8("ABcd").toAsciiUppercase())
//    }
//     */
//
//    @Test
//    fun substring() {
//        val byteString = ByteString("Hello, World!".encodeToByteArray())
//
//        assertEquals(byteString.substring(0), byteString)
//        assertEquals(byteString.substring(0, 5), ByteString("Hello".encodeToByteArray()))
//        assertEquals(byteString.substring(7), ByteString("World!".encodeToByteArray()))
//        assertEquals(byteString.substring(6, 6), ByteString("".encodeToByteArray()))
//    }
//
//    @Test
//    fun substringWithInvalidBounds() {
//        val byteString = ByteString("Hello, World!".encodeToByteArray())
//
//        assertFailsWith<IndexOutOfBoundsException> {
//            byteString.substring(-1)
//        }
//
//        assertFailsWith<IndexOutOfBoundsException> {
//            byteString.substring(0, 14)
//        }
//
//        assertFailsWith<IllegalArgumentException> {
//            byteString.substring(8, 7)
//        }
//    }
//
//    /*
//    @Test fun encodeBase64() {
//        assertEquals("", factory.encodeUtf8("").base64())
//        assertEquals("AA==", factory.encodeUtf8("\u0000").base64())
//        assertEquals("AAA=", factory.encodeUtf8("\u0000\u0000").base64())
//        assertEquals("AAAA", factory.encodeUtf8("\u0000\u0000\u0000").base64())
//        assertEquals(
//            "SG93IG1hbnkgbGluZXMgb2YgY29kZSBhcmUgdGhlcmU/ICdib3V0IDIgbWlsbGlvbi4=",
//            factory.encodeUtf8("How many lines of code are there? 'bout 2 million.").base64(),
//        )
//    }
//
//    @Test fun encodeBase64Url() {
//        assertEquals("", factory.encodeUtf8("").base64Url())
//        assertEquals("AA==", factory.encodeUtf8("\u0000").base64Url())
//        assertEquals("AAA=", factory.encodeUtf8("\u0000\u0000").base64Url())
//        assertEquals("AAAA", factory.encodeUtf8("\u0000\u0000\u0000").base64Url())
//        assertEquals(
//            "SG93IG1hbnkgbGluZXMgb2YgY29kZSBhcmUgdGhlcmU_ICdib3V0IDIgbWlsbGlvbi4=",
//            factory.encodeUtf8("How many lines of code are there? 'bout 2 million.").base64Url(),
//        )
//    }
//
//    @Test fun ignoreUnnecessaryPadding() {
//        assertEquals("", "====".decodeBase64()!!.utf8())
//        assertEquals("\u0000\u0000\u0000", "AAAA====".decodeBase64()!!.utf8())
//    }
//
//    @Test fun decodeBase64() {
//        assertEquals("", "".decodeBase64()!!.utf8())
//        assertEquals(null, "/===".decodeBase64()) // Can't do anything with 6 bits!
//        assertEquals("ff".decodeHex(), "//==".decodeBase64())
//        assertEquals("ff".decodeHex(), "__==".decodeBase64())
//        assertEquals("ffff".decodeHex(), "///=".decodeBase64())
//        assertEquals("ffff".decodeHex(), "___=".decodeBase64())
//        assertEquals("ffffff".decodeHex(), "////".decodeBase64())
//        assertEquals("ffffff".decodeHex(), "____".decodeBase64())
//        assertEquals("ffffffffffff".decodeHex(), "////////".decodeBase64())
//        assertEquals("ffffffffffff".decodeHex(), "________".decodeBase64())
//        assertEquals(
//            "What's to be scared about? It's just a little hiccup in the power...",
//            (
//                    "V2hhdCdzIHRvIGJlIHNjYXJlZCBhYm91dD8gSXQncyBqdXN0IGEgbGl0dGxlIGhpY2" +
//                            "N1cCBpbiB0aGUgcG93ZXIuLi4="
//                    ).decodeBase64()!!.utf8(),
//        )
//        // Uses two encoding styles. Malformed, but supported as a side-effect.
//        assertEquals("ffffff".decodeHex(), "__//".decodeBase64())
//    }
//
//    @Test fun decodeBase64WithWhitespace() {
//        assertEquals("\u0000\u0000\u0000", " AA AA ".decodeBase64()!!.utf8())
//        assertEquals("\u0000\u0000\u0000", " AA A\r\nA ".decodeBase64()!!.utf8())
//        assertEquals("\u0000\u0000\u0000", "AA AA".decodeBase64()!!.utf8())
//        assertEquals("\u0000\u0000\u0000", " AA AA ".decodeBase64()!!.utf8())
//        assertEquals("\u0000\u0000\u0000", " AA A\r\nA ".decodeBase64()!!.utf8())
//        assertEquals("\u0000\u0000\u0000", "A    AAA".decodeBase64()!!.utf8())
//        assertEquals("", "    ".decodeBase64()!!.utf8())
//    }
//
//     */
//
//    @Test
//    fun encodeHex() {
//        assertEquals("000102", ByteString(0x0, 0x1, 0x2).hex())
//    }
//
//    @Test
//    fun decodeHex() {
//        val actual = "CAFEBABE".decodeHex()
//        val expected = ByteString(-54, -2, -70, -66)
//        assertEquals(expected, actual)
//    }
//
//    @Test
//    fun decodeHexOddNumberOfChars() {
//        assertFailsWith<IllegalArgumentException> {
//            "aaa".decodeHex()
//        }
//    }
//
//    @Test
//    fun decodeHexInvalidChar() {
//        assertFailsWith<IllegalArgumentException> {
//            "a\u0000".decodeHex()
//        }
//    }
//
//    @Test
//    fun toStringOnEmpty() {
//        assertEquals("[size=0]", "".decodeHex().toString())
//    }
//
//    @Test
//    fun toStringOnShortText() {
//        assertEquals(
//            "[text=Tyrannosaur]",
//            ByteString("Tyrannosaur".encodeToByteArray()).toString(),
//        )
//        //assertEquals(
//        //"[text=təˈranəˌsôr]",
//        //factory.decodeHex("74c999cb8872616ec999cb8c73c3b472").toString(),
//        //)
//        TODO()
//    }
//
//    @Test
//    fun toStringOnLongTextIsTruncated() {
//        /*
//        val raw = (
//                "Um, I'll tell you the problem with the scientific power that you're using here, " +
//                        "it didn't require any discipline to attain it. You read what others had done and you " +
//                        "took the next step. You didn't earn the knowledge for yourselves, so you don't take any " +
//                        "responsibility for it. You stood on the shoulders of geniuses to accomplish something " +
//                        "as fast as you could, and before you even knew what you had, you patented it, and " +
//                        "packaged it, and slapped it on a plastic lunchbox, and now you're selling it, you wanna " +
//                        "sell it."
//                )
//
//         */
//        //assertEquals(
//        //    "[size=517 text=Um, I'll tell you the problem with the scientific power that " +
//        //            "you…]",
//        //    factory.encodeUtf8(raw).toString(),
//        //)
//        /*
//        val war = (
//                "Սｍ, I'll 𝓽𝖾ll ᶌօ𝘂 ᴛℎ℮ 𝜚𝕣०ｂl𝖾ｍ ｗі𝕥𝒽 𝘵𝘩𝐞 𝓼𝙘𝐢𝔢𝓷𝗍𝜄𝚏𝑖ｃ 𝛠𝝾ｗ𝚎𝑟 𝕥ｈ⍺𝞃 𝛄𝓸𝘂'𝒓𝗲 υ𝖘𝓲𝗇ɡ 𝕙𝚎𝑟ｅ, " +
//                        "𝛊𝓽 ⅆ𝕚𝐝𝝿'𝗍 𝔯𝙚𝙦ᴜ𝜾𝒓𝘦 𝔞𝘯𝐲 ԁ𝜄𝑠𝚌ι𝘱lι𝒏ｅ 𝑡𝜎 𝕒𝚝𝖙𝓪і𝞹 𝔦𝚝. 𝒀ο𝗎 𝔯𝑒⍺𝖉 ｗ𝐡𝝰𝔱 𝞂𝞽һ𝓮𝓇ƽ հ𝖺𝖉 ⅾ𝛐𝝅ⅇ 𝝰πԁ 𝔂ᴑᴜ 𝓉ﮨ၀𝚔 " +
//                        "т𝒽𝑒 𝗇𝕖ⅹ𝚝 𝔰𝒕е𝓅. 𝘠ⲟ𝖚 𝖉ⅰԁ𝝕'τ 𝙚𝚊ｒ𝞹 𝘵Ꮒ𝖾 𝝒𝐧هｗl𝑒𝖉ƍ𝙚 𝓯૦ｒ 𝔂𝞼𝒖𝕣𝑠𝕖l𝙫𝖊𝓼, 𐑈о ｙ𝘰𝒖 ⅆە𝗇'ｔ 𝜏α𝒌𝕖 𝛂𝟉ℽ " +
//                        "𝐫ⅇ𝗌ⲣ๐ϖ𝖘ꙇᖯ𝓲l𝓲𝒕𝘆 𝐟𝞼𝘳 𝚤𝑡. 𝛶𝛔𝔲 ｓ𝕥σσ𝐝 ﮩ𝕟 𝒕𝗁𝔢 𝘴𝐡𝜎ᴜlⅾ𝓮𝔯𝚜 𝛐𝙛 ᶃ𝚎ᴨᎥս𝚜𝘦𝓈 𝓽𝞸 ａ𝒄𝚌𝞸ｍρl𝛊ꜱ𝐡 𝓈𝚘ｍ𝚎𝞃𝔥⍳𝞹𝔤 𝐚𝗌 𝖋ａ𝐬𝒕 " +
//                        "αｓ γ𝛐𝕦 𝔠ﻫ𝛖lԁ, 𝚊π𝑑 Ь𝑒𝙛૦𝓇𝘦 𝓎٥𝖚 ⅇｖℯ𝝅 𝜅ո𝒆ｗ ｗ𝗵𝒂𝘁 ᶌ੦𝗎 ｈ𝐚𝗱, 𝜸ﮨ𝒖 𝓹𝝰𝔱𝖾𝗇𝓽𝔢ⅆ і𝕥, 𝚊𝜛𝓭 𝓹𝖺ⅽϰ𝘢ℊеᏧ 𝑖𝞃, " +
//                        "𝐚𝛑ꓒ 𝙨l𝔞р𝘱𝔢𝓭 ɩ𝗍 ہ𝛑 𝕒 ｐl𝛂ѕᴛ𝗂𝐜 l𝞄ℼ𝔠𝒽𝑏ﮪ⨯, 𝔞ϖ𝒹 ｎ𝛔ｗ 𝛾𝐨𝞄'𝗿𝔢 ꜱ℮ll𝙞ｎɡ ɩ𝘁, 𝙮𝕠𝛖 ｗ𝑎ℼ𝚗𝛂 𝕤𝓮ll 𝙞𝓉."
//                )
//
//         */
//        //assertEquals(
//        //    "[size=1496 text=Սｍ, I'll 𝓽𝖾ll ᶌօ𝘂 ᴛℎ℮ 𝜚𝕣०ｂl𝖾ｍ ｗі𝕥𝒽 𝘵𝘩𝐞 𝓼𝙘𝐢𝔢𝓷𝗍𝜄𝚏𝑖ｃ 𝛠𝝾ｗ𝚎𝑟 𝕥ｈ⍺𝞃 " +
//        //            "𝛄𝓸𝘂…]",
//        //    factory.encodeUtf8(war).toString(),
//        //)
//        TODO()
//    }
//
//    @Test
//    fun toStringOnTextWithNewlines() {
//        // Instead of emitting a literal newline in the toString(), these are escaped as "\n".
//        //assertEquals(
//        //    "[text=a\\r\\nb\\nc\\rd\\\\e]",
//        //    factory.encodeUtf8("a\r\nb\nc\rd\\e").toString(),
//        // )
//        TODO()
//    }
//
//    @Test
//    fun toStringOnData() {
//        /*
//        val byteString = factory.decodeHex(
//            "" +
//                    "60b420bb3851d9d47acb933dbe70399bf6c92da33af01d4fb770e98c0325f41d3ebaf8986da712c82bcd4d55" +
//                    "4bf0b54023c29b624de9ef9c2f931efc580f9afb",
//        )
//        assertEquals(
//            "[hex=" +
//                    "60b420bb3851d9d47acb933dbe70399bf6c92da33af01d4fb770e98c0325f41d3ebaf8986da712c82bcd4d55" +
//                    "4bf0b54023c29b624de9ef9c2f931efc580f9afb]",
//            byteString.toString(),
//        )
//         */
//        TODO()
//    }
//
//    @Test
//    fun toStringOnLongDataIsTruncated() {
//        /*
//        val byteString = factory.decodeHex(
//            "" +
//                    "60b420bb3851d9d47acb933dbe70399bf6c92da33af01d4fb770e98c0325f41d3ebaf8986da712c82bcd4d55" +
//                    "4bf0b54023c29b624de9ef9c2f931efc580f9afba1",
//        )
//        assertEquals(
//            "[size=65 hex=" +
//                    "60b420bb3851d9d47acb933dbe70399bf6c92da33af01d4fb770e98c0325f41d3ebaf8986da712c82bcd4d55" +
//                    "4bf0b54023c29b624de9ef9c2f931efc580f9afb…]",
//            byteString.toString(),
//        )
//         */
//        TODO()
//    }
//
//    @Test
//    fun compareToSingleBytes() {
//        val originalByteStrings = listOf(
//            "00".decodeHex(),
//            "01".decodeHex(),
//            "7e".decodeHex(),
//            "7f".decodeHex(),
//            "80".decodeHex(),
//            "81".decodeHex(),
//            "fe".decodeHex(),
//            "ff".decodeHex(),
//        )
//
//        val sortedByteStrings = originalByteStrings.toMutableList()
//        sortedByteStrings.shuffle(Random(0))
//        assertNotEquals(originalByteStrings, sortedByteStrings)
//
//        sortedByteStrings.sort()
//        assertEquals(originalByteStrings, sortedByteStrings)
//    }
//
//    @Test
//    fun compareToMultipleBytes() {
//        val originalByteStrings = listOf(
//            "".decodeHex(),
//            "00".decodeHex(),
//            "0000".decodeHex(),
//            "000000".decodeHex(),
//            "00000000".decodeHex(),
//            "0000000000".decodeHex(),
//            "0000000001".decodeHex(),
//            "000001".decodeHex(),
//            "00007f".decodeHex(),
//            "0000ff".decodeHex(),
//            "000100".decodeHex(),
//            "000101".decodeHex(),
//            "007f00".decodeHex(),
//            "00ff00".decodeHex(),
//            "010000".decodeHex(),
//            "010001".decodeHex(),
//            "01007f".decodeHex(),
//            "0100ff".decodeHex(),
//            "010100".decodeHex(),
//            "01010000".decodeHex(),
//            "0101000000".decodeHex(),
//            "0101000001".decodeHex(),
//            "010101".decodeHex(),
//            "7f0000".decodeHex(),
//            "7f0000ffff".decodeHex(),
//            "ffffff".decodeHex(),
//        )
//
//        val sortedByteStrings = originalByteStrings.toMutableList()
//        sortedByteStrings.shuffle(Random(0))
//        assertNotEquals(originalByteStrings, sortedByteStrings)
//
//        sortedByteStrings.sort()
//        assertEquals(originalByteStrings, sortedByteStrings)
//    }
//
//    /*
//    @Test fun testHash() = with(factory.encodeUtf8("Kevin")) {
//        assertEquals("e043899daa0c7add37bc99792b2c045d6abbc6dc", sha1().hex())
//        assertEquals("f1cd318e412b5f7226e5f377a9544ff7", md5().hex())
//        assertEquals("0e4dd66217fc8d2e298b78c8cd9392870dcd065d0ff675d0edff5bcd227837e9", sha256().hex())
//        assertEquals("483676b93c4417198b465083d196ec6a9fab8d004515874b8ff47e041f5f56303cc08179625030b8b5b721c09149a18f0f59e64e7ae099518cea78d3d83167e1", sha512().hex())
//    }
//*/
//
//    @Test
//    fun copyInto() {
//        val byteString = ByteString("abcdefgh".encodeToByteArray())
//        val byteArray = "WwwwXxxxYyyyZzzz".encodeToByteArray()
//        byteString.copyInto(byteArray, 0, 0, 5)
//        assertEquals("abcdexxxYyyyZzzz", byteArray.decodeToString())
//    }
//
//    @Test
//    fun copyIntoFullRange() {
//        val byteString = ByteString("abcdefghijklmnop".encodeToByteArray())
//        val byteArray = "WwwwXxxxYyyyZzzz".encodeToByteArray()
//        byteString.copyInto(byteArray, 0, 0, 16)
//        assertEquals("abcdefghijklmnop", byteArray.decodeToString())
//    }
//
//    @Test
//    fun copyIntoWithTargetOffset() {
//        val byteString = ByteString("abcdefgh".encodeToByteArray())
//        val byteArray = "WwwwXxxxYyyyZzzz".encodeToByteArray()
//        byteString.copyInto(byteArray, 11, 0, 5)
//        assertEquals("WwwwXxxxYyyabcde", byteArray.decodeToString())
//    }
//
//    @Test
//    fun copyIntoWithSourceOffset() {
//        val byteString = ByteString("abcdefgh".encodeToByteArray())
//        val byteArray = "WwwwXxxxYyyyZzzz".encodeToByteArray()
//        byteString.copyInto(byteArray, 0, 3, 5)
//        assertEquals("defghxxxYyyyZzzz", byteArray.decodeToString())
//    }
//
//    /*
//    @Test fun copyIntoWithAllParameters() {
//        val byteString = factory.encodeUtf8("abcdefgh")
//        val byteArray = "WwwwXxxxYyyyZzzz".encodeToByteArray()
//        byteString.copyInto(offset = 3, target = byteArray, targetOffset = 11, byteCount = 5)
//        assertEquals("WwwwXxxxYyydefgh", byteArray.decodeToString())
//    }
//
//     */
//
//    /*
//    TODO
//    @Test fun copyIntoBoundsChecks() {
//        val byteString = ByteString("abcdefgh".encodeToByteArray())
//        val byteArray = "WwwwXxxxYyyyZzzz".encodeToByteArray()
//        assertFailsWith<IndexOutOfBoundsException> {
//            byteString.copyInto(offset = -1, target = byteArray, targetOffset = 1, byteCount = 1)
//        }
//        assertFailsWith<IndexOutOfBoundsException> {
//            byteString.copyInto(offset = 9, target = byteArray, targetOffset = 0, byteCount = 0)
//        }
//        assertFailsWith<IndexOutOfBoundsException> {
//            byteString.copyInto(offset = 1, target = byteArray, targetOffset = -1, byteCount = 1)
//        }
//        assertFailsWith<IndexOutOfBoundsException> {
//            byteString.copyInto(offset = 1, target = byteArray, targetOffset = 17, byteCount = 1)
//        }
//        assertFailsWith<IndexOutOfBoundsException> {
//            byteString.copyInto(offset = 7, target = byteArray, targetOffset = 1, byteCount = 2)
//        }
//        assertFailsWith<IndexOutOfBoundsException> {
//            byteString.copyInto(offset = 1, target = byteArray, targetOffset = 15, byteCount = 2)
//        }
//    }
//
//    @Test fun copyEmptyAtBounds() {
//        val byteString = ByteString("abcdefgh".encodeToByteArray())
//        val byteArray = "WwwwXxxxYyyyZzzz".encodeToByteArray()
//        byteString.copyInto(offset = 0, target = byteArray, targetOffset = 0, byteCount = 0)
//        assertEquals("WwwwXxxxYyyyZzzz", byteArray.decodeToString())
//        byteString.copyInto(offset = 0, target = byteArray, targetOffset = 16, byteCount = 0)
//        assertEquals("WwwwXxxxYyyyZzzz", byteArray.decodeToString())
//        byteString.copyInto(offset = 8, target = byteArray, targetOffset = 0, byteCount = 0)
//        assertEquals("WwwwXxxxYyyyZzzz", byteArray.decodeToString())
//    }
//     */
}