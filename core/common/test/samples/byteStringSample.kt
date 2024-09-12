/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.samples

import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlin.test.*

class ByteStringSamples {
    @Test
    fun writeByteString() {
        val buffer = Buffer()

        buffer.write(ByteString(1, 2, 3, 4))
        assertEquals(4, buffer.size)
    }

    @Test
    fun readByteString() {
        val buffer = Buffer().also { it.write(byteArrayOf(1, 2, 3, 4, 5)) }

        assertEquals(ByteString(1, 2), buffer.readByteString(2)) // reads only two bytes
        assertEquals(ByteString(3, 4, 5), buffer.readByteString()) // reads until exhaustion
        assertTrue(buffer.exhausted())
    }

    @Test
    fun indexOfByteString() {
        val buffer = Buffer()

        assertEquals(-1, buffer.indexOf(ByteString(1, 2, 3, 4)))
        assertEquals(0, buffer.indexOf(ByteString(/* empty */)))

        buffer.writeString("Content-Type: text/plain\nContent-Length: 12\n\nhello world!")

        assertEquals(43, buffer.indexOf("\n\n".encodeToByteString()))
        assertEquals(-1, buffer.indexOf("application/json".encodeToByteString()))
    }
}
