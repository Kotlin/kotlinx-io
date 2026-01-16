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
 * Returns a [RawSource] that decompresses GZIP data read from this source.
 */
public actual fun RawSource.gzip(): RawSource {
    // For GZIP, we use the GzipDecompressor which handles the full GZIP format
    return DecompressingSource(this, GzipDecompressor())
}

/**
 * Returns a [RawSink] that compresses data written to it using GZIP format.
 */
public actual fun RawSink.gzip(level: Int): RawSink {
    require(level in 0..9) { "Compression level must be in 0..9, got $level" }
    // For GZIP, we use the GzipCompressor which handles the full GZIP format
    return CompressingSink(this, GzipCompressor(level))
}
