/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

import kotlinx.io.bytestring.ByteString
import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest {
    @Test
    fun test() {
        assertEquals("ByteString(size=1 hex=42)", ByteString(0x42).toString())
    }
}
