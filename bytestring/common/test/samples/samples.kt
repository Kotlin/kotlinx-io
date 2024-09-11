/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.bytestring.samples

import kotlinx.io.bytestring.*
import kotlin.test.*

class ByteStringSamples {
    @Test
    fun compareTo() {
        assertTrue(ByteString(1, 2, 3) == ByteString(1, 2, 3))
        assertTrue(ByteString(1, 2, 3) <= ByteString(1, 2, 3))
        assertTrue(ByteString(1, 2, 3) >= ByteString(1, 2, 3))
        assertTrue(ByteString(1, 2, 3) < ByteString(1, 3, 2))

        // If byte strings have different length, their content compared up to the length of the shortest string,
        // and if their content was the same, then the shortest string is considered "smaller"
        assertTrue(ByteString() < ByteString(1, 2, 3))
        assertTrue(ByteString(1, 2, 3) > ByteString(1))
        assertTrue(ByteString(1, 2, 3) < ByteString(1, 3))
        assertTrue(ByteString(1, 2, 3) > ByteString(1, 1, 1, 1))
    }

    @Test
    fun toStringSample() {
        assertEquals("ByteString(size=0)", ByteString().toString())
        assertEquals("ByteString(size=3 hex=000102)", ByteString(0, 1, 2).toString())
    }

    @Test
    fun substringSample() {
        val string = ByteString(1, 2, 3, 4, 5)
        assertEquals(ByteString(1, 2, 3), string.substring(startIndex = 0, endIndex = 3))
        assertEquals(ByteString(3, 4, 5), string.substring(startIndex = 2))
        assertEquals(ByteString(2, 3, 4), string.substring(startIndex = 1, endIndex = 4))
    }

    @Test
    fun toByteArraySample() {
        val string = ByteString(1, 2, 3, 4, 5)
        val array = string.toByteArray()

        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5), array)

        // Array is a copy of the byte string's content, so its modification won't affect the string.
        for (idx in array.indices) {
            array[idx] = (array[idx] * 2).toByte()
        }
        assertEquals(ByteString(1, 2, 3, 4, 5), string)
    }

    @Test
    fun toByteArrayWithIndicesSample() {
        val string = ByteString(1, 2, 3, 4, 5)

        assertContentEquals(byteArrayOf(2, 3, 4), string.toByteArray(startIndex = 1, endIndex = 4))
        assertContentEquals(byteArrayOf(4, 5), string.toByteArray(startIndex = 3))
    }

    @Test
    fun copyToSample() {
        val string = ByteString(1, 2, 3, 4, 5)
        val array = ByteArray(10)

        string.copyInto(array, destinationOffset = 3, startIndex = 1, endIndex = 4)
        assertContentEquals(byteArrayOf(0, 0, 0, 2, 3, 4, 0, 0, 0, 0), array)
    }

    @Test
    fun indexOfByteSample() {
        val string = ByteString(1, 2, 3, 2, 1)

        assertEquals(1, string.indexOf(2))
        assertEquals(3, string.indexOf(2, startIndex = 2))
        assertEquals(-1, string.indexOf(0))
    }

    @Test
    fun lastIndexOfByteSample() {
        val string = ByteString(1, 2, 3, 2, 1)

        assertEquals(3, string.lastIndexOf(2))
        assertEquals(-1, string.lastIndexOf(2, startIndex = 4))
        assertEquals(-1, string.indexOf(0))
    }

    @Test
    fun indexOfByteStringSample() {
        val string = ByteString(1, 2, 3, 4, 1, 3, 4)

        assertEquals(2, string.indexOf(ByteString(3, 4)))
        assertEquals(5, string.indexOf(ByteString(3, 4), startIndex = 3))
        assertEquals(-1, string.indexOf(ByteString(1, 1, 1)))
        assertEquals(-1, string.indexOf(ByteString(1, 3, 4, 5)))
        assertEquals(0, string.indexOf(ByteString(/* empty byte string */)))
    }

    @Test
    fun lastIndexOfByteStringSample() {
        val string = ByteString(1, 2, 3, 4, 1, 3, 4)

        assertEquals(5, string.lastIndexOf(ByteString(3, 4)))
        assertEquals(-1, string.lastIndexOf(ByteString(1, 2), startIndex = 3))
        assertEquals(0, string.lastIndexOf(ByteString(1, 2, 3)))
        assertEquals(-1, string.lastIndexOf(ByteString(1, 3, 4, 5)))
        assertEquals(string.size, string.lastIndexOf(ByteString(/* empty byte string */)))
    }

    @Test
    fun indexOfByteArraySample() {
        val string = ByteString(1, 2, 3, 4, 1, 3, 4)

        assertEquals(2, string.indexOf(byteArrayOf(3, 4)))
        assertEquals(5, string.indexOf(byteArrayOf(3, 4), startIndex = 3))
        assertEquals(-1, string.indexOf(byteArrayOf(1, 1, 1)))
        assertEquals(-1, string.indexOf(byteArrayOf(1, 3, 4, 5)))
        assertEquals(0, string.indexOf(byteArrayOf(/* empty byte array */)))
    }

    @Test
    fun lastIndexOfByteArraySample() {
        val string = ByteString(1, 2, 3, 4, 1, 3, 4)

        assertEquals(5, string.lastIndexOf(byteArrayOf(3, 4)))
        assertEquals(-1, string.lastIndexOf(byteArrayOf(1, 2), startIndex = 3))
        assertEquals(0, string.lastIndexOf(byteArrayOf(1, 2, 3)))
        assertEquals(-1, string.lastIndexOf(byteArrayOf(1, 3, 4, 5)))
        assertEquals(string.size, string.lastIndexOf(byteArrayOf(/* empty byte array */)))
    }

    @Test
    fun startsWithByteStringSample() {
        val string = ByteString(1, 2, 3, 4, 5)

        assertTrue(string.startsWith(string))
        assertTrue(string.startsWith(ByteString(/* empty byte string */)))
        assertTrue(string.startsWith(ByteString(1, 2, 3)))
        assertFalse(string.startsWith(ByteString(1, 3, 4)))
        assertFalse(string.startsWith(ByteString(1, 2, 3, 4, 5, 6)))
    }

    @Test
    fun endsWithByteStringSample() {
        val string = ByteString(1, 2, 3, 4, 5)

        assertTrue(string.endsWith(string))
        assertTrue(string.endsWith(ByteString(/* empty byte string */)))
        assertTrue(string.endsWith(ByteString(3, 4, 5)))
        assertFalse(string.endsWith(ByteString(2, 4, 5)))
        assertFalse(string.endsWith(ByteString(0, 1, 2, 3, 4, 5)))
    }

    @Test
    fun startsWithByteArraySample() {
        val string = ByteString(1, 2, 3, 4, 5)

        assertTrue(string.startsWith(byteArrayOf(1, 2, 3, 4, 5)))
        assertTrue(string.startsWith(byteArrayOf(/* empty byte array */)))
        assertTrue(string.startsWith(byteArrayOf(1, 2, 3)))
        assertFalse(string.startsWith(byteArrayOf(1, 3, 4)))
        assertFalse(string.startsWith(byteArrayOf(1, 2, 3, 4, 5, 6)))
    }

    @Test
    fun endsWithByteArraySample() {
        val string = ByteString(1, 2, 3, 4, 5)

        assertTrue(string.endsWith(byteArrayOf(1, 2, 3, 4, 5)))
        assertTrue(string.endsWith(byteArrayOf(/* empty byte array */)))
        assertTrue(string.endsWith(byteArrayOf(3, 4, 5)))
        assertFalse(string.endsWith(byteArrayOf(2, 4, 5)))
        assertFalse(string.endsWith(byteArrayOf(0, 1, 2, 3, 4, 5)))
    }

    @Test
    fun constructionSample() {
        val array = byteArrayOf(1, 2, 3)
        val byteStringFromArray = ByteString(array)
        array[1] = -1
        // The modification of the source array won't affect the content of the string.
        assertContentEquals(byteArrayOf(1, 2, 3), byteStringFromArray.toByteArray())

        val largeArray = byteArrayOf(1, 2, 3, 4 /*, ... */)
        val byteStringFromSubarray = ByteString(largeArray, startIndex = 1, endIndex = 3)
        assertContentEquals(byteArrayOf(2, 3), byteStringFromSubarray.toByteArray())
    }

    @Test
    fun constructionFromBytesSample() {
        val emptyByteString = ByteString()
        assertTrue(emptyByteString.isEmpty())
        assertEquals(0, emptyByteString.size)

        val byteStringFromBytes = ByteString(1, 2, 3)
        assertFalse(byteStringFromBytes.isEmpty())
        assertEquals(3, byteStringFromBytes.size)
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun constructionFromUBytesSample() {
        val byteStringFromBytes = ByteString(0xCAu, 0xFEu)
        assertFalse(byteStringFromBytes.isEmpty())
        assertEquals(2, byteStringFromBytes.size)
    }

    @Test
    fun encodeAndDecodeUtf8String() {
        val helloAsByteString = "hello".encodeToByteString()
        assertEquals(
            ByteString(
                'h'.code.toByte(),
                'e'.code.toByte(),
                'l'.code.toByte(),
                'l'.code.toByte(),
                'o'.code.toByte()
            ), helloAsByteString
        )
        assertEquals("hello", helloAsByteString.decodeToString())
    }

    @Test
    fun builderSample() {
        val byteString = buildByteString {
            append("hello".encodeToByteArray())
            append(' '.code.toByte())
            append("world".encodeToByteArray())
        }

        assertEquals("hello world".encodeToByteString(), byteString)
    }

    @Test
    fun builderSampleWithoutAdditionalAllocs() {
        val array = byteArrayOf(1, 2, 3, 4, 5, 6, 7)

        val byteString = buildByteString(4) {
            append(array, startIndex = 2, endIndex = 6)

            // When the capacity (4 in this case) matches the number of bytes appended,
            // then a ByteString will wrap builder's backing array without copying it.
            assertEquals(capacity, size)
        }

        assertEquals(ByteString(3, 4, 5, 6), byteString)
    }
}
