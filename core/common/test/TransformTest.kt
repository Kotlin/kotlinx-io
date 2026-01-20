/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.io.unsafe.UnsafeByteArrayTransformation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TransformTest {

    @Test
    fun transformSinkBasicRoundTrip() {
        val downstream = Buffer()
        val transform = DoubleByteTransform()

        val sink = (downstream as RawSink).transformedWith(transform)
        sink.buffered().use { bufferedSink ->
            bufferedSink.writeString("abc")
        }

        // Each byte should be doubled: 'a' -> 'aa', 'b' -> 'bb', 'c' -> 'cc'
        assertEquals("aabbcc", downstream.readString())
    }

    @Test
    fun transformSourceBasicRoundTrip() {
        val upstream = Buffer()
        upstream.writeString("aabbcc")

        val transform = HalveByteTransform()
        val source = (upstream as RawSource).transformedWith(transform)

        val result = source.buffered().readString()
        assertEquals("abc", result)
    }

    @Test
    fun transformSinkEmptyData() {
        val downstream = Buffer()
        val transform = DoubleByteTransform()

        val sink = (downstream as RawSink).transformedWith(transform)
        sink.buffered().use { /* write nothing */ }

        assertEquals("", downstream.readString())
    }

    @Test
    fun transformSourceEmptyData() {
        val upstream = Buffer()
        val transform = HalveByteTransform()

        val source = (upstream as RawSource).transformedWith(transform)
        val result = source.buffered().readString()

        assertEquals("", result)
    }

    @Test
    fun transformSinkLargeData() {
        val downstream = Buffer()
        val transform = DoubleByteTransform()

        val original = "x".repeat(10000)
        val sink = (downstream as RawSink).transformedWith(transform)
        sink.buffered().use { bufferedSink ->
            bufferedSink.writeString(original)
        }

        val expected = "xx".repeat(10000)
        assertEquals(expected, downstream.readString())
    }

    @Test
    fun transformSourceLargeData() {
        val upstream = Buffer()
        upstream.writeString("xx".repeat(10000))

        val transform = HalveByteTransform()
        val source = (upstream as RawSource).transformedWith(transform)

        val result = source.buffered().readString()
        assertEquals("x".repeat(10000), result)
    }

    @Test
    fun transformSinkBinaryData() {
        val downstream = Buffer()
        val transform = DoubleByteTransform()

        val original = ByteArray(256) { it.toByte() }
        val sink = (downstream as RawSink).transformedWith(transform)
        sink.buffered().use { bufferedSink ->
            bufferedSink.write(original)
        }

        // Each byte should appear twice
        val result = downstream.readByteArray()
        assertEquals(512, result.size)
        for (i in 0 until 256) {
            assertEquals(i.toByte(), result[i * 2])
            assertEquals(i.toByte(), result[i * 2 + 1])
        }
    }

    @Test
    fun transformSourceBinaryData() {
        val upstream = Buffer()
        // Write each byte twice
        for (i in 0 until 256) {
            upstream.writeByte(i.toByte())
            upstream.writeByte(i.toByte())
        }

        val transform = HalveByteTransform()
        val source = (upstream as RawSource).transformedWith(transform)

        val result = source.buffered().use { bufferedSource ->
            val buffer = Buffer()
            bufferedSource.transferTo(buffer)
            buffer.readByteArray()
        }

        assertEquals(256, result.size)
        for (i in 0 until 256) {
            assertEquals(i.toByte(), result[i])
        }
    }

    @Test
    fun transformSinkClosesProperly() {
        var transformClosed = false
        var downstreamClosed = false

        val downstream = object : RawSink {
            override fun write(source: Buffer, byteCount: Long) {
                source.skip(byteCount)
            }
            override fun flush() {}
            override fun close() {
                downstreamClosed = true
            }
        }

        val transform = object : Transformation {
            override fun transformTo(source: Buffer, byteCount: Long, sink: Buffer): Long {
                val toWrite = minOf(source.size, byteCount)
                sink.write(source, toWrite)
                return toWrite
            }
            override fun finalizeTo(sink: Buffer) {}
            override fun close() {
                transformClosed = true
            }
        }

        val sink = downstream.transformedWith(transform)
        sink.buffered().use { bufferedSink ->
            bufferedSink.writeString("test")
        }

        assertTrue(transformClosed, "Transform should be closed")
        assertTrue(downstreamClosed, "Downstream should be closed")
    }

    @Test
    fun transformSourceClosesProperly() {
        var transformClosed = false
        var upstreamClosed = false

        val upstream = object : RawSource {
            private val data = Buffer().also { it.writeString("test") }
            override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
                if (data.size == 0L) return -1L
                val toRead = minOf(byteCount, data.size)
                sink.write(data, toRead)
                return toRead
            }
            override fun close() {
                upstreamClosed = true
            }
        }

        val transform = object : Transformation {
            private var finished = false
            override fun transformTo(source: Buffer, byteCount: Long, sink: Buffer): Long {
                if (finished) return -1L
                if (source.size > 0) {
                    val toWrite = minOf(source.size, byteCount)
                    sink.write(source, toWrite)
                    return toWrite
                }
                return 0L
            }
            override fun finalizeTo(sink: Buffer) {
                finished = true
            }
            override fun close() {
                transformClosed = true
            }
        }

        val source = upstream.transformedWith(transform)
        source.buffered().use { bufferedSource ->
            bufferedSource.readString()
        }

        assertTrue(transformClosed, "Transform should be closed")
        assertTrue(upstreamClosed, "Upstream should be closed")
    }

    @Test
    fun transformSinkOperationsAfterClose() {
        val downstream = Buffer()
        val transform = DoubleByteTransform()

        val sink = (downstream as RawSink).transformedWith(transform)
        sink.close()

        assertFailsWith<IllegalStateException> {
            sink.write(Buffer().also { it.writeString("test") }, 4)
        }
        assertFailsWith<IllegalStateException> {
            sink.flush()
        }
    }

    @Test
    fun transformSourceOperationsAfterClose() {
        val upstream = Buffer()
        upstream.writeString("test")
        val transform = HalveByteTransform()

        val source = (upstream as RawSource).transformedWith(transform)
        source.close()

        assertFailsWith<IllegalStateException> {
            source.readAtMostTo(Buffer(), 10)
        }
    }

    @Test
    fun transformSinkWithFinishData() {
        val downstream = Buffer()
        val transform = AppendTrailerTransform("--END--")

        val sink = (downstream as RawSink).transformedWith(transform)
        sink.buffered().use { bufferedSink ->
            bufferedSink.writeString("Hello")
        }

        assertEquals("Hello--END--", downstream.readString())
    }

    @Test
    fun transformSinkMultipleWrites() {
        val downstream = Buffer()
        val transform = DoubleByteTransform()

        val sink = (downstream as RawSink).transformedWith(transform).buffered()
        sink.writeString("a")
        sink.writeString("b")
        sink.writeString("c")
        sink.close()

        assertEquals("aabbcc", downstream.readString())
    }

    @Test
    fun transformSinkFlushDoesNotFinish() {
        val downstream = Buffer()
        val transform = AppendTrailerTransform("--END--")

        val sink = (downstream as RawSink).transformedWith(transform).buffered()
        sink.writeString("Hello")
        sink.flush()

        // Flush should emit data but not the trailer
        assertEquals("Hello", downstream.readString())

        sink.writeString("World")
        sink.close()

        // Close should add the trailer
        assertEquals("World--END--", downstream.readString())
    }

    // Test transforms

    /**
     * A transform that doubles each byte (writes each byte twice).
     * Consumes 1 byte, produces 2 bytes.
     * Uses streaming mode (unbounded output) since output can be larger than input.
     */
    @OptIn(UnsafeIoApi::class)
    private class DoubleByteTransform : UnsafeByteArrayTransformation() {
        override fun maxOutputSize(inputSize: Int): Int = -1

        override fun transformIntoByteArray(
            source: ByteArray,
            sourceStartIndex: Int,
            sourceEndIndex: Int,
            sink: ByteArray,
            sinkStartIndex: Int,
            sinkEndIndex: Int
        ): TransformResult {
            val inputSize = sourceEndIndex - sourceStartIndex
            val availableOutput = sinkEndIndex - sinkStartIndex

            // Calculate how many bytes we can process given output space
            val canProcess = minOf(inputSize, availableOutput / 2)

            var sinkPos = sinkStartIndex
            for (i in 0 until canProcess) {
                val byte = source[sourceStartIndex + i]
                sink[sinkPos++] = byte
                sink[sinkPos++] = byte
            }

            return TransformResult(canProcess, canProcess * 2)
        }

        override fun transformToByteArray(
            source: ByteArray,
            sourceStartIndex: Int,
            sourceEndIndex: Int
        ): ByteArray = ByteArray(0)

        override fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): Int = -1

        override fun finalizeToByteArray(): ByteArray = ByteArray(0)

        override fun close() {}
    }

    /**
     * A transform that takes every other byte (halves the data).
     * Expects input where each byte appears twice.
     * Consumes 2 bytes, produces 1 byte.
     * Note: This is a pass-through transformation that doesn't have its own EOF marker.
     * It relies on TransformingSource to detect EOF when upstream is exhausted.
     */
    @OptIn(UnsafeIoApi::class)
    private class HalveByteTransform : UnsafeByteArrayTransformation() {
        override fun maxOutputSize(inputSize: Int): Int = -1

        override fun transformIntoByteArray(
            source: ByteArray,
            sourceStartIndex: Int,
            sourceEndIndex: Int,
            sink: ByteArray,
            sinkStartIndex: Int,
            sinkEndIndex: Int
        ): TransformResult {
            val inputSize = sourceEndIndex - sourceStartIndex
            val availableOutput = sinkEndIndex - sinkStartIndex

            // Process complete pairs only (2 input bytes -> 1 output byte)
            val pairs = minOf(inputSize / 2, availableOutput)

            var sourcePos = sourceStartIndex
            var sinkPos = sinkStartIndex
            for (i in 0 until pairs) {
                sink[sinkPos++] = source[sourcePos]
                sourcePos += 2 // Skip the duplicate
            }

            return TransformResult(pairs * 2, pairs)
        }

        override fun transformToByteArray(
            source: ByteArray,
            sourceStartIndex: Int,
            sourceEndIndex: Int
        ): ByteArray = ByteArray(0)

        override fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): Int = -1

        override fun finalizeToByteArray(): ByteArray = ByteArray(0)

        override fun close() {}
    }

    /**
     * A transform that appends a trailer on finish.
     * Pass-through for normal data, adds trailer on finish.
     */
    private class AppendTrailerTransform(private val trailer: String) : Transformation {
        override fun transformTo(source: Buffer, byteCount: Long, sink: Buffer): Long {
            val toConsume = minOf(source.size, byteCount)
            sink.write(source, toConsume)
            return toConsume
        }

        override fun finalizeTo(sink: Buffer) {
            sink.writeString(trailer)
        }

        override fun close() {}
    }

    // Tests for streaming transformations with large output

    /**
     * Test that a streaming transformation can produce output larger than segment size
     * when the transformation can produce partial output incrementally.
     *
     * DoubleByteTransform handles this correctly because it processes only as many
     * input bytes as will fit in the output buffer.
     */
    @Test
    fun streamingTransformWithLargeOutputIncremental() {
        val downstream = Buffer()
        val transform = DoubleByteTransform()

        // 10KB input -> 20KB output (larger than typical 8KB segment)
        val original = ByteArray(10_000) { it.toByte() }
        val sink = (downstream as RawSink).transformedWith(transform)
        sink.buffered().use { bufferedSink ->
            bufferedSink.write(original)
        }

        val result = downstream.readByteArray()
        assertEquals(20_000, result.size)
        // Verify each byte is doubled
        for (i in original.indices) {
            assertEquals(original[i], result[i * 2])
            assertEquals(original[i], result[i * 2 + 1])
        }
    }

    /**
     * Test transformation that buffers all input and produces output only when
     * a certain amount is accumulated. This simulates algorithms that can't
     * produce partial output (like some block-based operations).
     *
     * This transformation buffers input until it has `blockSize` bytes,
     * then expands each byte to `expansionFactor` bytes.
     */
    @Test
    fun streamingTransformWithBlockedOutput() {
        val downstream = Buffer()
        // Process in blocks of 100 bytes, expand each byte to 10 bytes
        // When block is ready: 100 input -> 1000 output
        val transform = BlockExpandTransform(blockSize = 100, expansionFactor = 10)

        // 500 bytes = 5 complete blocks
        val original = ByteArray(500) { it.toByte() }
        val sink = (downstream as RawSink).transformedWith(transform)
        sink.buffered().use { bufferedSink ->
            bufferedSink.write(original)
        }

        val result = downstream.readByteArray()
        assertEquals(5000, result.size)
    }

    /**
     * Test transformation that produces large output during finalization
     * in streaming mode (maxOutputSize returns -1).
     *
     * This simulates a transformation that buffers everything and only
     * outputs during finalize, but doesn't know the output size upfront.
     */
    @Test
    fun streamingFinalizeWithLargeOutput() {
        val downstream = Buffer()
        // Buffers all input, outputs expanded data (10x) during finalize
        val transform = BufferAllExpandOnFinalizeTransform(expansionFactor = 10)

        // 1000 bytes input -> 10000 bytes output during finalize
        val original = ByteArray(1000) { it.toByte() }
        val sink = (downstream as RawSink).transformedWith(transform)
        sink.buffered().use { bufferedSink ->
            bufferedSink.write(original)
        }

        val result = downstream.readByteArray()
        assertEquals(10_000, result.size)
    }

    /**
     * A streaming transformation that processes input in blocks.
     * Buffers input until blockSize is reached, then expands and outputs.
     * Simulates block-based algorithms that can't produce partial output.
     */
    @OptIn(UnsafeIoApi::class)
    private class BlockExpandTransform(
        private val blockSize: Int,
        private val expansionFactor: Int
    ) : UnsafeByteArrayTransformation() {
        private val inputBuffer = ByteArray(blockSize)
        private var bufferedCount = 0
        private var outputBuffer: ByteArray? = null
        private var outputPos = 0

        override fun maxOutputSize(inputSize: Int): Int = -1

        override fun transformIntoByteArray(
            source: ByteArray,
            sourceStartIndex: Int,
            sourceEndIndex: Int,
            sink: ByteArray,
            sinkStartIndex: Int,
            sinkEndIndex: Int
        ): TransformResult {
            var consumed = 0
            var produced = 0
            val availableOutput = sinkEndIndex - sinkStartIndex

            // First, drain any pending output
            val pending = outputBuffer
            if (pending != null && outputPos < pending.size) {
                val toCopy = minOf(pending.size - outputPos, availableOutput)
                pending.copyInto(sink, sinkStartIndex, outputPos, outputPos + toCopy)
                outputPos += toCopy
                produced += toCopy
                if (outputPos >= pending.size) {
                    outputBuffer = null
                    outputPos = 0
                }
                return TransformResult(consumed, produced)
            }

            // Buffer input until we have a complete block
            val inputSize = sourceEndIndex - sourceStartIndex
            val toBuffer = minOf(inputSize, blockSize - bufferedCount)
            source.copyInto(inputBuffer, bufferedCount, sourceStartIndex, sourceStartIndex + toBuffer)
            bufferedCount += toBuffer
            consumed += toBuffer

            // If we have a complete block, expand it
            if (bufferedCount == blockSize) {
                outputBuffer = ByteArray(blockSize * expansionFactor)
                for (i in 0 until blockSize) {
                    for (j in 0 until expansionFactor) {
                        outputBuffer!![i * expansionFactor + j] = inputBuffer[i]
                    }
                }
                bufferedCount = 0
                outputPos = 0

                // Output as much as we can
                val toCopy = minOf(outputBuffer!!.size, availableOutput - produced)
                outputBuffer!!.copyInto(sink, sinkStartIndex + produced, 0, toCopy)
                outputPos = toCopy
                produced += toCopy
                if (outputPos >= outputBuffer!!.size) {
                    outputBuffer = null
                    outputPos = 0
                }
            }

            return TransformResult(consumed, produced)
        }

        override fun transformToByteArray(
            source: ByteArray,
            sourceStartIndex: Int,
            sourceEndIndex: Int
        ): ByteArray = ByteArray(0)

        override fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): Int {
            // Drain any remaining output buffer
            val pending = outputBuffer
            if (pending != null && outputPos < pending.size) {
                val availableOutput = endIndex - startIndex
                val toCopy = minOf(pending.size - outputPos, availableOutput)
                pending.copyInto(sink, startIndex, outputPos, outputPos + toCopy)
                outputPos += toCopy
                if (outputPos >= pending.size) {
                    outputBuffer = null
                }
                return toCopy
            }

            // Handle any remaining buffered input (incomplete block)
            if (bufferedCount > 0) {
                outputBuffer = ByteArray(bufferedCount * expansionFactor)
                for (i in 0 until bufferedCount) {
                    for (j in 0 until expansionFactor) {
                        outputBuffer!![i * expansionFactor + j] = inputBuffer[i]
                    }
                }
                bufferedCount = 0
                outputPos = 0

                val availableOutput = endIndex - startIndex
                val toCopy = minOf(outputBuffer!!.size, availableOutput)
                outputBuffer!!.copyInto(sink, startIndex, 0, toCopy)
                outputPos = toCopy
                if (outputPos >= outputBuffer!!.size) {
                    outputBuffer = null
                }
                return toCopy
            }

            return -1
        }

        override fun finalizeToByteArray(): ByteArray = ByteArray(0)

        override fun close() {}
    }

    /**
     * Test that demonstrates using transformToByteArray() for atomic large output during transform.
     *
     * When a transformation:
     * - Can't produce partial output (atomic operation)
     * - Doesn't know output size upfront
     *
     * It should override transformToByteArray() to return the output directly.
     */
    @Test
    fun streamingTransformAtomicLargeOutputWithByteArrayReturn() {
        val downstream = Buffer()
        // This transformation expands each input byte to 1000 bytes atomically
        // using transformToByteArray() - works regardless of segment size
        val transform = AtomicExpandTransform(expansionFactor = 1000)

        val sink = (downstream as RawSink).transformedWith(transform)
        sink.buffered().use { bufferedSink ->
            // 20 bytes input -> 20,000 bytes output (larger than segment)
            bufferedSink.write(ByteArray(20) { it.toByte() })
        }

        val result = downstream.readByteArray()
        assertEquals(20_000, result.size)
        // Verify content - each input byte is repeated 1000 times
        for (i in 0 until 20) {
            for (j in 0 until 1000) {
                assertEquals(i.toByte(), result[i * 1000 + j], "Mismatch at position ${i * 1000 + j}")
            }
        }
    }

    /**
     * A transformation that expands each byte atomically to many bytes.
     * Uses transformToByteArray() to handle output that may exceed segment size.
     */
    @OptIn(UnsafeIoApi::class)
    private class AtomicExpandTransform(private val expansionFactor: Int) : UnsafeByteArrayTransformation() {
        override fun maxOutputSize(inputSize: Int): Int = -1

        override fun transformIntoByteArray(
            source: ByteArray,
            sourceStartIndex: Int,
            sourceEndIndex: Int,
            sink: ByteArray,
            sinkStartIndex: Int,
            sinkEndIndex: Int
        ): TransformResult {
            // Not used when transformToByteArray is overridden
            return TransformResult(0, 0)
        }

        override fun transformToByteArray(
            source: ByteArray,
            sourceStartIndex: Int,
            sourceEndIndex: Int
        ): ByteArray {
            val inputSize = sourceEndIndex - sourceStartIndex
            if (inputSize == 0) return ByteArray(0)

            // Expand each byte to expansionFactor bytes
            val output = ByteArray(inputSize * expansionFactor)
            for (i in 0 until inputSize) {
                val byte = source[sourceStartIndex + i]
                for (j in 0 until expansionFactor) {
                    output[i * expansionFactor + j] = byte
                }
            }
            return output
        }

        override fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): Int = -1

        override fun finalizeToByteArray(): ByteArray = ByteArray(0)

        override fun close() {}
    }

    /**
     * Test that demonstrates using finalizeToByteArray() for atomic large output.
     *
     * When a transformation:
     * - Can't produce partial output (atomic operation)
     * - Doesn't know output size upfront
     *
     * It should override finalizeToByteArray() to return the output directly.
     */
    @Test
    fun streamingFinalizeAtomicLargeOutputWithByteArrayReturn() {
        val downstream = Buffer()
        // This transformation produces 20KB atomically during finalize
        // using finalizeToByteArray() - it works correctly
        val transform = AtomicLargeOutputTransform(outputSize = 20_000)

        val sink = (downstream as RawSink).transformedWith(transform)
        sink.buffered().use { bufferedSink ->
            bufferedSink.writeString("trigger")
        }

        val result = downstream.readByteArray()
        // Now it works - full output is produced
        assertEquals(20_000, result.size)
        // Verify content
        for (i in result.indices) {
            assertEquals((i % 256).toByte(), result[i])
        }
    }

    /**
     * A transformation that produces large output atomically during finalize.
     * Uses finalizeToByteArray() to handle output that may exceed segment size.
     */
    @OptIn(UnsafeIoApi::class)
    private class AtomicLargeOutputTransform(private val outputSize: Int) : UnsafeByteArrayTransformation() {
        override fun maxOutputSize(inputSize: Int): Int = -1

        override fun transformIntoByteArray(
            source: ByteArray,
            sourceStartIndex: Int,
            sourceEndIndex: Int,
            sink: ByteArray,
            sinkStartIndex: Int,
            sinkEndIndex: Int
        ): TransformResult {
            // Consume input, produce nothing
            return TransformResult(sourceEndIndex - sourceStartIndex, 0)
        }

        override fun transformToByteArray(
            source: ByteArray,
            sourceStartIndex: Int,
            sourceEndIndex: Int
        ): ByteArray = ByteArray(0)

        override fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): Int {
            // Return -1 to signal no incremental progress, triggering finalizeToByteArray fallback
            return -1
        }

        override fun finalizeToByteArray(): ByteArray {
            // Produce outputSize bytes atomically - works regardless of segment size
            return ByteArray(outputSize) { (it % 256).toByte() }
        }

        override fun close() {}
    }

    /**
     * A streaming transformation that buffers ALL input and only outputs
     * during finalization. Does NOT override maxOutputSize (returns -1).
     * Simulates algorithms like AES-GCM decryption but without known output size.
     */
    @OptIn(UnsafeIoApi::class)
    private class BufferAllExpandOnFinalizeTransform(
        private val expansionFactor: Int
    ) : UnsafeByteArrayTransformation() {
        private val inputBuffer = Buffer()
        private var outputBuffer: ByteArray? = null
        private var outputPos = 0

        override fun maxOutputSize(inputSize: Int): Int = -1

        override fun transformIntoByteArray(
            source: ByteArray,
            sourceStartIndex: Int,
            sourceEndIndex: Int,
            sink: ByteArray,
            sinkStartIndex: Int,
            sinkEndIndex: Int
        ): TransformResult {
            // Buffer all input, produce no output during transform
            val inputSize = sourceEndIndex - sourceStartIndex
            inputBuffer.write(source, sourceStartIndex, sourceStartIndex + inputSize)
            return TransformResult(inputSize, 0)
        }

        override fun transformToByteArray(
            source: ByteArray,
            sourceStartIndex: Int,
            sourceEndIndex: Int
        ): ByteArray = ByteArray(0)

        override fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): Int {
            // On first call, expand all buffered input
            if (outputBuffer == null && inputBuffer.size > 0) {
                val input = inputBuffer.readByteArray()
                outputBuffer = ByteArray(input.size * expansionFactor)
                for (i in input.indices) {
                    for (j in 0 until expansionFactor) {
                        outputBuffer!![i * expansionFactor + j] = input[i]
                    }
                }
                outputPos = 0
            }

            // Output as much as we can
            val pending = outputBuffer
            if (pending != null && outputPos < pending.size) {
                val availableOutput = endIndex - startIndex
                val toCopy = minOf(pending.size - outputPos, availableOutput)
                pending.copyInto(sink, startIndex, outputPos, outputPos + toCopy)
                outputPos += toCopy
                if (outputPos >= pending.size) {
                    outputBuffer = null
                }
                return toCopy
            }

            return -1
        }

        override fun finalizeToByteArray(): ByteArray = ByteArray(0)

        override fun close() {}
    }
}
