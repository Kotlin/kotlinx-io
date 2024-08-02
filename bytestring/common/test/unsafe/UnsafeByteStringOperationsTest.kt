/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.bytestring.unsafe

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.io.bytestring.encodeToByteString

@OptIn(UnsafeByteStringApi::class)
class UnsafeByteStringOperationsTest {
    @Test
    fun callsInPlaceContract() {
        val byteString = "hello byte string".encodeToByteString()

        val called: Boolean
        UnsafeByteStringOperations.withByteArrayUnsafe(byteString) {
            called = true
        }
        assertTrue(called)
    }
}
