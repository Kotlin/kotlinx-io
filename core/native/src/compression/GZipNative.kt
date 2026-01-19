/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.compression

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.Transformation
import platform.zlib.MAX_WBITS

/**
 * GZIP compression format (RFC 1952) - Native implementation.
 */
public actual class GZip actual constructor(
    public actual val level: Int
) : Compressor, Decompressor {

    init {
        require(level in 0..9) { "Compression level must be in 0..9, got $level" }
    }

    actual override fun createCompressTransformation(): Transformation {
        // windowBits = MAX_WBITS + 16 for GZIP format
        return ZlibCompressor(level, windowBits = MAX_WBITS + 16)
    }

    actual override fun createDecompressTransformation(): Transformation {
        // windowBits = MAX_WBITS + 16 for GZIP automatic header detection
        return ZlibDecompressor(windowBits = MAX_WBITS + 16)
    }

    public actual companion object {
        public actual fun compressor(level: Int): Compressor = GZip(level)

        public actual fun decompressor(): Decompressor = GZip()
    }
}
