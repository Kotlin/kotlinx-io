/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.compression

import kotlinx.cinterop.*
import kotlinx.io.ByteArrayTransformation
import kotlinx.io.IOException
import kotlinx.io.TransformResult
import kotlinx.io.UnsafeIoApi
import platform.posix.memset
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
    private var finishCalled = false
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

    override fun transformIntoByteArray(
        source: ByteArray,
        sourceStart: Int,
        sourceEnd: Int,
        destination: ByteArray,
        destinationStart: Int,
        destinationEnd: Int
    ): TransformResult {
        check(initialized) { "Compressor is closed" }

        val inputSize = sourceEnd - sourceStart

        return source.usePinned { pinnedInput ->
            destination.usePinned { pinnedOutput ->
                zStream.next_in = pinnedInput.addressOf(sourceStart).reinterpret()
                zStream.avail_in = inputSize.convert()
                zStream.next_out = pinnedOutput.addressOf(destinationStart).reinterpret()
                zStream.avail_out = (destinationEnd - destinationStart).convert()

                val deflateResult = deflate(zStream.ptr, Z_NO_FLUSH)

                if (deflateResult != Z_OK && deflateResult != Z_BUF_ERROR) {
                    throw IOException("Compression failed: ${zlibErrorMessage(deflateResult)}")
                }

                val consumed = inputSize - zStream.avail_in.toInt()
                val produced = (destinationEnd - destinationStart) - zStream.avail_out.toInt()

                TransformResult(consumed, produced)
            }
        }
    }

    // Native zlib doesn't buffer internally, so no pending output
    override fun hasPendingOutput(): Boolean = false

    override fun finalizeOutput(destination: ByteArray, startIndex: Int, endIndex: Int): Int {
        check(initialized) { "Compressor is closed" }
        if (finished) return -1

        if (!finishCalled) {
            zStream.next_in = null
            zStream.avail_in = 0u
            finishCalled = true
        }

        return destination.usePinned { pinnedOutput ->
            zStream.next_out = pinnedOutput.addressOf(startIndex).reinterpret()
            zStream.avail_out = (endIndex - startIndex).convert()

            val deflateResult = deflate(zStream.ptr, Z_FINISH)

            val written = (endIndex - startIndex) - zStream.avail_out.toInt()

            when (deflateResult) {
                Z_OK, Z_BUF_ERROR -> {
                    // More output pending
                    written
                }
                Z_STREAM_END -> {
                    finished = true
                    if (written > 0) written else -1
                }
                else -> {
                    throw IOException("Compression failed: ${zlibErrorMessage(deflateResult)}")
                }
            }
        }
    }

    override fun close() {
        if (!initialized) return

        deflateEnd(zStream.ptr)
        arena.clear()
        initialized = false
    }
}
