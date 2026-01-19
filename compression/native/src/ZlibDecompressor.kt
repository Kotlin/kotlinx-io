/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.compression

import kotlinx.cinterop.*
import kotlinx.io.Buffer
import kotlinx.io.ByteArrayTransformation
import kotlinx.io.IOException
import kotlinx.io.UnsafeIoApi
import platform.zlib.*

/**
 * A [ByteArrayTransformation] implementation that uses zlib for DEFLATE/GZIP decompression.
 *
 * @param windowBits determines the format:
 *        - Negative (-15 to -8): raw DEFLATE
 *        - 8-15: zlib format
 *        - 24-31 (windowBits + 16): GZIP format
 *        - 32-47 (windowBits + 32): auto-detect zlib or GZIP
 */
@OptIn(UnsafeIoApi::class)
internal class ZlibDecompressor(
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

        val result = inflateInit2(zStream.ptr, windowBits)

        if (result != Z_OK) {
            arena.clear()
            throw IOException("Failed to initialize zlib inflate: ${zlibErrorMessage(result)}")
        }
    }

    override fun transformAtMostTo(source: Buffer, sink: Buffer, byteCount: Long): Long {
        check(initialized) { "Decompressor is closed" }

        // If already finished, return EOF
        if (finished) {
            return -1L
        }

        if (source.exhausted()) return 0L

        // Call parent implementation which will use transformToByteArray
        return super.transformAtMostTo(source, sink, byteCount)
    }

    override fun transformToByteArray(source: ByteArray, startIndex: Int, endIndex: Int): ByteArray {
        check(initialized) { "Decompressor is closed" }

        val inputSize = endIndex - startIndex
        if (inputSize == 0) return ByteArray(0)

        // If already finished, return empty
        if (finished) return ByteArray(0)

        // Collect all output
        val result = mutableListOf<ByteArray>()
        var totalSize = 0

        source.usePinned { pinnedInput ->
            zStream.next_in = pinnedInput.addressOf(startIndex).reinterpret()
            zStream.avail_in = inputSize.convert()

            outputBuffer.usePinned { pinnedOutput ->
                do {
                    zStream.next_out = pinnedOutput.addressOf(0).reinterpret()
                    zStream.avail_out = BUFFER_SIZE.convert()

                    val inflateResult = inflate(zStream.ptr, Z_NO_FLUSH)

                    when (inflateResult) {
                        Z_OK, Z_BUF_ERROR -> {
                            // Continue processing
                        }
                        Z_STREAM_END -> {
                            finished = true
                        }
                        Z_DATA_ERROR -> {
                            throw IOException("Invalid compressed data: ${zlibErrorMessage(inflateResult)}")
                        }
                        else -> {
                            throw IOException("Decompression failed: ${zlibErrorMessage(inflateResult)}")
                        }
                    }

                    val produced = BUFFER_SIZE - zStream.avail_out.toInt()
                    if (produced > 0) {
                        result.add(outputBuffer.copyOf(produced))
                        totalSize += produced
                    }

                } while (zStream.avail_out == 0u && !finished)
            }
        }

        return combineChunks(result, totalSize)
    }

    override fun finalizeToByteArray(): ByteArray {
        // Verify that decompression is complete
        if (!finished) {
            throw IOException("Truncated or corrupt deflate/gzip data")
        }
        return ByteArray(0)
    }

    override fun close() {
        if (!initialized) return

        inflateEnd(zStream.ptr)
        arena.clear()
        initialized = false
    }

    private companion object {
        private const val BUFFER_SIZE = 8192
    }
}
