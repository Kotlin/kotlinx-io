/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Buffer
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * A [Decompressor] implementation that uses [java.util.zip.Inflater] for DEFLATE decompression.
 */
internal class InflaterDecompressor(
    private val inflater: Inflater
) : Decompressor {

    private val outputArray = ByteArray(BUFFER_SIZE)

    override val isFinished: Boolean
        get() = inflater.finished()

    @OptIn(UnsafeIoApi::class)
    override fun decompress(source: Buffer, sink: Buffer) {
        // Feed data to the inflater if it needs input
        if (inflater.needsInput() && !source.exhausted()) {
            val _ = UnsafeBufferOperations.readFromHead(source) { data, pos, limit ->
                val count = limit - pos
                inflater.setInput(data, pos, count)
                count
            }
        }

        // Inflate while possible
        inflateToBuffer(sink)
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
            throw CompressionException("Invalid compressed data: ${e.message}", e)
        }
    }

    private companion object {
        private const val BUFFER_SIZE = 8192
    }
}
