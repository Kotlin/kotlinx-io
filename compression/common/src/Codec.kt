/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.Transform
import kotlinx.io.transform

/**
 * Provides a compression transform.
 *
 * Implementations of this interface create [Transform] instances that compress data.
 * Built-in implementations include [Deflate] and [GZip].
 *
 * Third-party libraries can implement this interface to provide additional compression
 * algorithms such as Zstandard (zstd), Brotli, or LZ4.
 *
 * Example usage:
 * ```kotlin
 * // Using built-in compression
 * sink.compress(Deflate(level = 6))
 * sink.compress(GZip(level = 9))
 *
 * // Using third-party compression
 * sink.compress(Zstd(level = 3))
 * ```
 *
 * @see Decompressor
 * @see Deflate
 * @see GZip
 */
public interface Compressor {
    /**
     * Creates a new compression transform.
     *
     * @return a new [Transform] instance configured for compression
     */
    public fun createCompressTransform(): Transform
}

/**
 * Provides a decompression transform.
 *
 * Implementations of this interface create [Transform] instances that decompress data.
 * Built-in implementations include [Deflate] and [GZip].
 *
 * Third-party libraries can implement this interface to provide additional decompression
 * algorithms such as Zstandard (zstd), Brotli, or LZ4.
 *
 * Example usage:
 * ```kotlin
 * // Using built-in decompression
 * source.decompress(Deflate.decompressor())
 * source.decompress(GZip.decompressor())
 *
 * // Using third-party decompression
 * source.decompress(Zstd())
 * ```
 *
 * @see Compressor
 * @see Deflate
 * @see GZip
 */
public interface Decompressor {
    /**
     * Creates a new decompression transform.
     *
     * @return a new [Transform] instance configured for decompression
     */
    public fun createDecompressTransform(): Transform
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
 * @throws IOException if compression fails
 *
 * @sample kotlinx.io.compression.samples.CompressionSamples.compressedSink
 */
public fun RawSink.compress(compressor: Compressor): RawSink {
    return transform(compressor.createCompressTransform())
}

/**
 * Returns a [RawSource] that decompresses data read from this source using the specified [decompressor].
 *
 * The returned source reads compressed data from this source and returns decompressed data.
 *
 * Closing the returned source will also close this source.
 *
 * @param decompressor the decompressor to use
 * @throws IOException if decompression fails or the compressed data is corrupted
 *
 * @sample kotlinx.io.compression.samples.CompressionSamples.decompressedSource
 */
public fun RawSource.decompress(decompressor: Decompressor): RawSource {
    return transform(decompressor.createDecompressTransform())
}
