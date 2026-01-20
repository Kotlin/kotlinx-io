/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.samples

import kotlinx.io.*
import kotlinx.io.unsafe.UnsafeByteArrayTransformation
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.*

class KotlinxIoSamplesJvm {
    @Test
    fun inputStreamAsSource() {
        val data = ByteArray(100) { it.toByte() }
        val inputStream = ByteArrayInputStream(data)

        val receivedData = inputStream.asSource().buffered().readByteArray()
        assertContentEquals(data, receivedData)
    }

    @Test
    fun outputStreamAsSink() {
        val data = ByteArray(100) { it.toByte() }
        val outputStream = ByteArrayOutputStream()

        val sink = outputStream.asSink().buffered()
        sink.write(data)
        sink.flush()

        assertContentEquals(data, outputStream.toByteArray())
    }

    @Test
    fun asStream() {
        val buffer = Buffer()
        val data = ByteArray(100) { it.toByte() }

        GZIPOutputStream(buffer.asOutputStream()).use {
            it.write(data)
        }
        val decodedData = GZIPInputStream(buffer.asInputStream()).use {
            it.readBytes()
        }
        assertContentEquals(data, decodedData)
    }

    @Test
    fun readWriteByteBuffer() {
        val buffer = Buffer()
        val nioByteBuffer = ByteBuffer.allocate(1024)

        buffer.writeString("hello")
        val bytesRead = buffer.readAtMostTo(nioByteBuffer)
        assertEquals(5, bytesRead)
        assertEquals(5, nioByteBuffer.capacity() - nioByteBuffer.remaining())

        nioByteBuffer.position(0)
        nioByteBuffer.limit(5)

        val bytesWrite = buffer.write(nioByteBuffer)
        assertEquals(5, bytesWrite)
        assertEquals("hello", buffer.readString())
    }

    @Test
    fun bufferTransferToStream() {
        val buffer = Buffer()
        buffer.writeString("hello")

        val outputStream = ByteArrayOutputStream()
        buffer.readTo(outputStream)

        assertTrue(buffer.exhausted())

        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        buffer.transferFrom(inputStream)

        assertEquals("hello", buffer.readString())
    }

    @Test
    fun writeInputStreamToBuffer() {
        val inputStream = ByteArrayInputStream("hello!".encodeToByteArray())
        val buffer = Buffer()

        buffer.write(inputStream, 5)
        assertEquals("hello", buffer.readString())
    }

    @Test
    fun copyBufferToOutputStream() {
        val buffer = Buffer()
        buffer.writeString("string")

        val outputStream = ByteArrayOutputStream()
        buffer.copyTo(outputStream, startIndex = 2, endIndex = 6)

        assertEquals("string", buffer.readString())
        assertEquals("ring", outputStream.toString("UTF-8"))
    }

    @Test
    fun transferBufferFromByteBuffer() {
        val buffer = Buffer()
        val nioBuffer = ByteBuffer.allocate(32)

        nioBuffer.put("hello".encodeToByteArray())
        nioBuffer.position(0)
        nioBuffer.limit(5)
        buffer.transferFrom(nioBuffer)

        assertEquals("hello", buffer.readString())
        assertEquals(5, nioBuffer.position())
    }

    @Test
    fun readWriteStrings() {
        val buffer = Buffer()

        buffer.write(byteArrayOf(0, 0, 0, 0x68, 0, 0, 0, 0x69))
        assertEquals("hi", buffer.readString(Charsets.UTF_32BE))

        buffer.writeString("hi", Charsets.UTF_16BE)
        assertContentEquals(byteArrayOf(0, 0x68, 0, 0x69), buffer.readByteArray())
    }

    @Test
    fun readStringBounded() {
        val buffer = Buffer()

        buffer.write(byteArrayOf(0, 0, 0, 0x68, 0, 0, 0, 0x69))
        assertEquals("h", buffer.readString(byteCount = 4, charset = Charsets.UTF_32BE))
    }
}

class ProcessorSamplesJvm {
    /**
     * A [Processor] that computes a hash using JDK's [java.security.MessageDigest].
     *
     * This demonstrates how to implement [Processor] for cryptographic hashing.
     * The processor reads bytes without consuming them from the buffer.
     */
    private class MessageDigestProcessor(algorithm: String) : Processor<ByteArray> {
        private val digest = java.security.MessageDigest.getInstance(algorithm)
        private var closed = false

        override fun process(source: Buffer, byteCount: Long) {
            check(!closed) { "Processor is closed" }
            require(byteCount >= 0) { "byteCount: $byteCount" }

            val toProcess = minOf(byteCount, source.size).toInt()
            // Read bytes without consuming - copy to temp buffer then to array
            val tempBuffer = Buffer()
            source.copyTo(tempBuffer, startIndex = 0, endIndex = toProcess.toLong())
            val bytes = tempBuffer.readByteArray()
            digest.update(bytes)
        }

        override fun compute(): ByteArray {
            check(!closed) { "Processor is closed" }
            // digest() returns result AND resets the MessageDigest
            return digest.digest()
        }

        override fun close() {
            closed = true
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun ByteArray.toHexString(): String =
        joinToString("") { "%02x".format(it) }

    @Test
    fun sha256Hash() {
        val data = "Hello, World!"
        val buffer = Buffer()
        buffer.writeString(data)

        val hash = (buffer as RawSource).compute(MessageDigestProcessor("SHA-256"))

        // SHA-256 produces 32 bytes
        assertEquals(32, hash.size)

        // Verify against known hash
        val expectedHex = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f"
        assertEquals(expectedHex, hash.toHexString())
    }

    @Test
    fun md5Hash() {
        val data = "Hello, World!"
        val buffer = Buffer()
        buffer.writeString(data)

        val hash = (buffer as RawSource).compute(MessageDigestProcessor("MD5"))

        // MD5 produces 16 bytes
        assertEquals(16, hash.size)

        // Verify against known hash
        val expectedHex = "65a8e27d8879283831b664bd8b7f0ad4"
        assertEquals(expectedHex, hash.toHexString())
    }

    @Test
    fun hashLargeData() {
        // Generate large data to verify streaming works
        val data = ByteArray(100_000) { (it % 256).toByte() }
        val buffer = Buffer()
        buffer.write(data)

        val hash = (buffer as RawSource).compute(MessageDigestProcessor("SHA-256"))

        assertEquals(32, hash.size)
    }

    @Test
    fun processorReuse() {
        val processor = MessageDigestProcessor("SHA-256")

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

    /**
     * A [Processor] that computes CRC32 using JDK's [java.util.zip.CRC32].
     */
    private class Crc32Processor : Processor<Long> {
        private val crc32 = java.util.zip.CRC32()
        private var closed = false

        override fun process(source: Buffer, byteCount: Long) {
            check(!closed) { "Processor is closed" }
            require(byteCount >= 0) { "byteCount: $byteCount" }

            val toProcess = minOf(byteCount, source.size).toInt()
            // Read bytes without consuming - copy to temp buffer then to array
            val tempBuffer = Buffer()
            source.copyTo(tempBuffer, startIndex = 0, endIndex = toProcess.toLong())
            val bytes = tempBuffer.readByteArray()
            crc32.update(bytes)
        }

        override fun compute(): Long {
            check(!closed) { "Processor is closed" }
            return crc32.value
        }

        override fun close() {
            closed = true
        }
    }

    @Test
    fun crc32Checksum() {
        val data = "Hello, World!"
        val buffer = Buffer()
        buffer.writeString(data)

        val checksum = (buffer as RawSource).compute(Crc32Processor())

        // Known CRC32 for "Hello, World!"
        assertEquals(3964322768L, checksum)
    }

    @Test
    fun crc32IntermediateValues() {
        val processor = Crc32Processor()

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
    fun crc32LargeData() {
        val data = ByteArray(100_000) { (it % 256).toByte() }
        val buffer = Buffer()
        buffer.write(data)

        val checksum = (buffer as RawSource).compute(Crc32Processor())

        assertTrue(checksum > 0)
    }
}

class CipherTransformationSamples {
    /**
     * A [UnsafeByteArrayTransformation] that encrypts or decrypts data using a [Cipher].
     *
     * This transformation can be used with any cipher algorithm supported by the JVM,
     * such as AES, DES, or RSA.
     */
    @OptIn(UnsafeIoApi::class)
    private class CipherTransformation(private val cipher: Cipher) : UnsafeByteArrayTransformation() {
        private var finalized = false

        override fun maxOutputSize(inputSize: Int): Int = cipher.getOutputSize(inputSize)

        override fun transformIntoByteArray(
            source: ByteArray,
            sourceStartIndex: Int,
            sourceEndIndex: Int,
            sink: ByteArray,
            sinkStartIndex: Int,
            sinkEndIndex: Int
        ): TransformResult {
            val inputSize = sourceEndIndex - sourceStartIndex
            val written = cipher.update(source, sourceStartIndex, inputSize, sink, sinkStartIndex)
            return TransformResult(inputSize, written)
        }

        override fun transformToByteArray(
            source: ByteArray,
            sourceStartIndex: Int,
            sourceEndIndex: Int
        ): ByteArray {
            val inputSize = sourceEndIndex - sourceStartIndex
            if (inputSize == 0) return ByteArray(0)
            return cipher.update(source, sourceStartIndex, inputSize) ?: ByteArray(0)
        }

        override fun finalizeIntoByteArray(sink: ByteArray, startIndex: Int, endIndex: Int): Int {
            if (finalized) return -1
            finalized = true
            return cipher.doFinal(sink, startIndex)
        }

        override fun finalizeToByteArray(): ByteArray {
            if (finalized) return ByteArray(0)
            finalized = true
            return cipher.doFinal()
        }

        override fun close() {}
    }

    @Test
    fun aesEncryptionDecryption() {
        val originalText = "Hello, AES encryption with kotlinx-io Transformation API!"

        // Generate a 128-bit AES key and IV
        val keyBytes = ByteArray(16) { it.toByte() }
        val ivBytes = ByteArray(16) { (it + 16).toByte() }
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val ivSpec = IvParameterSpec(ivBytes)

        // Encrypt
        val encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

        val encryptedBuffer = Buffer()
        (encryptedBuffer as RawSink).transformedWith(CipherTransformation(encryptCipher)).buffered().use { sink ->
            sink.writeString(originalText)
        }

        // The encrypted data should be different from the original
        val encryptedBytes = encryptedBuffer.copy().readByteArray()
        assertFalse(encryptedBytes.contentEquals(originalText.encodeToByteArray()))

        // Decrypt
        val decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

        val decryptedText = (encryptedBuffer as RawSource).transformedWith(CipherTransformation(decryptCipher)).buffered().readString()

        assertEquals(originalText, decryptedText)
    }

    @Test
    fun aesEncryptLargeData() {
        // Generate large data (larger than internal buffer size)
        val originalData = ByteArray(100_000) { (it % 256).toByte() }

        val keyBytes = ByteArray(16) { it.toByte() }
        val ivBytes = ByteArray(16) { (it + 16).toByte() }
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val ivSpec = IvParameterSpec(ivBytes)

        // Encrypt
        val encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

        val encryptedBuffer = Buffer()
        (encryptedBuffer as RawSink).transformedWith(CipherTransformation(encryptCipher)).buffered().use { sink ->
            sink.write(originalData)
        }

        // Decrypt
        val decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

        val decryptedBuffer = Buffer()
        (encryptedBuffer as RawSource).transformedWith(CipherTransformation(decryptCipher)).buffered().use { source ->
            source.transferTo(decryptedBuffer)
        }

        val decryptedData = decryptedBuffer.readByteArray()
        assertContentEquals(originalData, decryptedData)
    }

    /**
     * Test AES-GCM cipher which buffers all data during decryption.
     *
     * AES-GCM is an authenticated encryption mode where:
     * - Encryption: cipher.update() produces output incrementally
     * - Decryption: cipher.update() returns 0 bytes (buffers internally),
     *   cipher.doFinal() outputs ALL decrypted data at once after verifying the auth tag
     *
     * This tests that finalizeIntoByteArray can handle output larger than a single segment.
     */
    @Test
    fun aesGcmLargeDataDecryption() {
        // Use data larger than typical segment size (8KB) to test finalization handling
        val originalData = ByteArray(20_000) { (it % 256).toByte() }

        // AES-GCM uses 128-bit key and 96-bit (12 byte) IV/nonce
        val keyBytes = ByteArray(16) { it.toByte() }
        val ivBytes = ByteArray(12) { (it + 100).toByte() }
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val gcmSpec = GCMParameterSpec(128, ivBytes) // 128-bit auth tag

        // Encrypt - GCM produces output incrementally during encryption
        val encryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val encryptedBuffer = Buffer()
        (encryptedBuffer as RawSink).transformedWith(CipherTransformation(encryptCipher)).buffered().use { sink ->
            sink.write(originalData)
        }

        // Encrypted data should be original size + 16 bytes (auth tag)
        assertEquals(originalData.size.toLong() + 16, encryptedBuffer.size)

        // Decrypt - GCM buffers ALL data until doFinal() where it verifies tag and outputs everything
        val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedBuffer = Buffer()
        (encryptedBuffer as RawSource).transformedWith(CipherTransformation(decryptCipher)).buffered().use { source ->
            source.transferTo(decryptedBuffer)
        }

        val decryptedData = decryptedBuffer.readByteArray()
        assertContentEquals(originalData, decryptedData)
    }
}
