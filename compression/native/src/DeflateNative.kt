/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.compression

import kotlinx.cinterop.*
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import platform.zlib.*

/**
 * Returns a [RawSource] that decompresses data read from this source using the DEFLATE algorithm.
 */
public actual fun RawSource.inflate(): RawSource {
    // Negative windowBits for raw DEFLATE (no zlib/gzip header)
    return DecompressingSource(this, ZlibDecompressor(windowBits = -MAX_WBITS))
}

/**
 * Returns a [RawSink] that compresses data written to it using the DEFLATE algorithm.
 */
public actual fun RawSink.deflate(level: Int): RawSink {
    require(level in 0..9) { "Compression level must be in 0..9, got $level" }
    // Negative windowBits for raw DEFLATE (no zlib/gzip header)
    return CompressingSink(this, ZlibCompressor(level, windowBits = -MAX_WBITS))
}
