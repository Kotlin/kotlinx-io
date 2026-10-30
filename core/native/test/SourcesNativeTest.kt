/*
 * Copyright 2010-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlin.test.*


class BufferSourcesNativeTest : SourcesNativeTest(SourceFactory.BUFFER)
class BufferedSourceSourcesNativeTest : SourcesNativeTest(SourceFactory.REAL_BUFFERED_SOURCE)
class OneByteAtATimeBufferedSourceSourcesNativeTest : SourcesNativeTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFERED_SOURCE)
class OneByteAtATimeBufferSourcesNativeTest : SourcesNativeTest(SourceFactory.ONE_BYTE_AT_A_TIME_BUFFER)
class PeekBufferSourcesNativeTest : SourcesNativeTest(SourceFactory.PEEK_BUFFER)
class PeekBufferedSourceSourcesNativeTest : SourcesNativeTest(SourceFactory.PEEK_BUFFERED_SOURCE)

private const val SEGMENT_SIZE = Segment.SIZE

@OptIn(ExperimentalForeignApi::class, DelicateIoApi::class)
abstract class SourcesNativeTest internal constructor(private val factory: SourceFactory) {
    private val pipe = factory.pipe()
    private val source = pipe.source
    private val sink = pipe.sink

    @Test
    fun readAtMost() {
        if (factory.isOneByteAtATime) return

        val hello = "hello world"
        val dst = ByteArray(128)

        sink.apply {
            writeString(hello)
            emit()
        }

        dst.usePinned { pinned ->
            val bytesRead = source.readAtMostTo(pinned.addressOf(0), dst.size.toLong())
            assertEquals(hello.length.toLong(), bytesRead)

            assertEquals("hello world", dst.copyOfRange(0, hello.length).decodeToString())
            assertTrue(dst.copyOfRange(hello.length, dst.size).all { it == 0.toByte() })
        }
        dst.fill(0)

        sink.apply {
            writeString(hello)
            emit()
        }
        val helloLength = "hello".length
        dst.usePinned { pinned ->
            val bytesRead = source.readAtMostTo(pinned.addressOf(0), helloLength.toLong())
            assertEquals(helloLength.toLong(), bytesRead)

            assertEquals("hello", dst.copyOfRange(0, helloLength).decodeToString())
            assertTrue(dst.copyOfRange(helloLength, dst.size).all { it == 0.toByte() })
        }
        assertEquals(" world", source.readString())
        dst.fill(0)

        sink.apply {
            writeString(hello)
            emit()
        }
        dst.usePinned { pinned ->
            assertEquals(0, source.readAtMostTo(pinned.addressOf(0), 0))
        }
        assertEquals(hello, source.readString())
    }

    @Test
    fun readAtMostOneAtATime() {
        if (!factory.isOneByteAtATime) return

        val hello = "hello world"
        val dst = ByteArray(128)

        sink.apply {
            writeString(hello)
            emit()
        }

        dst.usePinned { pinned ->
            val bytesRead = source.readAtMostTo(pinned.addressOf(0), dst.size.toLong())
            assertEquals(1L, bytesRead)

            assertEquals('h'.code.toByte(), dst[0])
            assertTrue(dst.copyOfRange(1, dst.size).all { it == 0.toByte() })
        }
        assertEquals("ello world", source.readString())
        dst.fill(0)

        sink.apply {
            writeString(hello)
            emit()
        }
        dst.usePinned { pinned ->
            assertEquals(0, source.readAtMostTo(pinned.addressOf(0), 0))
        }
        assertEquals(hello, source.readString())
    }

    @Test
    fun readAtMostFromMultipleSegments() {
        if (factory.isOneByteAtATime) return

        sink.write(ByteArray(SEGMENT_SIZE * 3))
        sink.emit()

        val dst = ByteArray(SEGMENT_SIZE * 3)
        dst.usePinned { pinned ->
            assertEquals(SEGMENT_SIZE.toLong(), source.readAtMostTo(pinned.addressOf(0), dst.size.toLong()))
        }
        source.transferTo(discardingSink())
    }

    @Test
    fun readAtMostFromExhaustedSource() {
        val dst = ByteArray(128)
        dst.usePinned { pinned ->
            assertEquals(-1L, source.readAtMostTo(pinned.addressOf(0), dst.size.toLong()))
        }
    }

    @Test
    fun readAtMostWithIllegalLength() {
        val buffer = byteArrayOf(0)

        buffer.usePinned { pinned ->
            val ptr = pinned.addressOf(0)

            assertFailsWith<IllegalArgumentException> {
                source.readAtMostTo(ptr, byteCount = -1L)
            }
        }
    }

    @Test
    fun readTo() {
        val hello = "hello world"
        val dst = ByteArray(128)

        sink.writeString(hello)
        sink.emit()

        dst.usePinned { pinned ->
            source.readTo(pinned.addressOf(0), hello.length.toLong())
            assertEquals(hello, dst.copyOfRange(0, hello.length).decodeToString())
        }
        dst.fill(0)

        sink.writeString(hello)
        sink.emit()

        dst.usePinned { pinned ->
            source.readTo(pinned.addressOf(0), 5)
            assertEquals("hello", dst.copyOfRange(0, 5).decodeToString())
        }
        assertEquals(" world", source.readString())

        sink.writeString(hello)
        assertFailsWith<EOFException> {
            dst.usePinned { pinned ->
                source.readTo(pinned.addressOf(0), dst.size.toLong())
            }
        }
    }

    @Test
    fun readToMultipleSegments() {
        val data = ByteArray((2.5 * SEGMENT_SIZE).toInt()) { it.toByte() }
        val dst = ByteArray(data.size)

        sink.write(data)
        sink.emit()

        dst.usePinned { pinned ->
            source.readTo(pinned.addressOf(0), data.size.toLong())
        }

        assertContentEquals(data, dst)
    }

    @Test
    fun readToFromExhaustedSource() {
        val dst = ByteArray(128)
        assertFailsWith<EOFException> {
            dst.usePinned { pinned ->
                source.readTo(pinned.addressOf(0), 1L)
            }
        }
    }

    @Test
    fun readToWithIllegalLength() {
        val buffer = byteArrayOf(0)
        assertFailsWith<IllegalArgumentException> {
            buffer.usePinned { pinned ->
                source.readTo(pinned.addressOf(0), byteCount = -1L)
            }
        }
    }
}
