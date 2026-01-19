/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.compression

import kotlinx.cinterop.*
import kotlinx.io.ByteArrayTransformation
import kotlinx.io.IOException
import kotlinx.io.UnsafeIoApi
import platform.zlib.*

/**
 * A [ByteArrayTransformation] implementation that uses zlib for DEFLATE/GZIP compression.
 *
 * @param level compression level (0-9)
 * @param windowBits determines the format:
 *        - Negative (-15 to -8): raw DEFLATE
 *        - 8-15: zlib format
 *        - 24-31 (windowBits + 16): GZIP format
 */
@OptIn(UnsafeIoApi::class)
internal class ZlibCompressor(
    level: Int,
    windowBits: Int
) : ByteArrayTransformation() {

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

    override fun transformToByteArray(source: ByteArray, startIndex: Int, endIndex: Int): ByteArray {
        check(initialized) { "Compressor is closed" }

        val inputSize = endIndex - startIndex
        if (inputSize == 0) return ByteArray(0)

        // Collect all output
        val result = mutableListOf<ByteArray>()
        var totalSize = 0

        source.usePinned { pinnedInput ->
            zStream.next_in = pinnedInput.addressOf(startIndex).reinterpret()
            zStream.avail_in = inputSize.convert()

            outputBuffer.usePinned { pinnedOutput ->
                while (zStream.avail_in > 0u) {
                    zStream.next_out = pinnedOutput.addressOf(0).reinterpret()
                    zStream.avail_out = BUFFER_SIZE.convert()

                    val deflateResult = deflate(zStream.ptr, Z_NO_FLUSH)

                    if (deflateResult != Z_OK && deflateResult != Z_BUF_ERROR) {
                        throw IOException("Compression failed: ${zlibErrorMessage(deflateResult)}")
                    }

                    val produced = BUFFER_SIZE - zStream.avail_out.toInt()
                    if (produced > 0) {
                        result.add(outputBuffer.copyOf(produced))
                        totalSize += produced
                    }
                }
            }
        }

        return combineChunks(result, totalSize)
    }

    override fun finalizeToByteArray(): ByteArray {
        if (finished) return ByteArray(0)
        check(initialized) { "Compressor is closed" }

        // Set empty input and finish
        zStream.next_in = null
        zStream.avail_in = 0u

        // Collect all remaining output
        val result = mutableListOf<ByteArray>()
        var totalSize = 0

        outputBuffer.usePinned { pinnedOutput ->
            var deflateResult: Int
            do {
                zStream.next_out = pinnedOutput.addressOf(0).reinterpret()
                zStream.avail_out = BUFFER_SIZE.convert()

                deflateResult = deflate(zStream.ptr, Z_FINISH)

                if (deflateResult != Z_OK && deflateResult != Z_STREAM_END && deflateResult != Z_BUF_ERROR) {
                    throw IOException("Compression failed: ${zlibErrorMessage(deflateResult)}")
                }

                val produced = BUFFER_SIZE - zStream.avail_out.toInt()
                if (produced > 0) {
                    result.add(outputBuffer.copyOf(produced))
                    totalSize += produced
                }
            } while (deflateResult != Z_STREAM_END)
        }

        finished = true

        return combineChunks(result, totalSize)
    }

    override fun close() {
        if (!initialized) return

        deflateEnd(zStream.ptr)
        arena.clear()
        initialized = false
    }

    private companion object {
        private const val BUFFER_SIZE = 8192
    }
}
