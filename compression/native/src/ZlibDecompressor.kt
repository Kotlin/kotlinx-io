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
import kotlinx.io.unsafe.UnsafeBufferOperations
import platform.posix.memset
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
        if (finished) return -1L
        if (source.exhausted()) return 0L

        val consumed = UnsafeBufferOperations.readFromHead(source) { inputBytes, inputStart, inputEnd ->
            val available = minOf(byteCount.toInt(), inputEnd - inputStart)

            inputBytes.usePinned { pinnedInput ->
                zStream.next_in = pinnedInput.addressOf(inputStart).reinterpret()
                zStream.avail_in = available.convert()

                // Drain all output for this input
                while (zStream.avail_in > 0u && !finished) {
                    val written = UnsafeBufferOperations.writeToTail(sink, 1) { outputBytes, outputStart, outputEnd ->
                        outputBytes.usePinned { pinnedOutput ->
                            zStream.next_out = pinnedOutput.addressOf(outputStart).reinterpret()
                            zStream.avail_out = (outputEnd - outputStart).convert()

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

                            (outputEnd - outputStart) - zStream.avail_out.toInt()
                        }
                    }
                    if (written == 0) break
                }
            }

            available
        }

        return consumed.toLong()
    }

    override fun finalizeOutput(destination: ByteArray, startIndex: Int, endIndex: Int): Int {
        check(initialized) { "Decompressor is closed" }

        // Verify that decompression is complete
        if (!finished) {
            throw IOException("Truncated or corrupt deflate/gzip data")
        }
        return -1
    }

    override fun close() {
        if (!initialized) return

        inflateEnd(zStream.ptr)
        arena.clear()
        initialized = false
    }
}
