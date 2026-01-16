/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.compression

import kotlinx.cinterop.*
import kotlinx.io.Buffer
import kotlinx.io.UnsafeIoApi
import kotlinx.io.unsafe.UnsafeBufferOperations
import platform.zlib.*

/**
 * A [Decompressor] implementation that uses zlib for DEFLATE/GZIP decompression.
 *
 * @param windowBits determines the format:
 *        - Negative (-15 to -8): raw DEFLATE
 *        - 8-15: zlib format
 *        - 24-31 (windowBits + 16): GZIP format
 *        - 32-47 (windowBits + 32): auto-detect zlib or GZIP
 */
internal class ZlibDecompressor(
    windowBits: Int
) : Decompressor {

    private val arena = Arena()
    private val zStream: z_stream = arena.alloc()
    private val outputBuffer = ByteArray(BUFFER_SIZE)
    private var finished = false
    private var initialized = true

    init {
        // Initialize the z_stream structure
        memset(zStream.ptr, 0, sizeOf<z_stream>().convert())

        val result = inflateInit2(zStream.ptr, windowBits)

        if (result != Z_OK) {
            arena.clear()
            throw CompressionException("Failed to initialize zlib inflate: ${zlibErrorMessage(result)}")
        }
    }

    override val isFinished: Boolean
        get() = finished

    @OptIn(UnsafeIoApi::class)
    override fun decompress(source: Buffer, sink: Buffer) {
        check(initialized) { "Decompressor is closed" }

        if (finished) return

        // Feed data to zlib if available
        if (!source.exhausted()) {
            UnsafeBufferOperations.readFromHead(source) { data, pos, limit ->
                val count = limit - pos

                data.usePinned { pinnedInput ->
                    zStream.next_in = pinnedInput.addressOf(pos).reinterpret()
                    zStream.avail_in = count.convert()

                    inflateLoop(sink)
                }

                // Return the number of bytes consumed by zlib
                count - zStream.avail_in.toInt()
            }
        }
    }

    override fun close() {
        if (!initialized) return

        inflateEnd(zStream.ptr)
        arena.clear()
        initialized = false
    }

    private fun inflateLoop(sink: Buffer) {
        outputBuffer.usePinned { pinnedOutput ->
            do {
                zStream.next_out = pinnedOutput.addressOf(0).reinterpret()
                zStream.avail_out = BUFFER_SIZE.convert()

                val result = inflate(zStream.ptr, Z_NO_FLUSH)

                when (result) {
                    Z_OK, Z_BUF_ERROR -> {
                        // Continue processing
                    }
                    Z_STREAM_END -> {
                        finished = true
                    }
                    Z_DATA_ERROR -> {
                        throw CompressionException("Invalid compressed data: ${zlibErrorMessage(result)}")
                    }
                    else -> {
                        throw CompressionException("Decompression failed: ${zlibErrorMessage(result)}")
                    }
                }

                val produced = BUFFER_SIZE - zStream.avail_out.toInt()
                if (produced > 0) {
                    sink.write(outputBuffer, 0, produced)
                }

            } while (zStream.avail_out == 0u && !finished)
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
