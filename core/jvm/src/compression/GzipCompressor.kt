/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

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

    override fun transformIntoByteArray(
        source: ByteArray,
        sourceStartIndex: Int,
        sourceEndIndex: Int,
        sink: ByteArray,
        sinkStartIndex: Int,
        sinkEndIndex: Int
    ): TransformResult {
        var outputStart = sinkStartIndex
        val outputSize = sinkEndIndex - sinkStartIndex
        var totalProduced = 0

        // Write GZIP header if not yet written
        if (!headerWritten) {
            if (outputSize < GZIP_HEADER_SIZE) {
                return TransformResult.outputRequired(GZIP_HEADER_SIZE)
            }
            writeHeaderToByteArray(sink, outputStart)
            outputStart += GZIP_HEADER_SIZE
            totalProduced += GZIP_HEADER_SIZE
            headerWritten = true
        }

        val inputSize = sourceEndIndex - sourceStartIndex

        // If deflater needs input and we have some, provide it
        if (deflater.needsInput() && inputSize > 0) {
            // Update CRC32 checksum before compression
            crc32.update(source, sourceStartIndex, inputSize)
            uncompressedSize += inputSize
            deflater.setInput(source, sourceStartIndex, inputSize)
        }

        val availableForDeflate = sinkEndIndex - outputStart
        val produced = deflater.deflate(sink, outputStart, availableForDeflate)
        totalProduced += produced

        // JDK deflater copies all input at once, so consumed is either 0 or all of it
        val consumed = if (deflater.needsInput()) inputSize else 0

        return TransformResult.ok(consumed, totalProduced)
    }

    override fun transformFinalIntoByteArray(
        source: ByteArray,
        sourceStartIndex: Int,
        sourceEndIndex: Int,
        sink: ByteArray,
        sinkStartIndex: Int,
        sinkEndIndex: Int
    ): TransformResult {
        var outputStart = sinkStartIndex
        val outputSize = sinkEndIndex - sinkStartIndex
        var totalProduced = 0

        // Write GZIP header if not yet written
        if (!headerWritten) {
            if (outputSize < GZIP_HEADER_SIZE) {
                return TransformResult.outputRequired(GZIP_HEADER_SIZE)
            }
            writeHeaderToByteArray(sink, outputStart)
            outputStart += GZIP_HEADER_SIZE
            totalProduced += GZIP_HEADER_SIZE
            headerWritten = true
        }

        val inputSize = sourceEndIndex - sourceStartIndex

        // If deflater needs input and we have some, provide it
        if (deflater.needsInput() && inputSize > 0) {
            crc32.update(source, sourceStartIndex, inputSize)
            uncompressedSize += inputSize
            deflater.setInput(source, sourceStartIndex, inputSize)
        }

        // Signal finish if not yet done
        if (!finishCalled) {
            deflater.finish()
            finishCalled = true
        }

        // Write trailer if deflater is finished
        if (deflater.finished()) {
            if (!trailerWritten) {
                val availableForTrailer = sinkEndIndex - outputStart
                if (availableForTrailer < GZIP_TRAILER_SIZE) {
                    return if (totalProduced > 0) {
                        TransformResult.ok(inputSize, totalProduced)
                    } else {
                        TransformResult.outputRequired(GZIP_TRAILER_SIZE)
                    }
                }
                writeTrailerToByteArray(sink, outputStart)
                totalProduced += GZIP_TRAILER_SIZE
                trailerWritten = true
            }
            return if (totalProduced > 0) TransformResult.ok(inputSize, totalProduced) else TransformResult.done()
        }

        val availableForDeflate = sinkEndIndex - outputStart
        val produced = deflater.deflate(sink, outputStart, availableForDeflate)
        totalProduced += produced

        // JDK deflater copies all input at once, so consumed is either 0 or all of it
        val consumed = if (deflater.needsInput()) inputSize else 0

        return TransformResult.ok(consumed, totalProduced)
    }

    private fun writeHeaderToByteArray(sink: ByteArray, startIndex: Int) {
        sink[startIndex + 0] = 0x1f.toByte()
        sink[startIndex + 1] = 0x8b.toByte()
        sink[startIndex + 2] = 8 // Compression method: deflate
        sink[startIndex + 3] = 0 // Flags
        sink[startIndex + 4] = 0 // Modification time (4 bytes)
        sink[startIndex + 5] = 0
        sink[startIndex + 6] = 0
        sink[startIndex + 7] = 0
        sink[startIndex + 8] = 0 // Extra flags
        sink[startIndex + 9] = 255.toByte() // OS: unknown
    }

    private fun writeTrailerToByteArray(sink: ByteArray, startIndex: Int) {
        val crcValue = crc32.value.toInt()
        val sizeValue = (uncompressedSize and 0xFFFFFFFFL).toInt()

        sink[startIndex + 0] = (crcValue and 0xFF).toByte()
        sink[startIndex + 1] = (crcValue shr 8 and 0xFF).toByte()
        sink[startIndex + 2] = (crcValue shr 16 and 0xFF).toByte()
        sink[startIndex + 3] = (crcValue shr 24 and 0xFF).toByte()
        sink[startIndex + 4] = (sizeValue and 0xFF).toByte()
        sink[startIndex + 5] = (sizeValue shr 8 and 0xFF).toByte()
        sink[startIndex + 6] = (sizeValue shr 16 and 0xFF).toByte()
        sink[startIndex + 7] = (sizeValue shr 24 and 0xFF).toByte()
    }

    private companion object {
        private const val GZIP_HEADER_SIZE = 10
        private const val GZIP_TRAILER_SIZE = 8
    }

    override fun close() {
        deflater.end()
    }
}
