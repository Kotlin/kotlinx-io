/*
 * Copyright 2017-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Exercises [readString] against the JDK 9+ `Utf8Intrinsics` variant from `jvm/src9`,
 * which the `jvm9Test` task puts ahead of the base variant on the classpath, mirroring
 * how `META-INF/versions/9` entries shadow base entries in the packaged multi-release jar.
 */
class ReadStringJdk9Test {
    private fun bufferOf(bytes: ByteArray): Buffer = Buffer().apply { write(bytes) }

    @Test
    fun fastPathIsActive() {
        // Discriminates the two Utf8Intrinsics variants: for the overlong encoding 0xC0 0xAF
        // the JDK decoder emits two replacement characters (Unicode maximal subpart convention)
        // while the common decoder emits one. If this fails, the base variant was loaded and
        // the JDK 9+ specialization is not being tested.
        assertEquals("��", bufferOf(byteArrayOf(-64, -81)).readString())
    }

    @Test
    fun asciiSingleSegment() {
        val text = "The quick brown fox jumps over the lazy dog: 0123456789"
        assertEquals(text, bufferOf(text.encodeToByteArray()).readString())
    }

    @Test
    fun multiByteContent() {
        val text = "München Straße 日本語のテキスト 🚀 done"
        assertEquals(text, bufferOf(text.encodeToByteArray()).readString())
    }

    @Test
    fun multiSegmentWithCharsStraddlingSegmentBoundary() {
        // 8191 bytes of ASCII place the two-byte 'é' across the default segment boundary.
        val text = "a".repeat(8191) + "é" + "b".repeat(8192) + "☃" + "c".repeat(100)
        assertEquals(text, bufferOf(text.encodeToByteArray()).readString())
    }

    @Test
    fun byteCountBoundedRead() {
        val buffer = bufferOf("hello, world".encodeToByteArray())
        assertEquals("hello", buffer.readString(5L))
        assertEquals(", world", buffer.readString())
    }

    @Test
    fun emptyString() {
        assertEquals("", Buffer().readString())
    }

    @Test
    fun malformedSequencesFollowJdkConvention() {
        val cases = listOf(
            byteArrayOf(-64, -81), // overlong '/' (0xC0 0xAF)
            byteArrayOf(-32, -128, -81), // overlong, three-byte form (0xE0 0x80 0xAF)
            byteArrayOf(-19, -96, -128), // CESU-8 high surrogate (0xED 0xA0 0x80)
            byteArrayOf(-61), // truncated two-byte sequence
            byteArrayOf(-30, -126), // truncated three-byte sequence
            byteArrayOf(-16, -112, -115), // truncated four-byte sequence
            byteArrayOf(-128), // lone continuation byte
            byteArrayOf(65, -128, -128, 66), // continuation bytes between ASCII
            byteArrayOf(-12, -112, -128, -128), // code point above U+10FFFF (0xF4 0x90 0x80 0x80)
        )
        for (bytes in cases) {
            assertEquals(
                String(bytes, Charsets.UTF_8),
                bufferOf(bytes).readString(),
                "bytes: ${bytes.joinToString()}",
            )
        }
    }

    @Test
    fun randomBytesMatchJdkDecoder() {
        val random = Random(20260719)
        repeat(1000) {
            val bytes = random.nextBytes(random.nextInt(0, 512))
            assertEquals(String(bytes, Charsets.UTF_8), bufferOf(bytes).readString())
        }
    }

    @Test
    fun randomValidStringsRoundTrip() {
        val random = Random(715)
        repeat(1000) {
            val text = buildString {
                repeat(random.nextInt(0, 64)) {
                    when (random.nextInt(4)) {
                        0 -> append('a' + random.nextInt(26))
                        1 -> append('é')
                        2 -> append('日')
                        else -> append("🚀")
                    }
                }
            }
            assertEquals(text, Buffer().apply { writeString(text) }.readString())
        }
    }
}
