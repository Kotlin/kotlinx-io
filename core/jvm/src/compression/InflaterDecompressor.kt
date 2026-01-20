/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.unsafe.UnsafeByteArrayTransformation
import kotlinx.io.IOException
import kotlinx.io.UnsafeIoApi
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * A [UnsafeByteArrayTransformation] implementation that uses [java.util.zip.Inflater] for DEFLATE decompression.
 */
@OptIn(UnsafeIoApi::class)
internal class InflaterDecompressor(
    private val inflater: Inflater
) : UnsafeByteArrayTransformation() {

    override fun transformIntoByteArray(
        source: ByteArray,
        sourceStartIndex: Int,
        sourceEndIndex: Int,
        sink: ByteArray,
        sinkStartIndex: Int,
        sinkEndIndex: Int
    ): TransformResult {
        // If already finished, ignore any further input
        if (inflater.finished()) {
            return TransformResult.done()
        }

        // If inflater needs input and we have some, provide it
        val inputSize = sourceEndIndex - sourceStartIndex
        if (inflater.needsInput() && inputSize > 0) {
            inflater.setInput(source, sourceStartIndex, inputSize)
        }

        val produced = try {
            inflater.inflate(sink, sinkStartIndex, sinkEndIndex - sinkStartIndex)
        } catch (e: DataFormatException) {
            throw IOException("Invalid compressed data: ${e.message}", e)
        }

        // JDK inflater copies all input at once, so consumed is either 0 or all of it
        val consumed = if (inflater.needsInput() || inflater.finished()) inputSize else 0

        return TransformResult.ok(consumed, produced)
    }

    override fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): FinalizeResult {
        if (!inflater.finished()) {
            throw IOException("Truncated or corrupt deflate data")
        }
        return FinalizeResult.done()
    }

    override fun close() {
        inflater.end()
    }
}
