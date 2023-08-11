/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.async.samples

import kotlinx.io.Buffer
import kotlinx.io.RawSink
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
abstract class Crc32SinkCommon {
    internal val tempBuffer = Buffer()
    internal val crc32Table = generateCrc32Table()
    internal var crc32Val: UInt = 0xffffffffU

    fun update(value: Byte) {
        val index = value.xor(crc32Val.toByte()).toUByte()
        crc32Val = crc32Table[index.toInt()].xor(crc32Val.shr(8))
    }

    fun crc32(): UInt = crc32Val.xor(0xffffffffU)

    internal inline fun onWrite(source: Buffer, byteCount: Long, write: (Buffer, Long) -> Unit) {
        source.copyTo(tempBuffer, 0, byteCount)

        while (!tempBuffer.exhausted()) {
            update(tempBuffer.readByte())
        }

        write(source, byteCount)
    }
}

class AsyncCrc32Sink(private val sink: AsyncRawSink): Crc32SinkCommon(), AsyncRawSink {

    override suspend fun write(source: Buffer, byteCount: Long) {
        onWrite(source, byteCount) { buf, count ->
            sink.write(buf, count)
        }
    }

    override suspend fun flush() = sink.flush()

    override suspend fun close() = sink.close()
    override fun closeAbruptly() {
        sink.closeAbruptly()
    }
}

class BlockingCrc32Sink(private val sink: RawSink): Crc32SinkCommon(), RawSink {
    override fun write(source: Buffer, byteCount: Long) {
        onWrite(source, byteCount) { buf, count ->
            sink.write(buf, count)
        }
    }

    override fun flush() = sink.flush()

    override fun close() = sink.close()
}
