/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlinx.io.unsafe.withData

/**
 * A streaming processor that processes input bytes and produces a result of type [T].
 *
 * This interface provides a general-purpose abstraction for processing operations such as
 * hashing, MACs, checksums, and other computations over byte streams. Implementations
 * handle the stateful processing operation, including any required initialization and
 * finalization.
 *
 * The typical lifecycle is:
 * 1. Create a processor instance
 * 2. Call [process] multiple times with input data
 * 3. Call [compute] to retrieve the result
 * 4. Call [close] to release resources
 *
 * The [process] method reads bytes from the provided buffer without consuming them.
 * The bytes remain in the buffer after processing, allowing them to be used for
 * other purposes (e.g., writing to a sink while computing a checksum).
 *
 * Processor instances are not thread-safe and should only be used by a single thread.
 *
 * Example usage:
 * ```kotlin
 * // Hashing a file
 * val hash: ByteString = fileSource.compute(Sha256())
 *
 * // Manual usage
 * val hasher = Sha256()
 * hasher.process(buffer1, buffer1.size)
 * hasher.process(buffer2, buffer2.size)
 * val result = hasher.compute()
 * hasher.close()
 * ```
 *
 * @param T the type of the processed result (e.g., ByteString for hashes, Long for checksums)
 * @see RawSource.compute
 */
public interface Processor<out T> : AutoCloseable {
    /**
     * Processes up to [byteCount] bytes from [source] without consuming them.
     *
     * Reads up to [byteCount] bytes from [source] and incorporates them into the
     * computation. The bytes remain in [source] after this method returns.
     *
     * @param source buffer containing input data to process
     * @param byteCount maximum number of bytes to read from [source]
     * @throws IllegalArgumentException if [byteCount] is negative
     * @throws IllegalStateException if called after [close]
     * @throws IOException if an I/O error occurs
     */
    public fun process(source: Buffer, byteCount: Long)

    /**
     * Computes and returns the result.
     *
     * @return the computed result
     * @throws IllegalStateException if called after [close]
     * @throws IOException if computation fails
     */
    public fun compute(): T

    /**
     * Releases all resources associated with this processor.
     *
     * After closing, the processor should not be used.
     */
    override fun close()
}

/**
 * Processes all data from this source using the specified [processor] and returns the result.
 *
 * This function reads all remaining data from the source, processes it through
 * the processor, and returns the computed result. The source is fully consumed
 * and closed by this operation. The processor is NOT closed and can be reused.
 *
 * @param processor the processor to process data with
 * @return the processed result
 * @throws IllegalStateException when the source is closed
 * @throws IOException when some I/O error occurs
 */
public fun <T> RawSource.compute(processor: Processor<T>): T {
    val tempBuffer = Buffer()
    use {
        while (true) {
            val bytesRead = readAtMostTo(tempBuffer, PROCESSOR_BUFFER_SIZE)
            if (bytesRead == -1L) break
            processor.process(tempBuffer, tempBuffer.size)
            tempBuffer.clear()
        }
    }
    return processor.compute()
}

private const val PROCESSOR_BUFFER_SIZE = 8192L

/**
 * Returns a [RawSource] that passes data through while the [processor] observes it.
 *
 * The returned source reads data from this source and passes it to the processor
 * before returning it to the caller. The data flows through unchanged - the processor
 * only observes it.
 *
 * This allows computing a hash or checksum while streaming data through a pipeline:
 * ```kotlin
 * val crc = Crc32()
 * val source = fileSource.processedWith(crc)
 * source.transferTo(outputSink)  // data flows through, crc observes
 * val checksum = crc.compute()   // get the checksum
 * ```
 *
 * The processor is NOT closed when the returned source is closed. The caller
 * is responsible for managing the processor's lifecycle.
 *
 * @param processor the processor to observe the data
 * @return a new [RawSource] that passes data through while the processor observes
 */
public fun RawSource.processedWith(processor: Processor<*>): RawSource {
    return ProcessingSource(this, processor)
}

/**
 * Returns a [RawSink] that passes data through while the [processor] observes it.
 *
 * The returned sink passes data to the processor before writing it to this sink.
 * The data flows through unchanged - the processor only observes it.
 *
 * This allows computing a hash or checksum while streaming data through a pipeline:
 * ```kotlin
 * val sha = Sha256()
 * val sink = fileSink.processedWith(sha)
 * sink.write(data)  // data flows through, sha observes
 * sink.close()
 * val hash = sha.compute()  // get the hash
 * ```
 *
 * The processor is NOT closed when the returned sink is closed. The caller
 * is responsible for managing the processor's lifecycle.
 *
 * @param processor the processor to observe the data
 * @return a new [RawSink] that passes data through while the processor observes
 */
public fun RawSink.processedWith(processor: Processor<*>): RawSink {
    return ProcessingSink(this, processor)
}

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
 * class Sha256Processor : ByteArrayProcessor<ByteArray>() {
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
// TODO: rename it to some kind of `Unsafe` processor, move it to `unsafe` package and make `UnsafeIoApi` a sub-class opt-in
@UnsafeIoApi
public abstract class ByteArrayProcessor<out T> : Processor<T> {
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

    // TODO: drop final?
    final override fun process(source: Buffer, byteCount: Long) {
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

/**
 * A [RawSource] that passes data through while a processor observes it.
 */
internal class ProcessingSource(
    private val upstream: RawSource,
    private val processor: Processor<*>
) : RawSource {

    private var closed = false

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        check(!closed) { "Source is closed." }
        require(byteCount >= 0) { "byteCount: $byteCount" }

        if (byteCount == 0L) return 0L

        val bytesRead = upstream.readAtMostTo(sink, byteCount)

        if (bytesRead > 0) {
            processor.process(sink, bytesRead)
        }

        return bytesRead
    }

    override fun close() {
        if (closed) return
        closed = true
        // Don't close the processor - caller manages its lifecycle
        upstream.close()
    }
}

/**
 * A [RawSink] that passes data through while a processor observes it.
 */
internal class ProcessingSink(
    private val downstream: RawSink,
    private val processor: Processor<*>
) : RawSink {

    private var closed = false

    override fun write(source: Buffer, byteCount: Long) {
        check(!closed) { "Sink is closed." }
        require(byteCount >= 0) { "byteCount: $byteCount" }

        if (byteCount == 0L) return

        // Process the data before writing (process doesn't consume)
        processor.process(source, byteCount)

        // Write to downstream
        downstream.write(source, byteCount)
    }

    override fun flush() {
        check(!closed) { "Sink is closed." }
        downstream.flush()
    }

    override fun close() {
        if (closed) return
        closed = true
        // Don't close the processor - caller manages its lifecycle
        downstream.close()
    }
}
