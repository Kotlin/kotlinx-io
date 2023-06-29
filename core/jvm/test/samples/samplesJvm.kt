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
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

        buffer.writeUtf8("hello")
        val bytesRead = buffer.readAtMostTo(nioByteBuffer)
        assertEquals(5, bytesRead)
        assertEquals(5, nioByteBuffer.capacity() - nioByteBuffer.remaining())

        nioByteBuffer.position(0)
        nioByteBuffer.limit(5)

        val bytesWrite = buffer.write(nioByteBuffer)
        assertEquals(5, bytesWrite)
        assertEquals("hello", buffer.readUtf8())
    }

    @Test
    fun bufferTransferToStream() {
        val buffer = Buffer()
        buffer.writeUtf8("hello")

        val outputStream = ByteArrayOutputStream()
        buffer.readTo(outputStream)

        assertTrue(buffer.exhausted())

        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        buffer.transferFrom(inputStream)

        assertEquals("hello", buffer.readUtf8())
    }

    @Test
    fun writeInputStreamToBuffer() {
        val inputStream = ByteArrayInputStream("hello!".encodeToByteArray())
        val buffer = Buffer()

        buffer.write(inputStream, 5)
        assertEquals("hello", buffer.readUtf8())
    }

    @Test
    fun copyBufferToOutputStream() {
        val buffer = Buffer()
        buffer.writeUtf8("string")

        val outputStream = ByteArrayOutputStream()
        buffer.copyTo(outputStream, startIndex = 2, endIndex = 6)

        assertEquals("string", buffer.readUtf8())
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

        assertEquals("hello", buffer.readUtf8())
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
