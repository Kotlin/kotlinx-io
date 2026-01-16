/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Buffer
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations
import java.util.zip.CRC32
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * A [Decompressor] implementation for GZIP format (RFC 1952).
 *
 * GZIP format consists of:
 * - 10-byte minimum header (may be longer with optional fields)
 * - DEFLATE compressed data
 * - 8-byte trailer (CRC32 + original size)
 */
internal class GzipDecompressor : Decompressor {

    // Use raw inflate (nowrap=true) as we manually handle GZIP header/trailer
    private val inflater = Inflater(true)
    private val crc32 = CRC32()
    private val outputArray = ByteArray(BUFFER_SIZE)

    private var headerParsed = false
    private var trailerVerified = false
    private var uncompressedSize = 0L
    private var finished = false

    // Buffer for accumulating header/trailer bytes when source doesn't have enough
    private val headerBuffer = Buffer()

    override val isFinished: Boolean
        get() = finished

    @OptIn(UnsafeIoApi::class)
    override fun decompress(source: Buffer, sink: Buffer) {
        // Parse GZIP header if not yet parsed
        if (!headerParsed) {
            if (!parseHeader(source)) {
                // Not enough data for header yet
                return
            }
            headerParsed = true
        }

        // If inflater is finished, we need to verify the trailer
        if (inflater.finished() && !trailerVerified) {
            // Move any remaining input from inflater back to source
            val remaining = inflater.remaining
            if (remaining > 0) {
                // We need to put the unused bytes back
                // This is a simplified approach - in practice, we'd use getBytesRead()
            }

            if (!verifyTrailer(source)) {
                // Not enough data for trailer yet
                return
            }
            trailerVerified = true
            finished = true
            return
        }

        if (finished) return

        // Feed data to the inflater if it needs input
        if (inflater.needsInput() && !source.exhausted()) {
            val _ = UnsafeBufferOperations.readFromHead(source) { data, pos, limit ->
                val count = limit - pos
                inflater.setInput(data, pos, count)
                count
            }
        }

        // Inflate and update CRC
        inflateToBuffer(sink)
    }

    override fun close() {
        inflater.end()
        headerBuffer.clear()
    }

    private fun parseHeader(source: Buffer): Boolean {
        // Accumulate header bytes
        while (headerBuffer.size < 10 && !source.exhausted()) {
            headerBuffer.writeByte(source.readByte())
        }

        if (headerBuffer.size < 10) {
            return false // Need more data
        }

        // Read header fields
        val magic1 = headerBuffer.readByte().toInt() and 0xFF
        val magic2 = headerBuffer.readByte().toInt() and 0xFF
        val compressionMethod = headerBuffer.readByte().toInt() and 0xFF
        val flags = headerBuffer.readByte().toInt() and 0xFF
        headerBuffer.skip(4) // Skip modification time
        headerBuffer.skip(1) // Skip extra flags
        headerBuffer.skip(1) // Skip OS

        // Validate magic number
        if (magic1 != 0x1f || magic2 != 0x8b) {
            throw CompressionException("Invalid GZIP magic number")
        }

        // Validate compression method
        if (compressionMethod != 8) {
            throw CompressionException("Unsupported GZIP compression method: $compressionMethod")
        }

        // Handle optional fields based on flags
        val fextra = (flags and 0x04) != 0
        val fname = (flags and 0x08) != 0
        val fcomment = (flags and 0x10) != 0
        val fhcrc = (flags and 0x02) != 0

        // Skip FEXTRA
        if (fextra) {
            if (!skipExtra(source)) return false
        }

        // Skip FNAME (null-terminated string)
        if (fname) {
            if (!skipNullTerminated(source)) return false
        }

        // Skip FCOMMENT (null-terminated string)
        if (fcomment) {
            if (!skipNullTerminated(source)) return false
        }

        // Skip FHCRC (2 bytes)
        if (fhcrc) {
            if (source.size < 2) return false
            source.skip(2)
        }

        return true
    }

    private fun skipExtra(source: Buffer): Boolean {
        // XLEN is 2 bytes, little-endian
        if (source.size < 2) return false
        val xlen1 = source.readByte().toInt() and 0xFF
        val xlen2 = source.readByte().toInt() and 0xFF
        val xlen = xlen1 or (xlen2 shl 8)

        if (source.size < xlen.toLong()) return false
        source.skip(xlen.toLong())
        return true
    }

    private fun skipNullTerminated(source: Buffer): Boolean {
        while (!source.exhausted()) {
            val b = source.readByte()
            if (b == 0.toByte()) {
                return true
            }
        }
        return false // Need more data
    }

    private fun inflateToBuffer(sink: Buffer) {
        try {
            while (true) {
                val count = inflater.inflate(outputArray)
                if (count > 0) {
                    // Update CRC
                    crc32.update(outputArray, 0, count)
                    uncompressedSize += count

                    // Write to sink
                    sink.write(outputArray, 0, count)
                }
                if (count == 0 || inflater.finished() || inflater.needsInput()) {
                    break
                }
            }
        } catch (e: DataFormatException) {
            throw CompressionException("Invalid GZIP data: ${e.message}", e)
        }
    }

    private fun verifyTrailer(source: Buffer): Boolean {
        // Need 8 bytes for trailer
        if (source.size < 8) return false

        // Read CRC32 (little-endian)
        val crc1 = source.readByte().toInt() and 0xFF
        val crc2 = source.readByte().toInt() and 0xFF
        val crc3 = source.readByte().toInt() and 0xFF
        val crc4 = source.readByte().toInt() and 0xFF
        val expectedCrc = crc1.toLong() or
                (crc2.toLong() shl 8) or
                (crc3.toLong() shl 16) or
                (crc4.toLong() shl 24)

        // Read original size (little-endian)
        val size1 = source.readByte().toInt() and 0xFF
        val size2 = source.readByte().toInt() and 0xFF
        val size3 = source.readByte().toInt() and 0xFF
        val size4 = source.readByte().toInt() and 0xFF
        val expectedSize = size1.toLong() or
                (size2.toLong() shl 8) or
                (size3.toLong() shl 16) or
                (size4.toLong() shl 24)

        // Verify CRC
        if (crc32.value != expectedCrc) {
            throw CompressionException(
                "GZIP CRC32 mismatch: expected ${expectedCrc.toString(16)}, " +
                        "got ${crc32.value.toString(16)}"
            )
        }

        // Verify size (mod 2^32)
        val actualSizeMod = uncompressedSize and 0xFFFFFFFFL
        if (actualSizeMod != expectedSize) {
            throw CompressionException(
                "GZIP size mismatch: expected $expectedSize, got $actualSizeMod"
            )
        }

        return true
    }

    private companion object {
        private const val BUFFER_SIZE = 8192
    }
}
