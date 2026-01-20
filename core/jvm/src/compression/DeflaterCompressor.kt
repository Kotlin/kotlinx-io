/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.unsafe.UnsafeByteArrayTransformation
import kotlinx.io.UnsafeIoApi
import java.util.zip.Deflater

/**
 * A [UnsafeByteArrayTransformation] implementation that uses [java.util.zip.Deflater] for DEFLATE compression.
 */
@OptIn(UnsafeIoApi::class)
internal class DeflaterCompressor(
    private val deflater: Deflater
) : UnsafeByteArrayTransformation() {

    private var finishCalled = false

    override fun transformIntoByteArray(
        source: ByteArray,
        sourceStartIndex: Int,
        sourceEndIndex: Int,
        sink: ByteArray,
        sinkStartIndex: Int,
        sinkEndIndex: Int
    ): TransformResult {
        // If deflater needs input and we have some, provide it
        val inputSize = sourceEndIndex - sourceStartIndex
        if (deflater.needsInput() && inputSize > 0) {
            deflater.setInput(source, sourceStartIndex, inputSize)
        }

        val produced = deflater.deflate(sink, sinkStartIndex, sinkEndIndex - sinkStartIndex)

        // JDK deflater copies all input at once, so consumed is either 0 or all of it
        val consumed = if (deflater.needsInput()) inputSize else 0

        return TransformResult(consumed, produced)
    }

    override fun hasPendingOutput(): Boolean = !deflater.needsInput()

    override fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): Int {
        if (!finishCalled) {
            deflater.finish()
            finishCalled = true
        }
        if (deflater.finished()) return -1
        return deflater.deflate(sink, startIndex, endIndex - startIndex)
    }

    override fun close() {
        deflater.end()
    }
}
