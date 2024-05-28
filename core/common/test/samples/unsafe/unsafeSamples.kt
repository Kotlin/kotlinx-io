/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.samples.unsafe

import kotlinx.io.Buffer
import kotlinx.io.UnsafeIoApi
import kotlinx.io.readString
import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class UnsafeBufferOperationsSamples {
    @OptIn(UnsafeIoApi::class)
    @Test
    fun writeByteArrayToTail() {
        fun Buffer.writeRandomBytes(byteCount: Int) {
            require(byteCount > 0) { "byteCount should be positive. Was: $byteCount" }
            var remaining = byteCount
            while (remaining > 0) {
                UnsafeBufferOperations.writeToTail(this, 1) { data, startIndex, endIndex ->
                    // data's slice from startIndex to endIndex is available for writing,
                    // but that slice could be much larger than what remained to write.
                    val correctedEndIndex = min(endIndex, startIndex + remaining)
                    // write random bytes
                    Random.Default.nextBytes(data, startIndex, correctedEndIndex)
                    // number of bytes written
                    val written = correctedEndIndex - startIndex
                    remaining -= written
                    // that many bytes will be committed to the buffer
                    written
                }
            }
        }

        val buffer = Buffer()

        buffer.writeRandomBytes(42)
        assertEquals(42L, buffer.size)

        buffer.writeRandomBytes(10000)
        assertEquals(10042L, buffer.size)
    }

    @OptIn(ExperimentalEncodingApi::class, UnsafeIoApi::class)
    @Test
    fun moveToTail() {
        fun Buffer.writeBase64(data: ByteArray, encoder: Base64 = Base64.Default) {
            UnsafeBufferOperations.moveToTail(this, encoder.encodeToByteArray(data))
        }

        val buffer = Buffer()
        buffer.writeBase64(byteArrayOf(-1, 0, -2, 0))

        assertEquals("/wD+AA==", buffer.readString())
    }

    @OptIn(UnsafeIoApi::class)
    @Test
    fun readByteArrayFromHead() {
        // see https://en.wikipedia.org/wiki/LEB128#Unsigned_LEB128
        fun Buffer.readULeb128(): ULong {
            var shift = 0
            var result = 0L
            var complete = false

            while (!complete) {
                require(1) // check if we still have something to read

                UnsafeBufferOperations.readFromHead(this) { data, startOffset, endOffset ->
                    var offset = startOffset
                    do {
                        val b = data[offset++]
                        result = result.or(0x7fL.and(b.toLong()).shl(shift))
                        shift += 7
                        complete = b >= 0 // we're done if the most significant bit was not set
                    } while (!complete && offset < endOffset)
                    // return the number of consumed bytes
                    offset - startOffset
                }
            }

            return result.toULong()
        }

        val buffer = Buffer().apply {
            write(byteArrayOf(0)) // 0
            write(byteArrayOf(0xed.toByte(), 0x9b.toByte(), 0xb0.toByte(), 0x6f)) // dec0ded
            write(byteArrayOf(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1)) // ffffffffffffffff
        }
        assertEquals(0U, buffer.readULeb128())
        assertEquals(0xDEC0DEDu, buffer.readULeb128())
        assertEquals((-1).toULong(), buffer.readULeb128())
    }
}
