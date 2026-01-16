/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Returns a [RawSource] that decompresses data read from this source using the DEFLATE algorithm.
 */
public actual fun RawSource.inflate(): RawSource {
    // nowrap=true for raw DEFLATE (no zlib header/trailer)
    return DecompressingSource(this, InflaterDecompressor(Inflater(true)))
}

/**
 * Returns a [RawSink] that compresses data written to it using the DEFLATE algorithm.
 */
public actual fun RawSink.deflate(level: Int): RawSink {
    require(level in 0..9) { "Compression level must be in 0..9, got $level" }
    // nowrap=true for raw DEFLATE (no zlib header/trailer)
    return CompressingSink(this, DeflaterCompressor(Deflater(level, true)))
}
