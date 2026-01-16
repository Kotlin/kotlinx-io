/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.RawSink
import kotlinx.io.RawSource

/**
 * Returns a [RawSource] that decompresses data read from this source using the DEFLATE algorithm.
 *
 * The returned source reads compressed data from this source and returns decompressed data.
 * The source should contain data compressed using raw DEFLATE (RFC 1951) without any wrapper format.
 *
 * Closing the returned source will also close this source.
 *
 * @throws CompressionException if the compressed data is corrupted or in an invalid format
 *
 * @sample kotlinx.io.compression.samples.CompressionSamples.inflateDeflate
 */
public expect fun RawSource.inflate(): RawSource

/**
 * Returns a [RawSink] that compresses data written to it using the DEFLATE algorithm.
 *
 * The returned sink compresses data and writes the compressed output to this sink.
 * Data is compressed using raw DEFLATE (RFC 1951) without any wrapper format.
 *
 * It is important to close the returned sink to ensure all compressed data is flushed.
 * Closing the returned sink will also close this sink.
 *
 * @param level compression level from 0 (no compression) to 9 (maximum compression).
 *              Default is [CompressionLevel.DEFAULT] (6).
 *
 * @throws IllegalArgumentException if [level] is not in the range 0..9
 * @throws CompressionException if compression fails
 *
 * @sample kotlinx.io.compression.samples.CompressionSamples.inflateDeflate
 */
public expect fun RawSink.deflate(level: Int = CompressionLevel.DEFAULT): RawSink
