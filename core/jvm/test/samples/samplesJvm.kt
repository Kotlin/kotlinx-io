/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io.samples

import kotlinx.io.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
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

class CipherTransformationSamples {
    /**
     * A [Transformation] that encrypts or decrypts data using a [Cipher].
     *
     * This transformation can be used with any cipher algorithm supported by the JVM,
     * such as AES, DES, or RSA.
     */
    private class CipherTransformation(private val cipher: Cipher) : Transformation {
        private val outputBuffer = ByteArray(cipher.getOutputSize(8192))

        override fun transformAtMostTo(source: Buffer, sink: Buffer, byteCount: Long): Long {
            if (source.exhausted()) return 0L

            var totalConsumed = 0L

            while (!source.exhausted() && totalConsumed < byteCount) {
                val toConsume = minOf(source.size, byteCount - totalConsumed, 8192L).toInt()
                val inputBytes = source.readByteArray(toConsume)

                val outputSize = cipher.update(inputBytes, 0, toConsume, outputBuffer)
                if (outputSize > 0) {
                    sink.write(outputBuffer, 0, outputSize)
                }

                totalConsumed += toConsume
            }

            return totalConsumed
        }

        override fun finish(sink: Buffer) {
            val finalBytes = cipher.doFinal()
            if (finalBytes.isNotEmpty()) {
                sink.write(finalBytes)
            }
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

    @Test
    fun chainedTransformations() {
        val originalText = "Chained transformations: encrypt then CRC32!"

        val keyBytes = ByteArray(16) { it.toByte() }
        val ivBytes = ByteArray(16) { (it + 16).toByte() }
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val ivSpec = IvParameterSpec(ivBytes)

        // CRC32 transformation to verify data integrity
        // Pass-through that computes CRC32 as data flows through
        @OptIn(ExperimentalUnsignedTypes::class)
        class CRC32Transformation : Transformation {
            private val crc32Table = generateCrc32Table()
            private var crc32: UInt = 0xffffffffU

            private fun update(value: Byte) {
                val index = value.toUInt().xor(crc32).toUByte()
                crc32 = crc32Table[index.toInt()].xor(crc32.shr(8))
            }

            fun crc32(): UInt = crc32.xor(0xffffffffU)

            override fun transformAtMostTo(source: Buffer, sink: Buffer, byteCount: Long): Long {
                if (source.exhausted()) return 0L

                var bytesConsumed = 0L
                while (!source.exhausted() && bytesConsumed < byteCount) {
                    val byte = source.readByte()
                    update(byte)
                    sink.writeByte(byte)
                    bytesConsumed++
                }
                return bytesConsumed
            }

            override fun finish(sink: Buffer) {}

            override fun close() {}

            private fun generateCrc32Table(): UIntArray {
                val table = UIntArray(256)
                for (idx in table.indices) {
                    table[idx] = idx.toUInt()
                    for (bit in 8 downTo 1) {
                        table[idx] = if (table[idx] % 2U == 0U) {
                            table[idx].shr(1)
                        } else {
                            table[idx].shr(1).xor(0xEDB88320U)
                        }
                    }
                }
                return table
            }
        }

        // Encrypt with CRC32 calculation
        val encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val crc32Transform = CRC32Transformation()

        val encryptedBuffer = Buffer()
        // Chain: write -> CRC32 -> encrypt -> buffer
        (encryptedBuffer as RawSink)
            .transformedWith(CipherTransformation(encryptCipher))
            .transformedWith(crc32Transform)
            .buffered()
            .use { sink ->
                sink.writeString(originalText)
            }

        val originalCrc32 = crc32Transform.crc32()

        // Decrypt and verify CRC32
        val decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val verifyCrc32Transform = CRC32Transformation()

        val decryptedText = (encryptedBuffer as RawSource)
            .transformedWith(CipherTransformation(decryptCipher))
            .transformedWith(verifyCrc32Transform)
            .buffered()
            .readString()

        assertEquals(originalText, decryptedText)
        assertEquals(originalCrc32, verifyCrc32Transform.crc32())
    }
}
