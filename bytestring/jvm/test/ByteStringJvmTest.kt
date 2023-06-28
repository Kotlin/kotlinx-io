/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.bytestring

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Set of tests covering JVM-specific [ByteString] extensions.
 */
class ByteStringJvmTest {
    @Test
    fun createFromString() {
        val str = "hello"

        assertEquals(ByteString(byteArrayOf(0x68, 0x65, 0x6c, 0x6c, 0x6f)), ByteString.fromString(str, Charsets.UTF_8))
        assertEquals(
            ByteString(
                byteArrayOf(
                    0, 0, 0, 0x68, 0, 0, 0, 0x65, 0, 0, 0, 0x6c,
                    0, 0, 0, 0x6c, 0, 0, 0, 0x6f
                )
            ), ByteString.fromString(str, Charsets.UTF_32)
        )
    }

    @Test
    fun decodeToString() {
        assertEquals(
            "Ϭ",
            ByteString(0xfeU.toByte(), 0xffU.toByte(), 0x03, 0xecU.toByte()).toString(Charsets.UTF_16)
        )

        assertEquals("123", ByteString("123".encodeToByteArray()).toString(Charsets.UTF_8))
    }
}
