/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.samples.unsafe

import kotlinx.io.*
import kotlinx.io.unsafe.UnsafeByteArrayProcessor
import kotlin.test.*

/**
 * Samples demonstrating [UnsafeByteArrayProcessor] for zero-copy processing.
 */
class UnsafeByteArrayProcessorSamplesJvm {
    /**
     * A [UnsafeByteArrayProcessor] that computes a hash using JDK's [java.security.MessageDigest].
     *
     * This demonstrates zero-copy processing - bytes are accessed directly
     * from buffer segments without intermediate copies.
     */
    @OptIn(UnsafeIoApi::class)
    private class MessageDigestByteArrayProcessor(algorithm: String) : UnsafeByteArrayProcessor<ByteArray>() {
        private val digest = java.security.MessageDigest.getInstance(algorithm)

        override fun process(source: ByteArray, startIndex: Int, endIndex: Int) {
            digest.update(source, startIndex, endIndex - startIndex)
        }

        override fun compute(): ByteArray = digest.digest()

        override fun close() {}
    }

    /**
     * A [UnsafeByteArrayProcessor] that computes CRC32 using JDK's [java.util.zip.CRC32].
     *
     * This demonstrates zero-copy processing.
     */
    @OptIn(UnsafeIoApi::class)
    private class Crc32ByteArrayProcessor : UnsafeByteArrayProcessor<Long>() {
        private val crc32 = java.util.zip.CRC32()

        override fun process(source: ByteArray, startIndex: Int, endIndex: Int) {
            crc32.update(source, startIndex, endIndex - startIndex)
        }

        override fun compute(): Long = crc32.value

        override fun close() {}
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun ByteArray.toHexString(): String =
        joinToString("") { "%02x".format(it) }

    @Test
    fun sha256HashZeroCopy() {
        val data = "Hello, World!"
        val buffer = Buffer()
        buffer.writeString(data)

        @OptIn(UnsafeIoApi::class)
        val hash = (buffer as RawSource).compute(MessageDigestByteArrayProcessor("SHA-256"))

        // SHA-256 produces 32 bytes
        assertEquals(32, hash.size)

        // Verify against known hash
        val expectedHex = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f"
        assertEquals(expectedHex, hash.toHexString())
    }

    @Test
    fun crc32ZeroCopy() {
        val data = "Hello, World!"
        val buffer = Buffer()
        buffer.writeString(data)

        @OptIn(UnsafeIoApi::class)
        val checksum = (buffer as RawSource).compute(Crc32ByteArrayProcessor())

        // Known CRC32 for "Hello, World!"
        assertEquals(3964322768L, checksum)
    }

    @Test
    fun crc32IntermediateValuesZeroCopy() {
        @OptIn(UnsafeIoApi::class)
        val processor = Crc32ByteArrayProcessor()

        val buffer1 = Buffer()
        buffer1.writeString("Hello")
        processor.process(buffer1, buffer1.size)

        // Get intermediate CRC32
        val intermediate = processor.compute()
        assertTrue(intermediate > 0)

        val buffer2 = Buffer()
        buffer2.writeString(", World!")
        processor.process(buffer2, buffer2.size)

        // Final CRC32 should be different from intermediate
        val final = processor.compute()
        assertNotEquals(intermediate, final)

        // Multiple compute() calls return the same value
        assertEquals(final, processor.compute())

        processor.close()
    }

    @Test
    fun hashLargeDataZeroCopy() {
        // Generate large data to verify streaming works with multiple segments
        val data = ByteArray(100_000) { (it % 256).toByte() }
        val buffer = Buffer()
        buffer.write(data)

        @OptIn(UnsafeIoApi::class)
        val hash = (buffer as RawSource).compute(MessageDigestByteArrayProcessor("SHA-256"))

        assertEquals(32, hash.size)
    }

    @Test
    fun processorReuseZeroCopy() {
        @OptIn(UnsafeIoApi::class)
        val processor = MessageDigestByteArrayProcessor("SHA-256")

        // First hash
        val buffer1 = Buffer()
        buffer1.writeString("First")
        val hash1 = (buffer1 as RawSource).compute(processor)

        // Second hash - processor was reset by compute()
        val buffer2 = Buffer()
        buffer2.writeString("Second")
        val hash2 = (buffer2 as RawSource).compute(processor)

        // Hashes should be different
        assertFalse(hash1.contentEquals(hash2))

        processor.close()
    }
}
