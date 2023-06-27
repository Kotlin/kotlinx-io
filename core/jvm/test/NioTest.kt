/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.io

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.createFile
import kotlin.io.path.inputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.text.Charsets.UTF_8

/** Test interop with java.nio.  */
class NioTest {
    @TempDir
    lateinit var temporaryFolder: Path

    @Test
    fun sourceIsOpen() {
        val source = RealSource(Buffer()).asByteChannel()
        assertTrue(source.isOpen())
        source.close()
        assertFalse(source.isOpen())
    }

    @Test
    fun sinkIsOpen() {
        val sink = RealSink(Buffer()).asByteChannel()
        assertTrue(sink.isOpen())
        sink.close()
        assertFalse(sink.isOpen())
    }

    @Test
    fun writableChannelNioFile() {
        val file = Paths.get(temporaryFolder.toString(), "test").createFile()
        val fileChannel: FileChannel = FileChannel.open(file, StandardOpenOption.WRITE)
        testWritableByteChannel(false, fileChannel)
        val emitted: Source = file.inputStream().asSource().buffered()
        assertEquals("defghijklmnopqrstuvw", emitted.readUtf8())
        emitted.close()
    }

    @Test
    fun writableChannelBuffer() {
        val buffer = Buffer()
        testWritableByteChannel(true, buffer.asByteChannel())
        assertEquals("defghijklmnopqrstuvw", buffer.readUtf8())
    }

    @Test
    fun writableChannelBufferedSink() {
        val buffer = Buffer()
        val bufferedSink: Sink = buffer
        testWritableByteChannel(true, bufferedSink.asByteChannel())
        assertEquals("defghijklmnopqrstuvw", buffer.readUtf8())
    }

    @Test
    fun readableChannelNioFile() {
        val file: File = Paths.get(temporaryFolder.toString(), "test").toFile()
        val initialData: Sink = file.outputStream().asSink().buffered()
        initialData.writeUtf8("abcdefghijklmnopqrstuvwxyz")
        initialData.close()
        val fileChannel: FileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)
        testReadableByteChannel(false, fileChannel)
    }

    @Test
    fun readableChannelBuffer() {
        val buffer = Buffer()
        buffer.writeUtf8("abcdefghijklmnopqrstuvwxyz")
        testReadableByteChannel(true, buffer.asByteChannel())
    }

    @Test
    fun readableChannelBufferedSource() {
        val buffer = Buffer()
        val bufferedSource: Source = buffer
        buffer.writeUtf8("abcdefghijklmnopqrstuvwxyz")
        testReadableByteChannel(true, bufferedSource.asByteChannel())
    }

    /**
     * Does some basic writes to `channel`. We execute this against both Okio's channels and
     * also a standard implementation from the JDK to confirm that their behavior is consistent.
     */
    private fun testWritableByteChannel(isBuffer: Boolean, channel: WritableByteChannel) {
        assertTrue(channel.isOpen())
        val byteBuffer = ByteBuffer.allocate(1024)
        byteBuffer.put("abcdefghijklmnopqrstuvwxyz".toByteArray(UTF_8))
        byteBuffer.flip() // Cast necessary for Java 8.
        byteBuffer.position(3) // Cast necessary for Java 8.
        byteBuffer.limit(23) // Cast necessary for Java 8.
        val byteCount: Int = channel.write(byteBuffer)
        assertEquals(20, byteCount)
        assertEquals(23, byteBuffer.position())
        assertEquals(23, byteBuffer.limit())
        channel.close()
        assertEquals(isBuffer, channel.isOpen) // Buffer.close() does nothing.
    }

    /**
     * Does some basic reads from `channel`. We execute this against both Okio's channels and
     * also a standard implementation from the JDK to confirm that their behavior is consistent.
     */
    private fun testReadableByteChannel(isBuffer: Boolean, channel: ReadableByteChannel) {
        assertTrue(channel.isOpen)
        val byteBuffer = ByteBuffer.allocate(1024)
        byteBuffer.position(3) // Cast necessary for Java 8.
        byteBuffer.limit(23) // Cast necessary for Java 8.
        val byteCount: Int = channel.read(byteBuffer)
        assertEquals(20, byteCount)
        assertEquals(23, byteBuffer.position())
        assertEquals(23, byteBuffer.limit())
        channel.close()
        assertEquals(isBuffer, channel.isOpen()) // Buffer.close() does nothing.
        byteBuffer.flip() // Cast necessary for Java 8.
        byteBuffer.position(3) // Cast necessary for Java 8.
        val data = ByteArray(byteBuffer.remaining())
        byteBuffer[data]
        assertEquals("abcdefghijklmnopqrst", String(data, UTF_8))
    }
}
