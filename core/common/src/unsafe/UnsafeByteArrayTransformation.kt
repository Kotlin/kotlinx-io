package kotlinx.io.unsafe

import kotlinx.io.Buffer
import kotlinx.io.Transformation
import kotlinx.io.UnsafeIoApi

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
 * - [finalizeIntoByteArray] to produce final output after all input is processed
 * - [close] to release resources
 *
 * @see kotlinx.io.Transformation
 */
@OptIn(UnsafeIoApi::class)
@SubclassOptInRequired(UnsafeIoApi::class)
public abstract class UnsafeByteArrayTransformation : Transformation {

    /**
     * Result of a [transformIntoByteArray] operation.
     *
     * Use factory methods to create instances:
     * - [ok] for normal progress (consumed input and/or produced output)
     * - [done] when transformation is complete (no more output will be produced)
     * - [inputRequired] when more input is needed to proceed
     * - [outputRequired] when a larger output buffer is needed
     *
     * Use access properties to inspect results:
     * - [consumed] / [produced] for actual bytes processed (0 if requirement result)
     * - [inputRequired] / [outputRequired] for buffer size needed (0 if ok result)
     */
    protected class TransformResult private constructor(
        private val _consumed: Int,
        private val _produced: Int
    ) {
        /**
         * Number of bytes consumed from the input array.
         * Returns 0 if this is a requirement result ([inputRequired] or [outputRequired]).
         */
        public val consumed: Int get() = if (_consumed >= 0) _consumed else 0

        /**
         * Number of bytes written to the output array.
         * Returns 0 if this is a requirement result ([inputRequired] or [outputRequired]).
         */
        public val produced: Int get() = if (_produced >= 0) _produced else 0

        /**
         * Size of additional input required to proceed.
         * Returns 0 if this is an ok result (normal progress).
         */
        public val inputRequired: Int get() = if (_consumed < 0) -_consumed else 0

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
             * Creates a result indicating that more input is needed.
             *
             * @param size minimum number of additional input bytes required (must be > 0)
             * @throws IllegalArgumentException if size is not positive
             */
            public fun inputRequired(size: Int): TransformResult {
                require(size > 0) { "size must be positive: $size" }
                return TransformResult(-size, 0)
            }

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
     * Result of a [finalizeIntoByteArray] operation.
     *
     * Use factory methods to create instances:
     * - [ok] when bytes were produced (may have more)
     * - [done] when finalization is complete
     * - [outputRequired] when a larger output buffer is needed
     *
     * Use access properties to inspect results:
     * - [produced] for actual bytes produced (0 if done or requirement result)
     * - [outputRequired] for buffer size needed (0 if ok/done result)
     * - [isDone] to check if finalization is complete
     */
    protected class FinalizeResult private constructor(
        private val _produced: Int
    ) {
        /**
         * Number of bytes written to the output array.
         * Returns 0 if this is a done or requirement result.
         */
        public val produced: Int get() = if (_produced >= 0) _produced else 0

        /**
         * Size of output buffer required to proceed.
         * Returns 0 if this is an ok or done result.
         */
        public val outputRequired: Int get() = if (_produced < 0) -_produced else 0

        /**
         * Returns true if finalization is complete (no more output).
         */
        public val isDone: Boolean get() = _produced == 0

        public companion object {
            /**
             * Creates a result indicating bytes were produced.
             *
             * @param produced number of bytes written to output (must be > 0)
             * @throws IllegalArgumentException if produced is not positive
             */
            public fun ok(produced: Int): FinalizeResult {
                require(produced > 0) { "produced must be positive: $produced (use done() for completion)" }
                return FinalizeResult(produced)
            }

            /**
             * Creates a result indicating finalization is complete.
             */
            public fun done(): FinalizeResult = FinalizeResult(0)

            /**
             * Creates a result indicating that a larger output buffer is needed.
             *
             * @param size minimum output buffer size required (must be > 0)
             * @throws IllegalArgumentException if size is not positive
             */
            public fun outputRequired(size: Int): FinalizeResult {
                require(size > 0) { "size must be positive: $size" }
                return FinalizeResult(-size)
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
     * Produces finalization output into the sink array.
     *
     * This method is called repeatedly until it returns [FinalizeResult.done].
     * Implementations should track their finalization state internally.
     *
     * @param sink the byte array to write output to
     * @param startIndex the start index (inclusive) in [sink] to write from
     * @param endIndex the end index (exclusive) of available space in [sink]
     * @return result indicating progress, completion, or buffer requirements
     */
    protected abstract fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): FinalizeResult

    override fun transformTo(source: Buffer, byteCount: Long, sink: Buffer): Long {
        if (source.exhausted()) return 0L

        return UnsafeBufferOperations.readFromHead(source) { input, inputStart, inputEnd ->
            val available = minOf(byteCount.toInt(), inputEnd - inputStart)
            var totalConsumed = 0

            while (totalConsumed < available) {
                lateinit var result: TransformResult

                val _ = UnsafeBufferOperations.writeToTail(sink, 1) { output, outputStart, outputEnd ->
                    result = transformIntoByteArray(
                        input, inputStart + totalConsumed, inputStart + available,
                        output, outputStart, outputEnd
                    )
                    result.produced
                }

                when {
                    result.inputRequired > 0 -> break
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

    override fun finalizeTo(sink: Buffer) {
        while (true) {
            lateinit var result: FinalizeResult

            val _ = UnsafeBufferOperations.writeToTail(sink, 1) { bytes, startIndex, endIndex ->
                result = finalizeIntoByteArray(bytes, startIndex, endIndex)
                result.produced
            }

            when {
                result.isDone -> break
                result.outputRequired > 0 -> {
                    // Retry with larger buffer
                    if (result.outputRequired <= UnsafeBufferOperations.maxSafeWriteCapacity) {
                        @Suppress("UNUSED_VARIABLE")
                        val written = UnsafeBufferOperations.writeToTail(sink, result.outputRequired) { bytes, startIndex, endIndex ->
                            val retryResult = finalizeIntoByteArray(bytes, startIndex, endIndex)
                            retryResult.produced
                        }
                    } else {
                        // Fall back to temporary array for large outputs
                        val temp = ByteArray(result.outputRequired)
                        val retryResult = finalizeIntoByteArray(temp, 0, temp.size)
                        if (retryResult.produced > 0) {
                            sink.write(temp, 0, retryResult.produced)
                        }
                    }
                    // Continue loop to check for more output or completion
                }

                result.produced == 0 -> break
            }
        }
    }
}
