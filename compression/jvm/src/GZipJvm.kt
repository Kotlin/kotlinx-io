/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Transform

/**
 * GZIP compression format (RFC 1952) - JVM implementation.
 */
public actual class GZip actual constructor(
    public actual val level: Int
) : Compressor, Decompressor {

    init {
        require(level in 0..9) { "Compression level must be in 0..9, got $level" }
    }

    actual override fun createCompressTransform(): Transform {
        return GzipCompressor(level)
    }

    actual override fun createDecompressTransform(): Transform {
        return GzipDecompressor()
    }

    public actual companion object {
        public actual fun compressor(level: Int): Compressor = GZip(level)

        public actual fun decompressor(): Decompressor = GZip()
    }
}
