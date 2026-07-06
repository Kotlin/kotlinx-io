/*
 * Copyright 2010-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.bytestring

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class RandomByteStringTest {
    @Test
    fun testEmpty() {
        assertEquals(0, Random.nextByteString(0).size)
    }

    @Test
    fun testIllegalSize() {
        assertFailsWith<IllegalArgumentException> { Random.nextByteString(-1) }
    }

    @Test
    fun testRandomByteStrings() {
        val a = Random.nextByteString(10)
        val b = Random.nextByteString(10)
        assertNotEquals(a, b)
        assertEquals(10, a.size)
        assertEquals(10, b.size)

        assertEquals(42, Random.nextByteString(42).size)
    }
}
