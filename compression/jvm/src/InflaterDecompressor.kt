/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.Transformation
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * A [Transformation] implementation that uses [java.util.zip.Inflater] for DEFLATE decompression.
 */
internal class InflaterDecompressor(
    private val inflater: Inflater
) : Transformation {

    private val inputArray = ByteArray(BUFFER_SIZE)
    private val outputArray = ByteArray(BUFFER_SIZE)

    override fun transformAtMostTo(source: Buffer, sink: Buffer, byteCount: Long): Long {
        // If already finished, return EOF
        if (inflater.finished()) {
            return -1L
        }

        if (source.exhausted() && inflater.needsInput()) {
            return 0L
        }

        var totalConsumed = 0L

        // Consume up to byteCount bytes from source and decompress
        while (totalConsumed < byteCount && !source.exhausted()) {
            // Feed data to the inflater if it needs input
            if (inflater.needsInput()) {
                val toRead = minOf(source.size, byteCount - totalConsumed, BUFFER_SIZE.toLong()).toInt()
                @Suppress("UNUSED_VALUE")
                val ignored = source.readAtMostTo(inputArray, 0, toRead)
                inflater.setInput(inputArray, 0, toRead)
                totalConsumed += toRead
            }

            // Inflate while possible
            inflateToBuffer(sink)

            // If inflater finished, we're done
            if (inflater.finished()) {
                return if (totalConsumed == 0L) -1L else totalConsumed
            }
        }

        return totalConsumed
    }

    override fun finish(sink: Buffer) {
        // Verify that decompression is complete
        if (!inflater.finished()) {
            throw IOException("Truncated or corrupt deflate data")
        }
    }

    override fun close() {
        inflater.end()
    }

    private fun inflateToBuffer(sink: Buffer) {
        try {
            while (true) {
                val count = inflater.inflate(outputArray)
                if (count > 0) {
                    sink.write(outputArray, 0, count)
                }
                if (count == 0 || inflater.finished() || inflater.needsInput()) {
                    break
                }
            }
        } catch (e: DataFormatException) {
            throw IOException("Invalid compressed data: ${e.message}", e)
        }
    }

    private companion object {
        private const val BUFFER_SIZE = 8192
    }
}
