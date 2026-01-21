/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.*
import kotlinx.io.unsafe.UnsafeByteArrayTransformation
import java.util.zip.CRC32
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * A [kotlinx.io.unsafe.UnsafeByteArrayTransformation] implementation for GZIP decompression (RFC 1952).
 *
 * GZIP format consists of:
 * - 10-byte minimum header (may be longer with optional fields)
 * - DEFLATE compressed data
 * - 8-byte trailer (CRC32 + original size)
 */
@OptIn(UnsafeIoApi::class)
internal class GzipDecompressor : UnsafeByteArrayTransformation() {

    // Use raw inflate (nowrap=true) as we manually handle GZIP header/trailer
    private val inflater = Inflater(true)
    private val crc32 = CRC32()

    private var headerParsed = false
    private var trailerVerified = false
    private var uncompressedSize = 0L

    // Buffer for accumulating header bytes when source doesn't have enough
    private val headerBuffer = Buffer()

    // Buffer for storing remaining bytes after inflater finishes (for trailer verification)
    private val remainingBuffer = Buffer()

    override fun transformTo(source: Buffer, byteCount: Long, sink: Buffer): Long {
        if (source.exhausted() && inflater.needsInput()) return 0L

        // Parse GZIP header if not yet parsed
        if (!headerParsed) {
            if (!parseHeader(source)) {
                // Not enough data for header yet
                return 0L
            }
            headerParsed = true
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
        // If already finished, ignore any further input
        if (inflater.finished()) {
            return TransformResult.done()
        }

        val inputSize = sourceEndIndex - sourceStartIndex

        // If inflater needs input and we have some, provide it
        if (inflater.needsInput() && inputSize > 0) {
            inflater.setInput(source, sourceStartIndex, inputSize)
        }

        val produced = try {
            inflater.inflate(sink, sinkStartIndex, sinkEndIndex - sinkStartIndex)
        } catch (e: DataFormatException) {
            throw IOException("Invalid GZIP data: ${e.message}", e)
        }

        if (produced > 0) {
            crc32.update(sink, sinkStartIndex, produced)
            uncompressedSize += produced
        }

        // Store remaining bytes (trailer) if inflater finished
        if (inflater.finished()) {
            val remaining = inflater.remaining
            if (remaining > 0) {
                val remainingStart = sourceStartIndex + inputSize - remaining
                remainingBuffer.write(source, remainingStart, remainingStart + remaining)
            }
        }

        // JDK inflater copies all input at once, so consumed is either 0 or all of it
        val consumed = if (inflater.needsInput() || inflater.finished()) inputSize else 0

        return TransformResult.ok(consumed, produced)
    }

    override fun transformFinalIntoByteArray(
        source: ByteArray,
        sourceStartIndex: Int,
        sourceEndIndex: Int,
        sink: ByteArray,
        sinkStartIndex: Int,
        sinkEndIndex: Int
    ): TransformResult {
        var inputStart = sourceStartIndex
        val inputSize = sourceEndIndex - sourceStartIndex

        // Parse GZIP header if not yet parsed
        if (!headerParsed && inputSize > 0) {
            val headerParseResult = parseHeaderFromByteArray(source, sourceStartIndex, sourceEndIndex)
            if (headerParseResult < 0) {
                throw IOException("Incomplete or invalid GZIP header")
            }
            headerParsed = true
            inputStart = headerParseResult
        }

        val adjustedInputSize = sourceEndIndex - inputStart

        if (!inflater.finished()) {
            if (inflater.needsInput() && adjustedInputSize > 0) {
                inflater.setInput(source, inputStart, adjustedInputSize)
            }

            val produced = try {
                inflater.inflate(sink, sinkStartIndex, sinkEndIndex - sinkStartIndex)
            } catch (e: DataFormatException) {
                throw IOException("Invalid GZIP data: ${e.message}", e)
            }

            if (produced > 0) {
                crc32.update(sink, sinkStartIndex, produced)
                uncompressedSize += produced
            }

            // Store remaining bytes (trailer) if inflater finished
            if (inflater.finished()) {
                val remaining = inflater.remaining
                if (remaining > 0) {
                    val remainingStart = inputStart + adjustedInputSize - remaining
                    remainingBuffer.write(source, remainingStart, remainingStart + remaining)
                }
            }

            val consumed = if (inflater.needsInput() || inflater.finished()) inputSize else 0

            if (produced > 0 || consumed > 0) {
                return TransformResult.ok(consumed, produced)
            }
        }

        // If inflater is finished but trailer not verified, verify it now
        if (inflater.finished() && !trailerVerified) {
            if (!verifyTrailer()) {
                throw IOException("Truncated or corrupt gzip data: incomplete trailer")
            }
            trailerVerified = true
        }

        // Verify that decompression is complete
        if (!inflater.finished()) {
            throw IOException("Truncated or corrupt gzip data")
        }
        return TransformResult.done()
    }

    /**
     * Parses the GZIP header directly from a ByteArray.
     * Returns the index after the header on success, or -1 on failure.
     */
    private fun parseHeaderFromByteArray(source: ByteArray, startIndex: Int, endIndex: Int): Int {
        val size = endIndex - startIndex
        if (size < GZIP_MIN_HEADER_SIZE) return -1

        var pos = startIndex

        // Validate magic number
        val magic1 = source[pos++].toInt() and 0xFF
        val magic2 = source[pos++].toInt() and 0xFF
        if (magic1 != 0x1f || magic2 != 0x8b) {
            throw IOException("Invalid GZIP magic number")
        }

        // Validate compression method
        val compressionMethod = source[pos++].toInt() and 0xFF
        if (compressionMethod != 8) {
            throw IOException("Unsupported GZIP compression method: $compressionMethod")
        }

        val flags = source[pos++].toInt() and 0xFF
        pos += 4 // Skip modification time
        pos += 1 // Skip extra flags
        pos += 1 // Skip OS

        // Handle optional fields
        val fextra = (flags and 0x04) != 0
        val fname = (flags and 0x08) != 0
        val fcomment = (flags and 0x10) != 0
        val fhcrc = (flags and 0x02) != 0

        if (fextra) {
            if (pos + 2 > endIndex) return -1
            val xlen1 = source[pos++].toInt() and 0xFF
            val xlen2 = source[pos++].toInt() and 0xFF
            val xlen = xlen1 or (xlen2 shl 8)
            if (pos + xlen > endIndex) return -1
            pos += xlen
        }

        if (fname) {
            while (pos < endIndex && source[pos++] != 0.toByte()) { }
            if (pos > endIndex) return -1
        }

        if (fcomment) {
            while (pos < endIndex && source[pos++] != 0.toByte()) { }
            if (pos > endIndex) return -1
        }

        if (fhcrc) {
            if (pos + 2 > endIndex) return -1
            pos += 2
        }

        return pos
    }

    private companion object {
        private const val GZIP_MIN_HEADER_SIZE = 10
    }

    override fun close() {
        inflater.end()
        headerBuffer.clear()
        remainingBuffer.clear()
    }

    private fun parseHeader(source: Buffer): Boolean {
        // Accumulate header bytes
        while (headerBuffer.size < 10 && !source.exhausted()) {
            headerBuffer.writeByte(source.readByte())
        }

        if (headerBuffer.size < 10) {
            return false
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
            throw IOException("Invalid GZIP magic number")
        }

        // Validate compression method
        if (compressionMethod != 8) {
            throw IOException("Unsupported GZIP compression method: $compressionMethod")
        }

        // Handle optional fields
        val fextra = (flags and 0x04) != 0
        val fname = (flags and 0x08) != 0
        val fcomment = (flags and 0x10) != 0
        val fhcrc = (flags and 0x02) != 0

        if (fextra && !skipExtra(source)) return false
        if (fname && !skipNullTerminated(source)) return false
        if (fcomment && !skipNullTerminated(source)) return false
        if (fhcrc) {
            if (source.size < 2) return false
            source.skip(2)
        }

        return true
    }

    private fun skipExtra(source: Buffer): Boolean {
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
            if (source.readByte() == 0.toByte()) return true
        }
        return false
    }

    private fun verifyTrailer(): Boolean {
        if (remainingBuffer.size < 8) {
            return false
        }

        // Read CRC32 (little-endian)
        val crc1 = remainingBuffer.readByte().toInt() and 0xFF
        val crc2 = remainingBuffer.readByte().toInt() and 0xFF
        val crc3 = remainingBuffer.readByte().toInt() and 0xFF
        val crc4 = remainingBuffer.readByte().toInt() and 0xFF
        val expectedCrc = crc1.toLong() or (crc2.toLong() shl 8) or
                (crc3.toLong() shl 16) or (crc4.toLong() shl 24)

        // Read original size (little-endian)
        val size1 = remainingBuffer.readByte().toInt() and 0xFF
        val size2 = remainingBuffer.readByte().toInt() and 0xFF
        val size3 = remainingBuffer.readByte().toInt() and 0xFF
        val size4 = remainingBuffer.readByte().toInt() and 0xFF
        val expectedSize = size1.toLong() or (size2.toLong() shl 8) or
                (size3.toLong() shl 16) or (size4.toLong() shl 24)

        // Verify CRC
        if (crc32.value != expectedCrc) {
            throw IOException("GZIP CRC32 mismatch: expected ${expectedCrc.toString(16)}, got ${crc32.value.toString(16)}")
        }

        // Verify size (mod 2^32)
        val actualSizeMod = uncompressedSize and 0xFFFFFFFFL
        if (actualSizeMod != expectedSize) {
            throw IOException("GZIP size mismatch: expected $expectedSize, got $actualSizeMod")
        }

        return true
    }
}
