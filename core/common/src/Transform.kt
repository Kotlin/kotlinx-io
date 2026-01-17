/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

/**
 * A streaming transformation that converts input bytes to output bytes.
 *
 * This interface provides a general-purpose abstraction for data transformations such as
 * compression, decompression, encryption, decryption, or encoding. Implementations handle
 * the stateful transformation operation, including any required headers, trailers, and
 * internal buffering.
 *
 * The typical lifecycle is:
 * 1. Create a transform instance
 * 2. Call [transformAtMostTo] multiple times with input data
 * 3. Call [finish] to signal end of input and flush remaining output
 * 4. Call [close] to release resources
 *
 * Transform instances are not thread-safe and should only be used by a single thread.
 *
 * @sample kotlinx.io.samples.TransformSamples.customTransform
 */
public interface Transformation : AutoCloseable {
    /**
     * Removes at least 1 and up to [byteCount] bytes from [source], transforms them,
     * and appends the result to [sink].
     *
     * The number of bytes written to [sink] may differ from the number of bytes read
     * from [source] depending on the transformation (e.g., compression may produce
     * fewer bytes, decompression may produce more).
     *
     * @param source buffer containing input data to transform
     * @param sink buffer to write transformed output to
     * @param byteCount maximum number of bytes to read from source
     * @return the number of bytes read from [source], or `-1` if this transformation
     *         is exhausted and will produce no more output
     * @throws IOException if transformation fails
     */
    public fun transformAtMostTo(source: Buffer, sink: Buffer, byteCount: Long): Long

    /**
     * Signals end of input and flushes any remaining transformed data.
     *
     * After calling this method, no more data should be transformed with this instance.
     * This method may need to be called multiple times until all output is flushed.
     *
     * @param sink buffer to write remaining transformed output to
     * @throws IOException if finishing fails
     */
    public fun finish(sink: Buffer)

    /**
     * Releases all resources associated with this transformation.
     *
     * After closing, the transform should not be used.
     */
    override fun close()
}

/**
 * Returns a [RawSink] that transforms data written to it using the specified [transformation].
 *
 * The returned sink transforms data and writes the transformed output to this sink.
 *
 * It is important to close the returned sink to ensure all transformed data is flushed
 * and any trailers are written. Closing the returned sink will also close this sink.
 *
 * @param transformation the transformation to apply to written data
 * @throws IOException if transformation fails
 *
 * @sample kotlinx.io.samples.TransformSamples.transformSink
 */
public fun RawSink.transformedWith(transformation: Transformation): RawSink {
    return TransformingSink(this, transformation)
}

/**
 * Returns a [RawSource] that transforms data read from this source using the specified [transformation].
 *
 * The returned source reads data from this source and returns transformed data.
 *
 * Closing the returned source will also close this source.
 *
 * @param transformation the transformation to apply to read data
 * @throws IOException if transformation fails
 *
 * @sample kotlinx.io.samples.TransformSamples.transformSource
 */
public fun RawSource.transformedWith(transformation: Transformation): RawSource {
    return TransformingSource(this, transformation)
}

/**
 * A [RawSink] that transforms data written to it and forwards the transformed data to a downstream sink.
 *
 * This class implements streaming transformation: data is transformed as it's written,
 * and transformed chunks are forwarded to the downstream sink periodically.
 */
internal class TransformingSink(
    private val downstream: RawSink,
    private val transformation: Transformation
) : RawSink {

    private val outputBuffer = Buffer()
    private var closed = false

    override fun write(source: Buffer, byteCount: Long) {
        check(!closed) { "Sink is closed." }
        require(byteCount >= 0) { "byteCount: $byteCount" }

        if (byteCount == 0L) return

        // Extract the requested bytes into a temporary buffer for transformation
        val inputBuffer = Buffer()
        inputBuffer.write(source, byteCount)

        // Transform all input - loop until all bytes are consumed
        while (!inputBuffer.exhausted()) {
            val consumed = transformation.transformAtMostTo(inputBuffer, outputBuffer, inputBuffer.size)
            if (consumed == -1L) break
            if (consumed == 0L && !inputBuffer.exhausted()) {
                // Transformation didn't consume anything but there's still input
                // This shouldn't happen with well-behaved transformations
                break
            }
        }

        // Forward any transformed data to downstream
        emitTransformedData()
    }

    override fun flush() {
        check(!closed) { "Sink is closed." }

        // Emit any pending transformed data
        emitTransformedData()
        downstream.flush()
    }

    override fun close() {
        if (closed) return
        closed = true

        var thrown: Throwable? = null

        // Finish transformation and write any remaining data
        try {
            transformation.finish(outputBuffer)
            emitTransformedData()
        } catch (e: Throwable) {
            thrown = e
        }

        // Close the transform to release resources
        try {
            transformation.close()
        } catch (e: Throwable) {
            if (thrown == null) thrown = e
        }

        // Close downstream
        try {
            downstream.close()
        } catch (e: Throwable) {
            if (thrown == null) thrown = e
        }

        if (thrown != null) throw thrown
    }

    private fun emitTransformedData() {
        if (outputBuffer.size > 0L) {
            downstream.write(outputBuffer, outputBuffer.size)
        }
    }
}

/**
 * A [RawSource] that transforms data read from an upstream source.
 *
 * This class implements streaming transformation: data is read from upstream
 * and transformed on demand as the caller reads from this source.
 */
internal class TransformingSource(
    private val upstream: RawSource,
    private val transformation: Transformation
) : RawSource {

    private val inputBuffer = Buffer()
    private var closed = false
    private var transformationFinished = false

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        check(!closed) { "Source is closed." }
        require(byteCount >= 0) { "byteCount: $byteCount" }

        if (byteCount == 0L) return 0L

        // If transformation is already finished, return EOF
        if (transformationFinished) return -1L

        val startSize = sink.size

        while (sink.size - startSize < byteCount) {
            val sinkSizeBefore = sink.size

            // Try to transform (even with empty input, to allow decompressors to finalize)
            val consumed = transformation.transformAtMostTo(inputBuffer, sink, inputBuffer.size)

            if (consumed == -1L) {
                // Transformation is complete
                transformationFinished = true
                val bytesRead = sink.size - startSize
                return if (bytesRead == 0L) -1L else bytesRead
            }

            // Check if transformation produced any output
            if (sink.size > sinkSizeBefore) {
                // We have some transformed data, continue to get more if needed
                continue
            }

            // No output produced and no input consumed, need more input
            // Read from upstream
            val read = upstream.readAtMostTo(inputBuffer, BUFFER_SIZE)
            if (read == -1L) {
                // Upstream exhausted
                // If inputBuffer is also empty, the transformation has nothing more to process
                if (inputBuffer.exhausted()) {
                    // Call finish to allow the transformation to finalize (e.g., for block ciphers)
                    transformation.finish(sink)
                    transformationFinished = true
                    val bytesRead = sink.size - startSize
                    return if (bytesRead == 0L) -1L else bytesRead
                }
                // Otherwise, there's still data in inputBuffer that wasn't consumed
                // This indicates the transformation expected more input (e.g., truncated compressed data)
                throw IOException("Unexpected end of stream")
            }
        }

        return sink.size - startSize
    }

    override fun close() {
        if (closed) return
        closed = true

        var thrown: Throwable? = null

        // Clear input buffer
        try {
            inputBuffer.clear()
        } catch (e: Throwable) {
            thrown = e
        }

        // Close transform
        try {
            transformation.close()
        } catch (e: Throwable) {
            if (thrown == null) thrown = e
        }

        // Close upstream
        try {
            upstream.close()
        } catch (e: Throwable) {
            if (thrown == null) thrown = e
        }

        if (thrown != null) throw thrown
    }

    private companion object {
        private const val BUFFER_SIZE = 8192L
    }
}
