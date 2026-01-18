/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

/**
 * A streaming processor that consumes input bytes and produces a result of type [T].
 *
 * This interface provides a general-purpose abstraction for processing operations such as
 * hashing, MACs, checksums, and other computations over byte streams. Implementations
 * handle the stateful processing operation, including any required initialization and
 * finalization.
 *
 * The typical lifecycle is:
 * 1. Create a processor instance
 * 2. Call [process] multiple times with input data
 * 3. Call [compute] to retrieve the result (this also resets the processor)
 * 4. Optionally reuse by going back to step 2, or call [close] to release resources
 *
 * The [process] method reads bytes from the provided buffer without consuming them.
 * The bytes remain in the buffer after processing, allowing them to be used for
 * other purposes (e.g., writing to a sink while computing a checksum).
 *
 * Calling [compute] returns the result and resets the processor to its initial state,
 * ready for a new computation.
 *
 * For processors that support reading intermediate results without resetting,
 * see [RunningProcessor].
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
 * val result = hasher.compute()  // get result and reset
 * hasher.close()
 * ```
 *
 * @param T the type of the processed result (e.g., ByteString for hashes, Long for checksums)
 * @see RunningProcessor
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
     * Computes and returns the result, then resets the processor to its initial state.
     *
     * After calling this method, the processor is ready for a new computation.
     * Any subsequent calls to [process] will start fresh.
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
 * A [Processor] that supports reading intermediate results without resetting.
 *
 * This interface extends [Processor] to add the ability to read the current
 * computed value at any point without affecting the ongoing computation.
 * This is useful for checksums and other computations where intermediate
 * values are meaningful.
 *
 * Example usage:
 * ```kotlin
 * val crc = Crc32()  // implements RunningProcessor<Long>
 * crc.process(chunk1, chunk1.size)
 * val intermediate = crc.current()  // read without resetting
 * crc.process(chunk2, chunk2.size)
 * val stillRunning = crc.current()  // read again
 * val final = crc.compute()  // get final result and reset
 * ```
 *
 * @param T the type of the processed result
 * @see Processor
 */
public interface RunningProcessor<out T> : Processor<T> {
    /**
     * Returns the current computed value without resetting the processor.
     *
     * Unlike [compute], this method does not reset the processor state.
     * Further calls to [process] will continue from the current state,
     * and subsequent calls to [current] will reflect the updated value.
     *
     * @return the current computed value
     * @throws IllegalStateException if called after [close]
     * @throws IOException if computation fails
     */
    public fun current(): T
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
