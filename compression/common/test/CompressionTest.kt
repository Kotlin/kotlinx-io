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
        compressed.compressed(Deflate()).buffered().use { sink ->
            sink.writeString(original)
        }

        // Decompress
        val decompressed = compressed.decompressed(Deflate.decompressor()).buffered().readString()

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
        compressed.compressed(Deflate()).buffered().use { sink ->
            sink.writeString(original)
        }

        // Verify compression actually reduced size
        assertTrue(compressed.size < original.length, "Compression should reduce size")

        // Decompress
        val decompressed = compressed.decompressed(Deflate.decompressor()).buffered().readString()

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
        compressedNoCompression.compressed(Deflate(level = 0)).buffered().use { sink ->
            sink.writeString(original)
        }

        val compressedFast = Buffer()
        compressedFast.compressed(Deflate(level = 1)).buffered().use { sink ->
            sink.writeString(original)
        }

        val compressedBest = Buffer()
        compressedBest.compressed(Deflate(level = 9)).buffered().use { sink ->
            sink.writeString(original)
        }

        // Verify all can be decompressed
        assertEquals(original, compressedNoCompression.decompressed(Deflate.decompressor()).buffered().readString())
        assertEquals(original, compressedFast.decompressed(Deflate.decompressor()).buffered().readString())
        assertEquals(original, compressedBest.decompressed(Deflate.decompressor()).buffered().readString())

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
        compressed.compressed(GZip()).buffered().use { sink ->
            sink.writeString(original)
        }

        // Verify GZIP magic bytes
        val magic1 = compressed.readByte().toInt() and 0xFF
        val magic2 = compressed.readByte().toInt() and 0xFF

        assertEquals(0x1f, magic1, "First GZIP magic byte should be 0x1f")
        assertEquals(0x8b, magic2, "Second GZIP magic byte should be 0x8b")

        // Put bytes back and decompress
        val fullCompressed = Buffer()
        fullCompressed.writeByte(0x1f.toByte())
        fullCompressed.writeByte(0x8b.toByte())
        fullCompressed.write(compressed, compressed.size)

        // Decompress
        val decompressed = fullCompressed.decompressed(GZip.decompressor()).buffered().readString()

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
        compressed.compressed(GZip()).buffered().use { sink ->
            sink.writeString(original)
        }

        // Decompress
        val decompressed = compressed.decompressed(GZip.decompressor()).buffered().readString()

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
        compressedFast.compressed(GZip(level = 1)).buffered().use { sink ->
            sink.writeString(original)
        }

        val compressedBest = Buffer()
        compressedBest.compressed(GZip(level = 9)).buffered().use { sink ->
            sink.writeString(original)
        }

        // Both should decompress to original
        assertEquals(original, compressedFast.decompressed(GZip.decompressor()).buffered().readString())
        assertEquals(original, compressedBest.decompressed(GZip.decompressor()).buffered().readString())
    }

    @Test
    fun deflateEmptyData() {
        val original = ""

        val compressed = Buffer()
        compressed.compressed(Deflate()).buffered().use { sink ->
            sink.writeString(original)
        }

        val decompressed = compressed.decompressed(Deflate.decompressor()).buffered().readString()

        assertEquals(original, decompressed)
    }

    @Test
    fun gzipEmptyData() {
        val original = ""

        val compressed = Buffer()
        compressed.compressed(GZip()).buffered().use { sink ->
            sink.writeString(original)
        }

        val decompressed = compressed.decompressed(GZip.decompressor()).buffered().readString()

        assertEquals(original, decompressed)
    }

    @Test
    fun deflateBinaryData() {
        // Create binary data with all byte values
        val original = ByteArray(256) { it.toByte() }

        val compressed = Buffer()
        compressed.compressed(Deflate()).buffered().use { sink ->
            sink.write(original)
        }

        val decompressedBuffer = Buffer()
        compressed.decompressed(Deflate.decompressor()).buffered().use { source ->
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
        compressed.compressed(GZip()).buffered().use { sink ->
            sink.write(original)
        }

        val decompressedBuffer = Buffer()
        compressed.decompressed(GZip.decompressor()).buffered().use { source ->
            source.transferTo(decompressedBuffer)
        }

        val decompressed = ByteArray(256)
        decompressedBuffer.readTo(decompressed)

        assertTrue(original.contentEquals(decompressed))
    }

    @Test
    fun invalidCompressionLevelDeflate() {
        assertFailsWith<IllegalArgumentException> {
            Deflate(-1)
        }

        assertFailsWith<IllegalArgumentException> {
            Deflate(10)
        }
    }

    @Test
    fun invalidCompressionLevelGzip() {
        assertFailsWith<IllegalArgumentException> {
            GZip(-1)
        }

        assertFailsWith<IllegalArgumentException> {
            GZip(10)
        }
    }

    @Test
    fun invalidDeflateData() {
        // Create invalid compressed data
        val invalidData = Buffer()
        invalidData.writeString("This is not valid deflate data!")

        assertFailsWith<IOException> {
            invalidData.decompressed(Deflate.decompressor()).buffered().readString()
        }
    }

    @Test
    fun invalidGzipData() {
        // Create invalid GZIP data (wrong magic bytes)
        val invalidData = Buffer()
        invalidData.writeByte(0x00)
        invalidData.writeByte(0x00)
        invalidData.writeString("Not valid GZIP!")

        assertFailsWith<IOException> {
            invalidData.decompressed(GZip.decompressor()).buffered().readString()
        }
    }

    @Test
    fun truncatedDeflateData() {
        // Create valid compressed data, then truncate it
        val original = "Hello, World!"
        val compressed = Buffer()
        compressed.compressed(Deflate()).buffered().use { sink ->
            sink.writeString(original)
        }

        // Truncate the compressed data
        val truncated = Buffer()
        truncated.write(compressed, compressed.size / 2)

        assertFailsWith<IOException> {
            truncated.decompressed(Deflate.decompressor()).buffered().readString()
        }
    }

    // Tests for the new API

    @Test
    fun deflateClassUsage() {
        val original = "Test data for Deflate class"

        // Using Deflate class directly
        val compressed = Buffer()
        compressed.compressed(Deflate(level = 6)).buffered().use { sink ->
            sink.writeString(original)
        }

        val decompressed = compressed.decompressed(Deflate.decompressor()).buffered().readString()
        assertEquals(original, decompressed)
    }

    @Test
    fun gzipClassUsage() {
        val original = "Test data for GZip class"

        // Using GZip class directly
        val compressed = Buffer()
        compressed.compressed(GZip(level = 6)).buffered().use { sink ->
            sink.writeString(original)
        }

        val decompressed = compressed.decompressed(GZip.decompressor()).buffered().readString()
        assertEquals(original, decompressed)
    }

    @Test
    fun deflateFactoryMethods() {
        val original = "Test factory methods"

        // Using compression factory
        val compressed = Buffer()
        compressed.compressed(Deflate.compressor(level = 9)).buffered().use { sink ->
            sink.writeString(original)
        }

        // Using decompression factory
        val decompressed = compressed.decompressed(Deflate.decompressor()).buffered().readString()
        assertEquals(original, decompressed)
    }

    @Test
    fun gzipFactoryMethods() {
        val original = "Test factory methods"

        // Using compression factory
        val compressed = Buffer()
        compressed.compressed(GZip.compressor(level = 9)).buffered().use { sink ->
            sink.writeString(original)
        }

        // Using decompression factory
        val decompressed = compressed.decompressed(GZip.decompressor()).buffered().readString()
        assertEquals(original, decompressed)
    }
}
