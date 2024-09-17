/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.samples

import kotlinx.io.*
import kotlin.test.*

class Crc32Sample {
    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun crc32() {
        /**
         * Sink calculating CRC-32 code for all the data written to it and sending this data to the upstream afterward.
         * The CRC-32 value could be obtained using [crc32] method.
         *
         * See https://en.wikipedia.org/wiki/Cyclic_redundancy_check for more information about CRC-32.
         */
        class CRC32Sink(private val upstream: RawSink): RawSink {
            private val tempBuffer = Buffer()
            private val crc32Table = generateCrc32Table()
            private var crc32: UInt = 0xffffffffU

            private fun update(value: Byte) {
                val index = value.toUInt().xor(crc32).toUByte()
                crc32 = crc32Table[index.toInt()].xor(crc32.shr(8))
            }

            fun crc32(): UInt = crc32.xor(0xffffffffU)

            override fun write(source: Buffer, byteCount: Long) {
                source.copyTo(tempBuffer, 0, byteCount)

                while (!tempBuffer.exhausted()) {
                    update(tempBuffer.readByte())
                }

                upstream.write(source, byteCount)
            }

            override fun flush() = upstream.flush()

            override fun close() = upstream.close()

            private fun generateCrc32Table(): UIntArray {
                val table = UIntArray(256)

                for (idx in table.indices) {
                    table[idx] = idx.toUInt()
                    for (bit in 8 downTo 1) {
                        table[idx] = if (table[idx] % 2U == 0U) {
                            table[idx].shr(1)
                        } else {
                            table[idx].shr(1).xor(0xEDB88320U)
                        }
                    }
                }

                return table
            }
        }

        val crc32Sink = CRC32Sink(discardingSink())

        crc32Sink.buffered().use {
            it.writeString("hello crc32")
        }

        assertEquals(0x9896d398U, crc32Sink.crc32())
    }
}
