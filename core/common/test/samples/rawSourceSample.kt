/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.samples

import kotlinx.io.*
import kotlin.test.*

class RC4SourceSample {
    @Test
    fun rc4() {
        /**
         * Source decrypting all the data read from the downstream using RC4 algorithm.
         *
         * See https://en.wikipedia.org/wiki/RC4 for more information about the cypher.
         *
         * Implementation of RC4 stream cypher based on http://cypherpunks.venona.com/archive/1994/09/msg00304.html
         */
        @OptIn(ExperimentalUnsignedTypes::class)
        class RC4DecryptingSource(private val downstream: RawSource, key: String): RawSource {
            private val buffer = Buffer()
            private val key = RC4Key(key)

            override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
                val bytesRead = downstream.readAtMostTo(buffer, byteCount)
                if (bytesRead == -1L) {
                    return -1L
                }

                while (!buffer.exhausted()) {
                    val byte = buffer.readByte()
                    sink.writeByte(byte.xor(key.nextByte()))
                }

                return bytesRead
            }

            override fun close() = downstream.close()

            private inner class RC4Key(key: String) {
                private var keyState: UByteArray
                private var keyX: Int = 0
                private var keyY: Int = 0

                init {
                    require(key.isNotEmpty()) { "Key could not be empty" }
                    val keyBytes = key.encodeToByteArray()
                    keyState = UByteArray(256) { it.toUByte() }
                    var index1 = 0
                    var index2 = 0

                    for (idx in keyState.indices) {
                        index2 = (keyBytes[index1] + keyState[idx].toInt() + index2) % 256
                        swapStateBytes(idx, index2)
                        index1 = (index1 + 1) % keyBytes.size
                    }
                }

                fun nextByte(): Byte {
                    keyX = (keyX + 1) % 256
                    keyY = (keyState[keyX].toInt() + keyY) % 256
                    swapStateBytes(keyX, keyY)
                    val idx = (keyState[keyX] + keyState[keyY]) % 256U
                    return keyState[idx.toInt()].toByte()
                }

                private fun swapStateBytes(x: Int, y: Int) {
                    val tmp = keyState[x]
                    keyState[x] = keyState[y]
                    keyState[y] = tmp
                }
            }
        }

        val key = "key"
        val source = Buffer().also { it.write(byteArrayOf(0x58, 0x09, 0x57, 0x9fU.toByte(), 0x41, 0xfbU.toByte())) }
        val rc4Source = RC4DecryptingSource(source, key).buffered()

        assertEquals("Secret", rc4Source.readString())
    }
}
