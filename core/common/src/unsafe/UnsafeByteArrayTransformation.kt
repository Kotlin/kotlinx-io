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
     * Transforms input bytes and returns output as a ByteArray.
     *
     * Override this method when:
     * - Output size is unknown upfront (can't override [maxOutputSize])
     * - AND the underlying API cannot produce output incrementally
     *
     * When this method returns non-null, [transformIntoByteArray] is not called for that input.
     * All input bytes are considered consumed when this method returns non-null.
     * The default implementation returns null, meaning [transformIntoByteArray] will be used.
     *
     * @param source the byte array containing input data
     * @param sourceStartIndex the start index (inclusive) of input data in [source]
     * @param sourceEndIndex the end index (exclusive) of input data in [source]
     * @return the output ByteArray, or null to use [transformIntoByteArray]
     */
    protected open fun transformToByteArray(
        source: ByteArray,
        sourceStartIndex: Int,
        sourceEndIndex: Int
    ): ByteArray? = null

    /**
     * Returns the maximum output size for the given input size, or -1 if unknown.
     *
     * Override this method for bounded transformations (like Cipher) where the
     * maximum output size can be determined from the input size. This enables
     * optimized buffer allocation.
     *
     * When called with inputSize=0, this should return the maximum finalization
     * output size (e.g., buffered data that will be flushed during finalization).
     * This is used by [finalizeTo] to allocate appropriate buffers for transformations
     * like AES-GCM that buffer all data until finalization.
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
     * Produces finalization output as a ByteArray.
     *
     * Override this method when:
     * - Finalization output size is unknown upfront (can't override [maxOutputSize])
     * - AND the underlying API cannot produce output incrementally (e.g., AES-GCM decryption
     *   where all output is produced atomically by doFinal)
     *
     * When this method returns non-null, [finalizeIntoByteArray] is not called.
     * The default implementation returns null, meaning [finalizeIntoByteArray] will be used.
     *
     * @return the finalization output as a ByteArray, or null to use [finalizeIntoByteArray]
     */
    protected open fun finalizeToByteArray(): ByteArray? = null

    override fun transformTo(source: Buffer, byteCount: Long, sink: Buffer): Long {
        if (source.exhausted()) return 0L

        return UnsafeBufferOperations.readFromHead(source) { input, inputStart, inputEnd ->
            val available = minOf(byteCount.toInt(), inputEnd - inputStart)

            // First, try the ByteArray-returning method for atomic large output
            val directOutput = transformToByteArray(input, inputStart, inputStart + available)
            if (directOutput != null) {
                if (directOutput.isNotEmpty()) {
                    sink.write(directOutput)
                }
                return@readFromHead available // All input consumed
            }

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

                totalConsumed
            }
        }.toLong()
    }

    override fun finalizeTo(sink: Buffer) {
        // First, try the ByteArray-returning method for atomic large output
        val directOutput = finalizeToByteArray()
        if (directOutput != null) {
            if (directOutput.isNotEmpty()) {
                sink.write(directOutput)
            }
            return
        }

        val maxFinalize = maxOutputSize(0)

        if (maxFinalize > 0) {
            // Known finalization size - may need temp buffer for large output
            val tempBuffer = ByteArray(maxFinalize)
            val written = finalizeIntoByteArray(tempBuffer, 0, maxFinalize)
            if (written > 0) {
                sink.write(tempBuffer, 0, written)
            }
        } else {
            // Unknown size - loop with segment-sized chunks
            while (true) {
                val written = UnsafeBufferOperations.writeToTail(sink, 1) { bytes, startIndex, endIndex ->
                    val result = finalizeIntoByteArray(bytes, startIndex, endIndex)
                    if (result == -1) 0 else result
                }
                if (written == 0) break
            }
        }
    }
}