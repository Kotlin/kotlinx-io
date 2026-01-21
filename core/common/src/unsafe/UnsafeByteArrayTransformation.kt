package kotlinx.io.unsafe

import kotlinx.io.Buffer
import kotlinx.io.Transformation
import kotlinx.io.UnsafeIoApi
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.unsafe.UnsafeByteStringApi
import kotlinx.io.bytestring.unsafe.UnsafeByteStringOperations
import kotlinx.io.readByteString

/**
 * Abstract base class for implementing [kotlinx.io.Transformation] using `ByteArray`-based APIs.
 *
 * This class provides a unified pattern for transformations where both input and output
 * arrays are provided simultaneously. This design works for:
 * - JDK APIs like [java.util.zip.Deflater] that copy input internally
 * - Native APIs like zlib that require input to stay valid during processing
 * - Bounded APIs like [javax.crypto.Cipher] where output size is predictable
 *
 * Subclasses must implement:
 * - [transformIntoByteArray] to transform input bytes into output bytes
 * - [transformFinalIntoByteArray] to process final input and produce all remaining output
 * - [close] to release resources
 *
 * @see kotlinx.io.Transformation
 */
@OptIn(UnsafeIoApi::class)
@SubclassOptInRequired(UnsafeIoApi::class)
public abstract class UnsafeByteArrayTransformation : Transformation {

    /**
     * Result of a [transformIntoByteArray] or [transformFinalIntoByteArray] operation.
     *
     * Use factory methods to create instances:
     * - [ok] for normal progress (consumed input and/or produced output)
     * - [done] when transformation is complete (no more output will be produced)
     * - [outputRequired] when a larger output buffer is needed
     *
     * Use access properties to inspect results:
     * - [consumed] / [produced] for actual bytes processed (0 if requirement result)
     * - [outputRequired] for buffer size needed (0 if ok result)
     */
    protected class TransformResult private constructor(
        private val _consumed: Int,
        private val _produced: Int
    ) {
        /**
         * Number of bytes consumed from the input array.
         * Returns 0 if this is a requirement result ([outputRequired]).
         */
        public val consumed: Int get() = _consumed

        /**
         * Number of bytes written to the output array.
         * Returns 0 if this is a requirement result ([outputRequired]).
         */
        public val produced: Int get() = if (_produced >= 0) _produced else 0

        /**
         * Size of output buffer required to proceed.
         * Returns 0 if this is an ok result (normal progress).
         */
        public val outputRequired: Int get() = if (_produced < 0) -_produced else 0

        public companion object {
            /**
             * Creates a result indicating normal progress.
             *
             * @param consumed number of bytes consumed from input (must be >= 0)
             * @param produced number of bytes written to output (must be >= 0)
             * @throws IllegalArgumentException if consumed or produced is negative
             */
            public fun ok(consumed: Int, produced: Int): TransformResult {
                require(consumed >= 0) { "consumed must be non-negative: $consumed" }
                require(produced >= 0) { "produced must be non-negative: $produced" }
                return TransformResult(consumed, produced)
            }

            /**
             * Creates a result indicating transformation is complete.
             * Equivalent to `ok(0, 0)`.
             */
            public fun done(): TransformResult = TransformResult(0, 0)

            /**
             * Creates a result indicating that a larger output buffer is needed.
             *
             * @param size minimum output buffer size required (must be > 0)
             * @throws IllegalArgumentException if size is not positive
             */
            public fun outputRequired(size: Int): TransformResult {
                require(size > 0) { "size must be positive: $size" }
                return TransformResult(0, -size)
            }
        }
    }

    /**
     * Transforms input bytes into output bytes.
     *
     * Both input and output arrays are valid for the duration of this call,
     * allowing implementations to pin memory or hold references as needed.
     *
     * @param source the byte array containing input data
     * @param sourceStartIndex the start index (inclusive) of input data in [source]
     * @param sourceEndIndex the end index (exclusive) of input data in [source]
     * @param sink the byte array to write output to
     * @param sinkStartIndex the start index (inclusive) in [sink] to write from
     * @param sinkEndIndex the end index (exclusive) of available space in [sink]
     * @return result indicating progress, or buffer requirements
     */
    protected abstract fun transformIntoByteArray(
        source: ByteArray,
        sourceStartIndex: Int,
        sourceEndIndex: Int,
        sink: ByteArray,
        sinkStartIndex: Int,
        sinkEndIndex: Int
    ): TransformResult

    /**
     * Transforms the final chunk of input and produces all remaining output.
     *
     * This method is called to signal that no more input will be provided.
     * It should consume any provided input, flush internal buffers, and produce
     * any remaining output (e.g., trailers for compression formats).
     *
     * The method is called repeatedly until it returns [TransformResult.done].
     *
     * @param source the byte array containing final input data
     * @param sourceStartIndex the start index (inclusive) of input data in [source]
     * @param sourceEndIndex the end index (exclusive) of input data in [source]
     * @param sink the byte array to write output to
     * @param sinkStartIndex the start index (inclusive) in [sink] to write from
     * @param sinkEndIndex the end index (exclusive) of available space in [sink]
     * @return result indicating progress, completion, or buffer requirements
     */
    protected abstract fun transformFinalIntoByteArray(
        source: ByteArray,
        sourceStartIndex: Int,
        sourceEndIndex: Int,
        sink: ByteArray,
        sinkStartIndex: Int,
        sinkEndIndex: Int
    ): TransformResult

    final override fun transformTo(source: Buffer, byteCount: Long, sink: Buffer): Long {
        if (source.exhausted()) return 0L

        return UnsafeBufferOperations.readFromHead(source) { input, inputStart, inputEnd ->
            val available = minOf(byteCount.toInt(), inputEnd - inputStart)
            var totalConsumed = 0

            while (totalConsumed < available) {
                lateinit var result: TransformResult

                @Suppress("UNUSED_VARIABLE")
                val _ = UnsafeBufferOperations.writeToTail(sink, 1) { output, outputStart, outputEnd ->
                    result = transformIntoByteArray(
                        input, inputStart + totalConsumed, inputStart + available,
                        output, outputStart, outputEnd
                    )
                    result.produced
                }

                when {
                    result.outputRequired > 0 -> {
                        // Retry with larger buffer
                        if (result.outputRequired <= UnsafeBufferOperations.maxSafeWriteCapacity) {
                            @Suppress("UNUSED_VARIABLE")
                            val written = UnsafeBufferOperations.writeToTail(sink, result.outputRequired) { output, outputStart, outputEnd ->
                                val retryResult = transformIntoByteArray(
                                    input, inputStart + totalConsumed, inputStart + available,
                                    output, outputStart, outputEnd
                                )
                                totalConsumed += retryResult.consumed
                                retryResult.produced
                            }
                        } else {
                            // Fall back to temporary array for large outputs
                            val temp = ByteArray(result.outputRequired)
                            val retryResult = transformIntoByteArray(
                                input, inputStart + totalConsumed, inputStart + available,
                                temp, 0, temp.size
                            )
                            if (retryResult.produced > 0) {
                                sink.write(temp, 0, retryResult.produced)
                            }
                            totalConsumed += retryResult.consumed
                        }
                    }

                    else -> {
                        totalConsumed += result.consumed
                    }
                }

                if (result.consumed == 0 && result.produced == 0) break
            }

            totalConsumed
        }.toLong()
    }

    final override fun transformFinalTo(source: Buffer, byteCount: Long, sink: Buffer): Long {
        return readFromHead(source) { input, inputStart, inputEnd ->
            val available = minOf(byteCount.toInt(), inputEnd - inputStart)
            var consumed = 0

            while (true) {
                lateinit var result: TransformResult

                @Suppress("UNUSED_VARIABLE")
                val _ = UnsafeBufferOperations.writeToTail(sink, 1) { output, outputStart, outputEnd ->
                    result = transformFinalIntoByteArray(
                        input, inputStart + consumed, inputStart + available,
                        output, outputStart, outputEnd
                    )
                    result.produced
                }

                when {
                    result.outputRequired > 0 -> {
                        if (result.outputRequired <= UnsafeBufferOperations.maxSafeWriteCapacity) {
                            @Suppress("UNUSED_VARIABLE")
                            val written = UnsafeBufferOperations.writeToTail(sink, result.outputRequired) { output, outputStart, outputEnd ->
                                val retryResult = transformFinalIntoByteArray(
                                    input, inputStart + consumed, inputStart + available,
                                    output, outputStart, outputEnd
                                )
                                consumed += retryResult.consumed
                                retryResult.produced
                            }
                        } else {
                            val temp = ByteArray(result.outputRequired)
                            val retryResult = transformFinalIntoByteArray(
                                input, inputStart + consumed, inputStart + available,
                                temp, 0, temp.size
                            )
                            if (retryResult.produced > 0) {
                                sink.write(temp, 0, retryResult.produced)
                            }
                            consumed += retryResult.consumed
                        }
                    }

                    else -> {
                        consumed += result.consumed
                        if (result.consumed == 0 && result.produced == 0) break
                    }
                }
            }

            consumed
        }.toLong()
    }

    // TODO: decide on it later
    private inline fun readFromHead(source: Buffer, block: (ByteArray, Int, Int) -> Int): Int {
        return if(source.exhausted()) {
            block(ByteArray(0), 0, 0)
        } else {
            UnsafeBufferOperations.readFromHead(source, block)
        }
    }

    /**
     * Optimized implementation that transforms a [ByteString] using direct ByteArray access.
     *
     * This override uses [transformFinalIntoByteArray] directly since this is a one-shot operation.
     * It reads from the ByteString's underlying array directly and writes output to a Buffer.
     *
     * @param source the ByteString to transform
     * @return a new ByteString containing the transformed data
     * @throws kotlinx.io.IOException if transformation fails
     */
    @OptIn(UnsafeByteStringApi::class)
    override fun transformFinal(source: ByteString): ByteString {
        val sink = Buffer()

        UnsafeByteStringOperations.withByteArrayUnsafe(source) { input ->
            var consumed = 0

            while (true) {
                lateinit var result: TransformResult

                @Suppress("UNUSED_VARIABLE")
                val _ = UnsafeBufferOperations.writeToTail(sink, 1) { output, outputStart, outputEnd ->
                    result = transformFinalIntoByteArray(
                        input, consumed, input.size,
                        output, outputStart, outputEnd
                    )
                    result.produced
                }

                when {
                    result.outputRequired > 0 -> {
                        if (result.outputRequired <= UnsafeBufferOperations.maxSafeWriteCapacity) {
                            @Suppress("UNUSED_VARIABLE")
                            val written = UnsafeBufferOperations.writeToTail(sink, result.outputRequired) { output, outputStart, outputEnd ->
                                val retryResult = transformFinalIntoByteArray(
                                    input, consumed, input.size,
                                    output, outputStart, outputEnd
                                )
                                consumed += retryResult.consumed
                                retryResult.produced
                            }
                        } else {
                            val temp = ByteArray(result.outputRequired)
                            val retryResult = transformFinalIntoByteArray(
                                input, consumed, input.size,
                                temp, 0, temp.size
                            )
                            if (retryResult.produced > 0) {
                                sink.write(temp, 0, retryResult.produced)
                            }
                            consumed += retryResult.consumed
                        }
                    }

                    else -> {
                        consumed += result.consumed
                        if (result.consumed == 0 && result.produced == 0) break
                    }
                }
            }
        }

        return sink.readByteString()
    }
}
