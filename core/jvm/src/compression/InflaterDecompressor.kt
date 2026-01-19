/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Buffer
import kotlinx.io.ByteArrayTransformation
import kotlinx.io.IOException
import kotlinx.io.TransformResult
import kotlinx.io.UnsafeIoApi
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * A [ByteArrayTransformation] implementation that uses [java.util.zip.Inflater] for DEFLATE decompression.
 */
@OptIn(UnsafeIoApi::class)
internal class InflaterDecompressor(
    private val inflater: Inflater
) : ByteArrayTransformation() {

    override fun transformAtMostTo(source: Buffer, sink: Buffer, byteCount: Long): Long {
        // If already finished, return EOF
        if (inflater.finished()) {
            return -1L
        }
        return super.transformAtMostTo(source, sink, byteCount)
    }

    override fun transformIntoByteArray(
        source: ByteArray,
        sourceStart: Int,
        sourceEnd: Int,
        destination: ByteArray,
        destinationStart: Int,
        destinationEnd: Int
    ): TransformResult {
        // If inflater needs input and we have some, provide it
        val inputSize = sourceEnd - sourceStart
        if (inflater.needsInput() && inputSize > 0) {
            inflater.setInput(source, sourceStart, inputSize)
        }

        val produced = try {
            inflater.inflate(destination, destinationStart, destinationEnd - destinationStart)
        } catch (e: DataFormatException) {
            throw IOException("Invalid compressed data: ${e.message}", e)
        }

        // JDK inflater copies all input at once, so consumed is either 0 or all of it
        val consumed = if (inflater.needsInput() || inflater.finished()) inputSize else 0

        return TransformResult(consumed, produced)
    }

    override fun hasPendingOutput(): Boolean = !inflater.needsInput() && !inflater.finished()

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
