/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlin.test.Test
import kotlin.test.assertEquals

class UtilsTest {
    @Test
    fun hexNumberLength() {
        val num2length: Map<Long, Int> = mapOf(
            0x1L to 1,
            0x10L to 2,
            0x100L to 3,
            0x1000L to 4,
            0x10000L to 5,
            0x100000L to 6,
            0x1000000L to 7,
            0x10000000L to 8,
            0x100000000L to 9,
            0x1000000000L to 10,
            0x10000000000L to 11,
            0x100000000000L to 12,
            0x1000000000000L to 13,
            0x10000000000000L to 14,
            0x100000000000000L to 15,
            0x1000000000000000L to 16,
            -1L to 16,
            0x3fL to 2,
            0x7fL to 2,
            0xffL to 2,
            0L to 1
        )

        num2length.forEach { (num, length) ->
            assertEquals(length, hexNumberLength(num), "Wrong length for 0x${num.toString(16)}")
        }
    }
}
