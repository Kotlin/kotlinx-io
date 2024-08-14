package org.example

import kotlinx.io.bytestring.ByteString
import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest {
    @Test
    fun test() {
        assertEquals("ByteString(size=1 hex=42)", ByteString(0x42).toString())
    }
}
