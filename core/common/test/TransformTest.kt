/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TransformTest {

    @Test
    fun transformSinkBasicRoundTrip() {
        val downstream = Buffer()
        val transform = DoubleByteTransform()

        val sink = downstream.transform(transform)
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
        val source = upstream.transform(transform)

        val result = source.buffered().readString()
        assertEquals("abc", result)
    }

    @Test
    fun transformSinkEmptyData() {
        val downstream = Buffer()
        val transform = DoubleByteTransform()

        val sink = downstream.transform(transform)
        sink.buffered().use { /* write nothing */ }

        assertEquals("", downstream.readString())
    }

    @Test
    fun transformSourceEmptyData() {
        val upstream = Buffer()
        val transform = HalveByteTransform()

        val source = upstream.transform(transform)
        val result = source.buffered().readString()

        assertEquals("", result)
    }

    @Test
    fun transformSinkLargeData() {
        val downstream = Buffer()
        val transform = DoubleByteTransform()

        val original = "x".repeat(10000)
        val sink = downstream.transform(transform)
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
        val source = upstream.transform(transform)

        val result = source.buffered().readString()
        assertEquals("x".repeat(10000), result)
    }

    @Test
    fun transformSinkBinaryData() {
        val downstream = Buffer()
        val transform = DoubleByteTransform()

        val original = ByteArray(256) { it.toByte() }
        val sink = downstream.transform(transform)
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
        val source = upstream.transform(transform)

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

        val transform = object : Transform {
            override fun transform(source: Buffer, sink: Buffer) {
                sink.write(source, source.size)
            }
            override fun finish(sink: Buffer) {}
            override val isFinished: Boolean = false
            override fun close() {
                transformClosed = true
            }
        }

        val sink = downstream.transform(transform)
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

        val transform = object : Transform {
            private var finished = false
            override fun transform(source: Buffer, sink: Buffer) {
                if (source.size > 0) {
                    sink.write(source, source.size)
                }
            }
            override fun finish(sink: Buffer) {
                finished = true
            }
            override val isFinished: Boolean get() = finished
            override fun close() {
                transformClosed = true
            }
        }

        val source = upstream.transform(transform)
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

        val sink = downstream.transform(transform)
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

        val source = upstream.transform(transform)
        source.close()

        assertFailsWith<IllegalStateException> {
            source.readAtMostTo(Buffer(), 10)
        }
    }

    @Test
    fun transformSinkWithFinishData() {
        val downstream = Buffer()
        val transform = AppendTrailerTransform("--END--")

        val sink = downstream.transform(transform)
        sink.buffered().use { bufferedSink ->
            bufferedSink.writeString("Hello")
        }

        assertEquals("Hello--END--", downstream.readString())
    }

    @Test
    fun transformSinkMultipleWrites() {
        val downstream = Buffer()
        val transform = DoubleByteTransform()

        val sink = downstream.transform(transform).buffered()
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

        val sink = downstream.transform(transform).buffered()
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
     */
    private class DoubleByteTransform : Transform {
        private var finished = false

        override fun transform(source: Buffer, sink: Buffer) {
            while (!source.exhausted()) {
                val byte = source.readByte()
                sink.writeByte(byte)
                sink.writeByte(byte)
            }
        }

        override fun finish(sink: Buffer) {
            finished = true
        }

        override val isFinished: Boolean get() = finished

        override fun close() {}
    }

    /**
     * A transform that takes every other byte (halves the data).
     * Expects input where each byte appears twice.
     */
    private class HalveByteTransform : Transform {
        private var finished = false

        override fun transform(source: Buffer, sink: Buffer) {
            while (source.size >= 2) {
                val byte = source.readByte()
                source.skip(1) // Skip the duplicate
                sink.writeByte(byte)
            }
            // If source has remaining data and is finished, we're done
            if (source.exhausted()) {
                finished = true
            }
        }

        override fun finish(sink: Buffer) {
            finished = true
        }

        override val isFinished: Boolean get() = finished

        override fun close() {}
    }

    /**
     * A transform that appends a trailer on finish.
     */
    private class AppendTrailerTransform(private val trailer: String) : Transform {
        private var finished = false

        override fun transform(source: Buffer, sink: Buffer) {
            sink.write(source, source.size)
        }

        override fun finish(sink: Buffer) {
            sink.writeString(trailer)
            finished = true
        }

        override val isFinished: Boolean get() = finished

        override fun close() {}
    }
}
