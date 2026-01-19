/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Buffer
import kotlinx.io.ByteArrayTransformation
import kotlinx.io.IOException
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

    private val outputArray = ByteArray(BUFFER_SIZE)

    override fun transformAtMostTo(source: Buffer, sink: Buffer, byteCount: Long): Long {
        // If already finished, return EOF
        if (inflater.finished()) {
            return -1L
        }

        if (source.exhausted() && inflater.needsInput()) {
            return 0L
        }

        // Call parent implementation which will use transformToByteArray
        return super.transformAtMostTo(source, sink, byteCount)
    }

    override fun transformToByteArray(source: ByteArray, startIndex: Int, endIndex: Int): ByteArray {
        val inputSize = endIndex - startIndex
        if (inputSize == 0) return ByteArray(0)

        // If already finished, return empty
        if (inflater.finished()) return ByteArray(0)

        // Feed data to inflater
        inflater.setInput(source, startIndex, inputSize)

        // Collect all output
        val result = mutableListOf<ByteArray>()
        var totalSize = 0

        try {
            while (!inflater.needsInput() && !inflater.finished()) {
                val count = inflater.inflate(outputArray)
                if (count > 0) {
                    result.add(outputArray.copyOf(count))
                    totalSize += count
                } else {
                    break
                }
            }
        } catch (e: DataFormatException) {
            throw IOException("Invalid compressed data: ${e.message}", e)
        }

        return combineChunks(result, totalSize)
    }

    override fun finalizeToByteArray(): ByteArray {
        // Verify that decompression is complete
        if (!inflater.finished()) {
            throw IOException("Truncated or corrupt deflate data")
        }
        return ByteArray(0)
    }

    override fun close() {
        inflater.end()
    }

    private companion object {
        private const val BUFFER_SIZE = 8192
    }
}
