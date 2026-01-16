/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CompressionTest {

    @Test
    fun deflateRoundTrip() {
        val original = "Hello, World! This is a test of DEFLATE compression."

        // Compress
        val compressed = Buffer()
        compressed.deflate().buffered().use { sink ->
            sink.writeString(original)
        }

        // Decompress
        val decompressed = compressed.inflate().buffered().readString()

        assertEquals(original, decompressed)
    }

    @Test
    fun deflateRoundTripLargeData() {
        // Generate large data (larger than internal buffer size)
        val original = buildString {
            repeat(10000) {
                append("Line $it: The quick brown fox jumps over the lazy dog.\n")
            }
        }

        // Compress
        val compressed = Buffer()
        compressed.deflate().buffered().use { sink ->
            sink.writeString(original)
        }

        // Verify compression actually reduced size
        assertTrue(compressed.size < original.length, "Compression should reduce size")

        // Decompress
        val decompressed = compressed.inflate().buffered().readString()

        assertEquals(original, decompressed)
    }

    @Test
    fun deflateCompressionLevels() {
        val original = buildString {
            repeat(1000) {
                append("Repeating pattern for better compression ratio. ")
            }
        }

        // Compress with different levels
        val compressedNoCompression = Buffer()
        compressedNoCompression.deflate(CompressionLevel.NO_COMPRESSION).buffered().use { sink ->
            sink.writeString(original)
        }

        val compressedFast = Buffer()
        compressedFast.deflate(CompressionLevel.BEST_SPEED).buffered().use { sink ->
            sink.writeString(original)
        }

        val compressedBest = Buffer()
        compressedBest.deflate(CompressionLevel.BEST_COMPRESSION).buffered().use { sink ->
            sink.writeString(original)
        }

        // Verify all can be decompressed
        assertEquals(original, compressedNoCompression.inflate().buffered().readString())
        assertEquals(original, compressedFast.inflate().buffered().readString())
        assertEquals(original, compressedBest.inflate().buffered().readString())

        // Best compression should be smaller than fast (usually)
        assertTrue(
            compressedBest.size <= compressedFast.size,
            "Best compression should be at least as good as fast compression"
        )
    }

    @Test
    fun gzipRoundTrip() {
        val original = "Hello, World! This is a test of GZIP compression."

        // Compress
        val compressed = Buffer()
        compressed.gzip().buffered().use { sink ->
            sink.writeString(original)
        }

        // Verify GZIP magic bytes
        val magic1 = compressed.readByte().toInt() and 0xFF
        val magic2 = compressed.readByte().toInt() and 0xFF
        compressed.skip(-2) // Put the bytes back for decompression

        assertEquals(0x1f, magic1, "First GZIP magic byte should be 0x1f")
        assertEquals(0x8b, magic2, "Second GZIP magic byte should be 0x8b")

        // Decompress
        val decompressed = compressed.gzip().buffered().readString()

        assertEquals(original, decompressed)
    }

    @Test
    fun gzipRoundTripLargeData() {
        // Generate large data
        val original = buildString {
            repeat(10000) {
                append("Line $it: The quick brown fox jumps over the lazy dog.\n")
            }
        }

        // Compress
        val compressed = Buffer()
        compressed.gzip().buffered().use { sink ->
            sink.writeString(original)
        }

        // Decompress
        val decompressed = compressed.gzip().buffered().readString()

        assertEquals(original, decompressed)
    }

    @Test
    fun gzipCompressionLevels() {
        val original = buildString {
            repeat(1000) {
                append("Repeating pattern for compression. ")
            }
        }

        val compressedFast = Buffer()
        compressedFast.gzip(CompressionLevel.BEST_SPEED).buffered().use { sink ->
            sink.writeString(original)
        }

        val compressedBest = Buffer()
        compressedBest.gzip(CompressionLevel.BEST_COMPRESSION).buffered().use { sink ->
            sink.writeString(original)
        }

        // Both should decompress to original
        assertEquals(original, compressedFast.gzip().buffered().readString())
        assertEquals(original, compressedBest.gzip().buffered().readString())
    }

    @Test
    fun deflateEmptyData() {
        val original = ""

        val compressed = Buffer()
        compressed.deflate().buffered().use { sink ->
            sink.writeString(original)
        }

        val decompressed = compressed.inflate().buffered().readString()

        assertEquals(original, decompressed)
    }

    @Test
    fun gzipEmptyData() {
        val original = ""

        val compressed = Buffer()
        compressed.gzip().buffered().use { sink ->
            sink.writeString(original)
        }

        val decompressed = compressed.gzip().buffered().readString()

        assertEquals(original, decompressed)
    }

    @Test
    fun deflateBinaryData() {
        // Create binary data with all byte values
        val original = ByteArray(256) { it.toByte() }

        val compressed = Buffer()
        compressed.deflate().buffered().use { sink ->
            sink.write(original)
        }

        val decompressedBuffer = Buffer()
        compressed.inflate().buffered().use { source ->
            source.transferTo(decompressedBuffer)
        }

        val decompressed = ByteArray(256)
        decompressedBuffer.readTo(decompressed)

        assertTrue(original.contentEquals(decompressed))
    }

    @Test
    fun gzipBinaryData() {
        // Create binary data with all byte values
        val original = ByteArray(256) { it.toByte() }

        val compressed = Buffer()
        compressed.gzip().buffered().use { sink ->
            sink.write(original)
        }

        val decompressedBuffer = Buffer()
        compressed.gzip().buffered().use { source ->
            source.transferTo(decompressedBuffer)
        }

        val decompressed = ByteArray(256)
        decompressedBuffer.readTo(decompressed)

        assertTrue(original.contentEquals(decompressed))
    }

    @Test
    fun invalidCompressionLevelDeflate() {
        val buffer = Buffer()

        assertFailsWith<IllegalArgumentException> {
            buffer.deflate(-1)
        }

        assertFailsWith<IllegalArgumentException> {
            buffer.deflate(10)
        }
    }

    @Test
    fun invalidCompressionLevelGzip() {
        val buffer = Buffer()

        assertFailsWith<IllegalArgumentException> {
            buffer.gzip(-1)
        }

        assertFailsWith<IllegalArgumentException> {
            buffer.gzip(10)
        }
    }

    @Test
    fun invalidDeflateData() {
        // Create invalid compressed data
        val invalidData = Buffer()
        invalidData.writeString("This is not valid deflate data!")

        assertFailsWith<CompressionException> {
            invalidData.inflate().buffered().readString()
        }
    }

    @Test
    fun invalidGzipData() {
        // Create invalid GZIP data (wrong magic bytes)
        val invalidData = Buffer()
        invalidData.writeByte(0x00)
        invalidData.writeByte(0x00)
        invalidData.writeString("Not valid GZIP!")

        assertFailsWith<CompressionException> {
            invalidData.gzip().buffered().readString()
        }
    }

    @Test
    fun truncatedDeflateData() {
        // Create valid compressed data, then truncate it
        val original = "Hello, World!"
        val compressed = Buffer()
        compressed.deflate().buffered().use { sink ->
            sink.writeString(original)
        }

        // Truncate the compressed data
        val truncated = Buffer()
        truncated.write(compressed, compressed.size / 2)

        assertFailsWith<CompressionException> {
            truncated.inflate().buffered().readString()
        }
    }
}
