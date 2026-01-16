/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.RawSink
import kotlinx.io.RawSource

/**
 * A codec interface for compression and decompression algorithms.
 *
 * This interface enables extensibility of the compression module, allowing external libraries
 * to provide implementations for additional algorithms like Zstandard (zstd), Brotli, or LZ4.
 *
 * Implementations must be thread-safe: a single [Compressor] or [Decompressor] instance
 * may only be used by one thread at a time, but the same codec can create multiple
 * compressors/decompressors for concurrent use.
 *
 * ## Example: Custom Codec Implementation
 *
 * ```kotlin
 * object ZstdCodec : Codec {
 *     override fun compressor(level: Int): Compressor = ZstdCompressor(level)
 *     override fun decompressor(): Decompressor = ZstdDecompressor()
 * }
 *
 * // Usage
 * rawSink.compressed(ZstdCodec, level = 3)
 * rawSource.decompressed(ZstdCodec)
 * ```
 *
 * @sample kotlinx.io.compression.samples.CodecSamples.customCodec
 */
public interface Codec {
    /**
     * Creates a new compressor with the specified compression level.
     *
     * @param level compression level from 0 (no compression) to 9 (maximum compression).
     *              The exact interpretation of levels depends on the specific algorithm.
     * @return a new [Compressor] instance
     */
    public fun compressor(level: Int): Compressor

    /**
     * Creates a new decompressor.
     *
     * @return a new [Decompressor] instance
     */
    public fun decompressor(): Decompressor
}

/**
 * A compressor that transforms uncompressed data into compressed output.
 *
 * Implementations handle the stateful compression operation, including any required
 * headers, trailers, and internal buffering.
 *
 * The typical lifecycle is:
 * 1. Create a compressor via [Codec.compressor]
 * 2. Call [compress] multiple times with input data
 * 3. Call [finish] to signal end of input
 * 4. Call [close] to release resources
 *
 * Compressor instances are not thread-safe and should only be used by a single thread.
 */
public interface Compressor : AutoCloseable {
    /**
     * Compresses data from [source], consuming input bytes and writing compressed output to [sink].
     *
     * This method may not consume all available input in [source] if the internal buffer
     * is full. Callers should continue calling this method while [source] has remaining data.
     *
     * @param source buffer containing uncompressed input data
     * @param sink buffer to write compressed output to
     * @throws CompressionException if compression fails
     */
    public fun compress(source: kotlinx.io.Buffer, sink: kotlinx.io.Buffer)

    /**
     * Finishes the compression operation, writing any remaining compressed data and trailers.
     *
     * After calling this method, no more data should be compressed with this compressor.
     * This method may need to be called multiple times until it returns without writing
     * any data, to ensure all compressed data is flushed.
     *
     * @param sink buffer to write remaining compressed output to
     * @throws CompressionException if finishing fails
     */
    public fun finish(sink: kotlinx.io.Buffer)

    /**
     * Releases all resources associated with this compressor.
     *
     * After closing, the compressor should not be used.
     */
    override fun close()
}

/**
 * A decompressor that transforms compressed data back into its original uncompressed form.
 *
 * Implementations handle the stateful decompression operation, including any required
 * header parsing, checksum verification, and internal buffering.
 *
 * The typical lifecycle is:
 * 1. Create a decompressor via [Codec.decompressor]
 * 2. Call [decompress] multiple times to process compressed input
 * 3. Call [close] to release resources
 *
 * Decompressor instances are not thread-safe and should only be used by a single thread.
 */
public interface Decompressor : AutoCloseable {
    /**
     * Indicates whether the decompression is complete.
     *
     * Returns `true` when the decompressor has processed all compressed data and
     * reached the end of the compressed stream. After this returns `true`,
     * [decompress] should not be called again.
     */
    public val isFinished: Boolean

    /**
     * Decompresses data from [source], consuming compressed bytes and writing
     * decompressed output to [sink].
     *
     * This method may not consume all available input in [source] if the internal buffer
     * is full or if only a portion of the input can be decompressed. Callers should continue
     * calling this method while [source] has data and [isFinished] is `false`.
     *
     * @param source buffer containing compressed input data
     * @param sink buffer to write decompressed output to
     * @throws CompressionException if decompression fails due to corrupted or invalid data
     */
    public fun decompress(source: kotlinx.io.Buffer, sink: kotlinx.io.Buffer)

    /**
     * Releases all resources associated with this decompressor.
     *
     * After closing, the decompressor should not be used.
     */
    override fun close()
}

/**
 * Returns a [RawSink] that compresses data written to it using the specified [codec].
 *
 * The returned sink compresses data and writes the compressed output to this sink.
 *
 * It is important to close the returned sink to ensure all compressed data is flushed
 * and any trailers are written. Closing the returned sink will also close this sink.
 *
 * @param codec the compression codec to use
 * @param level compression level from 0 (no compression) to 9 (maximum compression).
 *              Default is [CompressionLevel.DEFAULT] (6). The exact interpretation
 *              of levels depends on the specific codec.
 *
 * @throws IllegalArgumentException if [level] is not in the range 0..9
 * @throws CompressionException if compression fails
 *
 * @sample kotlinx.io.compression.samples.CodecSamples.customCodecUsage
 */
public fun RawSink.compressed(codec: Codec, level: Int = CompressionLevel.DEFAULT): RawSink {
    require(level in 0..9) { "Compression level must be in 0..9, got $level" }
    return CompressingSink(this, codec.compressor(level))
}

/**
 * Returns a [RawSource] that decompresses data read from this source using the specified [codec].
 *
 * The returned source reads compressed data from this source and returns decompressed data.
 *
 * Closing the returned source will also close this source.
 *
 * @param codec the decompression codec to use
 *
 * @throws CompressionException if the compressed data is corrupted or in an invalid format
 *
 * @sample kotlinx.io.compression.samples.CodecSamples.customCodecUsage
 */
public fun RawSource.decompressed(codec: Codec): RawSource {
    return DecompressingSource(this, codec.decompressor())
}
