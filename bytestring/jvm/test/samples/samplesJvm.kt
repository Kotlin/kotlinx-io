/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.bytestring.samples

import kotlinx.io.bytestring.*
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import kotlin.test.*

public class ByteStringSamplesJvm {
    @Test
    fun toReadOnlyByteBuffer() {
        val str = "Hello World".encodeToByteString()
        val buffer = str.asReadOnlyByteBuffer()

        assertEquals(11, buffer.remaining())
        assertEquals(0x48656c6c, buffer.getInt())

        buffer.flip()
        assertFailsWith<ReadOnlyBufferException> { buffer.put(42) }
    }

    @Test
    fun getByteStringFromBuffer() {
        val buffer = ByteBuffer.wrap("Hello World".encodeToByteArray())

        // Consume the whole buffer
        val byteString = buffer.getByteString()
        assertEquals(0, buffer.remaining())
        assertEquals("Hello World".encodeToByteString(), byteString)

        // Reset the buffer
        buffer.flip()
        // Consume only first 5 bytes from the buffer
        assertEquals("Hello".encodeToByteString(), buffer.getByteString(length = 5))
    }

    @Test
    fun getByteStringFromBufferAbsolute() {
        val buffer = ByteBuffer.wrap("Hello World".encodeToByteArray())

        // Read 2 bytes starting from offset 6
        val byteString = buffer.getByteString(at = 6, length = 2)
        // Buffer's position is not affected
        assertEquals(11, buffer.remaining())
        assertEquals(byteString, "Wo".encodeToByteString())
    }

    @Test
    fun putByteStringToBuffer() {
        val buffer = ByteBuffer.allocate(32)
        val byteString = ByteString(0x66, 0xdb.toByte(), 0x11, 0x50)

        // Putting a ByteString into a buffer will advance its position
        buffer.putByteString(byteString)
        assertEquals(4, buffer.position())

        buffer.flip()
        assertEquals(1725632848, buffer.getInt())
    }

    @Test
    fun putByteStringToBufferAbsolute() {
        val buffer = ByteBuffer.allocate(8)
        val byteString = ByteString(0x78, 0x5e)

        // Putting a ByteString into a buffer using an absolute offset
        // won't change buffer's position.
        buffer.putByteString(at = 3, string = byteString)
        assertEquals(0, buffer.position())
        assertEquals(8, buffer.remaining())

        assertEquals(0x000000785e000000L, buffer.getLong())
    }
}
