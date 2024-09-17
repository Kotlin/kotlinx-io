/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.bytestring

import kotlinx.io.bytestring.unsafe.UnsafeByteStringApi
import kotlinx.io.bytestring.unsafe.UnsafeByteStringOperations
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ByteStringConversionsTest {
    @Test
    fun int8ArrayToByteString() {
        assertTrue(Int8Array(0).toByteString().isEmpty())

        val str = Int8Array(byteArrayOf(1, 2, 3, 4).toTypedArray()).toByteString()
        assertContentEquals(byteArrayOf(1, 2, 3, 4), str.toByteArray())
    }

    @Test
    fun arrayBufferToByteString() {
        assertTrue(ArrayBuffer(0).toByteString().isEmpty())

        val str = Int8Array(byteArrayOf(1, 2, 3, 4).toTypedArray()).buffer.toByteString()
        assertContentEquals(byteArrayOf(1, 2, 3, 4), str.toByteArray())
    }

    @Test
    fun byteStringToInt8Array() {
        assertEquals(0, ByteString().toInt8Array().length)

        val array = ByteString(1, 2, 3, 4).toInt8Array()
        for (idx in 0..<3) {
            assertEquals((idx + 1).toByte(), array[idx], "idx = $idx")
        }
    }

    @Test
    fun byteStringToArrayBuffer() {
        assertEquals(0, ByteString().toArrayBuffer().byteLength)

        val buffer = ByteString(1, 2, 3, 4).toArrayBuffer()
        val array = Int8Array(buffer)
        for (idx in 0..<3) {
            assertEquals((idx + 1).toByte(), array[idx], "idx = $idx")
        }
    }

    @OptIn(UnsafeByteStringApi::class)
    @Test
    fun integrityCheck() {
        val array = Int8Array(byteArrayOf(1, 2, 3, 4).toTypedArray())
        val str = array.toByteString()

        array[0] = 42
        assertContentEquals(byteArrayOf(1, 2, 3, 4), str.toByteArray())

        val array2 = str.toInt8Array()
        UnsafeByteStringOperations.withByteArrayUnsafe(str) { it[1] = 42 }
        assertEquals(2, array2[1])
    }
}