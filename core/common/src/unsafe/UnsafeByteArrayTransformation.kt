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
 * - [hasPendingOutput] to indicate if there's buffered output to drain
 * - [finalizeIntoByteArray] to produce final output after all input is processed
 * - [close] to release resources
 *
 * For bounded transformations (like Cipher), also override [maxOutputSize] to return
 * the maximum output size for a given input size. This enables optimized buffer handling.
 *
 * @see kotlinx.io.Transformation
 */
@OptIn(UnsafeIoApi::class)
@SubclassOptInRequired(UnsafeIoApi::class)
public abstract class UnsafeByteArrayTransformation : Transformation {

    /**
     * Result of a [transformIntoByteArray] operation.
     *
     * @property consumed number of bytes consumed from the input array
     * @property produced number of bytes written to the output array
     */
    // TODO: convert to value class over `Long`
    public class TransformResult(
        public val consumed: Int,
        public val produced: Int
    )

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
     * @return result containing bytes consumed from input and produced to output
     */
    // TODO: we might need to have `transformToByteArray` in case we can't fit output into one segment
    protected abstract fun transformIntoByteArray(
        source: ByteArray,
        sourceStartIndex: Int,
        sourceEndIndex: Int,
        sink: ByteArray,
        sinkStartIndex: Int,
        sinkEndIndex: Int
    ): TransformResult

    /**
     * Returns true if there's pending output that can be produced without new input.
     *
     * This is used for transformations that buffer data internally. When true,
     * [transformIntoByteArray] may be called with empty input to drain the buffer.
     *
     * For transformations without internal buffering (like native zlib or Cipher),
     * this should return false.
     */
    // TODO: try to drop it
    protected abstract fun hasPendingOutput(): Boolean

    /**
     * Returns the maximum output size for the given input size, or -1 if unknown.
     *
     * Override this method for bounded transformations (like Cipher) where the
     * maximum output size can be determined from the input size. This enables
     * optimized buffer allocation.
     *
     * The default implementation returns -1, indicating streaming behavior where
     * output size is unpredictable.
     *
     * @param inputSize the input size in bytes
     * @return the maximum output size, or -1 if unknown/unbounded
     */
    protected open fun maxOutputSize(inputSize: Int): Int = -1

    /**
     * Produces finalization output into the sink array.
     *
     * This method is called repeatedly until it returns -1, indicating no more output.
     * Implementations should track their finalization state internally.
     *
     * @param sink the byte array to write output to
     * @param startIndex the start index (inclusive) in [sink] to write from
     * @param endIndex the end index (exclusive) of available space in [sink]
     * @return the number of bytes written, or -1 if finalization is complete
     */
    // TODO: we might need to have `finalize` which returns an array (same as with transform)
    protected abstract fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): Int

    override fun transformTo(source: Buffer, byteCount: Long, sink: Buffer): Long {
        // Handle case where source is exhausted but we have pending output
        if (source.exhausted()) {
            if (!hasPendingOutput()) return 0L
            drainPendingOutput(sink)
            return 0L
        }

        return UnsafeBufferOperations.readFromHead(source) { input, inputStart, inputEnd ->
            val available = minOf(byteCount.toInt(), inputEnd - inputStart)
            val maxOutput = maxOutputSize(available)

            if (maxOutput >= 0) {
                // Bounded transformation - output size is known, consume all input at once
                val _ = UnsafeBufferOperations.writeToTail(
                    sink,
                    kotlin.comparisons.minOf(maxOutput, UnsafeBufferOperations.maxSafeWriteCapacity)
                ) { dest, destStart, destEnd ->
                    if (destEnd - destStart >= maxOutput) {
                        // Output fits in segment
                        transformIntoByteArray(
                            input,
                            inputStart,
                            inputStart + available,
                            dest,
                            destStart,
                            destEnd
                        ).produced
                    } else {
                        // Need separate buffer for large output
                        val tempBuffer = ByteArray(maxOutput)
                        val result =
                            transformIntoByteArray(input, inputStart, inputStart + available, tempBuffer, 0, maxOutput)
                        tempBuffer.copyInto(dest, destStart, 0, result.produced)
                        result.produced
                    }
                }
                available
            } else {
                // Streaming transformation - output size unknown, loop until input consumed
                var totalConsumed = 0

                while (totalConsumed < available || hasPendingOutput()) {
                    var progressMade = false

                    val _ = UnsafeBufferOperations.writeToTail(sink, 1) { output, outputStart, outputEnd ->
                        val result = transformIntoByteArray(
                            input, inputStart + totalConsumed, inputStart + available,
                            output, outputStart, outputEnd
                        )
                        totalConsumed += result.consumed
                        progressMade = result.consumed > 0 || result.produced > 0
                        result.produced
                    }

                    if (!progressMade) break
                }

                totalConsumed
            }
        }.toLong()
    }

    private fun drainPendingOutput(sink: Buffer) {
        while (hasPendingOutput()) {
            val written = UnsafeBufferOperations.writeToTail(sink, 1) { output, outputStart, outputEnd ->
                val result = transformIntoByteArray(
                    EMPTY_BYTE_ARRAY, 0, 0,
                    output, outputStart, outputEnd
                )
                result.produced
            }
            if (written == 0) break
        }
    }

    override fun finalizeTo(sink: Buffer) {
        while (true) {
            val written = UnsafeBufferOperations.writeToTail(sink, 1) { bytes, startIndex, endIndex ->
                val result = finalizeIntoByteArray(bytes, startIndex, endIndex)
                if (result == -1) 0 else result
            }
            if (written == 0) break
        }
    }

    private companion object {
        private val EMPTY_BYTE_ARRAY = ByteArray(0)
    }
}