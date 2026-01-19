/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

internal actual fun crc32(): Processor<Long> = Crc32Processor()

@OptIn(ExperimentalUnsignedTypes::class)
private class Crc32Processor : Processor<Long> {
    private var crc: UInt = 0u

    override fun process(source: Buffer, byteCount: Long) {
        require(byteCount >= 0) { "byteCount: $byteCount" }

        if (byteCount == 0L || source.exhausted()) return

        val toProcess = minOf(byteCount, source.size)
        for (i in 0 until toProcess) {
            val byte = source[i]
            val index = (crc xor byte.toUInt()).toUByte()
            crc = CRC32_TABLE[index.toInt()] xor (crc shr 8)
        }
    }

    override fun compute(): Long = crc.toLong()

    override fun close() {}

    private companion object {
        // Pre-computed CRC32 lookup table (IEEE 802.3 polynomial)
        private val CRC32_TABLE: UIntArray = UIntArray(256) { i ->
            var crc = i.toUInt()
            repeat(8) {
                crc = if (crc and 1u != 0u) {
                    (crc shr 1) xor 0xEDB88320u
                } else {
                    crc shr 1
                }
            }
            crc
        }
    }
}
