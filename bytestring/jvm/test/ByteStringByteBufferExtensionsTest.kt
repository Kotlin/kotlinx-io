/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.bytestring

import org.junit.jupiter.api.Test
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Suppress("RETURN_VALUE_NOT_USED")
public class ByteStringByteBufferExtensionsTest {
    @Test
    fun asReadOnlyByteBuffer() {
        val buffer = ByteString(1, 2, 3, 4).asReadOnlyByteBuffer()

        assertTrue(buffer.isReadOnly)
        assertEquals(4, buffer.remaining())

        ByteArray(4).let {
            buffer.get(it)
            assertContentEquals(byteArrayOf(1, 2, 3, 4), it)
        }
    }

    @Test
    fun getByteString() {
        val bb = ByteBuffer.allocate(8)
        bb.put(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
        bb.flip()

        assertEquals(ByteString(1, 2, 3, 4, 5, 6, 7, 8), bb.getByteString())
        bb.flip()

        assertEquals(ByteString(1, 2, 3, 4), bb.getByteString(length = 4))
        assertEquals(ByteString(), bb.getByteString(length = 0))
        assertFailsWith<IndexOutOfBoundsException> { bb.getByteString(length = -1) }
        val p = bb.position()
        assertFailsWith<IndexOutOfBoundsException> { bb.getByteString(length = 5) }
        assertEquals(p, bb.position())
        bb.clear()

        assertEquals(ByteString(1, 2, 3, 4, 5, 6, 7, 8), bb.getByteString(at = 0, length = 8))
        assertEquals(0, bb.position())

        assertEquals(ByteString(2, 3, 4, 5), bb.getByteString(at = 1, length = 4))
        assertEquals(0, bb.position())

        assertFailsWith<IndexOutOfBoundsException> { bb.getByteString(at = -1, length = 8) }
        assertFailsWith<IndexOutOfBoundsException> { bb.getByteString(at = 9, length = 1) }
        assertFailsWith<IndexOutOfBoundsException> { bb.getByteString(at = 7, length = 2) }
        assertFailsWith<IndexOutOfBoundsException> { bb.getByteString(at = 0, length = -1) }
    }

    @Test
    fun putString() {
        val bb = ByteBuffer.allocate(8)
        val string = ByteString(1, 2, 3, 4, 5, 6, 7, 8)
        val shortString = ByteString(-1, -2, -3)

        bb.putByteString(string)
        assertEquals(8, bb.position())
        bb.flip()
        ByteArray(8).let {
            bb.get(it)
            assertContentEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8), it)
        }

        bb.clear()
        bb.position(1)
        assertFailsWith<BufferOverflowException> { bb.putByteString(string) }
        assertEquals(1, bb.position())

        bb.putByteString(at = 0, string = shortString)
        bb.putByteString(at = 5, string = shortString)
        assertEquals(1, bb.position())
        bb.clear()
        ByteArray(8).let {
            bb.get(it)
            assertContentEquals(byteArrayOf(-1, -2, -3, 4, 5, -1, -2, -3), it)
        }

        assertFailsWith<IndexOutOfBoundsException> { bb.putByteString(at = 7, string = shortString) }
        assertFailsWith<IndexOutOfBoundsException> { bb.putByteString(at = -1, string = string) }
        assertFailsWith<IndexOutOfBoundsException> { bb.putByteString(at = 8, string = string) }
        assertFailsWith<ReadOnlyBufferException> {
            bb.asReadOnlyBuffer().putByteString(string)
        }
        assertFailsWith<ReadOnlyBufferException> {
            bb.asReadOnlyBuffer().putByteString(at = 0, string = string)
        }
    }
}
