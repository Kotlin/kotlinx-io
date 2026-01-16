/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Buffer
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * A [Compressor] implementation for GZIP format (RFC 1952).
 *
 * GZIP format consists of:
 * - 10-byte header
 * - DEFLATE compressed data
 * - 8-byte trailer (CRC32 + original size)
 */
internal class GzipCompressor(level: Int) : Compressor {

    // Use raw deflate (nowrap=true) as we manually handle GZIP header/trailer
    private val deflater = Deflater(level, true)
    private val crc32 = CRC32()
    private val outputArray = ByteArray(BUFFER_SIZE)

    private var headerWritten = false
    private var finished = false
    private var uncompressedSize = 0L

    @OptIn(UnsafeIoApi::class)
    override fun compress(source: Buffer, sink: Buffer) {
        // Write GZIP header if not yet written
        if (!headerWritten) {
            writeHeader(sink)
            headerWritten = true
        }

        // Feed data to the deflater and update CRC
        while (!source.exhausted()) {
            val _ = UnsafeBufferOperations.readFromHead(source) { data, pos, limit ->
                val count = limit - pos

                // Update CRC32 checksum
                crc32.update(data, pos, count)
                uncompressedSize += count

                // Compress the data
                deflater.setInput(data, pos, count)
                deflateToBuffer(sink)

                count
            }
        }
    }

    override fun finish(sink: Buffer) {
        if (finished) return

        // Ensure header is written even if no data was compressed
        if (!headerWritten) {
            writeHeader(sink)
            headerWritten = true
        }

        // Finish deflate
        deflater.finish()
        while (!deflater.finished()) {
            deflateToBuffer(sink)
        }

        // Write GZIP trailer
        writeTrailer(sink)

        finished = true
    }

    override fun close() {
        deflater.end()
    }

    private fun deflateToBuffer(sink: Buffer) {
        while (true) {
            val count = deflater.deflate(outputArray)
            if (count > 0) {
                sink.write(outputArray, 0, count)
            }
            if (count < outputArray.size) {
                break
            }
        }
    }

    private fun writeHeader(sink: Buffer) {
        // GZIP header (10 bytes minimum):
        // - Magic number: 0x1f 0x8b
        // - Compression method: 8 (deflate)
        // - Flags: 0
        // - Modification time: 0 (4 bytes)
        // - Extra flags: 0
        // - Operating system: 255 (unknown)
        sink.writeByte(0x1f.toByte())
        sink.writeByte(0x8b.toByte())
        sink.writeByte(8) // Compression method: deflate
        sink.writeByte(0) // Flags
        sink.writeInt(0)  // Modification time (4 bytes, little-endian but we use 0)
        sink.writeByte(0) // Extra flags
        sink.writeByte(255.toByte()) // OS: unknown
    }

    private fun writeTrailer(sink: Buffer) {
        // GZIP trailer (8 bytes):
        // - CRC32 (4 bytes, little-endian)
        // - Original size mod 2^32 (4 bytes, little-endian)
        val crcValue = crc32.value.toInt()
        val sizeValue = (uncompressedSize and 0xFFFFFFFFL).toInt()

        // Write in little-endian order
        sink.writeByte((crcValue and 0xFF).toByte())
        sink.writeByte((crcValue shr 8 and 0xFF).toByte())
        sink.writeByte((crcValue shr 16 and 0xFF).toByte())
        sink.writeByte((crcValue shr 24 and 0xFF).toByte())

        sink.writeByte((sizeValue and 0xFF).toByte())
        sink.writeByte((sizeValue shr 8 and 0xFF).toByte())
        sink.writeByte((sizeValue shr 16 and 0xFF).toByte())
        sink.writeByte((sizeValue shr 24 and 0xFF).toByte())
    }

    private companion object {
        private const val BUFFER_SIZE = 8192
    }
}
