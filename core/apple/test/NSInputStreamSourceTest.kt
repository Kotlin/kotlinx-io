/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlinx.io.files.Path
import kotlinx.io.files.sink
import platform.Foundation.NSInputStream
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NSInputStreamSourceTest {
    @Test
    fun nsInputStreamSource() {
        val input = NSInputStream(data = byteArrayOf(0x61).toNSData())
        val source = input.asSource()
        val buffer = Buffer()
        assertEquals(1, source.readAtMostTo(buffer, 1L))
        assertEquals("a", buffer.readString())
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun nsInputStreamSourceFromFile() {
        // can be replaced with createTempFile() when #183 is fixed
        // https://github.com/Kotlin/kotlinx-io/issues/183
        val file = "${NSTemporaryDirectory()}${NSUUID().UUIDString()}"
        try {
            Path(file).sink().use {
                it.writeString("example")
            }

            val input = NSInputStream(uRL = NSURL.fileURLWithPath(file))
            val source = input.asSource()
            val buffer = Buffer()
            assertEquals(7, source.readAtMostTo(buffer, 10))
            assertEquals("example", buffer.readString())
        } finally {
            deleteFile(file)
        }
    }

    @Test
    fun sourceFromInputStream() {
        val input = NSInputStream(data = ("a" + "b".repeat(Segment.SIZE * 2) + "c").encodeToByteArray().toNSData())

        // Source: ab...bc
        val source: RawSource = input.asSource()
        val sink = Buffer()

        // Source: b...bc. Sink: abb.
        assertEquals(3, source.readAtMostTo(sink, 3))
        assertEquals("abb", sink.readString(3))

        // Source: b...bc. Sink: b...b.
        assertEquals(Segment.SIZE.toLong(), source.readAtMostTo(sink, 20000))
        assertEquals("b".repeat(Segment.SIZE), sink.readString())

        // Source: b...bc. Sink: b...bc.
        assertEquals((Segment.SIZE - 1).toLong(), source.readAtMostTo(sink, 20000))
        assertEquals("b".repeat(Segment.SIZE - 2) + "c", sink.readString())

        // Source and sink are empty.
        assertEquals(-1, source.readAtMostTo(sink, 1))
    }

    @Test
    fun sourceFromInputStreamWithSegmentSize() {
        val input = NSInputStream(data = ByteArray(Segment.SIZE).toNSData())
        val source = input.asSource()
        val sink = Buffer()

        assertEquals(Segment.SIZE.toLong(), source.readAtMostTo(sink, Segment.SIZE.toLong()))
        assertEquals(-1, source.readAtMostTo(sink, Segment.SIZE.toLong()))

        assertNoEmptySegments(sink)
    }

    @Test
    fun sourceFromInputStreamBounds() {
        val source = NSInputStream(data = ByteArray(100).toNSData()).asSource()
        assertFailsWith<IllegalArgumentException> { source.readAtMostTo(Buffer(), -1) }
    }
}
