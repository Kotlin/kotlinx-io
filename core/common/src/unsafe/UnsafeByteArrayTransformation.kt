package kotlinx.io.unsafe

import kotlinx.io.Buffer
import kotlinx.io.Transformation
import kotlinx.io.UnsafeIoApi
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.unsafe.UnsafeByteStringApi
import kotlinx.io.bytestring.unsafe.UnsafeByteStringOperations
import kotlinx.io.readByteArray

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
 * - [transformFinalIntoByteArray] to produce final output after all input is processed
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
     * Result of a [transformFinalIntoByteArray] operation.
     *
     * Use factory methods to create instances:
     * - [ok] when bytes were produced (may have more)
     * - [done] when transformation is complete
     * - [outputRequired] when a larger output buffer is needed
     *
     * Use access properties to inspect results:
     * - [produced] for actual bytes produced (0 if done or requirement result)
     * - [outputRequired] for buffer size needed (0 if ok/done result)
     * - [isDone] to check if transformation is complete
     */
    protected class TransformFinalResult private constructor(
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
         * Returns true if transformation is complete (no more output).
         */
        public val isDone: Boolean get() = _produced == 0

        public companion object {
            /**
             * Creates a result indicating bytes were produced.
             *
             * @param produced number of bytes written to output (must be > 0)
             * @throws IllegalArgumentException if produced is not positive
             */
            public fun ok(produced: Int): TransformFinalResult {
                require(produced > 0) { "produced must be positive: $produced (use done() for completion)" }
                return TransformFinalResult(produced)
            }

            /**
             * Creates a result indicating transformation is complete.
             */
            public fun done(): TransformFinalResult = TransformFinalResult(0)

            /**
             * Creates a result indicating that a larger output buffer is needed.
             *
             * @param size minimum output buffer size required (must be > 0)
             * @throws IllegalArgumentException if size is not positive
             */
            public fun outputRequired(size: Int): TransformFinalResult {
                require(size > 0) { "size must be positive: $size" }
                return TransformFinalResult(-size)
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
     * Produces final transformation output into the sink array.
     *
     * This method is called repeatedly until it returns [TransformFinalResult.done].
     * Implementations should track their state internally and produce any remaining
     * buffered output, trailers, etc.
     *
     * @param sink the byte array to write output to
     * @param startIndex the start index (inclusive) in [sink] to write from
     * @param endIndex the end index (exclusive) of available space in [sink]
     * @return result indicating progress, completion, or buffer requirements
     */
    protected abstract fun transformFinalIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): TransformFinalResult

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

    override fun transformFinalTo(source: Buffer, byteCount: Long, sink: Buffer) {
        // First, process remaining input
        if (!source.exhausted() && byteCount > 0) {
            var remaining = byteCount
            while (!source.exhausted() && remaining > 0) {
                val consumed = transformTo(source, remaining, sink)
                if (consumed <= 0) break
                remaining -= consumed
            }
        }

        // Then produce final output
        while (true) {
            lateinit var result: TransformFinalResult

            val _ = UnsafeBufferOperations.writeToTail(sink, 1) { bytes, startIndex, endIndex ->
                result = transformFinalIntoByteArray(bytes, startIndex, endIndex)
                result.produced
            }

            when {
                result.isDone -> break
                result.outputRequired > 0 -> {
                    // Retry with larger buffer
                    if (result.outputRequired <= UnsafeBufferOperations.maxSafeWriteCapacity) {
                        @Suppress("UNUSED_VARIABLE")
                        val written = UnsafeBufferOperations.writeToTail(sink, result.outputRequired) { bytes, startIndex, endIndex ->
                            val retryResult = transformFinalIntoByteArray(bytes, startIndex, endIndex)
                            retryResult.produced
                        }
                    } else {
                        // Fall back to temporary array for large outputs
                        val temp = ByteArray(result.outputRequired)
                        val retryResult = transformFinalIntoByteArray(temp, 0, temp.size)
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

    /**
     * Optimized implementation that transforms a [ByteString] using direct ByteArray access.
     *
     * This override avoids Buffer allocation overhead by accessing the ByteString's internal
     * ByteArray directly and using [transformIntoByteArray] and [transformFinalIntoByteArray].
     *
     * Concrete implementations can override this method further for algorithm-specific
     * optimizations (e.g., pre-calculated output sizes, special handling for empty input).
     *
     * @param source the ByteString to transform
     * @return a new ByteString containing the transformed data
     * @throws kotlinx.io.IOException if transformation fails
     */
    @OptIn(UnsafeByteStringApi::class)
    override fun transformFinal(source: ByteString): ByteString {
        val sink = Buffer()

        // Optimized path: use ByteArray directly without input Buffer allocation
        UnsafeByteStringOperations.withByteArrayUnsafe(source) { inputArray ->
            var offset = 0

            // Transform all input using ByteArray methods
            while (offset < inputArray.size) {
                lateinit var result: TransformResult

                val _ = UnsafeBufferOperations.writeToTail(sink, 1) { output, outputStart, outputEnd ->
                    result = transformIntoByteArray(
                        inputArray, offset, inputArray.size,
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
                                    inputArray, offset, inputArray.size,
                                    output, outputStart, outputEnd
                                )
                                offset += retryResult.consumed
                                retryResult.produced
                            }
                        } else {
                            // Fall back to temporary array for large outputs
                            val temp = ByteArray(result.outputRequired)
                            val retryResult = transformIntoByteArray(
                                inputArray, offset, inputArray.size,
                                temp, 0, temp.size
                            )
                            if (retryResult.produced > 0) {
                                sink.write(temp, 0, retryResult.produced)
                            }
                            offset += retryResult.consumed
                        }
                    }

                    else -> {
                        offset += result.consumed
                    }
                }

                if (result.consumed == 0 && result.produced == 0) break
            }
        }

        // Produce final output
        while (true) {
            lateinit var result: TransformFinalResult

            val _ = UnsafeBufferOperations.writeToTail(sink, 1) { bytes, startIndex, endIndex ->
                result = transformFinalIntoByteArray(bytes, startIndex, endIndex)
                result.produced
            }

            when {
                result.isDone -> break
                result.outputRequired > 0 -> {
                    if (result.outputRequired <= UnsafeBufferOperations.maxSafeWriteCapacity) {
                        @Suppress("UNUSED_VARIABLE")
                        val written = UnsafeBufferOperations.writeToTail(sink, result.outputRequired) { bytes, startIndex, endIndex ->
                            val retryResult = transformFinalIntoByteArray(bytes, startIndex, endIndex)
                            retryResult.produced
                        }
                    } else {
                        val temp = ByteArray(result.outputRequired)
                        val retryResult = transformFinalIntoByteArray(temp, 0, temp.size)
                        if (retryResult.produced > 0) {
                            sink.write(temp, 0, retryResult.produced)
                        }
                    }
                }

                result.produced == 0 -> break
            }
        }

        return ByteString(sink.readByteArray())
    }
}
