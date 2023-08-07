/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:OptIn(ExperimentalStdlibApi::class)

package kotlinx.io.bytestring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ByteStringHexTest {
    private val byteString = ByteString.wrap(byteArrayOf(10, 11, 12, 13, 14, 15))

    @Test
    fun testEmpty() {
        assertTrue(ByteString.EMPTY.toHexString().isEmpty())
        assertTrue("".hexToByteString().isEmpty())
    }

    @Test
    fun testIndexes() {
        assertEquals("0b0c", byteString.toHexString(1, 3))
        assertEquals("0c0d0e0f", byteString.toHexString(2))
        assertEquals("0a0b0c0d", byteString.toHexString(endIndex = 4))
    }

    @Test
    fun testFormats() {
        val format = HexFormat {
            bytes {
                byteSeparator = "|"
            }
        }
        assertEquals("0a|0b|0c|0d|0e|0f", byteString.toHexString(format))
        assertEquals("0b|0c|0d", byteString.toHexString(1, 4, format))

        assertEquals(byteString, "0a|0b|0c|0d|0e|0f".hexToByteString(format))
    }

    @Test
    fun testDefault() {
        assertEquals("0a0b0c0d0e0f", byteString.toHexString())
        assertEquals("0b0c0d", byteString.toHexString(1, 4))

        assertEquals(byteString, "0a0b0c0d0e0f".hexToByteString())
    }

}