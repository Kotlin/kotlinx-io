/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Buffer
import kotlinx.io.unsafe.UnsafeByteArrayTransformation
import kotlinx.io.UnsafeIoApi
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * A [UnsafeByteArrayTransformation] implementation for GZIP compression (RFC 1952).
 *
 * GZIP format consists of:
 * - 10-byte header
 * - DEFLATE compressed data
 * - 8-byte trailer (CRC32 + original size)
 */
@OptIn(UnsafeIoApi::class)
internal class GzipCompressor(level: Int) : UnsafeByteArrayTransformation() {

    // Use raw deflate (nowrap=true) as we manually handle GZIP header/trailer
    private val deflater = Deflater(level, true)
    private val crc32 = CRC32()

    private var headerWritten = false
    private var finishCalled = false
    private var trailerWritten = false
    private var uncompressedSize = 0L

    override fun transformTo(source: Buffer, byteCount: Long, sink: Buffer): Long {
        if (source.exhausted() && !hasPendingOutput()) return 0L

        // Write GZIP header if not yet written
        if (!headerWritten) {
            writeHeader(sink)
            headerWritten = true
        }

        return super.transformTo(source, byteCount, sink)
    }

    override fun transformIntoByteArray(
        source: ByteArray,
        sourceStartIndex: Int,
        sourceEndIndex: Int,
        sink: ByteArray,
        sinkStartIndex: Int,
        sinkEndIndex: Int
    ): TransformResult {
        val inputSize = sourceEndIndex - sourceStartIndex

        // If deflater needs input and we have some, provide it
        if (deflater.needsInput() && inputSize > 0) {
            // Update CRC32 checksum before compression
            crc32.update(source, sourceStartIndex, inputSize)
            uncompressedSize += inputSize
            deflater.setInput(source, sourceStartIndex, inputSize)
        }

        val produced = deflater.deflate(sink, sinkStartIndex, sinkEndIndex - sinkStartIndex)

        // JDK deflater copies all input at once, so consumed is either 0 or all of it
        val consumed = if (deflater.needsInput()) inputSize else 0

        return TransformResult(consumed, produced)
    }

    override fun hasPendingOutput(): Boolean = !deflater.needsInput()

    override fun finalizeTo(sink: Buffer) {
        if (trailerWritten) return

        // Ensure header is written even if no data was compressed
        if (!headerWritten) {
            writeHeader(sink)
            headerWritten = true
        }

        // Finalize deflate output
        super.finalizeTo(sink)

        // Write GZIP trailer
        writeTrailer(sink)
        trailerWritten = true
    }

    override fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): Int {
        if (!finishCalled) {
            deflater.finish()
            finishCalled = true
        }
        if (deflater.finished()) return -1
        return deflater.deflate(sink, startIndex, endIndex - startIndex)
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
}
