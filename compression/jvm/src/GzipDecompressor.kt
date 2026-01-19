/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Buffer
import kotlinx.io.ByteArrayTransformation
import kotlinx.io.IOException
import kotlinx.io.UnsafeIoApi
import java.util.zip.CRC32
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * A [ByteArrayTransformation] implementation for GZIP decompression (RFC 1952).
 *
 * GZIP format consists of:
 * - 10-byte minimum header (may be longer with optional fields)
 * - DEFLATE compressed data
 * - 8-byte trailer (CRC32 + original size)
 */
@OptIn(UnsafeIoApi::class)
internal class GzipDecompressor : ByteArrayTransformation() {

    // Use raw inflate (nowrap=true) as we manually handle GZIP header/trailer
    private val inflater = Inflater(true)
    private val crc32 = CRC32()
    private val outputArray = ByteArray(BUFFER_SIZE)

    private var headerParsed = false
    private var trailerVerified = false
    private var uncompressedSize = 0L
    private var finished = false

    // Buffer for accumulating header bytes when source doesn't have enough
    private val headerBuffer = Buffer()

    // Buffer for storing remaining bytes after inflater finishes (for trailer verification)
    private val remainingBuffer = Buffer()

    // Track the last input for extracting remaining bytes
    private var lastInput: ByteArray? = null
    private var lastInputStart: Int = 0
    private var lastInputLength: Int = 0

    override fun transformAtMostTo(source: Buffer, sink: Buffer, byteCount: Long): Long {
        // If already finished, return EOF
        if (finished) {
            return -1L
        }

        // Parse GZIP header if not yet parsed
        if (!headerParsed) {
            if (!parseHeader(source)) {
                // Not enough data for header yet
                return 0L
            }
            headerParsed = true
        }

        // If inflater is finished, we need to verify the trailer
        if (inflater.finished() && !trailerVerified) {
            // Extract remaining bytes from inflater and put them in remainingBuffer
            extractRemainingBytes()

            if (!verifyTrailer(source)) {
                // Not enough data for trailer yet
                return 0L
            }
            trailerVerified = true
            finished = true
            return -1L
        }

        if (source.exhausted() && inflater.needsInput()) {
            return 0L
        }

        // Call parent implementation which will use transformToByteArray
        return super.transformAtMostTo(source, sink, byteCount)
    }

    override fun transformToByteArray(source: ByteArray, startIndex: Int, endIndex: Int): ByteArray {
        val inputSize = endIndex - startIndex
        if (inputSize == 0) return ByteArray(0)

        // If already finished, return empty
        if (inflater.finished()) return ByteArray(0)

        // Track input for extracting remaining bytes
        lastInput = source
        lastInputStart = startIndex
        lastInputLength = inputSize

        // Feed data to inflater
        inflater.setInput(source, startIndex, inputSize)

        // Collect all output
        val result = mutableListOf<ByteArray>()
        var totalSize = 0

        try {
            while (!inflater.needsInput() && !inflater.finished()) {
                val count = inflater.inflate(outputArray)
                if (count > 0) {
                    // Update CRC
                    crc32.update(outputArray, 0, count)
                    uncompressedSize += count

                    result.add(outputArray.copyOf(count))
                    totalSize += count
                } else {
                    break
                }
            }
        } catch (e: DataFormatException) {
            throw IOException("Invalid GZIP data: ${e.message}", e)
        }

        // If inflater finished, extract remaining bytes
        if (inflater.finished()) {
            extractRemainingBytes()
        }

        return combineChunks(result, totalSize)
    }

    override fun finalize(sink: Buffer) {
        // If inflater is finished but trailer not verified, verify it now
        // This can happen when the deflate data and trailer are in the same input chunk
        if (inflater.finished() && !trailerVerified) {
            extractRemainingBytes()
            // Create an empty buffer - all trailer data should be in remainingBuffer
            val emptySource = Buffer()
            if (!verifyTrailer(emptySource)) {
                throw IOException("Truncated or corrupt gzip data: incomplete trailer")
            }
            trailerVerified = true
            finished = true
        }

        // Verify that decompression is complete (header parsed, data inflated, trailer verified)
        if (!finished) {
            throw IOException("Truncated or corrupt gzip data")
        }
    }

    override fun finalizeToByteArray(): ByteArray {
        // Nothing to do - verification is done in finalize()
        return ByteArray(0)
    }

    override fun close() {
        inflater.end()
        headerBuffer.clear()
        remainingBuffer.clear()
    }

    private fun extractRemainingBytes() {
        val remaining = inflater.remaining
        if (remaining > 0 && lastInput != null && lastInputLength > 0) {
            // Calculate where the remaining bytes start in the original input
            val remainingStart = lastInputStart + lastInputLength - remaining
            remainingBuffer.write(lastInput!!, remainingStart, remainingStart + remaining)
            lastInputLength = 0
        }
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
            throw IOException("Invalid GZIP magic number")
        }

        // Validate compression method
        if (compressionMethod != 8) {
            throw IOException("Unsupported GZIP compression method: $compressionMethod")
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

    private fun verifyTrailer(source: Buffer): Boolean {
        // First, use any remaining bytes from the inflater
        val trailerSource = if (remainingBuffer.size > 0) {
            // Combine remaining bytes with source
            val combined = Buffer()
            combined.write(remainingBuffer, remainingBuffer.size)
            combined.write(source, source.size)
            combined
        } else {
            source
        }

        // Need 8 bytes for trailer
        if (trailerSource.size < 8) {
            // Put unused bytes back to source if we borrowed them
            if (trailerSource !== source && trailerSource.size > 0) {
                val temp = Buffer()
                temp.write(trailerSource, trailerSource.size)
                temp.write(source, source.size)
                source.write(temp, temp.size)
            }
            return false
        }

        // Read CRC32 (little-endian)
        val crc1 = trailerSource.readByte().toInt() and 0xFF
        val crc2 = trailerSource.readByte().toInt() and 0xFF
        val crc3 = trailerSource.readByte().toInt() and 0xFF
        val crc4 = trailerSource.readByte().toInt() and 0xFF
        val expectedCrc = crc1.toLong() or
                (crc2.toLong() shl 8) or
                (crc3.toLong() shl 16) or
                (crc4.toLong() shl 24)

        // Read original size (little-endian)
        val size1 = trailerSource.readByte().toInt() and 0xFF
        val size2 = trailerSource.readByte().toInt() and 0xFF
        val size3 = trailerSource.readByte().toInt() and 0xFF
        val size4 = trailerSource.readByte().toInt() and 0xFF
        val expectedSize = size1.toLong() or
                (size2.toLong() shl 8) or
                (size3.toLong() shl 16) or
                (size4.toLong() shl 24)

        // Put any remaining bytes back to source
        if (trailerSource !== source && trailerSource.size > 0) {
            source.write(trailerSource, trailerSource.size)
        }

        // Verify CRC
        if (crc32.value != expectedCrc) {
            throw IOException(
                "GZIP CRC32 mismatch: expected ${expectedCrc.toString(16)}, " +
                        "got ${crc32.value.toString(16)}"
            )
        }

        // Verify size (mod 2^32)
        val actualSizeMod = uncompressedSize and 0xFFFFFFFFL
        if (actualSizeMod != expectedSize) {
            throw IOException(
                "GZIP size mismatch: expected $expectedSize, got $actualSizeMod"
            )
        }

        return true
    }

    private companion object {
        private const val BUFFER_SIZE = 8192
    }
}
