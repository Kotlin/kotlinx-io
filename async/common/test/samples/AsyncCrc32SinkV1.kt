/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async.samples

import kotlinx.io.Buffer
import kotlinx.io.async.AsyncRawSink
import kotlin.experimental.xor

@OptIn(ExperimentalUnsignedTypes::class)
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

@OptIn(ExperimentalUnsignedTypes::class)
class AsyncCrc32SinkV1(private val upstream: AsyncRawSink): AsyncRawSink {
    private val tempBuffer = Buffer()
    private val crc32Table = generateCrc32Table()
    private var crc32: UInt = 0xffffffffU

    private fun update(value: Byte) {
        val index = value.xor(crc32.toByte()).toUByte()
        crc32 = crc32Table[index.toInt()].xor(crc32.shr(8))
    }

    fun crc32(): UInt = crc32.xor(0xffffffffU)

    override suspend fun write(source: Buffer, byteCount: Long) {
        source.copyTo(tempBuffer, 0, byteCount)

        while (!tempBuffer.exhausted()) {
            update(tempBuffer.readByte())
        }

        upstream.write(source, byteCount)
    }

    override suspend fun flush() = upstream.flush()

    override suspend fun close() = upstream.close()
    override fun closeAbruptly() {
        upstream.closeAbruptly()
    }
}
