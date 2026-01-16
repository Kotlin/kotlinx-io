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
 * 2. Call [transform] multiple times with input data
 * 3. Call [finish] to signal end of input and flush remaining output
 * 4. Call [close] to release resources
 *
 * Transform instances are not thread-safe and should only be used by a single thread.
 *
 * @sample kotlinx.io.samples.TransformSamples.customTransform
 */
public interface Transform : AutoCloseable {
    /**
     * Transforms data from [source], consuming input bytes and writing transformed output to [sink].
     *
     * This method may not consume all available input in [source] if the internal buffer
     * is full. Callers should continue calling this method while [source] has remaining data.
     *
     * @param source buffer containing input data to transform
     * @param sink buffer to write transformed output to
     * @throws IOException if transformation fails
     */
    public fun transform(source: Buffer, sink: Buffer)

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
     * Indicates whether the transformation is complete.
     *
     * Returns `true` when the transform has processed all input data and
     * reached the end of the transformation. For some transforms (like decompression),
     * this is determined by the input stream format. For others (like compression),
     * this becomes `true` after [finish] is called.
     */
    public val isFinished: Boolean

    /**
     * Releases all resources associated with this transform.
     *
     * After closing, the transform should not be used.
     */
    override fun close()
}

/**
 * Returns a [RawSink] that transforms data written to it using the specified [transform].
 *
 * The returned sink transforms data and writes the transformed output to this sink.
 *
 * It is important to close the returned sink to ensure all transformed data is flushed
 * and any trailers are written. Closing the returned sink will also close this sink.
 *
 * @param transform the transformation to apply to written data
 * @throws IOException if transformation fails
 *
 * @sample kotlinx.io.samples.TransformSamples.transformSink
 */
public fun RawSink.transform(transform: Transform): RawSink {
    return TransformingSink(this, transform)
}

/**
 * Returns a [RawSource] that transforms data read from this source using the specified [transform].
 *
 * The returned source reads data from this source and returns transformed data.
 *
 * Closing the returned source will also close this source.
 *
 * @param transform the transformation to apply to read data
 * @throws IOException if transformation fails
 *
 * @sample kotlinx.io.samples.TransformSamples.transformSource
 */
public fun RawSource.transform(transform: Transform): RawSource {
    return TransformingSource(this, transform)
}

/**
 * A [RawSink] that transforms data written to it and forwards the transformed data to a downstream sink.
 *
 * This class implements streaming transformation: data is transformed as it's written,
 * and transformed chunks are forwarded to the downstream sink periodically.
 */
internal class TransformingSink(
    private val downstream: RawSink,
    private val transform: Transform
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

        // Transform the input
        transform.transform(inputBuffer, outputBuffer)

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
            transform.finish(outputBuffer)
            emitTransformedData()
        } catch (e: Throwable) {
            thrown = e
        }

        // Close the transform to release resources
        try {
            transform.close()
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
    private val transform: Transform
) : RawSource {

    private val inputBuffer = Buffer()
    private var closed = false

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        check(!closed) { "Source is closed." }
        require(byteCount >= 0) { "byteCount: $byteCount" }

        if (byteCount == 0L) return 0L

        // If transformation is already finished, return EOF
        if (transform.isFinished) return -1L

        val startSize = sink.size

        while (sink.size - startSize < byteCount) {
            // Try to transform from existing input
            transform.transform(inputBuffer, sink)

            // Check if we got some output
            if (sink.size - startSize > 0) {
                // We have some transformed data, return what we have
                break
            }

            // Check if transformation is complete
            if (transform.isFinished) {
                val bytesRead = sink.size - startSize
                return if (bytesRead == 0L) -1L else bytesRead
            }

            // Need more input data - read from upstream
            val read = upstream.readAtMostTo(inputBuffer, BUFFER_SIZE)
            if (read == -1L) {
                // Upstream exhausted before transformation complete
                if (!transform.isFinished) {
                    throw IOException("Unexpected end of stream")
                }
                val bytesRead = sink.size - startSize
                return if (bytesRead == 0L) -1L else bytesRead
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
            transform.close()
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
