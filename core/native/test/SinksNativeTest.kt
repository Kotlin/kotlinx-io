/*
 * Copyright 2010-2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.cinterop.*
import kotlin.test.*

class BufferSinksNativeTest : SinksNativeTest(SinkFactory.BUFFER)
class BufferedSinkSinksNativeTest : SinksNativeTest(SinkFactory.REAL_BUFFERED_SINK)

private const val SEGMENT_SIZE = Segment.SIZE

@OptIn(ExperimentalForeignApi::class, DelicateIoApi::class)
abstract class SinksNativeTest internal constructor(factory: SinkFactory) {
    private val buffer = Buffer()
    private val sink = factory.create(buffer)

    @Test
    fun writePointer() {
        val data = "hello world".encodeToByteArray()

        data.usePinned { pinned ->
            sink.write(pinned.addressOf(0), data.size.toLong())
        }
        sink.flush()
        assertEquals("hello world", buffer.readString())

        data.usePinned { pinned ->
            sink.write(pinned.addressOf(0), 5)
        }
        sink.flush()
        assertEquals("hello", buffer.readString())

        data.usePinned { pinned ->
            sink.write(pinned.addressOf(6), 5)
        }
        sink.flush()
        assertEquals("world", buffer.readString())

        data.usePinned { pinned ->
            sink.write(pinned.addressOf(0), 0)
        }
        sink.flush()
        assertTrue(buffer.exhausted())
    }

    @Test
    fun writeOnSegmentsBorder() {
        val data = "hello world".encodeToByteArray()
        val padding = ByteArray(SEGMENT_SIZE - 3) { 0xaa.toByte() }

        sink.write(padding)
        data.usePinned { pinned ->
            sink.write(pinned.addressOf(0), data.size.toLong())
        }
        sink.flush()

        buffer.skip(padding.size.toLong())
        assertEquals("hello world", buffer.readString())
    }

    @Test
    fun writeOverMultipleSegments() {
        val data = ByteArray((2.5 * SEGMENT_SIZE).toInt()) { 0xaa.toByte() }

        data.usePinned { pinned ->
            sink.write(pinned.addressOf(0), data.size.toLong())
        }
        sink.flush()

        assertContentEquals(data, buffer.readByteArray())
    }

    @Test
    fun writeUsingIllegalLength() {
        byteArrayOf(0).usePinned { pinned ->
            val ptr = pinned.addressOf(0)

            assertFailsWith<IllegalArgumentException> {
                sink.write(ptr, byteCount = -1L)
            }
        }
    }
}
