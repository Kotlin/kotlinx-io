/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(DelicateIoApi::class)
class DelicateApiTest {
    @Test
    @OptIn(InternalIoApi::class)
    fun testWriteIntoBuffer() {
        val sink = Buffer()
        val rawSink = sink as RawSink
        val bufferedSink = rawSink.buffered()

        bufferedSink.writeToInternalBuffer {
            it.writeByte(42)
        }

        assertEquals(0, sink.size)
        assertEquals(1, bufferedSink.buffer.size)

        bufferedSink.writeToInternalBuffer {
            // 1 byte missing for segment to be complete
            it.write(ByteArray(Segment.SIZE - 2))
            // skip everything (skip everything but one last byte)
            it.skip(Segment.SIZE - 2L)
            // this will complete the segment
            it.writeByte(0x12)
            // this will start a new one
            it.writeByte(0x34)
        }

        assertArrayEquals(byteArrayOf(0x0, 0x12), sink.readByteArray())
        assertArrayEquals(byteArrayOf(0x34), bufferedSink.buffer.readByteArray())
    }
}