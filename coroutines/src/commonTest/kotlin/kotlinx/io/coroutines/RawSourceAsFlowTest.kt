/*
 * Copyright 2017-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.RawSource
import kotlinx.io.writeString
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RawSourceAsFlowTest {

    @Test
    fun consumeDelimitedStream() = runTest {
        val source = Buffer().apply {
            writeString("line1\nline2\nline3")
        }

        val decoder = DelimitingByteStreamDecoder()
        val results = source.asFlow(decoder, READ_BUFFER_SIZE).toList()

        assertEquals(3, results.size)
        assertContentEquals("line1".encodeToByteArray(), results[0])
        assertContentEquals("line2".encodeToByteArray(), results[1])
        assertContentEquals("line3".encodeToByteArray(), results[2])
    }

    @Test
    fun emptySource() = runTest {
        val source = Buffer()
        val decoder = DelimitingByteStreamDecoder()
        val results = source.asFlow(decoder, READ_BUFFER_SIZE).toList()

        assertEquals(0, results.size)
    }

    @Test
    fun largeStreamWithMultipleChunks() = runTest {
        // Create a large source that will require multiple read operations
        val source = Buffer().apply {
            repeat(1000) { i ->
                writeString("message_$i\n")
            }
        }

        val decoder = DelimitingByteStreamDecoder()
        val results = source.asFlow(decoder, READ_BUFFER_SIZE).toList()

        assertEquals(1000, results.size)
        results.forEachIndexed { index, bytes ->
            assertContentEquals("message_$index".encodeToByteArray(), bytes)
        }
    }

    @Test
    fun flowCancellationClosesSource() = runTest {
        var sourceClosed = false
        val source = object : RawSource {
            val buffer = Buffer().apply {
                repeat(100) { writeString("data\n") }
            }

            override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
                return buffer.readAtMostTo(sink, byteCount)
            }

            override fun close() {
                sourceClosed = true
            }
        }

        val decoder = DelimitingByteStreamDecoder()

        // Take only the first 5 items, cancelling the flow early
        val results = source.asFlow(decoder, READ_BUFFER_SIZE).take(5).toList()

        assertEquals(5, results.size)
        assertTrue(sourceClosed, "Source should be closed when flow is cancelled")
    }

    @Test
    fun ioExceptionPropagatedAsFlowFailure() = runTest {
        val expectedMessage = "Simulated I/O error"
        val failingSource = object : RawSource {
            override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
                throw IOException(expectedMessage)
            }

            override fun close() {
                // no-op
            }
        }

        val decoder = DelimitingByteStreamDecoder()
        var caughtException: Throwable? = null

        failingSource.asFlow(decoder)
            .catch { e -> caughtException = e }
            .toList()

        assertTrue(caughtException is IOException)
        assertEquals(expectedMessage, caughtException?.message)
    }

    @Test
    fun partialMessageHandlingAcrossReadBoundaries() = runTest {
        // Create a source where messages span across READ_BUFFER_SIZE boundaries
        val longMessage = "x".repeat(READ_BUFFER_SIZE.toInt() + 100)
        val source = Buffer().apply {
            writeString("short\n")
            writeString("$longMessage\n")
            writeString("another\n")
        }

        val decoder = DelimitingByteStreamDecoder()
        val results = source.asFlow(decoder, READ_BUFFER_SIZE).toList()

        assertEquals(3, results.size)
        assertContentEquals("short".encodeToByteArray(), results[0])
        assertContentEquals(longMessage.encodeToByteArray(), results[1])
        assertContentEquals("another".encodeToByteArray(), results[2])
    }

    @Test
    fun customDecoderWithCustomLogic() = runTest {
        // Custom decoder that counts bytes and emits the count
        val countingDecoder = object : StreamingDecoder<Int> {
            override suspend fun decode(bytes: ByteArray, byteConsumer: suspend (Int) -> Unit) {
                byteConsumer(bytes.size)
            }

            override suspend fun onClose(byteConsumer: suspend (Int) -> Unit) {
                byteConsumer(0)
            }
        }

        val source = Buffer().apply {
            write(ByteArray(100))
            write(ByteArray(200))
            write(ByteArray(150))
        }

        val results = source.asFlow(countingDecoder, READ_BUFFER_SIZE).toList()

        assertTrue(results.isNotEmpty())
        assertEquals(results.sum(), 450)
    }

    @Test
    fun sourceExhaustionHandledGracefully() = runTest {
        val source = Buffer().apply {
            writeString("first\nsecond\n")
        }

        val decoder = DelimitingByteStreamDecoder()

        // Collect flow twice to ensure it properly handles exhaustion
        val firstCollection = source.asFlow(decoder, READ_BUFFER_SIZE).toList()

        assertEquals(2, firstCollection.size)

        // Source is now exhausted, second collection should be empty
        assertEquals(0, source.asFlow(decoder, READ_BUFFER_SIZE).toList().size)
    }

    @Test
    fun coroutineCancellationDuringRead() = runTest {
        val source = Buffer().apply {
            repeat(1000) { writeString("message $it\n") }
        }

        val decoder = DelimitingByteStreamDecoder()
        val results = mutableListOf<ByteArray>()

        try {
            source.asFlow(decoder, READ_BUFFER_SIZE).collect { bytes ->
                results.add(bytes)
                if (results.size == 10) {
                    throw CancellationException("Test cancellation")
                }
            }
        } catch (_: CancellationException) {
            // Expected
        }

        assertEquals(10, results.size, "Should collect exactly 10 items before cancellation")
    }

    @Test
    fun veryLargeMessagesHandledCorrectly() = runTest {
        // Message larger than multiple READ_BUFFER_SIZE chunks
        val veryLargeMessage = "A".repeat(READ_BUFFER_SIZE.toInt() * 3)
        val source = Buffer().apply {
            writeString("$veryLargeMessage\n")
        }

        val decoder = DelimitingByteStreamDecoder()
        val results = source.asFlow(decoder, READ_BUFFER_SIZE).toList()

        assertEquals(1, results.size)
        assertContentEquals(veryLargeMessage.encodeToByteArray(), results[0])
    }
}
