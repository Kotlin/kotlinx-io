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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException
import java.net.Socket
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.StandardOpenOption
import kotlin.test.*

class JvmPlatformTest {
    @TempDir
    lateinit var tempDir: File

    @Test fun outputStreamSink() {
        val baos = ByteArrayOutputStream()
        val sink = baos.sink()
        sink.write(Buffer().also { it.writeUtf8("a") }, 1L)
        assertArrayEquals(baos.toByteArray(), byteArrayOf(0x61))
    }

    @Test fun outputStreamSinkWriteZeroBytes() {
        val baos = ByteArrayOutputStream()
        val sink = baos.sink()
        sink.write(Buffer().also { it.writeUtf8("a") }, 0L)
        assertEquals(0, baos.size())
    }

    @Test fun outputStreamSinkWriteNegativeNumberOfBytes() {
        val baos = ByteArrayOutputStream()
        val sink = baos.sink()
        assertFailsWith<IllegalArgumentException> {
            sink.write(Buffer().also { it.writeUtf8("a") }, -1)
        }
    }

    @Test fun outputStreamSinkWritePartOfTheBuffer() {
        val baos = ByteArrayOutputStream()
        val sink = baos.sink()
        val buffer = Buffer().also { it.writeUtf8("hello") }
        sink.write(buffer, 2)
        assertArrayEquals(baos.toByteArray(), byteArrayOf('h'.code.toByte(), 'e'.code.toByte()))
        assertEquals("llo", buffer.readUtf8())
    }

    @Test fun inputStreamSource() {
        val bais = ByteArrayInputStream(byteArrayOf(0x61))
        val source = bais.source()
        val buffer = Buffer()
        source.readAtMostTo(buffer, 1)
        assertEquals(buffer.readUtf8(), "a")
    }

    @Test fun inputStreamSourceReadZeroBytes() {
        val bais = ByteArrayInputStream(ByteArray(128))
        val source = bais.source()
        val buffer = Buffer()
        source.readAtMostTo(buffer, 0)
        assertEquals(0, buffer.size)
    }

    @Test fun inputStreamSourceReadNegativeNumberOfBytes() {
        val bais = ByteArrayInputStream(ByteArray(128))
        val source = bais.source()
        assertFailsWith<IllegalArgumentException> { source.readAtMostTo(Buffer(), -1) }
    }

    @Test fun fileSink() {
        val file = File(tempDir, "test")
        file.sink().use { sink ->
            sink.write(Buffer().also { it.writeUtf8("a") }, 1L)
        }
        assertEquals(file.readText(), "a")
    }

    @Test fun fileAppendingSink() {
        val file = File(tempDir, "test")
        file.writeText("a")
        file.sink(append = true).use { sink ->
            sink.write(Buffer().also { it.writeUtf8("b") }, 1L)
        }
        assertEquals(file.readText(), "ab")
    }

    @Test fun fileSource() {
        val file = File(tempDir, "test")
        file.writeText("a")
        val buffer = Buffer()
        file.source().use { source ->
            source.readAtMostTo(buffer, 1L)
        }
        assertEquals(buffer.readUtf8(), "a")
    }

    @Test fun pathSink() {
        val file = File(tempDir, "test")
        file.toPath().sink().use { sink ->
            sink.write(Buffer().also { it.writeUtf8("a") }, 1L)
        }
        assertEquals(file.readText(), "a")
    }

    @Test fun pathSinkWithOptions() {
        val file = File(tempDir, "test")
        file.writeText("a")
        file.toPath().sink(StandardOpenOption.APPEND).use { sink ->
            sink.write(Buffer().also { it.writeUtf8("b") }, 1L)
        }
        assertEquals(file.readText(), "ab")
    }

    @Test fun pathSource() {
        val file = File(tempDir, "test")
        file.writeText("a")
        val buffer = Buffer()
        file.toPath().source().use { source ->
            source.readAtMostTo(buffer, 1L)
        }
        assertEquals(buffer.readUtf8(), "a")
    }

    @Test
    fun pathSourceWithOptions() {
        val file = File(tempDir, "new.txt")
        assertTrue(file.createNewFile())
        val link = File(tempDir, "link.txt")
        try {
            Files.createSymbolicLink(link.toPath(), file.toPath())
        } catch (e: UnsupportedOperationException) {
            // the FS does not support symlinks
            return
        }

        assertFailsWith<IOException> {
            link.toPath().source(LinkOption.NOFOLLOW_LINKS).use { it.buffer().readUtf8Line() }
        }
        assertNull(link.toPath().source().use { it.buffer().readUtf8Line() })
    }

    @Test fun socketSink() {
        val baos = ByteArrayOutputStream()
        val socket = object : Socket() {
            override fun getOutputStream() = baos
        }
        val sink = socket.sink()
        sink.write(Buffer().also { it.writeUtf8("a") }, 1L)
        assertArrayEquals(baos.toByteArray(), byteArrayOf(0x61))
    }

    @Test fun socketSource() {
        val bais = ByteArrayInputStream(byteArrayOf(0x61))
        val socket = object : Socket() {
            override fun getInputStream() = bais
        }
        val source = socket.source()
        val buffer = Buffer()
        source.readAtMostTo(buffer, 1L)
        assertEquals(buffer.readUtf8(), "a")
    }
}
