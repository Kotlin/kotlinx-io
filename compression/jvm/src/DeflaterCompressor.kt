/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.ByteArrayTransformation
import kotlinx.io.TransformResult
import kotlinx.io.UnsafeIoApi
import java.util.zip.Deflater

/**
 * A [ByteArrayTransformation] implementation that uses [java.util.zip.Deflater] for DEFLATE compression.
 */
@OptIn(UnsafeIoApi::class)
internal class DeflaterCompressor(
    private val deflater: Deflater
) : ByteArrayTransformation() {

    private var finishCalled = false

    override fun transformIntoByteArray(
        source: ByteArray,
        sourceStart: Int,
        sourceEnd: Int,
        destination: ByteArray,
        destinationStart: Int,
        destinationEnd: Int
    ): TransformResult {
        // If deflater needs input and we have some, provide it
        val inputSize = sourceEnd - sourceStart
        if (deflater.needsInput() && inputSize > 0) {
            deflater.setInput(source, sourceStart, inputSize)
        }

        val produced = deflater.deflate(destination, destinationStart, destinationEnd - destinationStart)

        // JDK deflater copies all input at once, so consumed is either 0 or all of it
        val consumed = if (deflater.needsInput()) inputSize else 0

        return TransformResult(consumed, produced)
    }

    override fun hasPendingOutput(): Boolean = !deflater.needsInput()

    override fun finalizeOutput(destination: ByteArray, startIndex: Int, endIndex: Int): Int {
        if (!finishCalled) {
            deflater.finish()
            finishCalled = true
        }
        if (deflater.finished()) return -1
        return deflater.deflate(destination, startIndex, endIndex - startIndex)
    }

    override fun close() {
        deflater.end()
    }
}
