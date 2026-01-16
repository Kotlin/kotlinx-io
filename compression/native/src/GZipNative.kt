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
 * Returns a [RawSource] that decompresses GZIP data read from this source.
 */
public actual fun RawSource.gzip(): RawSource {
    // windowBits = MAX_WBITS + 16 for GZIP automatic header detection
    return DecompressingSource(this, ZlibDecompressor(windowBits = MAX_WBITS + 16))
}

/**
 * Returns a [RawSink] that compresses data written to it using GZIP format.
 */
public actual fun RawSink.gzip(level: Int): RawSink {
    require(level in 0..9) { "Compression level must be in 0..9, got $level" }
    // windowBits = MAX_WBITS + 16 for GZIP format
    return CompressingSink(this, ZlibCompressor(level, windowBits = MAX_WBITS + 16))
}
