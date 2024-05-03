/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.io

import kotlinx.io.internal.REPLACEMENT_CHARACTER
import kotlinx.io.internal.REPLACEMENT_CODE_POINT
import kotlinx.io.internal.processUtf8CodePoints
import kotlin.test.*

class Utf8Test {
    @Test
    fun oneByteCharacters() {
        assertEncoded("00", 0x00) // Smallest 1-byte character.
        assertEncoded("20", ' '.code)
        assertEncoded("7e", '~'.code)
        assertEncoded("7f", 0x7f) // Largest 1-byte character.
    }

    @Test
    fun twoByteCharacters() {
        assertEncoded("c280", 0x0080) // Smallest 2-byte character.
        assertEncoded("c3bf", 0x00ff)
        assertEncoded("c480", 0x0100)
        assertEncoded("dfbf", 0x07ff) // Largest 2-byte character.
    }

    @Test
    fun threeByteCharacters() {
        assertEncoded("e0a080", 0x0800) // Smallest 3-byte character.
        assertEncoded("e0bfbf", 0x0fff)
        assertEncoded("e18080", 0x1000)
        assertEncoded("e1bfbf", 0x1fff)
        assertEncoded("ed8080", 0xd000)
        assertEncoded("ed9fbf", 0xd7ff) // Largest character lower than the min surrogate.
        assertEncoded("ee8080", 0xe000) // Smallest character greater than the max surrogate.
        assertEncoded("eebfbf", 0xefff)
        assertEncoded("ef8080", 0xf000)
        assertEncoded("efbfbf", 0xffff) // Largest 3-byte character.
    }

    @Test
    fun fourByteCharacters() {
        assertEncoded("f0908080", 0x010000) // Smallest surrogate pair.
        assertEncoded("f48fbfbf", 0x10ffff) // Largest code point expressible by UTF-16.
    }

    @Test
    fun unknownBytes() {
        assertCodePointDecoded("f8", REPLACEMENT_CODE_POINT) // Too large
        assertCodePointDecoded("f0f8", REPLACEMENT_CODE_POINT, REPLACEMENT_CODE_POINT)
        assertCodePointDecoded("ff", REPLACEMENT_CODE_POINT) // Largest
        assertCodePointDecoded("f0ff", REPLACEMENT_CODE_POINT, REPLACEMENT_CODE_POINT)

        // Lone continuation
        assertCodePointDecoded("80", REPLACEMENT_CODE_POINT) // Smallest
        assertCodePointDecoded("bf", REPLACEMENT_CODE_POINT) // Largest
    }

    @Test
    fun overlongSequences() {
        // Overlong representation of the NUL character
        assertCodePointDecoded("c080", REPLACEMENT_CODE_POINT)
        assertCodePointDecoded("e08080", REPLACEMENT_CODE_POINT)
        assertCodePointDecoded("f0808080", REPLACEMENT_CODE_POINT)

        // Maximum overlong sequences
        assertCodePointDecoded("c1bf", REPLACEMENT_CODE_POINT)
        assertCodePointDecoded("e09fbf", REPLACEMENT_CODE_POINT)
        assertCodePointDecoded("f08fbfbf", REPLACEMENT_CODE_POINT)
    }

    @Test
    fun danglingHighSurrogate() {
        assertStringEncoded("3f", "\ud800") // "?"
        assertCodePointDecoded("eda080", REPLACEMENT_CODE_POINT)
    }

    @Test
    fun lowSurrogateWithoutHighSurrogate() {
        assertStringEncoded("3f", "\udc00") // "?"
        assertCodePointDecoded("edb080", REPLACEMENT_CODE_POINT)
    }

    @Test
    fun highSurrogateFollowedByNonSurrogate() {
        assertStringEncoded("3fee8080", "\ud800\ue000") // "?\ue000": Following character is too high.
        assertCodePointDecoded("f090ee8080", REPLACEMENT_CODE_POINT, '\ue000'.code)

        assertStringEncoded("3f61", "\ud800\u0061") // "?a": Following character is too low.
        assertCodePointDecoded("f09061", REPLACEMENT_CODE_POINT, 'a'.code)
    }

    @Test
    fun doubleLowSurrogate() {
        assertStringEncoded("3f3f", "\udc00\udc00") // "??"
        assertCodePointDecoded("edb080edb080", REPLACEMENT_CODE_POINT, REPLACEMENT_CODE_POINT)
    }

    @Test
    fun doubleHighSurrogate() {
        assertStringEncoded("3f3f", "\ud800\ud800") // "??"
        assertCodePointDecoded("eda080eda080", REPLACEMENT_CODE_POINT, REPLACEMENT_CODE_POINT)
    }

    @Test
    fun lowSurrogateHighSurrogate() {
        assertStringEncoded("3f3f", "\udc00\ud800") // "??"
        assertCodePointDecoded("edb080eda080", REPLACEMENT_CODE_POINT, REPLACEMENT_CODE_POINT)
    }

    @Test
    fun writeSurrogateCodePoint() {
        assertStringEncoded("ed9fbf", "\ud7ff") // Below lowest surrogate is okay.
        assertCodePointDecoded("ed9fbf", '\ud7ff'.code)

        assertStringEncoded("3f", "\ud800") // Lowest surrogate gets '?'.
        assertCodePointDecoded("eda080", REPLACEMENT_CODE_POINT)

        assertStringEncoded("3f", "\udfff") // Highest surrogate gets '?'.
        assertCodePointDecoded("edbfbf", REPLACEMENT_CODE_POINT)

        assertStringEncoded("ee8080", "\ue000") // Above highest surrogate is okay.
        assertCodePointDecoded("ee8080", '\ue000'.code)
    }

    @Test
    fun bufferWriteCodePoints() {
        bufferWriteCodePointsCheck(0)
    }

    @Test
    fun bufferWriteCodePointsCrossSegments() {
        bufferWriteCodePointsCheck(Segment.SIZE - 1)
    }

    private fun bufferWriteCodePointsCheck(prefixLength: Int) {
        val buffer = Buffer()
        buffer.assertCodePointEncoded("40", '@'.code, prefixLength)
        buffer.assertCodePointEncoded("7f", '\u007f'.code, prefixLength)
        buffer.assertCodePointEncoded("c280", '\u0080'.code, prefixLength)
        buffer.assertCodePointEncoded("c2a9", '\u00a9'.code, prefixLength)
        buffer.assertCodePointEncoded("c3bf", '\u00ff'.code, prefixLength)
        buffer.assertCodePointEncoded("dfbf", '\u07ff'.code, prefixLength)
        buffer.assertCodePointEncoded("e0a080", '\u0800'.code, prefixLength)
        buffer.assertCodePointEncoded("e1839a", '\u10da'.code, prefixLength)
        buffer.assertCodePointEncoded("efbfbf", '\uffff'.code, prefixLength)
        buffer.assertCodePointEncoded("f0908080", 0x10000, prefixLength)
        buffer.assertCodePointEncoded("f48087bf", 0x1001FF, prefixLength)
    }

    @Test
    fun bufferReadCodePoints() {
        bufferReadCodePointsCheck(0)
    }

    @Test
    fun bufferReadCodePointsCrossSegments() {
        bufferReadCodePointsCheck(Segment.SIZE - 1)
    }

    private fun bufferReadCodePointsCheck(prefixLength: Int) {
        val buffer = Buffer()
        buffer.assertCodePointDecoded('@'.code, "40", prefixLength)
        buffer.assertCodePointDecoded('\u007f'.code, "7f", prefixLength)
        buffer.assertCodePointDecoded('\u0080'.code, "c280", prefixLength)
        buffer.assertCodePointDecoded('\u00a9'.code, "c2a9", prefixLength)
        buffer.assertCodePointDecoded('\u00ff'.code, "c3bf", prefixLength)
        buffer.assertCodePointDecoded('\u07ff'.code, "dfbf", prefixLength)
        buffer.assertCodePointDecoded('\u0800'.code, "e0a080", prefixLength)
        buffer.assertCodePointDecoded('\u10da'.code, "e1839a", prefixLength)
        buffer.assertCodePointDecoded('\uffff'.code, "efbfbf", prefixLength)
        buffer.assertCodePointDecoded(0x10000, "f0908080", prefixLength)
        buffer.assertCodePointDecoded(0x1001FF, "f48087bf", prefixLength)
    }

    @Test
    fun bufferWriteUtf8String() {
        bufferWriteUtf8StringCheck(0)
    }

    @Test
    fun bufferWriteUtf8StringCrossSegments() {
        bufferWriteUtf8StringCheck(Segment.SIZE - 1)
    }

    private fun bufferWriteUtf8StringCheck(prefixLength: Int) {
        val buffer = Buffer()
        buffer.assertUtf8StringEncoded("68656c6c6f", "hello", prefixLength)
        buffer.assertUtf8StringEncoded("cf87ceb5cf81ceb5cf84ceb9cf83cebccf8ccf82", "χερετισμός",
            prefixLength)
        buffer.assertUtf8StringEncoded(
            "e18392e18390e1839be18390e183a0e183afe1839de18391e18390",
            "გამარჯობა",
            prefixLength
        )
        buffer.assertUtf8StringEncoded(
            "f093878bf0938bb4f09380a5",
            "\uD80C\uDDCB\uD80C\uDEF4\uD80C\uDC25",/* 𓇋𓋴𓀥, to hail, AN EGYPTIAN HIEROGLYPHIC DICTIONARY, p. 79b */
            prefixLength
        )

        // two consecutive high surrogates, replace with '?'
        buffer.assertUtf8StringEncoded("3f3f", "\ud801\uD801", prefixLength)
    }

    @Test
    fun bufferReadUtf8String() {
        bufferReadUtf8StringCheck(0)
    }

    @Test
    fun bufferReadUtf8StringCrossSegments() {
        bufferReadUtf8StringCheck(Segment.SIZE - 1)
    }

    private fun bufferReadUtf8StringCheck(prefixLength: Int) {
        val buffer = Buffer()
        buffer.assertUtf8StringDecoded("hello","68656c6c6f",  prefixLength)
        buffer.assertUtf8StringDecoded("χερετισμός", "cf87ceb5cf81ceb5cf84ceb9cf83cebccf8ccf82",
            prefixLength)
        buffer.assertUtf8StringDecoded(
            "გამარჯობა",
            "e18392e18390e1839be18390e183a0e183afe1839de18391e18390",
            prefixLength
        )
        buffer.assertUtf8StringDecoded(
            "\uD80C\uDDCB\uD80C\uDEF4\uD80C\uDC25",/* 𓇋𓋴𓀥, to hail, AN EGYPTIAN HIEROGLYPHIC DICTIONARY, p. 79b */
            "f093878bf0938bb4f09380a5",
            prefixLength
        )
    }

    @Test
    fun size() {
        assertEquals(0, "".utf8Size())
        assertEquals(3, "abc".utf8Size())
        assertEquals(16, "təˈranəˌsôr".utf8Size())
    }

    @Test
    fun sizeWithBounds() {
        assertEquals(0, "".utf8Size(0, 0))
        assertEquals(0, "abc".utf8Size(0, 0))
        assertEquals(1, "abc".utf8Size(1, 2))
        assertEquals(2, "abc".utf8Size(0, 2))
        assertEquals(3, "abc".utf8Size(0, 3))
        assertEquals(16, "təˈranəˌsôr".utf8Size(0, 11))
        assertEquals(5, "təˈranəˌsôr".utf8Size(3, 7))
    }

    @Test
    fun sizeBoundsCheck() {
        assertFailsWith<IndexOutOfBoundsException> {
            "abc".utf8Size(-1, 2)
        }

        assertFailsWith<IllegalArgumentException> {
            "abc".utf8Size(2, 1)
        }

        assertFailsWith<IndexOutOfBoundsException> {
            "abc".utf8Size(1, 4)
        }
    }

    @Test
    fun readCodePointFromEmptyBufferThrowsEofException() {
        val buffer = Buffer()
        assertFailsWith<EOFException> { buffer.readCodePointValue() }
    }

    @Test
    fun readLeadingContinuationByteReturnsReplacementCharacter() {
        val buffer = Buffer()
        buffer.writeByte(0xbf.toByte())
        assertEquals(REPLACEMENT_CODE_POINT, buffer.readCodePointValue())
        assertTrue(buffer.exhausted())
    }

    @Test
    fun readMissingContinuationBytesThrowsEofException() {
        val buffer = Buffer()
        buffer.writeByte(0xdf.toByte())
        assertFailsWith<EOFException> { buffer.readCodePointValue() }
        assertFalse(buffer.exhausted()) // Prefix byte wasn't consumed.
    }

    @Test
    fun readTooLargeCodepointReturnsReplacementCharacter() {
        // 5-byte and 6-byte code points are not supported.
        val buffer = Buffer()
        buffer.write("f888808080".decodeHex())
        assertEquals(REPLACEMENT_CODE_POINT, buffer.readCodePointValue())
        assertEquals(REPLACEMENT_CODE_POINT, buffer.readCodePointValue())
        assertEquals(REPLACEMENT_CODE_POINT, buffer.readCodePointValue())
        assertEquals(REPLACEMENT_CODE_POINT, buffer.readCodePointValue())
        assertEquals(REPLACEMENT_CODE_POINT, buffer.readCodePointValue())
        assertTrue(buffer.exhausted())

        buffer.write(ByteArray(Segment.SIZE - 2))
        buffer.write("f888808080".decodeHex())
        buffer.skip(Segment.SIZE - 2L)
        assertEquals(REPLACEMENT_CODE_POINT, buffer.readUtf8CodePoint())
        assertEquals(REPLACEMENT_CODE_POINT, buffer.readUtf8CodePoint())
        assertEquals(REPLACEMENT_CODE_POINT, buffer.readUtf8CodePoint())
        assertEquals(REPLACEMENT_CODE_POINT, buffer.readUtf8CodePoint())
        assertEquals(REPLACEMENT_CODE_POINT, buffer.readUtf8CodePoint())
        assertTrue(buffer.exhausted())
    }

    @Test
    fun readNonContinuationBytesReturnsReplacementCharacter() {
        // Use a non-continuation byte where a continuation byte is expected.
        val buffer = Buffer()
        buffer.write("df20".decodeHex())
        assertEquals(REPLACEMENT_CODE_POINT, buffer.readCodePointValue())
        assertEquals(0x20, buffer.readCodePointValue()) // Non-continuation character not consumed.
        assertTrue(buffer.exhausted())
    }

    @Test
    fun readCodePointBeyondUnicodeMaximum() {
        // A 4-byte encoding with data above the U+10ffff Unicode maximum.
        val buffer = Buffer()
        buffer.write("f4908080".decodeHex())
        assertEquals(REPLACEMENT_CODE_POINT, buffer.readCodePointValue())
        assertTrue(buffer.exhausted())
    }

    @Test
    fun readSurrogateCodePoint() {
        val buffer = Buffer()
        buffer.write("eda080".decodeHex())
        assertEquals(REPLACEMENT_CODE_POINT, buffer.readCodePointValue())
        assertTrue(buffer.exhausted())
        buffer.write("edbfbf".decodeHex())
        assertEquals(REPLACEMENT_CODE_POINT, buffer.readCodePointValue())
        assertTrue(buffer.exhausted())
    }

    @Test
    fun readOverlongCodePoint() {
        // Use 2 bytes to encode data that only needs 1 byte.
        val buffer = Buffer()
        buffer.write("c080".decodeHex())
        assertEquals(REPLACEMENT_CODE_POINT, buffer.readCodePointValue())
        assertTrue(buffer.exhausted())
    }

    @Test
    fun writeCodePointBeyondUnicodeMaximum() {
        val buffer = Buffer()
        assertFailsWith<IllegalArgumentException>("Unexpected code point: 0x110000") {
            buffer.writeCodePointValue(0x110000)
        }
    }

    @Test
    fun readStringWithUnderflow() {
        val buffer = Buffer()
        // 3 byte-encoded, last byte missing
        buffer.assertUtf8StringDecoded(REPLACEMENT_CHARACTER.toString(), "e183")
        // 3 byte-encoded, last two bytes missing
        buffer.assertUtf8StringDecoded(REPLACEMENT_CHARACTER.toString(), "e1")
        // 2 byte-encoded, last byte missing
        buffer.assertUtf8StringDecoded(REPLACEMENT_CHARACTER.toString(), "cf")
        // 4 byte encoded, various underflows
        buffer.assertUtf8StringDecoded(REPLACEMENT_CHARACTER.toString(), "f09383")
        buffer.assertUtf8StringDecoded(REPLACEMENT_CHARACTER.toString(), "f093")
        buffer.assertUtf8StringDecoded(REPLACEMENT_CHARACTER.toString(), "f0")
    }

    @Test
    fun readStringWithoutContinuationByte() {
        val buffer = Buffer()
        // 2 byte-encoded, last byte corrupted
        buffer.assertUtf8StringDecoded("${REPLACEMENT_CHARACTER}a", "cf61")
        // 3 byte-encoded, last byte corrupted
        buffer.assertUtf8StringDecoded("${REPLACEMENT_CHARACTER}a", "e18361")
        // 3 byte-encoded, last two bytes corrupted
        buffer.assertUtf8StringDecoded("${REPLACEMENT_CHARACTER}aa", "e16161")
        // 4 byte-encoded, various bytes corrupterd
        buffer.assertUtf8StringDecoded("${REPLACEMENT_CHARACTER}a", "f0938361")
        buffer.assertUtf8StringDecoded("${REPLACEMENT_CHARACTER}aa", "f0936161")
        buffer.assertUtf8StringDecoded("${REPLACEMENT_CHARACTER}aaa", "f0616161")
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun encodeUtf16SurrogatePair() {
        val buffer = Buffer()
        buffer.writeString("\uD852\uDF62")
        println(buffer.readByteArray().toHexString())
    }

    private fun assertEncoded(hex: String, vararg codePoints: Int) {
        assertCodePointDecoded(hex, *codePoints)
    }

    private fun assertCodePointDecoded(hex: String, vararg codePoints: Int) {
        val bytes = hex.decodeHex()
        var i = 0
        bytes.processUtf8CodePoints(0, bytes.size) { codePoint ->
            if (i < codePoints.size) assertEquals(codePoints[i], codePoint, "index=$i")
            i++
        }
        assertEquals(i, codePoints.size) // Checked them all
    }

    private fun Buffer.assertCodePointEncoded(expectedHex: String, codePoint: Int, prefixLength: Int = 0) {
        write(ByteArray(prefixLength))
        writeCodePointValue(codePoint)
        skip(prefixLength.toLong())
        assertArrayEquals(expectedHex.decodeHex(), readByteArray())
    }

    private fun Buffer.assertCodePointDecoded(expectedCodePoint: Int, hex: String, prefixLength: Int = 0) {
        write(ByteArray(prefixLength))
        write(hex.decodeHex())
        skip(prefixLength.toLong())
        assertEquals(expectedCodePoint, readCodePointValue())
    }

    private fun Buffer.assertUtf8StringEncoded(expectedHex: String, string: String, prefixLength: Int = 0) {
        write(ByteArray(prefixLength))
        writeString(string)
        skip(prefixLength.toLong())
        assertArrayEquals(expectedHex.decodeHex(), readByteArray())
    }

    private fun Buffer.assertUtf8StringDecoded(expectedString: String, hex: String, prefixLength: Int = 0) {
        write(ByteArray(prefixLength))
        write(hex.decodeHex())
        skip(prefixLength.toLong())
        assertEquals(expectedString, readString())
    }

    private fun assertStringEncoded(hex: String, string: String) {
        val expectedUtf8 = hex.decodeHex()

        // Confirm our expectations are consistent with the platform.
        val platformUtf8 = string.asUtf8ToByteArray()
        assertArrayEquals(expectedUtf8, platformUtf8)

        // Confirm our implementations matches those expectations.
        val actualUtf8 = string.commonAsUtf8ToByteArray()
        assertArrayEquals(expectedUtf8, actualUtf8)

        // Confirm we are consistent when writing one code point at a time.
        val bufferUtf8 = Buffer()
        for (charIdx in string.indices) {
            val c = string[charIdx]
            bufferUtf8.writeCodePointValue(c.code)
        }
        assertArrayEquals(expectedUtf8, bufferUtf8.readByteArray())

        // Confirm we are consistent when measuring lengths.
        assertEquals(expectedUtf8.size.toLong(), string.utf8Size())
        assertEquals(expectedUtf8.size.toLong(), string.utf8Size(0, string.length))
    }
}
