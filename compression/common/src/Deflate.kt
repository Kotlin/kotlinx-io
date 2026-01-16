/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Transform

/**
 * DEFLATE compression algorithm (RFC 1951).
 *
 * This class provides both compression and decompression using raw DEFLATE format
 * without any wrapper (no zlib or gzip headers/trailers).
 *
 * @param level compression level from 0 (no compression) to 9 (maximum compression).
 *              Default is 6, which provides a good balance between speed and compression ratio.
 *              This parameter is only used for compression; it is ignored for decompression.
 *
 * @throws IllegalArgumentException if [level] is not in the range 0..9
 *
 * @sample kotlinx.io.compression.samples.CompressionSamples.deflateUsage
 */
public expect class Deflate(level: Int = 6) : Compressor, Decompressor {
    /**
     * The compression level (0-9).
     */
    public val level: Int

    override fun createCompressTransform(): Transform

    override fun createDecompressTransform(): Transform

    public companion object {
        /**
         * Creates a compression-only configuration with the specified level.
         *
         * @param level compression level from 0 (no compression) to 9 (maximum compression).
         *              Default is 6.
         * @return a [Compressor] instance for DEFLATE compression
         * @throws IllegalArgumentException if [level] is not in the range 0..9
         */
        public fun compressor(level: Int = 6): Compressor

        /**
         * Creates a decompression-only configuration.
         *
         * @return a [Decompressor] instance for DEFLATE decompression
         */
        public fun decompressor(): Decompressor
    }
}
