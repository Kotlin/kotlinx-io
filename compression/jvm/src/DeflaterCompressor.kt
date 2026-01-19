/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.ByteArrayTransformation
import kotlinx.io.UnsafeIoApi
import java.util.zip.Deflater

/**
 * A [ByteArrayTransformation] implementation that uses [java.util.zip.Deflater] for DEFLATE compression.
 */
@OptIn(UnsafeIoApi::class)
internal class DeflaterCompressor(
    private val deflater: Deflater
) : ByteArrayTransformation() {

    private val outputArray = ByteArray(BUFFER_SIZE)
    private var finished = false

    // Note: maxOutputSize returns -1 to use allocating API because DEFLATE
    // with Z_NO_FLUSH buffers data internally, making output size unpredictable.
    // The allocating API handles this by collecting output chunks.

    override fun transformToByteArray(source: ByteArray, startIndex: Int, endIndex: Int): ByteArray {
        val inputSize = endIndex - startIndex
        if (inputSize == 0) return ByteArray(0)

        deflater.setInput(source, startIndex, inputSize)

        // Collect all output from deflater
        val result = mutableListOf<ByteArray>()
        var totalSize = 0

        while (!deflater.needsInput()) {
            val count = deflater.deflate(outputArray)
            if (count > 0) {
                result.add(outputArray.copyOf(count))
                totalSize += count
            } else {
                break
            }
        }

        return combineChunks(result, totalSize)
    }

    override fun finalizeToByteArray(): ByteArray {
        if (finished) return ByteArray(0)

        deflater.finish()

        // Collect all remaining output
        val result = mutableListOf<ByteArray>()
        var totalSize = 0

        while (!deflater.finished()) {
            val count = deflater.deflate(outputArray)
            if (count > 0) {
                result.add(outputArray.copyOf(count))
                totalSize += count
            } else if (deflater.finished()) {
                break
            }
        }

        finished = true

        return combineChunks(result, totalSize)
    }

    private companion object {
        private const val BUFFER_SIZE = 8192
    }

    override fun close() {
        deflater.end()
    }
}
