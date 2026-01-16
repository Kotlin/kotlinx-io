/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import kotlinx.io.Buffer
import kotlinx.io.RawSource

/**
 * A [RawSource] that decompresses data read from an upstream source.
 *
 * This class implements streaming decompression: compressed data is read from upstream
 * and decompressed on demand as the caller reads from this source.
 */
internal class DecompressingSource(
    private val upstream: RawSource,
    private val decompressor: Decompressor
) : RawSource {

    private val inputBuffer = Buffer()
    private var closed = false

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        check(!closed) { "Source is closed." }
        require(byteCount >= 0) { "byteCount: $byteCount" }

        if (byteCount == 0L) return 0L

        // If decompression is already finished, return EOF
        if (decompressor.isFinished) return -1L

        val startSize = sink.size

        while (sink.size - startSize < byteCount) {
            // Try to decompress from existing input
            decompressor.decompress(inputBuffer, sink)

            // Check if we got some output
            if (sink.size - startSize > 0) {
                // We have some decompressed data, return what we have
                break
            }

            // Check if decompression is complete
            if (decompressor.isFinished) {
                val bytesRead = sink.size - startSize
                return if (bytesRead == 0L) -1L else bytesRead
            }

            // Need more input data - read from upstream
            val read = upstream.readAtMostTo(inputBuffer, BUFFER_SIZE)
            if (read == -1L) {
                // Upstream exhausted before decompression complete
                if (!decompressor.isFinished) {
                    throw CompressionException("Unexpected end of compressed stream")
                }
                val bytesRead = sink.size - startSize
                return if (bytesRead == 0L) -1L else bytesRead
            }
        }

        return sink.size - startSize
    }

    override fun close() {
        if (closed) return
        closed = true

        var thrown: Throwable? = null

        // Clear input buffer
        try {
            inputBuffer.clear()
        } catch (e: Throwable) {
            thrown = e
        }

        // Close decompressor
        try {
            decompressor.close()
        } catch (e: Throwable) {
            if (thrown == null) thrown = e
        }

        // Close upstream
        try {
            upstream.close()
        } catch (e: Throwable) {
            if (thrown == null) thrown = e
        }

        if (thrown != null) throw thrown
    }

    private companion object {
        private const val BUFFER_SIZE = 8192L
    }
}
