/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Transform
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * DEFLATE compression algorithm (RFC 1951) - JVM implementation.
 */
public actual class Deflate actual constructor(
    public actual val level: Int
) : Compressor, Decompressor {

    init {
        require(level in 0..9) { "Compression level must be in 0..9, got $level" }
    }

    actual override fun createCompressTransform(): Transform {
        // nowrap=true for raw DEFLATE (no zlib header/trailer)
        return DeflaterCompressor(Deflater(level, true))
    }

    actual override fun createDecompressTransform(): Transform {
        // nowrap=true for raw DEFLATE (no zlib header/trailer)
        return InflaterDecompressor(Inflater(true))
    }

    public actual companion object {
        public actual fun compressor(level: Int): Compressor = Deflate(level)

        public actual fun decompressor(): Decompressor = Deflate()
    }
}
