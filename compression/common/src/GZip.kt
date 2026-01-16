/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.RawSink
import kotlinx.io.RawSource

/**
 * Returns a [RawSource] that decompresses GZIP data read from this source.
 *
 * The returned source reads GZIP compressed data (RFC 1952) from this source
 * and returns decompressed data. The GZIP format includes a header, compressed data
 * using DEFLATE, and a trailer with CRC32 checksum.
 *
 * Closing the returned source will also close this source.
 *
 * @throws CompressionException if the compressed data is corrupted, has an invalid
 *         GZIP header/trailer, or the CRC32 checksum doesn't match
 *
 * @sample kotlinx.io.compression.samples.CompressionSamples.gzipFile
 */
public expect fun RawSource.gzip(): RawSource

/**
 * Returns a [RawSink] that compresses data written to it using GZIP format.
 *
 * The returned sink compresses data using the GZIP format (RFC 1952) and writes
 * the compressed output to this sink. The output includes a GZIP header,
 * DEFLATE compressed data, and a trailer with CRC32 checksum.
 *
 * It is important to close the returned sink to ensure the GZIP trailer is written
 * and all compressed data is flushed. Closing the returned sink will also close this sink.
 *
 * @param level compression level from 0 (no compression) to 9 (maximum compression).
 *              Default is [CompressionLevel.DEFAULT] (6).
 *
 * @throws IllegalArgumentException if [level] is not in the range 0..9
 * @throws CompressionException if compression fails
 *
 * @sample kotlinx.io.compression.samples.CompressionSamples.gzipFile
 */
public expect fun RawSink.gzip(level: Int = CompressionLevel.DEFAULT): RawSink
