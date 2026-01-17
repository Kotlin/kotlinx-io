/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.compression

import kotlinx.cinterop.*
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.Transformation
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations
import platform.zlib.*

/**
 * A [Transformation] implementation that uses zlib for DEFLATE/GZIP compression.
 *
 * @param level compression level (0-9)
 * @param windowBits determines the format:
 *        - Negative (-15 to -8): raw DEFLATE
 *        - 8-15: zlib format
 *        - 24-31 (windowBits + 16): GZIP format
 */
internal class ZlibCompressor(
    level: Int,
    windowBits: Int
) : Transformation {

    private val arena = Arena()
    private val zStream: z_stream = arena.alloc()
    private val outputBuffer = ByteArray(BUFFER_SIZE)
    private var finished = false
    private var initialized = true

    init {
        // Initialize the z_stream structure
        memset(zStream.ptr, 0, sizeOf<z_stream>().convert())

        val result = deflateInit2(
            zStream.ptr,
            level,
            Z_DEFLATED,
            windowBits,
            8, // memLevel: default value
            Z_DEFAULT_STRATEGY
        )

        if (result != Z_OK) {
            arena.clear()
            throw IOException("Failed to initialize zlib deflate: ${zlibErrorMessage(result)}")
        }
    }

    @OptIn(UnsafeIoApi::class)
    override fun transformAtMostTo(source: Buffer, sink: Buffer, byteCount: Long): Long {
        check(initialized) { "Compressor is closed" }

        if (source.exhausted()) return 0L

        var totalConsumed = 0L

        // Consume up to byteCount bytes from source
        while (!source.exhausted() && totalConsumed < byteCount) {
            val consumed = UnsafeBufferOperations.readFromHead(source) { data, pos, limit ->
                val available = limit - pos
                val toConsume = minOf(available.toLong(), byteCount - totalConsumed).toInt()

                data.usePinned { pinnedInput ->
                    zStream.next_in = pinnedInput.addressOf(pos).reinterpret()
                    zStream.avail_in = toConsume.convert()

                    deflateLoop(sink, Z_NO_FLUSH)
                }

                toConsume
            }
            totalConsumed += consumed
        }

        return totalConsumed
    }

    override fun finish(sink: Buffer) {
        if (finished) return
        check(initialized) { "Compressor is closed" }

        // Set empty input and finish
        zStream.next_in = null
        zStream.avail_in = 0u

        deflateLoop(sink, Z_FINISH)

        finished = true
    }

    override fun close() {
        if (!initialized) return

        deflateEnd(zStream.ptr)
        arena.clear()
        initialized = false
    }

    private fun deflateLoop(sink: Buffer, flush: Int) {
        outputBuffer.usePinned { pinnedOutput ->
            do {
                zStream.next_out = pinnedOutput.addressOf(0).reinterpret()
                zStream.avail_out = BUFFER_SIZE.convert()

                val result = deflate(zStream.ptr, flush)

                if (result != Z_OK && result != Z_STREAM_END && result != Z_BUF_ERROR) {
                    throw IOException("Compression failed: ${zlibErrorMessage(result)}")
                }

                val produced = BUFFER_SIZE - zStream.avail_out.toInt()
                if (produced > 0) {
                    sink.write(outputBuffer, 0, produced)
                }
            } while (zStream.avail_out == 0u || (flush == Z_FINISH && result != Z_STREAM_END))
        }
    }

    private fun zlibErrorMessage(code: Int): String {
        return when (code) {
            Z_ERRNO -> "Z_ERRNO"
            Z_STREAM_ERROR -> "Z_STREAM_ERROR"
            Z_DATA_ERROR -> "Z_DATA_ERROR"
            Z_MEM_ERROR -> "Z_MEM_ERROR"
            Z_BUF_ERROR -> "Z_BUF_ERROR"
            Z_VERSION_ERROR -> "Z_VERSION_ERROR"
            else -> "Unknown error: $code"
        }
    }

    private companion object {
        private const val BUFFER_SIZE = 8192
    }
}
