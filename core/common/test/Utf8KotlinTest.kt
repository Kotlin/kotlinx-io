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

import kotlinx.io.internal.REPLACEMENT_CODE_POINT
import kotlinx.io.internal.commonAsUtf8ToByteArray
import kotlinx.io.internal.processUtf8CodePoints
import kotlin.test.*

class Utf8KotlinTest {
  @Test fun oneByteCharacters() {
    assertEncoded("00", 0x00) // Smallest 1-byte character.
    assertEncoded("20", ' '.code)
    assertEncoded("7e", '~'.code)
    assertEncoded("7f", 0x7f) // Largest 1-byte character.
  }

  @Test fun twoByteCharacters() {
    assertEncoded("c280", 0x0080) // Smallest 2-byte character.
    assertEncoded("c3bf", 0x00ff)
    assertEncoded("c480", 0x0100)
    assertEncoded("dfbf", 0x07ff) // Largest 2-byte character.
  }

  @Test fun threeByteCharacters() {
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

  @Test fun fourByteCharacters() {
    assertEncoded("f0908080", 0x010000) // Smallest surrogate pair.
    assertEncoded("f48fbfbf", 0x10ffff) // Largest code point expressible by UTF-16.
  }

  @Test fun unknownBytes() {
    assertCodePointDecoded("f8", REPLACEMENT_CODE_POINT) // Too large
    assertCodePointDecoded("f0f8", REPLACEMENT_CODE_POINT, REPLACEMENT_CODE_POINT)
    assertCodePointDecoded("ff", REPLACEMENT_CODE_POINT) // Largest
    assertCodePointDecoded("f0ff", REPLACEMENT_CODE_POINT, REPLACEMENT_CODE_POINT)

    // Lone continuation
    assertCodePointDecoded("80", REPLACEMENT_CODE_POINT) // Smallest
    assertCodePointDecoded("bf", REPLACEMENT_CODE_POINT) // Largest
  }

  @Test fun overlongSequences() {
    // Overlong representation of the NUL character
    assertCodePointDecoded("c080", REPLACEMENT_CODE_POINT)
    assertCodePointDecoded("e08080", REPLACEMENT_CODE_POINT)
    assertCodePointDecoded("f0808080", REPLACEMENT_CODE_POINT)

    // Maximum overlong sequences
    assertCodePointDecoded("c1bf", REPLACEMENT_CODE_POINT)
    assertCodePointDecoded("e09fbf", REPLACEMENT_CODE_POINT)
    assertCodePointDecoded("f08fbfbf", REPLACEMENT_CODE_POINT)
  }

  @Test fun danglingHighSurrogate() {
    assertStringEncoded("3f", "\ud800") // "?"
    assertCodePointDecoded("eda080", REPLACEMENT_CODE_POINT)
  }

  @Test fun lowSurrogateWithoutHighSurrogate() {
    assertStringEncoded("3f", "\udc00") // "?"
    assertCodePointDecoded("edb080", REPLACEMENT_CODE_POINT)
  }

  @Test fun highSurrogateFollowedByNonSurrogate() {
    assertStringEncoded("3fee8080", "\ud800\ue000") // "?\ue000": Following character is too high.
    assertCodePointDecoded("f090ee8080", REPLACEMENT_CODE_POINT, '\ue000'.code)

    assertStringEncoded("3f61", "\ud800\u0061") // "?a": Following character is too low.
    assertCodePointDecoded("f09061", REPLACEMENT_CODE_POINT, 'a'.code)
  }

  @Test fun doubleLowSurrogate() {
    assertStringEncoded("3f3f", "\udc00\udc00") // "??"
    assertCodePointDecoded("edb080edb080", REPLACEMENT_CODE_POINT, REPLACEMENT_CODE_POINT)
  }

  @Test fun doubleHighSurrogate() {
    assertStringEncoded("3f3f", "\ud800\ud800") // "??"
    assertCodePointDecoded("eda080eda080", REPLACEMENT_CODE_POINT, REPLACEMENT_CODE_POINT)
  }

  @Test fun lowSurrogateHighSurrogate() {
    assertStringEncoded("3f3f", "\udc00\ud800") // "??"
    assertCodePointDecoded("edb080eda080", REPLACEMENT_CODE_POINT, REPLACEMENT_CODE_POINT)
  }

  @Test fun writeSurrogateCodePoint() {
    assertStringEncoded("ed9fbf", "\ud7ff") // Below lowest surrogate is okay.
    assertCodePointDecoded("ed9fbf", '\ud7ff'.code)

    assertStringEncoded("3f", "\ud800") // Lowest surrogate gets '?'.
    assertCodePointDecoded("eda080", REPLACEMENT_CODE_POINT)

    assertStringEncoded("3f", "\udfff") // Highest surrogate gets '?'.
    assertCodePointDecoded("edbfbf", REPLACEMENT_CODE_POINT)

    assertStringEncoded("ee8080", "\ue000") // Above highest surrogate is okay.
    assertCodePointDecoded("ee8080", '\ue000'.code)
  }

  @Test fun bufferWriteCodePoints() {
    val buffer = Buffer()
    buffer.assertCodePointEncoded("40", '@'.code)
    buffer.assertCodePointEncoded("7f", '\u007f'.code)
    buffer.assertCodePointEncoded("c280", '\u0080'.code)
    buffer.assertCodePointEncoded("c2a9", '\u00a9'.code)
    buffer.assertCodePointEncoded("c3bf", '\u00ff'.code)
    buffer.assertCodePointEncoded("dfbf", '\u07ff'.code)
    buffer.assertCodePointEncoded("e0a080", '\u0800'.code)
    buffer.assertCodePointEncoded("e1839a", '\u10da'.code)
    buffer.assertCodePointEncoded("efbfbf", '\uffff'.code)
    buffer.assertCodePointEncoded("f0908080", 0x10000)
    buffer.assertCodePointEncoded("f48087bf", 0x1001FF)
  }

  @Test fun bufferReadCodePoints() {
    val buffer = Buffer()
    buffer.assertCodePointDecoded('@'.code, "40")
    buffer.assertCodePointDecoded('\u007f'.code, "7f", )
    buffer.assertCodePointDecoded('\u0080'.code, "c280")
    buffer.assertCodePointDecoded('\u00a9'.code, "c2a9")
    buffer.assertCodePointDecoded('\u00ff'.code, "c3bf")
    buffer.assertCodePointDecoded('\u07ff'.code, "dfbf")
    buffer.assertCodePointDecoded('\u0800'.code, "e0a080")
    buffer.assertCodePointDecoded('\u10da'.code, "e1839a")
    buffer.assertCodePointDecoded('\uffff'.code, "efbfbf", )
    buffer.assertCodePointDecoded(0x10000, "f0908080")
    buffer.assertCodePointDecoded(0x1001FF, "f48087bf")
  }

  @Test fun bufferWriteUtf8String() {
    val buffer = Buffer()
    buffer.assertUtf8StringEncoded("68656c6c6f", "hello")
    buffer.assertUtf8StringEncoded("cf87ceb5cf81ceb5cf84ceb9cf83cebccf8ccf82", "χερετισμός")
    buffer.assertUtf8StringEncoded("e18392e18390e1839be18390e183a0e183afe1839de18391e18390",
      "გამარჯობა")
    buffer.assertUtf8StringEncoded("f093878bf0938bb4f09380a5",
      "\uD80C\uDDCB\uD80C\uDEF4\uD80C\uDC25" /* 𓇋𓋴𓀥, to hail, AN EGYPTIAN HIEROGLYPHIC DICTIONARY, p. 79b */)

    // two consecutive high surrogates, replace with '?'
    buffer.assertUtf8StringEncoded("3f3f", "\ud801\uD801")
  }

  @Test fun size() {
    assertEquals(0, "".utf8Size())
    assertEquals(3, "abc".utf8Size())
    assertEquals(16, "təˈranəˌsôr".utf8Size())
  }

  @Test fun sizeWithBounds() {
    assertEquals(0, "".utf8Size(0, 0))
    assertEquals(0, "abc".utf8Size(0, 0))
    assertEquals(1, "abc".utf8Size(1, 2))
    assertEquals(2, "abc".utf8Size(0, 2))
    assertEquals(3, "abc".utf8Size(0, 3))
    assertEquals(16, "təˈranəˌsôr".utf8Size(0, 11))
    assertEquals(5, "təˈranəˌsôr".utf8Size(3, 7))
  }

  @Test fun sizeBoundsCheck() {
    assertFailsWith<IllegalArgumentException> {
      "abc".utf8Size(-1, 2)
    }

    assertFailsWith<IllegalArgumentException> {
      "abc".utf8Size(2, 1)
    }

    assertFailsWith<IllegalArgumentException> {
      "abc".utf8Size(1, 4)
    }
  }

  @Test
  fun readCodePointFromEmptyBufferThrowsEofException() {
    val buffer = Buffer()
    assertFailsWith<EOFException> { buffer.readUtf8CodePoint() }
  }

  @Test
  fun readLeadingContinuationByteReturnsReplacementCharacter() {
    val buffer = Buffer()
    buffer.writeByte(0xbf.toByte())
    assertEquals(REPLACEMENT_CODE_POINT, buffer.readUtf8CodePoint())
    assertTrue(buffer.exhausted())
  }

  @Test
  fun readMissingContinuationBytesThrowsEofException() {
    val buffer = Buffer()
    buffer.writeByte(0xdf.toByte())
    assertFailsWith<EOFException> { buffer.readUtf8CodePoint() }
    assertFalse(buffer.exhausted()) // Prefix byte wasn't consumed.
  }

  @Test
  fun readTooLargeCodepointReturnsReplacementCharacter() {
    // 5-byte and 6-byte code points are not supported.
    val buffer = Buffer()
    buffer.write("f888808080".decodeHex())
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
    assertEquals(REPLACEMENT_CODE_POINT, buffer.readUtf8CodePoint())
    assertEquals(0x20, buffer.readUtf8CodePoint()) // Non-continuation character not consumed.
    assertTrue(buffer.exhausted())
  }

  @Test
  fun readCodePointBeyondUnicodeMaximum() {
    // A 4-byte encoding with data above the U+10ffff Unicode maximum.
    val buffer = Buffer()
    buffer.write("f4908080".decodeHex())
    assertEquals(REPLACEMENT_CODE_POINT, buffer.readUtf8CodePoint())
    assertTrue(buffer.exhausted())
  }

  @Test
  fun readSurrogateCodePoint() {
    val buffer = Buffer()
    buffer.write("eda080".decodeHex())
    assertEquals(REPLACEMENT_CODE_POINT, buffer.readUtf8CodePoint())
    assertTrue(buffer.exhausted())
    buffer.write("edbfbf".decodeHex())
    assertEquals(REPLACEMENT_CODE_POINT, buffer.readUtf8CodePoint())
    assertTrue(buffer.exhausted())
  }

  @Test
  fun readOverlongCodePoint() {
    // Use 2 bytes to encode data that only needs 1 byte.
    val buffer = Buffer()
    buffer.write("c080".decodeHex())
    assertEquals(REPLACEMENT_CODE_POINT, buffer.readUtf8CodePoint())
    assertTrue(buffer.exhausted())
  }

  @Test
  fun writeCodePointBeyondUnicodeMaximum() {
    val buffer = Buffer()
    assertFailsWith<IllegalArgumentException>("Unexpected code point: 0x110000") {
      buffer.writeUtf8CodePoint(0x110000)
    }
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

  private fun Buffer.assertCodePointEncoded(expectedHex: String, codePoint: Int) {
    buffer.writeUtf8CodePoint(codePoint)
    assertArrayEquals(expectedHex.decodeHex(), buffer.readByteArray())
  }

  private fun Buffer.assertCodePointDecoded(expectedCodePoint: Int, hex: String) {
    buffer.write(hex.decodeHex())
    assertEquals(expectedCodePoint, buffer.readUtf8CodePoint())
  }

  private fun Buffer.assertUtf8StringEncoded(expectedHex: String, string: String) {
    buffer.writeUtf8(string)
    assertArrayEquals(expectedHex.decodeHex(), buffer.readByteArray())
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
      bufferUtf8.writeUtf8CodePoint(c.code)
    }
    assertArrayEquals(expectedUtf8, bufferUtf8.readByteArray())

    // Confirm we are consistent when measuring lengths.
    assertEquals(expectedUtf8.size.toLong(), string.utf8Size())
    assertEquals(expectedUtf8.size.toLong(), string.utf8Size(0, string.length))
  }
}
