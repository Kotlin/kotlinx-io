package org.example

import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteArray
import kotlinx.io.readByteString
import kotlinx.io.write
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class SmokeTest {
    @Test
    fun testCore() {
        val buffer = Buffer()
        buffer.writeLong(0)
        assertContentEquals(ByteArray(8), buffer.readByteArray())
    }

    @Test
    fun testByteString() {
        val byteString = ByteString(0x42)
        val buffer = Buffer()
        buffer.write(byteString)

        assertEquals(ByteString(0x42), buffer.readByteString())
    }
}
