/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(ExperimentalForeignApi::class)

package kotlinx.io.compression

import kotlinx.cinterop.*
import kotlinx.io.Buffer
import kotlinx.io.unsafe.UnsafeByteArrayTransformation
import kotlinx.io.IOException
import kotlinx.io.UnsafeIoApi
import platform.posix.memset
import platform.zlib.*

/**
 * A [UnsafeByteArrayTransformation] implementation that uses zlib for DEFLATE/GZIP decompression.
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
) : UnsafeByteArrayTransformation() {

    private val arena = Arena()
    private val zStream: z_stream = arena.alloc()
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

    override fun maxOutputSize(inputSize: Int): Int = -1

    override fun transformTo(source: Buffer, byteCount: Long, sink: Buffer): Long {
        check(initialized) { "Decompressor is closed" }

        // If already finished, return EOF
        if (finished) return -1L
        if (source.exhausted()) return 0L

        return super.transformTo(source, byteCount, sink)
    }

    override fun transformIntoByteArray(
        source: ByteArray,
        sourceStartIndex: Int,
        sourceEndIndex: Int,
        sink: ByteArray,
        sinkStartIndex: Int,
        sinkEndIndex: Int
    ): TransformResult {
        check(initialized) { "Decompressor is closed" }

        val inputSize = sourceEndIndex - sourceStartIndex
        val outputSize = sinkEndIndex - sinkStartIndex

        return source.usePinned { pinnedInput ->
            sink.usePinned { pinnedOutput ->
                zStream.next_in = pinnedInput.addressOf(sourceStartIndex).reinterpret()
                zStream.avail_in = inputSize.convert()
                zStream.next_out = pinnedOutput.addressOf(sinkStartIndex).reinterpret()
                zStream.avail_out = outputSize.convert()

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

                val consumed = inputSize - zStream.avail_in.toInt()
                val produced = outputSize - zStream.avail_out.toInt()

                TransformResult(consumed, produced)
            }
        }
    }

    override fun transformToByteArray(
        source: ByteArray,
        sourceStartIndex: Int,
        sourceEndIndex: Int
    ): ByteArray = ByteArray(0)

    override fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): Int {
        check(initialized) { "Decompressor is closed" }

        // Verify that decompression is complete
        if (!finished) {
            throw IOException("Truncated or corrupt deflate/gzip data")
        }
        return -1
    }

    override fun finalizeToByteArray(): ByteArray = ByteArray(0)

    override fun close() {
        if (!initialized) return

        inflateEnd(zStream.ptr)
        arena.clear()
        initialized = false
    }
}
