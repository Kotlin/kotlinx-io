/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.Transformation
import kotlinx.io.bytestring.ByteString
import kotlinx.io.transform
import kotlinx.io.transformedWith

/**
 * Provides a compression transform.
 *
 * Implementations of this interface create [Transformation] instances that compress data.
 *
 * Third-party libraries can implement this interface to provide additional compression
 * algorithms such as Zstandard (zstd), Brotli, or LZ4.
 *
 * Example usage:
 * ```kotlin
 * // Using built-in compression (on JVM and Native)
 * sink.compressed(Deflate(level = 6))
 * sink.compressed(GZip(level = 9))
 *
 * // Using third-party compression
 * sink.compressed(Zstd(level = 3))
 * ```
 *
 * @see Decompressor
 */
public interface Compressor {
    /**
     * Creates a new compression transform.
     *
     * @return a new [Transformation] instance configured for compression
     */
    public fun createCompressTransformation(): Transformation
}

/**
 * Provides a decompression transform.
 *
 * Implementations of this interface create [Transformation] instances that decompress data.
 *
 * Third-party libraries can implement this interface to provide additional decompression
 * algorithms such as Zstandard (zstd), Brotli, or LZ4.
 *
 * Example usage:
 * ```kotlin
 * // Using built-in decompression (on JVM and Native)
 * source.decompressed(Deflate.decompressor())
 * source.decompressed(GZip.decompressor())
 *
 * // Using third-party decompression
 * source.decompressed(Zstd())
 * ```
 *
 * @see Compressor
 */
public interface Decompressor {
    /**
     * Creates a new decompression transform.
     *
     * @return a new [Transformation] instance configured for decompression
     */
    public fun createDecompressTransformation(): Transformation
}

/**
 * Returns a [RawSink] that compresses data written to it using the specified [compressor].
 *
 * The returned sink compresses data and writes the compressed output to this sink.
 *
 * It is important to close the returned sink to ensure all compressed data is flushed
 * and any trailers are written. Closing the returned sink will also close this sink.
 *
 * @param compressor the compressor to use
 * @throws kotlinx.io.IOException if compression fails
 */
public fun RawSink.compressed(compressor: Compressor): RawSink {
    return transformedWith(compressor.createCompressTransformation())
}

/**
 * Returns a [RawSource] that decompresses data read from this source using the specified [decompressor].
 *
 * The returned source reads compressed data from this source and returns decompressed data.
 *
 * Closing the returned source will also close this source.
 *
 * @param decompressor the decompressor to use
 * @throws kotlinx.io.IOException if decompression fails or the compressed data is corrupted
 */
public fun RawSource.decompressed(decompressor: Decompressor): RawSource {
    return transformedWith(decompressor.createDecompressTransformation())
}

/**
 * Compresses this byte string using the specified [compressor] and returns the result
 * as a new byte string.
 *
 * The compressor's transformation is automatically closed after use.
 *
 * Example:
 * ```kotlin
 * val original = "Hello, World!".encodeToByteString()
 * val compressed = original.compress(GZip())
 * ```
 *
 * @param compressor the compressor to use for compression
 * @return a new byte string containing the compressed data
 * @throws kotlinx.io.IOException if compression fails
 *
 * @see decompress
 */
public fun ByteString.compress(compressor: Compressor): ByteString {
    return transform(compressor.createCompressTransformation())
}

/**
 * Decompresses this byte string using the specified [decompressor] and returns the result
 * as a new byte string.
 *
 * The decompressor's transformation is automatically closed after use.
 *
 * Example:
 * ```kotlin
 * val compressed = getCompressedData()
 * val original = compressed.decompress(GZip.decompressor())
 * ```
 *
 * @param decompressor the decompressor to use for decompression
 * @return a new byte string containing the decompressed data
 * @throws kotlinx.io.IOException if decompression fails or the compressed data is corrupted
 *
 * @see compress
 */
public fun ByteString.decompress(decompressor: Decompressor): ByteString {
    return transform(decompressor.createDecompressTransformation())
}
