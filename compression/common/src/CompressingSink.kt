/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Buffer
import kotlinx.io.RawSink

/**
 * A [RawSink] that compresses data written to it and forwards the compressed data to a downstream sink.
 *
 * This class implements streaming compression: data is compressed as it's written,
 * and compressed chunks are forwarded to the downstream sink periodically.
 */
internal class CompressingSink(
    private val downstream: RawSink,
    private val compressor: Compressor
) : RawSink {

    private val outputBuffer = Buffer()
    private var closed = false

    override fun write(source: Buffer, byteCount: Long) {
        check(!closed) { "Sink is closed." }
        require(byteCount >= 0) { "byteCount: $byteCount" }

        if (byteCount == 0L) return

        // Extract the requested bytes into a temporary buffer for compression
        val inputBuffer = Buffer()
        inputBuffer.write(source, byteCount)

        // Compress the input
        compressor.compress(inputBuffer, outputBuffer)

        // Forward any compressed data to downstream
        emitCompressedData()
    }

    override fun flush() {
        check(!closed) { "Sink is closed." }

        // Emit any pending compressed data
        emitCompressedData()
        downstream.flush()
    }

    override fun close() {
        if (closed) return
        closed = true

        var thrown: Throwable? = null

        // Finish compression and write any remaining data
        try {
            compressor.finish(outputBuffer)
            emitCompressedData()
        } catch (e: Throwable) {
            thrown = e
        }

        // Close the compressor to release resources
        try {
            compressor.close()
        } catch (e: Throwable) {
            if (thrown == null) thrown = e
        }

        // Close downstream
        try {
            downstream.close()
        } catch (e: Throwable) {
            if (thrown == null) thrown = e
        }

        if (thrown != null) throw thrown
    }

    private fun emitCompressedData() {
        if (outputBuffer.size > 0L) {
            downstream.write(outputBuffer, outputBuffer.size)
        }
    }
}
