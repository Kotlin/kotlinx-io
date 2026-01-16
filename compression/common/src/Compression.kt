/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.IOException

/**
 * Signals that an error occurred during a compression or decompression operation.
 *
 * This exception is thrown when:
 * - The compressed data format is invalid or corrupted
 * - The compression operation fails due to invalid parameters
 * - The decompression operation encounters unexpected end of stream
 *
 * @sample kotlinx.io.compression.samples.CompressionSamples.handleCompressionError
 */
public expect open class CompressionException : IOException {
    public constructor()
    public constructor(message: String?)
    public constructor(cause: Throwable?)
    public constructor(message: String?, cause: Throwable?)
}

/**
 * Compression level constants for use with compression functions.
 *
 * Compression levels range from 0 (no compression, fastest) to 9 (maximum compression, slowest).
 */
public object CompressionLevel {
    /**
     * No compression at all. Data is stored as-is with only format headers/trailers.
     */
    public const val NO_COMPRESSION: Int = 0

    /**
     * Fastest compression speed with minimal compression ratio.
     */
    public const val BEST_SPEED: Int = 1

    /**
     * Default compression level providing a good balance between speed and compression ratio.
     */
    public const val DEFAULT: Int = 6

    /**
     * Maximum compression ratio at the cost of slower compression speed.
     */
    public const val BEST_COMPRESSION: Int = 9
}
