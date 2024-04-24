/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.samples

import kotlinx.io.*
import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlinx.io.unsafe.withData
import kotlin.test.Test
import kotlin.test.assertEquals

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

/**
 * Sink calculating CRC-32 code for all the data written to it and sending this data to the upstream afterward.
 * The CRC-32 value could be obtained using [crc32] method.
 *
 * See https://en.wikipedia.org/wiki/Cyclic_redundancy_check for more information about CRC-32.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class CRC32Sink(private val upstream: RawSink) : RawSink {
    private val tempBuffer = Buffer()
    private val crc32Table = generateCrc32Table()
    private var crc32: UInt = 0xffffffffU

    private fun update(value: Byte) {
        val index = value.xor(crc32.toByte()).toUByte()
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
}

@OptIn(ExperimentalUnsignedTypes::class)
private val crc32Table = generateCrc32Table()

@OptIn(UnsafeIoApi::class, ExperimentalUnsignedTypes::class)
fun Buffer.crc32UsingGetUnchecked(): UInt {
    var crc32 = 0xffffffffU
    // iterate over all segments
    UnsafeBufferOperations.iterate(this) { ctx, head ->
        var currentSegment = head
        while (currentSegment != null) {
            // Get data from a segment
            for (offset in 0..<currentSegment.size) {
                val index = ctx.getUnchecked(currentSegment, offset).xor(crc32.toByte()).toUByte()
                crc32 = crc32Table[index.toInt()].xor(crc32.shr(8))
            }
            currentSegment = ctx.next(currentSegment)
        }
    }
    return crc32.xor(0xffffffffU)
}

@OptIn(UnsafeIoApi::class, ExperimentalUnsignedTypes::class)

class CRC32Source(private val underlyingSource: RawSource) : RawSource {
    private val crc32Table = generateCrc32Table()
    private var crc32 = 0xffffffffU

    fun crc32(): UInt = crc32.xor(0xffffffffU)

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        // The buffer may already contain some data,
        // let's save the size to update crc32 with only bytes read from the underlyingSource.
        val originalSize = sink.size

        val bytesRead = underlyingSource.readAtMostTo(sink, byteCount)
        // If there is no new data, we're done here.
        if (bytesRead <= 0) {
            return bytesRead
        }

        // Let's iterate over data that we just read into the sink.
        // For that, we need to find the segment containing data at the offset equal to originalSize.
        UnsafeBufferOperations.iterate(sink, originalSize) { ctx, segment, offset ->
            // The offset value corresponds to the offset at the beginning of the segment,
            // we need to subtract originalSize to find the relative offset inside the first segment.
            var initialOffset = (originalSize - offset).toInt()
            var currentSegment = segment
            // Now, we can iterate over all data we just read and update the crc32 with it.
            while (currentSegment != null) {
                for (segmentOffset in initialOffset until currentSegment.size) {
                    // Read the byte at segmentOffset
                    val index = ctx.getUnchecked(currentSegment, segmentOffset).xor(crc32.toByte()).toUByte()
                    crc32 = crc32Table[index.toInt()].xor(crc32.shr(8))
                }
                // All later segments should be scanned fully, meaning that the initial offset is always 0.
                initialOffset = 0
                // Advance to the next segment.
                currentSegment = ctx.next(currentSegment)
            }
        }
        return bytesRead
    }

    override fun close() {
        underlyingSource.close()
    }
}

class Crc32Sample {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun crc32() {
        val crc32Sink = CRC32Sink(discardingSink())

        crc32Sink.buffered().use {
            it.writeString("hello crc32")
        }

        assertEquals(0x9896d398U, crc32Sink.crc32())
    }

    @Test
    fun crc32Read() {
        val source = Buffer().also { it.writeString("hello crc32") }
        var crc32Source = CRC32Source(source)

        assertEquals("hello crc32", crc32Source.buffered().readString())
        assertEquals(0x9896d398U, crc32Source.crc32())

        // Let's do it again, but this time a buffer we will be reading into will contain
        // some data.
        source.writeString("hello crc32")
        val buffer = Buffer().also { it.write(ByteArray(123)) }
        crc32Source = CRC32Source(source)
        crc32Source.readAtMostTo(buffer, 1024)
        assertEquals(0x9896d398U, crc32Source.crc32())
    }

    @Test
    fun crc32Unsafe() {
        // Compute a CRC32 checksum over Buffer's content without consuming it
        @OptIn(UnsafeIoApi::class, ExperimentalUnsignedTypes::class)
        fun Buffer.crc32(): UInt {
            var crc32 = 0xffffffffU
            // Iterate over segments starting from buffer's head
            UnsafeBufferOperations.iterate(this) { ctx, head ->
                var currentSegment = head
                // If a current segment is null, it means we ran out of segments.
                while (currentSegment != null) {
                    // Get data from a segment
                    ctx.withData(currentSegment) { data, startIndex, endIndex ->
                        for (idx in startIndex..<endIndex) {
                            // Update crc32
                            val index = data[idx].xor(crc32.toByte()).toUByte()
                            crc32 = crc32Table[index.toInt()].xor(crc32.shr(8))
                        }
                    }
                    // Advance to the next segment
                    currentSegment = ctx.next(currentSegment)
                }
            }
            return crc32.xor(0xffffffffU)
        }

        val buffer = Buffer().also { it.writeString("hello crc32") }
        assertEquals(0x9896d398U, buffer.crc32())
    }

    @Test
    fun crc32GetUnchecked() {
        val buffer = Buffer().also { it.writeString("hello crc32") }

        assertEquals(0x9896d398U, buffer.crc32UsingGetUnchecked())
    }
}
