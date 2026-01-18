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
}

/**
 * Simple test processor that counts bytes.
 * Implements RunningProcessor to support current().
 */
private class ByteCountProcessor : RunningProcessor<Long> {
    private var count: Long = 0
    private var closed = false

    override fun process(source: Buffer, byteCount: Long) {
        check(!closed) { "Processor is closed" }
        require(byteCount >= 0) { "byteCount: $byteCount" }

        val toProcess = minOf(byteCount, source.size)
        // Read without consuming - just count
        count += toProcess
    }

    override fun current(): Long {
        check(!closed) { "Processor is closed" }
        return count
    }

    override fun compute(): Long {
        check(!closed) { "Processor is closed" }
        val result = count
        count = 0  // reset
        return result
    }

    override fun close() {
        closed = true
    }
}

/**
 * Test processor that sums byte values.
 * Implements RunningProcessor to support current().
 */
private class ByteSumProcessor : RunningProcessor<Int> {
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

    override fun current(): Int {
        check(!closed) { "Processor is closed" }
        return sum
    }

    override fun compute(): Int {
        check(!closed) { "Processor is closed" }
        val result = sum
        sum = 0  // reset
        return result
    }

    override fun close() {
        closed = true
    }
}
