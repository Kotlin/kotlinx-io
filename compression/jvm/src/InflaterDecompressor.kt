/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.StreamingTransformation
import kotlinx.io.UnsafeIoApi
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * A [StreamingTransformation] implementation that uses [java.util.zip.Inflater] for DEFLATE decompression.
 */
@OptIn(UnsafeIoApi::class)
internal class InflaterDecompressor(
    private val inflater: Inflater
) : StreamingTransformation() {

    override fun transformAtMostTo(source: Buffer, sink: Buffer, byteCount: Long): Long {
        // If already finished, return EOF
        if (inflater.finished()) {
            return -1L
        }
        return super.transformAtMostTo(source, sink, byteCount)
    }

    override fun feedInput(source: ByteArray, startIndex: Int, endIndex: Int) {
        inflater.setInput(source, startIndex, endIndex - startIndex)
    }

    override fun needsInput(): Boolean = inflater.needsInput() || inflater.finished()

    override fun drainOutput(destination: ByteArray, startIndex: Int, endIndex: Int): Int {
        return try {
            inflater.inflate(destination, startIndex, endIndex - startIndex)
        } catch (e: DataFormatException) {
            throw IOException("Invalid compressed data: ${e.message}", e)
        }
    }

    override fun finalizeOutput(destination: ByteArray, startIndex: Int, endIndex: Int): Int {
        if (!inflater.finished()) {
            throw IOException("Truncated or corrupt deflate data")
        }
        return -1
    }

    override fun close() {
        inflater.end()
    }
}
