/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlin.test.Test
import kotlin.test.assertEquals

class ProcessorTest {

    @Test
    fun computeFromSource() {
        val buffer = Buffer()
        buffer.writeString("Hello, World!")
        val source = buffer as RawSource

        val result = source.compute(ByteCountProcessor())

        assertEquals(13L, result)
    }

    @Test
    fun computeFromEmptySource() {
        val buffer = Buffer()
        val source = buffer as RawSource

        val result = source.compute(ByteCountProcessor())

        assertEquals(0L, result)
    }

    @Test
    fun computeLargeData() {
        val buffer = Buffer()
        val data = "x".repeat(10000)
        buffer.writeString(data)
        val source = buffer as RawSource

        val result = source.compute(ByteCountProcessor())

        assertEquals(10000L, result)
    }

    @Test
    fun processDoesNotConsumeBytes() {
        val processor = ByteCountProcessor()
        val buffer = Buffer()
        buffer.writeString("Hello")

        processor.process(buffer, buffer.size)

        // Buffer should still contain the data
        assertEquals(5L, buffer.size)
        assertEquals("Hello", buffer.readString())
    }

    @Test
    fun computeResetsProcessor() {
        val processor = ByteCountProcessor()
        val buffer = Buffer()
        buffer.writeString("Test")

        processor.process(buffer, buffer.size)
        val first = processor.compute()

        assertEquals(4L, first)

        // After compute(), processor should be reset
        val buffer2 = Buffer()
        buffer2.writeString("More")
        processor.process(buffer2, buffer2.size)
        val second = processor.compute()

        // Should be 4, not 8, because compute() reset the state
        assertEquals(4L, second)
    }

    @Test
    fun currentDoesNotReset() {
        val processor = ByteCountProcessor()

        val buffer1 = Buffer()
        buffer1.writeString("Hello")
        processor.process(buffer1, buffer1.size)

        // Get intermediate result with current()
        assertEquals(5L, processor.current())

        val buffer2 = Buffer()
        buffer2.writeString(", World!")
        processor.process(buffer2, buffer2.size)

        // current() should reflect cumulative value
        assertEquals(13L, processor.current())

        // current() can be called multiple times
        assertEquals(13L, processor.current())

        // compute() returns same value but resets
        assertEquals(13L, processor.compute())

        // After compute(), should be reset to 0
        assertEquals(0L, processor.current())
    }

    @Test
    fun processorMultipleChunks() {
        val processor = ByteCountProcessor()

        val buffer1 = Buffer()
        buffer1.writeString("Hello")
        processor.process(buffer1, buffer1.size)

        val buffer2 = Buffer()
        buffer2.writeString(", ")
        processor.process(buffer2, buffer2.size)

        val buffer3 = Buffer()
        buffer3.writeString("World!")
        processor.process(buffer3, buffer3.size)

        val result = processor.compute()
        assertEquals(13L, result)
    }

    @Test
    fun sumProcessor() {
        val buffer = Buffer()
        buffer.writeByte(1)
        buffer.writeByte(2)
        buffer.writeByte(3)
        buffer.writeByte(4)
        val source = buffer as RawSource

        val result = source.compute(ByteSumProcessor())

        assertEquals(10, result)
    }

    @Test
    fun processorReuseAfterCompute() {
        val processor = ByteCountProcessor()

        // First computation
        val buffer1 = Buffer()
        buffer1.writeString("Hello")
        processor.process(buffer1, buffer1.size)
        @Suppress("UNUSED_VARIABLE")
        val ignored = processor.compute()  // resets

        // Second computation - should start fresh
        val buffer2 = Buffer()
        buffer2.writeString("World!")
        processor.process(buffer2, buffer2.size)
        val result = processor.compute()

        assertEquals(6L, result)  // Just "World!", not cumulative
    }

    @Test
    fun processPartialBuffer() {
        val processor = ByteCountProcessor()
        val buffer = Buffer()
        buffer.writeString("Hello, World!")

        // Process only first 5 bytes
        processor.process(buffer, 5)

        assertEquals(5L, processor.current())
        // Buffer should still have all data
        assertEquals(13L, buffer.size)
    }

    @Test
    fun processedWithSource() {
        val processor = ByteCountProcessor()
        val upstream = Buffer()
        upstream.writeString("Hello, World!")

        val processedWithSource = (upstream as RawSource).processedWith(processor)
        val output = Buffer()

        // Read all data through the processedWith source
        processedWithSource.buffered().use { source ->
            source.transferTo(output)
        }

        // Data should flow through unchanged
        assertEquals("Hello, World!", output.readString())

        // Processor should have processedWith all data
        assertEquals(13L, processor.current())

        // Can still compute after source is closed
        assertEquals(13L, processor.compute())

        processor.close()
    }

    @Test
    fun processedWithSourcePartialReads() {
        val processor = ByteCountProcessor()
        val upstream = Buffer()
        upstream.writeString("Hello, World!")

        val processedWithSource = (upstream as RawSource).processedWith(processor)
        val output = Buffer()

        // Read in chunks directly from RawSource (buffered reads ahead)
        @Suppress("UNUSED_VARIABLE")
        var bytesRead = processedWithSource.readAtMostTo(output, 5)
        assertEquals("Hello", output.readString())
        assertEquals(5L, processor.current())

        bytesRead = processedWithSource.readAtMostTo(output, 2)
        assertEquals(", ", output.readString())
        assertEquals(7L, processor.current())

        bytesRead = processedWithSource.readAtMostTo(output, 6)
        assertEquals("World!", output.readString())
        assertEquals(13L, processor.current())

        processedWithSource.close()
        processor.close()
    }

    @Test
    fun processedWithSink() {
        val processor = ByteCountProcessor()
        val downstream = Buffer()

        val processedWithSink = (downstream as RawSink).processedWith(processor)

        processedWithSink.buffered().use { sink ->
            sink.writeString("Hello, World!")
        }

        // Data should flow through unchanged
        assertEquals("Hello, World!", downstream.readString())

        // Processor should have processedWith all data
        assertEquals(13L, processor.compute())

        processor.close()
    }

    @Test
    fun processedWithSinkMultipleWrites() {
        val processor = ByteCountProcessor()
        val downstream = Buffer()

        val processedWithSink = (downstream as RawSink).processedWith(processor)
        val buffered = processedWithSink.buffered()

        buffered.writeString("Hello")
        buffered.flush()
        assertEquals(5L, processor.current())

        buffered.writeString(", World!")
        buffered.flush()
        assertEquals(13L, processor.current())

        buffered.close()

        assertEquals("Hello, World!", downstream.readString())
        assertEquals(13L, processor.compute())

        processor.close()
    }

    @Test
    fun processedWithSourceProcessorNotClosed() {
        val processor = ByteCountProcessor()
        val upstream = Buffer()
        upstream.writeString("Test")

        val processedWithSource = (upstream as RawSource).processedWith(processor)
        processedWithSource.buffered().use { it.readString() }

        // Processor should still be usable after source is closed
        assertEquals(4L, processor.current())
        assertEquals(4L, processor.compute())

        // Can reuse processor with another source
        val upstream2 = Buffer()
        upstream2.writeString("More")
        val processedWithSource2 = (upstream2 as RawSource).processedWith(processor)
        processedWithSource2.buffered().use { it.readString() }

        assertEquals(4L, processor.compute())

        processor.close()
    }

    @Test
    fun processedWithSinkProcessorNotClosed() {
        val processor = ByteCountProcessor()
        val downstream = Buffer()

        val processedWithSink = (downstream as RawSink).processedWith(processor)
        processedWithSink.buffered().use { it.writeString("Test") }

        // Processor should still be usable after sink is closed
        assertEquals(4L, processor.current())

        // Can reuse processor with another sink
        val downstream2 = Buffer()
        val processedWithSink2 = (downstream2 as RawSink).processedWith(processor)
        processedWithSink2.buffered().use { it.writeString("More") }

        assertEquals(8L, processor.compute())

        processor.close()
    }
}

/**
 * Simple test processor that counts bytes.
 */
private class ByteCountProcessor : Processor<Long> {
    private var count: Long = 0
    private var closed = false

    override fun process(source: Buffer, byteCount: Long) {
        check(!closed) { "Processor is closed" }
        require(byteCount >= 0) { "byteCount: $byteCount" }

        val toProcess = minOf(byteCount, source.size)
        // Read without consuming - just count
        count += toProcess
    }

    /** Returns current value without resetting. */
    fun current(): Long {
        check(!closed) { "Processor is closed" }
        return count
    }

    override fun compute(): Long {
        check(!closed) { "Processor is closed" }
        val result = count
        count = 0  // reset for reuse
        return result
    }

    override fun close() {
        closed = true
    }
}

/**
 * Test processor that sums byte values.
 */
private class ByteSumProcessor : Processor<Int> {
    private var sum: Int = 0
    private var closed = false

    override fun process(source: Buffer, byteCount: Long) {
        check(!closed) { "Processor is closed" }
        require(byteCount >= 0) { "byteCount: $byteCount" }

        val toProcess = minOf(byteCount, source.size).toInt()
        // Peek at bytes without consuming
        for (i in 0 until toProcess) {
            sum += source[i.toLong()].toInt() and 0xFF
        }
    }

    override fun compute(): Int {
        check(!closed) { "Processor is closed" }
        val result = sum
        sum = 0  // reset for reuse
        return result
    }

    override fun close() {
        closed = true
    }
}
