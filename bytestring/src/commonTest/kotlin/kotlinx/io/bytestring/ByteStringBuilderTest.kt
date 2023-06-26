/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.bytestring

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ByteStringBuilderTest {
    @Test
    fun emptyString() {
        assertTrue(ByteStringBuilder().toByteString().isEmpty())
        assertTrue(ByteStringBuilder(1024).toByteString().isEmpty())
    }

    @Test
    fun appendByte() {
        val builder = ByteStringBuilder()
        with(builder) {
            append(1)
            append(2)
            append(3)
        }
        assertEquals(ByteString(1, 2, 3), builder.toByteString())
    }

    @Test
    fun appendBytes() {
        assertEquals(ByteString(1, 2, 3), buildByteString { append(1, 2, 3) })
    }

    @Test
    fun appendUByte() {
        val builder = ByteStringBuilder()
        with(builder) {
            append(0x80U)
            append(0x81U)
            append(0x82U)
        }
        assertEquals(ByteString(0x80U.toByte(), 0x81U.toByte(), 0x82U.toByte()), builder.toByteString())
    }

    @Test
    fun appendArray() {
        with(ByteStringBuilder()) {
            append(byteArrayOf(1, 2, 3, 4))
            assertEquals(ByteString(1, 2, 3, 4), toByteString())
        }

        with(ByteStringBuilder()) {
            append(byteArrayOf(1, 2, 3, 4), startIndex = 2)
            assertEquals(ByteString(3, 4), toByteString())
        }

        with(ByteStringBuilder()) {
            append(byteArrayOf(1, 2, 3, 4), endIndex = 2)
            assertEquals(ByteString(1, 2), toByteString())
        }

        with(ByteStringBuilder()) {
            append(byteArrayOf(1, 2, 3, 4), startIndex = 1, endIndex = 3)
            assertEquals(ByteString(2, 3), toByteString())
        }

        with(ByteStringBuilder()) {
            append(byteArrayOf(1, 2, 3, 4), startIndex = 1, endIndex = 1)
            assertEquals(ByteString(), toByteString())
        }
    }

    @Test
    fun testAppendByteArrayWithInvalidIndices() {
        val builder = ByteStringBuilder()
        val array = ByteArray(10)
        assertFailsWith<IllegalArgumentException> { builder.append(array, 2, 0) }
        assertFailsWith<IndexOutOfBoundsException> { builder.append(array, -1, 2) }
        assertFailsWith<IndexOutOfBoundsException> { builder.append(array, 0, 1000) }
        assertFailsWith<IndexOutOfBoundsException> { builder.append(array, 1000, 1001) }
    }

    @Test
    fun appendByteString() {
        val builder = ByteStringBuilder()
        builder.append(ByteString(1, 2, 3, 4))
        assertEquals(ByteString(1, 2, 3, 4), builder.toByteString())
    }

    @Test
    fun appendMultipleValues() {
        val string = with(ByteStringBuilder()) {
            append(42)
            append(ByteArray(10) { it.toByte() })
            append(42)
            append(ByteString(10, 5, 57))
            toByteString()
        }

        assertEquals(ByteString(42, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 42, 10, 5, 57), string)
    }

    @Test
    fun resizeMultipleTimes() {
        val builder = ByteStringBuilder()
        builder.append(ByteArray(1))
        builder.append(ByteArray(32))
        builder.append(ByteArray(120))
        builder.append(ByteArray(1024))

        assertEquals(ByteString(ByteArray(1 + 32 + 120 + 1024)), builder.toByteString())
    }

    @Test
    fun testSize() {
        val builder = ByteStringBuilder()
        assertEquals(0, builder.size)
        builder.append(1)
        assertEquals(1, builder.size)
        builder.append(ByteArray(33))
        assertEquals(34, builder.size)
    }

    @Test
    fun testCapacity() {
        assertEquals(0, ByteStringBuilder().capacity)
        assertEquals(10, ByteStringBuilder(10).capacity)

        with(ByteStringBuilder()) {
            append(1)
            assertTrue(capacity >= 1)
            append(ByteArray(1024))
            assertTrue(capacity >= 1025)
        }
    }

    @Test
    fun createMultipleByteStrings() {
        val builder = ByteStringBuilder()
        builder.append(1)
        val str0 = builder.toByteString()
        assertEquals(ByteString(1), str0)
        assertEquals(ByteString(1), builder.toByteString())
        builder.append(2)
        assertEquals(ByteString(1, 2), builder.toByteString())
        assertEquals(ByteString(1), str0)
    }
}
