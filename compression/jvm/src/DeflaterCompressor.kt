/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Buffer
import kotlinx.io.Transformation
import java.util.zip.Deflater

/**
 * A [Transformation] implementation that uses [java.util.zip.Deflater] for DEFLATE compression.
 */
internal class DeflaterCompressor(
    private val deflater: Deflater
) : Transformation {

    private val inputArray = ByteArray(BUFFER_SIZE)
    private val outputArray = ByteArray(BUFFER_SIZE)
    private var finished = false

    override fun transformAtMostTo(source: Buffer, sink: Buffer, byteCount: Long): Long {
        if (source.exhausted()) return 0L

        var totalConsumed = 0L

        // Consume up to byteCount bytes from source
        while (!source.exhausted() && totalConsumed < byteCount) {
            // Wait for deflater to be ready for more input
            if (!deflater.needsInput()) {
                deflateToBuffer(sink)
                continue
            }

            // Read into our own array (deflater keeps reference, so we can't use buffer's internal array)
            val toRead = minOf(source.size, byteCount - totalConsumed, BUFFER_SIZE.toLong()).toInt()
            @Suppress("UNUSED_VALUE")
            val ignored = source.readAtMostTo(inputArray, 0, toRead)

            deflater.setInput(inputArray, 0, toRead)
            deflateToBuffer(sink)

            totalConsumed += toRead
        }

        return totalConsumed
    }

    override fun finish(sink: Buffer) {
        if (finished) return

        deflater.finish()

        while (!deflater.finished()) {
            deflateToBuffer(sink)
        }

        finished = true
    }

    override fun close() {
        deflater.end()
    }

    private fun deflateToBuffer(sink: Buffer) {
        while (true) {
            val count = deflater.deflate(outputArray)
            if (count > 0) {
                sink.write(outputArray, 0, count)
            }
            if (count < outputArray.size) {
                // No more output available
                break
            }
        }
    }

    private companion object {
        private const val BUFFER_SIZE = 8192
    }
}
