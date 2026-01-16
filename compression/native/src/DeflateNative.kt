/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.compression

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.Transform
import platform.zlib.MAX_WBITS

/**
 * DEFLATE compression algorithm (RFC 1951) - Native implementation.
 */
public actual class Deflate actual constructor(
    public actual val level: Int
) : Compressor, Decompressor {

    init {
        require(level in 0..9) { "Compression level must be in 0..9, got $level" }
    }

    actual override fun createCompressTransform(): Transform {
        // Negative windowBits for raw DEFLATE (no zlib/gzip header)
        return ZlibCompressor(level, windowBits = -MAX_WBITS)
    }

    actual override fun createDecompressTransform(): Transform {
        // Negative windowBits for raw DEFLATE (no zlib/gzip header)
        return ZlibDecompressor(windowBits = -MAX_WBITS)
    }

    public actual companion object {
        public actual fun compressor(level: Int): Compressor = Deflate(level)

        public actual fun decompressor(): Decompressor = Deflate()
    }
}
