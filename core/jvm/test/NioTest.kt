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
import kotlin.test.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.createFile
import kotlin.text.Charsets.UTF_8

/** Test interop with java.nio.  */
class NioTest {
    @TempDir
    lateinit var temporaryFolder: Path
    @Test
    @Throws(Exception::class)
    fun sourceIsOpen() {
        val source: Source = RealSource(Buffer())
        assertTrue(source.isOpen())
        source.close()
        assertFalse(source.isOpen())
    }

    @Test
    @Throws(Exception::class)
    fun sinkIsOpen() {
        val sink: Sink = RealSink(Buffer())
        assertTrue(sink.isOpen())
        sink.close()
        assertFalse(sink.isOpen())
    }

    @Test
    @Throws(Exception::class)
    fun writableChannelNioFile() {
        val file = Paths.get(temporaryFolder.toString(), "test").createFile()
        val fileChannel: FileChannel = FileChannel.open(file, StandardOpenOption.WRITE)
        testWritableByteChannel(fileChannel)
        val emitted: Source = file.source().buffer()
        assertEquals("defghijklmnopqrstuvw", emitted.readUtf8())
        emitted.close()
    }

    @Test
    @Throws(Exception::class)
    fun writableChannelBuffer() {
        val buffer = Buffer()
        testWritableByteChannel(buffer)
        assertEquals("defghijklmnopqrstuvw", buffer.readUtf8())
    }

    @Test
    @Throws(Exception::class)
    fun writableChannelBufferedSink() {
        val buffer = Buffer()
        val bufferedSink: Sink = buffer
        testWritableByteChannel(bufferedSink)
        assertEquals("defghijklmnopqrstuvw", buffer.readUtf8())
    }

    @Test
    @Throws(Exception::class)
    fun readableChannelNioFile() {
        val file: File = Paths.get(temporaryFolder.toString(), "test").toFile()
        val initialData: Sink = file.sink().buffer()
        initialData.writeUtf8("abcdefghijklmnopqrstuvwxyz")
        initialData.close()
        val fileChannel: FileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ)
        testReadableByteChannel(fileChannel)
    }

    @Test
    @Throws(Exception::class)
    fun readableChannelBuffer() {
        val buffer = Buffer()
        buffer.writeUtf8("abcdefghijklmnopqrstuvwxyz")
        testReadableByteChannel(buffer)
    }

    @Test
    @Throws(Exception::class)
    fun readableChannelBufferedSource() {
        val buffer = Buffer()
        val bufferedSource: Source = buffer
        buffer.writeUtf8("abcdefghijklmnopqrstuvwxyz")
        testReadableByteChannel(bufferedSource)
    }

    /**
     * Does some basic writes to `channel`. We execute this against both Okio's channels and
     * also a standard implementation from the JDK to confirm that their behavior is consistent.
     */
    @Throws(Exception::class)
    private fun testWritableByteChannel(channel: WritableByteChannel) {
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
        assertEquals(channel is Buffer, channel.isOpen) // Buffer.close() does nothing.
    }

    /**
     * Does some basic reads from `channel`. We execute this against both Okio's channels and
     * also a standard implementation from the JDK to confirm that their behavior is consistent.
     */
    @Throws(Exception::class)
    private fun testReadableByteChannel(channel: ReadableByteChannel) {
        assertTrue(channel.isOpen)
        val byteBuffer = ByteBuffer.allocate(1024)
        byteBuffer.position(3) // Cast necessary for Java 8.
        byteBuffer.limit(23) // Cast necessary for Java 8.
        val byteCount: Int = channel.read(byteBuffer)
        assertEquals(20, byteCount)
        assertEquals(23, byteBuffer.position())
        assertEquals(23, byteBuffer.limit())
        channel.close()
        assertEquals(channel is Buffer, channel.isOpen()) // Buffer.close() does nothing.
        byteBuffer.flip() // Cast necessary for Java 8.
        byteBuffer.position(3) // Cast necessary for Java 8.
        val data = ByteArray(byteBuffer.remaining())
        byteBuffer[data]
        assertEquals("abcdefghijklmnopqrst", String(data, UTF_8))
    }
}
