/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalEncodingApi::class)

package kotlinx.io.bytestring

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.*

class ByteStringBase64Test {
    private fun bytes(vararg values: Int): ByteArray {
        return ByteArray(values.size) { values[it].toByte() }
    }


    private val byteArray: ByteArray = bytes(0b0000_0100, 0b0010_0000, 0b1100_0100, 0b0001_0100, 0b0110_0001, 0b1100_1000)
    private val byteString = ByteString.wrap(byteArray)

    private val encodedSymbols = "BCDEFGHI"
    private val encodedBytes = encodedSymbols.encodeToByteArray()
    private val encodedByteString = ByteString.wrap(encodedBytes)


    @Test
    fun testEncodeToByteArray() {
        assertFailsWith<IndexOutOfBoundsException> { Base64.encodeToByteArray(byteString, startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { Base64.encodeToByteArray(byteString, endIndex = byteString.size + 1) }
        assertFailsWith<IllegalArgumentException> { Base64.encodeToByteArray(byteString, startIndex = byteString.size + 1) }
        assertFailsWith<IllegalArgumentException> { Base64.encodeToByteArray(byteString, startIndex = 3, endIndex = 0) }

        assertTrue(Base64.encodeToByteArray(ByteString.EMPTY).isEmpty())
        assertContentEquals(encodedBytes, Base64.encodeToByteArray(byteString))

        assertContentEquals(encodedSymbols.encodeToByteArray(0, 4), Base64.encodeToByteArray(byteString, endIndex = 3))
        assertContentEquals(encodedSymbols.encodeToByteArray(4), Base64.encodeToByteArray(byteString, startIndex = 3))
    }

    @Test
    fun testEncodeIntoByteArray() {
        val destination = ByteArray(encodedBytes.size)

        assertFailsWith<IndexOutOfBoundsException> { Base64.encodeIntoByteArray(byteString, destination, destinationOffset = -1) }
        assertFailsWith<IndexOutOfBoundsException> { Base64.encodeIntoByteArray(byteString, destination, destinationOffset = destination.size + 1) }
        assertFailsWith<IndexOutOfBoundsException> { Base64.encodeIntoByteArray(byteString, destination, destinationOffset = 1) }

        assertEquals(0, Base64.encodeIntoByteArray(ByteString.EMPTY, destination))
        assertEquals(encodedBytes.size, Base64.encodeIntoByteArray(byteString, destination))
        assertContentEquals(encodedBytes, destination.copyOf(encodedBytes.size))

        var length = Base64.encodeIntoByteArray(byteString, destination, endIndex = 3)
        assertContentEquals(encodedSymbols.encodeToByteArray(0, 4), destination.copyOf(length))
        length += Base64.encodeIntoByteArray(byteString, destination, destinationOffset = length, startIndex = 3)
        assertContentEquals(encodedSymbols.encodeToByteArray(), destination)
    }

    @Test
    fun testEncode() {
        assertFailsWith<IndexOutOfBoundsException> { Base64.encode(byteString, startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { Base64.encode(byteString, endIndex = byteString.size + 1) }
        assertFailsWith<IllegalArgumentException> { Base64.encode(byteString, startIndex = byteString.size + 1) }
        assertFailsWith<IllegalArgumentException> { Base64.encode(byteString, startIndex = 3, endIndex = 0) }

        assertTrue(Base64.encode(ByteArray(0)).isEmpty())
        assertEquals(encodedSymbols, Base64.encode(byteString))
        assertEquals(encodedSymbols.substring(0, 4), Base64.encode(byteString, endIndex = 3))
        assertEquals(encodedSymbols.substring(4), Base64.encode(byteString, startIndex = 3))

        val destination = StringBuilder()
        Base64.encodeToAppendable(byteString, destination, endIndex = 3)
        assertEquals(encodedSymbols.substring(0, 4), destination.toString())
        Base64.encodeToAppendable(byteString, destination, startIndex = 3)
        assertEquals(encodedSymbols, destination.toString())
    }

    @Test
    fun testDecode() {
        assertFailsWith<IndexOutOfBoundsException> { Base64.decode(encodedByteString, startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { Base64.decode(encodedByteString, endIndex = encodedByteString.size + 1) }
        assertFailsWith<IllegalArgumentException> { Base64.decode(encodedByteString, startIndex = encodedByteString.size + 1) }
        assertFailsWith<IllegalArgumentException> { Base64.decode(encodedByteString, startIndex = 4, endIndex = 0) }

        assertEquals(0, Base64.decode(ByteString.EMPTY).size)
        assertContentEquals(byteArray, Base64.decode(encodedByteString))
        assertContentEquals(byteArray.copyOfRange(0, 3), Base64.decode(encodedByteString, endIndex = 4))
        assertContentEquals(byteArray.copyOfRange(3, byteString.size), Base64.decode(encodedByteString, startIndex = 4))
    }

    @Test
    fun testDecodeIntoByteArray() {
        val destination = ByteArray(6)
        assertFailsWith<IndexOutOfBoundsException> { Base64.decodeIntoByteArray(encodedByteString, destination, destinationOffset = -1) }
        assertFailsWith<IndexOutOfBoundsException> { Base64.decodeIntoByteArray(encodedByteString, destination, destinationOffset = destination.size + 1) }
        assertFailsWith<IndexOutOfBoundsException> { Base64.decodeIntoByteArray(encodedByteString, destination, destinationOffset = 1) }

        assertTrue(destination.all { it == 0.toByte() })

        assertEquals(0, Base64.decodeIntoByteArray(ByteString.EMPTY, destination))

        var length = Base64.decodeIntoByteArray(encodedByteString, destination, endIndex = 4)
        assertContentEquals(byteArray.copyOfRange(0, 3), destination.copyOf(length))
        length += Base64.decodeIntoByteArray(encodedByteString, destination, destinationOffset = length, startIndex = 4)
        assertContentEquals(byteArray, destination)
    }

    @Test
    fun testDecodeToByteString() {
        assertFailsWith<IndexOutOfBoundsException> { Base64.decodeToByteString(encodedSymbols, startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { Base64.decodeToByteString(encodedSymbols, endIndex = encodedByteString.size + 1) }
        assertFailsWith<IllegalArgumentException> { Base64.decodeToByteString(encodedSymbols, startIndex = encodedByteString.size + 1) }
        assertFailsWith<IllegalArgumentException> { Base64.decodeToByteString(encodedSymbols, startIndex = 4, endIndex = 0) }

        assertEquals(0, Base64.decodeToByteString(ByteArray(0)).size)
        assertEquals(byteString, Base64.decodeToByteString(encodedSymbols))
        assertEquals(ByteString.wrap(byteArray.copyOfRange(0, 3)), Base64.decodeToByteString(encodedSymbols, endIndex = 4))
        assertEquals(ByteString.wrap(byteArray.copyOfRange(3, byteString.size)), Base64.decodeToByteString(encodedSymbols, startIndex = 4))
    }

    @Test
    fun testByteArrayDecodeToByteString() {
        assertFailsWith<IndexOutOfBoundsException> { Base64.decodeToByteString(encodedBytes, startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { Base64.decodeToByteString(encodedBytes, endIndex = encodedByteString.size + 1) }
        assertFailsWith<IllegalArgumentException> { Base64.decodeToByteString(encodedBytes, startIndex = encodedByteString.size + 1) }
        assertFailsWith<IllegalArgumentException> { Base64.decodeToByteString(encodedBytes, startIndex = 4, endIndex = 0) }

        assertEquals(0, Base64.decodeToByteString(ByteArray(0)).size)
        assertEquals(byteString, Base64.decodeToByteString(encodedBytes))
        assertEquals(ByteString.wrap(byteArray.copyOfRange(0, 3)), Base64.decodeToByteString(encodedBytes, endIndex = 4))
        assertEquals(ByteString.wrap(byteArray.copyOfRange(3, byteString.size)), Base64.decodeToByteString(encodedBytes, startIndex = 4))
    }

    @Test
    fun testByteStringDecodeToByteString() {
        assertFailsWith<IndexOutOfBoundsException> { Base64.decodeToByteString(encodedByteString, startIndex = -1) }
        assertFailsWith<IndexOutOfBoundsException> { Base64.decodeToByteString(encodedByteString, endIndex = encodedByteString.size + 1) }
        assertFailsWith<IllegalArgumentException> { Base64.decodeToByteString(encodedByteString, startIndex = encodedByteString.size + 1) }
        assertFailsWith<IllegalArgumentException> { Base64.decodeToByteString(encodedByteString, startIndex = 4, endIndex = 0) }

        assertEquals(0, Base64.decodeToByteString(ByteString.EMPTY).size)
        assertEquals(byteString, Base64.decodeToByteString(encodedByteString))
        assertEquals(ByteString.wrap(byteArray.copyOfRange(0, 3)), Base64.decodeToByteString(encodedByteString, endIndex = 4))
        assertEquals(ByteString.wrap(byteArray.copyOfRange(3, byteString.size)), Base64.decodeToByteString(encodedByteString, startIndex = 4))
    }
}