/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Buffer
import kotlinx.io.ByteArrayTransformation
import kotlinx.io.UnsafeIoApi
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * A [ByteArrayTransformation] implementation for GZIP compression (RFC 1952).
 *
 * GZIP format consists of:
 * - 10-byte header
 * - DEFLATE compressed data
 * - 8-byte trailer (CRC32 + original size)
 */
@OptIn(UnsafeIoApi::class)
internal class GzipCompressor(level: Int) : ByteArrayTransformation() {

    // Use raw deflate (nowrap=true) as we manually handle GZIP header/trailer
    private val deflater = Deflater(level, true)
    private val crc32 = CRC32()
    private val outputArray = ByteArray(BUFFER_SIZE)

    private var headerWritten = false
    private var finished = false
    private var uncompressedSize = 0L

    override fun transformAtMostTo(source: Buffer, sink: Buffer, byteCount: Long): Long {
        if (source.exhausted()) return 0L

        // Write GZIP header if not yet written
        if (!headerWritten) {
            writeHeader(sink)
            headerWritten = true
        }

        // Call parent implementation which will use transformToByteArray
        return super.transformAtMostTo(source, sink, byteCount)
    }

    override fun transformToByteArray(source: ByteArray, startIndex: Int, endIndex: Int): ByteArray {
        val inputSize = endIndex - startIndex
        if (inputSize == 0) return ByteArray(0)

        // Update CRC32 checksum
        crc32.update(source, startIndex, inputSize)
        uncompressedSize += inputSize

        // Compress the data
        deflater.setInput(source, startIndex, inputSize)

        // Collect all output from deflater
        val result = mutableListOf<ByteArray>()
        var totalSize = 0

        while (!deflater.needsInput()) {
            val count = deflater.deflate(outputArray)
            if (count > 0) {
                result.add(outputArray.copyOf(count))
                totalSize += count
            } else {
                break
            }
        }

        return combineChunks(result, totalSize)
    }

    override fun finalize(sink: Buffer) {
        if (finished) return

        // Ensure header is written even if no data was compressed
        if (!headerWritten) {
            writeHeader(sink)
            headerWritten = true
        }

        // Call parent finalize which will call finalizeToByteArray
        super.finalize(sink)

        // Write GZIP trailer
        writeTrailer(sink)

        finished = true
    }

    override fun finalizeToByteArray(): ByteArray {
        // Finish deflate
        deflater.finish()

        // Collect all remaining output
        val result = mutableListOf<ByteArray>()
        var totalSize = 0

        while (!deflater.finished()) {
            val count = deflater.deflate(outputArray)
            if (count > 0) {
                result.add(outputArray.copyOf(count))
                totalSize += count
            } else if (deflater.finished()) {
                break
            }
        }

        return combineChunks(result, totalSize)
    }

    override fun close() {
        deflater.end()
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
