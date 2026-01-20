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
 * - [maxOutputSize] to return the maximum output size for a given input size (-1 if unknown)
 * - [transformIntoByteArray] to transform input bytes into output bytes incrementally
 * - [transformToByteArray] fallback for atomic transformations that can't produce incremental output
 * - [finalizeIntoByteArray] to produce final output after all input is processed
 * - [finalizeToByteArray] fallback for atomic finalizations that can't produce incremental output
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
     * For transformations that cannot produce output incrementally and don't know
     * the output size upfront (can't override [maxOutputSize]), override
     * [transformToByteArray] instead.
     *
     * @param source the byte array containing input data
     * @param sourceStartIndex the start index (inclusive) of input data in [source]
     * @param sourceEndIndex the end index (exclusive) of input data in [source]
     * @param sink the byte array to write output to
     * @param sinkStartIndex the start index (inclusive) in [sink] to write from
     * @param sinkEndIndex the end index (exclusive) of available space in [sink]
     * @return result containing bytes consumed from input and produced to output
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
     * Fallback method that transforms input bytes and returns output as a ByteArray.
     *
     * This method is called as a **last resort** when [transformIntoByteArray] makes no progress
     * (returns 0 consumed and 0 produced). It allocates a new ByteArray, so prefer:
     * 1. Returning appropriate [maxOutputSize] if output size is known
     * 2. Implementing incremental output in [transformIntoByteArray]
     *
     * All input bytes are considered consumed when this method returns a non-empty array.
     * Returning an empty array indicates no fallback is available (transformation needs more input).
     *
     * @param source the byte array containing input data
     * @param sourceStartIndex the start index (inclusive) of input data in [source]
     * @param sourceEndIndex the end index (exclusive) of input data in [source]
     * @return the output ByteArray, or empty array if no fallback is available
     */
    protected abstract fun transformToByteArray(
        source: ByteArray,
        sourceStartIndex: Int,
        sourceEndIndex: Int
    ): ByteArray

    /**
     * Returns the maximum output size for the given input size, or -1 if unknown.
     *
     * For bounded transformations (like Cipher) where the maximum output size
     * can be determined from the input size, return that value. This enables
     * optimized buffer allocation.
     *
     * When called with inputSize=0, this should return the maximum finalization
     * output size (e.g., buffered data that will be flushed during finalization).
     * This is used by [finalizeTo] to allocate appropriate buffers for transformations
     * like AES-GCM that buffer all data until finalization.
     *
     * Return -1 to indicate streaming behavior where output size is unpredictable.
     *
     * @param inputSize the input size in bytes
     * @return the maximum output size, or -1 if unknown/unbounded
     */
    protected abstract fun maxOutputSize(inputSize: Int): Int

    /**
     * Produces finalization output into the sink array.
     *
     * This method is called repeatedly until it returns -1, indicating no more output.
     * Implementations should track their finalization state internally.
     *
     * For transformations that cannot produce output incrementally and don't know
     * the output size upfront (can't override [maxOutputSize]), override
     * [finalizeToByteArray] instead.
     *
     * @param sink the byte array to write output to
     * @param startIndex the start index (inclusive) in [sink] to write from
     * @param endIndex the end index (exclusive) of available space in [sink]
     * @return the number of bytes written, or -1 if finalization is complete
     */
    protected abstract fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): Int

    /**
     * Fallback method that produces finalization output as a ByteArray.
     *
     * This method is called as a **last resort** when [finalizeIntoByteArray] makes no progress
     * (returns 0 or -1 immediately). It allocates a new ByteArray, so prefer:
     * 1. Returning appropriate [maxOutputSize] with inputSize=0 for known finalization size
     * 2. Implementing incremental output in [finalizeIntoByteArray]
     *
     * @return the finalization output as a ByteArray (empty if no output)
     */
    protected abstract fun finalizeToByteArray(): ByteArray

    override fun transformTo(source: Buffer, byteCount: Long, sink: Buffer): Long {
        if (source.exhausted()) return 0L

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

                while (totalConsumed < available) {
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

                // If no progress was made, try ByteArray-returning method as last resort
                if (totalConsumed == 0) {
                    val directOutput = transformToByteArray(input, inputStart, inputStart + available)
                    if (directOutput.isNotEmpty()) {
                        sink.write(directOutput)
                        return@readFromHead available // All input consumed
                    }
                    // Empty output means no fallback available (needs more input)
                }

                totalConsumed
            }
        }.toLong()
    }

    override fun finalizeTo(sink: Buffer) {
        val maxFinalize = maxOutputSize(0)

        if (maxFinalize > 0) {
            // Known finalization size - may need temp buffer for large output
            val tempBuffer = ByteArray(maxFinalize)
            val written = finalizeIntoByteArray(tempBuffer, 0, maxFinalize)
            if (written > 0) {
                sink.write(tempBuffer, 0, written)
            }
        } else {
            // Unknown size - try incremental with segment-sized chunks first
            var madeProgress = false
            while (true) {
                val written = UnsafeBufferOperations.writeToTail(sink, 1) { bytes, startIndex, endIndex ->
                    val result = finalizeIntoByteArray(bytes, startIndex, endIndex)
                    if (result == -1) 0 else result
                }
                if (written > 0) madeProgress = true
                if (written == 0) break
            }

            // If no progress was made, try ByteArray-returning method as last resort
            if (!madeProgress) {
                val directOutput = finalizeToByteArray()
                if (directOutput.isNotEmpty()) {
                    sink.write(directOutput)
                }
            }
        }
    }
}