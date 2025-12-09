/*
 * Copyright 2017-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.coroutines

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class DelimitingByteStreamDecoderTest {

    @Test
    fun singleMessageWithNewline() = runTest {
        val decoder = DelimitingByteStreamDecoder()
        val results = mutableListOf<ByteArray>()

        decoder.decode("hello\n".encodeToByteArray()) { results.add(it) }

        assertEquals(1, results.size)
        assertContentEquals("hello".encodeToByteArray(), results[0])
    }

    @Test
    fun multipleMessagesInSingleBatch() = runTest {
        val decoder = DelimitingByteStreamDecoder()
        val results = mutableListOf<ByteArray>()

        decoder.decode("first\nsecond\nthird\n".encodeToByteArray()) { results.add(it) }

        assertEquals(3, results.size)
        assertContentEquals("first".encodeToByteArray(), results[0])
        assertContentEquals("second".encodeToByteArray(), results[1])
        assertContentEquals("third".encodeToByteArray(), results[2])
    }

    @Test
    fun fragmentedMessageAcrossMultipleCalls() = runTest {
        val decoder = DelimitingByteStreamDecoder()
        val results = mutableListOf<ByteArray>()

        // Message split across three decode calls
        decoder.decode("hel".encodeToByteArray()) { results.add(it) }
        assertEquals(0, results.size, "No complete message yet")

        decoder.decode("lo wor".encodeToByteArray()) { results.add(it) }
        assertEquals(0, results.size, "Still no complete message")

        decoder.decode("ld\n".encodeToByteArray()) { results.add(it) }
        assertEquals(1, results.size)
        assertContentEquals("hello world".encodeToByteArray(), results[0])
    }

    @Test
    fun partialMessageFollowedByCompleteMessages() = runTest {
        val decoder = DelimitingByteStreamDecoder()
        val results = mutableListOf<ByteArray>()

        // First call: incomplete message
        decoder.decode("partial".encodeToByteArray()) { results.add(it) }
        assertEquals(0, results.size)

        // Second call: complete the first and add two more
        decoder.decode(" message\nfull message\nanother\n".encodeToByteArray()) { results.add(it) }

        assertEquals(3, results.size)
        assertContentEquals("partial message".encodeToByteArray(), results[0])
        assertContentEquals("full message".encodeToByteArray(), results[1])
        assertContentEquals("another".encodeToByteArray(), results[2])
    }

    @Test
    fun emptyMessages() = runTest {
        val decoder = DelimitingByteStreamDecoder()
        val results = mutableListOf<ByteArray>()

        decoder.decode("\n\n\n".encodeToByteArray()) { results.add(it) }

        assertEquals(3, results.size)
        assertEquals(0, results[0].size, "Empty message should produce empty byte array")
        assertEquals(0, results[1].size)
        assertEquals(0, results[2].size)
    }

    @Test
    fun noDelimiterLeavesDataInBuffer() = runTest {
        val decoder = DelimitingByteStreamDecoder()
        val results = mutableListOf<ByteArray>()

        // Data without delimiter stays in buffer
        decoder.decode("no delimiter here".encodeToByteArray()) { results.add(it) }
        assertEquals(0, results.size, "Should buffer incomplete message")

        // Adding delimiter flushes the buffer
        decoder.decode(" - now it ends\n".encodeToByteArray()) { results.add(it) }
        assertEquals(1, results.size)
        assertContentEquals("no delimiter here - now it ends".encodeToByteArray(), results[0])
    }

    @Test
    fun customDelimiter() = runTest {
        val decoder = DelimitingByteStreamDecoder(delimiter = '|'.code.toByte())
        val results = mutableListOf<ByteArray>()

        decoder.decode("first|second|third|".encodeToByteArray()) { results.add(it) }

        assertEquals(3, results.size)
        assertContentEquals("first".encodeToByteArray(), results[0])
        assertContentEquals("second".encodeToByteArray(), results[1])
        assertContentEquals("third".encodeToByteArray(), results[2])
    }

    @Test
    fun emptyInput() = runTest {
        val decoder = DelimitingByteStreamDecoder()
        val results = mutableListOf<ByteArray>()

        decoder.decode(ByteArray(0)) { results.add(it) }

        assertEquals(0, results.size)
    }

    @Test
    fun singleDelimiterOnly() = runTest {
        val decoder = DelimitingByteStreamDecoder()
        val results = mutableListOf<ByteArray>()

        decoder.decode("\n".encodeToByteArray()) { results.add(it) }

        assertEquals(1, results.size)
        assertEquals(0, results[0].size)
    }

    @Test
    fun byteByByteDecoding() = runTest {
        val decoder = DelimitingByteStreamDecoder()
        val results = mutableListOf<ByteArray>()
        val message = "test\n"

        // Decode one byte at a time
        message.forEach { char ->
            decoder.decode(byteArrayOf(char.code.toByte())) { results.add(it) }
        }

        assertEquals(1, results.size)
        assertContentEquals("test".encodeToByteArray(), results[0])
    }

    @Test
    fun largeMessage() = runTest {
        val decoder = DelimitingByteStreamDecoder()
        val results = mutableListOf<ByteArray>()
        val largeContent = "x".repeat(10000)

        decoder.decode("$largeContent\n".encodeToByteArray()) { results.add(it) }

        assertEquals(1, results.size)
        assertContentEquals(largeContent.encodeToByteArray(), results[0])
    }

    @Test
    fun mixedCompleteAndIncompleteMessages() = runTest {
        val decoder = DelimitingByteStreamDecoder()
        val results = mutableListOf<ByteArray>()

        // Complete message followed by incomplete
        decoder.decode("complete\nincomp".encodeToByteArray()) { results.add(it) }
        assertEquals(1, results.size)
        assertContentEquals("complete".encodeToByteArray(), results[0])

        // Complete the incomplete message and add new incomplete
        decoder.decode("lete\nanother incomplete".encodeToByteArray()) { results.add(it) }
        assertEquals(2, results.size)
        assertContentEquals("incomplete".encodeToByteArray(), results[1])

        // Finish with complete message
        decoder.decode(" message\nfinal\n".encodeToByteArray()) { results.add(it) }
        assertEquals(4, results.size)
        assertContentEquals("another incomplete message".encodeToByteArray(), results[2])
        assertContentEquals("final".encodeToByteArray(), results[3])
    }

    @Test
    fun delimiterAtBufferBoundary() = runTest {
        val decoder = DelimitingByteStreamDecoder()
        val results = mutableListOf<ByteArray>()

        // Delimiter exactly at the end of input
        decoder.decode("message".encodeToByteArray()) { results.add(it) }
        decoder.decode("\n".encodeToByteArray()) { results.add(it) }

        assertEquals(1, results.size)
        assertContentEquals("message".encodeToByteArray(), results[0])
    }

    @Test
    fun consecutiveDelimiters() = runTest {
        val decoder = DelimitingByteStreamDecoder()
        val results = mutableListOf<ByteArray>()

        decoder.decode("a\n\n\nb".encodeToByteArray()) { results.add(it) }

        // Should produce: "a", "", "", and buffered "b"
        assertEquals(3, results.size)
        assertContentEquals("a".encodeToByteArray(), results[0])
        assertEquals(0, results[1].size)
        assertEquals(0, results[2].size)

        // Complete the last message
        decoder.decode("\n".encodeToByteArray()) { results.add(it) }
        assertEquals(4, results.size)
        assertContentEquals("b".encodeToByteArray(), results[3])
    }

    @Test
    fun binaryDataWithNewlineDelimiter() = runTest {
        val decoder = DelimitingByteStreamDecoder()
        val results = mutableListOf<ByteArray>()

        // Binary data that happens to contain our delimiter byte
        val binaryData = byteArrayOf(0x00, 0x01, 0x02, '\n'.code.toByte(), 0x03, 0x04, '\n'.code.toByte())

        decoder.decode(binaryData) { results.add(it) }

        assertEquals(2, results.size)
        assertContentEquals(byteArrayOf(0x00, 0x01, 0x02), results[0])
        assertContentEquals(byteArrayOf(0x03, 0x04), results[1])
    }
}
