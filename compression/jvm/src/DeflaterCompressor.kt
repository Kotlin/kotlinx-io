/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Buffer
import kotlinx.io.Transformation
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations
import java.util.zip.Deflater

/**
 * A [Transformation] implementation that uses [java.util.zip.Deflater] for DEFLATE compression.
 */
internal class DeflaterCompressor(
    private val deflater: Deflater
) : Transformation {

    private val outputArray = ByteArray(BUFFER_SIZE)
    private var finished = false

    @OptIn(UnsafeIoApi::class)
    override fun transform(source: Buffer, sink: Buffer) {
        // Feed data to the deflater
        while (!source.exhausted()) {
            UnsafeBufferOperations.readFromHead(source) { data, pos, limit ->
                val count = limit - pos
                deflater.setInput(data, pos, count)

                // Deflate while the deflater can produce output
                deflateToBuffer(sink)

                count
            }
        }
    }

    override fun finish(sink: Buffer) {
        if (finished) return

        deflater.finish()

        while (!deflater.finished()) {
            deflateToBuffer(sink)
        }

        finished = true
    }

    override val isFinished: Boolean
        get() = finished

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
