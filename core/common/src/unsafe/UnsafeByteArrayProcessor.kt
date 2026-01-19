/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.unsafe

import kotlinx.io.Buffer
import kotlinx.io.Processor
import kotlinx.io.UnsafeIoApi

/**
 * Abstract base class for implementing [Processor] using `ByteArray`-based APIs.
 *
 * This class provides a convenient way to implement processors that work with
 * `ByteArray` operations (such as JDK's `MessageDigest` or `CRC32`) without
 * requiring manual buffer management. The class uses kotlinx.io's unsafe API
 * internally to avoid unnecessary copies when accessing buffer data.
 *
 * Subclasses implement [process] with a `ByteArray` signature instead of `Buffer`.
 * The base class handles buffer iteration using the unsafe API.
 *
 * Example implementation for a hash processor:
 * ```kotlin
 * class Sha256Processor : UnsafeByteArrayProcessor<ByteArray>() {
 *     private val digest = MessageDigest.getInstance("SHA-256")
 *
 *     override fun process(source: ByteArray, startIndex: Int, endIndex: Int) {
 *         digest.update(source, startIndex, endIndex - startIndex)
 *     }
 *
 *     override fun compute(): ByteArray = digest.digest()
 *
 *     override fun close() {}
 * }
 * ```
 *
 * @param T the type of the computed result
 * @see Processor
 */
@SubclassOptInRequired(UnsafeIoApi::class)
public abstract class UnsafeByteArrayProcessor<out T> : Processor<T> {
    /**
     * Processes bytes from the given array range.
     *
     * This method is called with direct access to buffer segment data,
     * avoiding unnecessary copies. The bytes in the specified range should
     * be incorporated into the computation.
     *
     * @param source the byte array containing data to process
     * @param startIndex the start index (inclusive) of data to process
     * @param endIndex the end index (exclusive) of data to process
     */
    protected abstract fun process(source: ByteArray, startIndex: Int, endIndex: Int)

    @OptIn(UnsafeIoApi::class)
    override fun process(source: Buffer, byteCount: Long) {
        require(byteCount >= 0) { "byteCount: $byteCount" }

        if (byteCount == 0L || source.exhausted()) return

        val toProcess = minOf(byteCount, source.size)
        var remaining = toProcess

        UnsafeBufferOperations.forEachSegment(source) { context, segment ->
            if (remaining <= 0) return@forEachSegment

            context.withData(segment) { bytes, startIndex, endIndex ->
                val segmentSize = endIndex - startIndex
                val bytesToProcess = minOf(remaining, segmentSize.toLong()).toInt()
                process(bytes, startIndex, startIndex + bytesToProcess)
                remaining -= bytesToProcess
            }
        }
    }
}
