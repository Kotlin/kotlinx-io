/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2014 Square, Inc.
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

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val SEGMENT_SIZE = Segment.SIZE

/**
 * Tests solely for the behavior of Buffer's implementation. For generic BufferedSink or
 * BufferedSource behavior, use BufferedSinkTest or BufferedSourceTest, respectively.
 */
class CommonBufferTest {
    @Test
    fun readAndWriteUtf8() {
        val buffer = Buffer()
        buffer.writeString("ab")
        assertEquals(2, buffer.size)
        buffer.writeString("cdef")
        assertEquals(6, buffer.size)
        assertEquals("abcd", buffer.readString(4))
        assertEquals(2, buffer.size)
        assertEquals("ef", buffer.readString(2))
        assertEquals(0, buffer.size)
        assertFailsWith<EOFException> {
            buffer.readString(1)
        }
    }

    @Test
    fun bufferToString() {
        assertEquals("Buffer(size=0)", Buffer().toString())

        assertEquals(
            "Buffer(size=10 hex=610d0a620a630d645c65)",
            Buffer().also { it.writeString("a\r\nb\nc\rd\\e") }.toString()
        )

        assertEquals(
            "Buffer(size=11 hex=547972616e6e6f73617572)",
            Buffer().also { it.writeString("Tyrannosaur") }.toString()
        )

        assertEquals(
            "Buffer(size=16 hex=74c999cb8872616ec999cb8c73c3b472)",
            Buffer().also { it.write("74c999cb8872616ec999cb8c73c3b472".decodeHex()) }.toString()
        )

        assertEquals(
            "Buffer(size=64 hex=00000000000000000000000000000000000000000000000000000000000000000000000" +
                    "000000000000000000000000000000000000000000000000000000000)",
            Buffer().also { it.write(ByteArray(64)) }.toString()
        )

        assertEquals(
            "Buffer(size=66 hex=000000000000000000000000000000000000000000000000000000000000" +
                    "00000000000000000000000000000000000000000000000000000000000000000000…)",
            Buffer().also { it.write(ByteArray(66)) }.toString()
        )
    }

    @Test
    fun multipleSegmentBuffers() {
        val buffer = Buffer()
        buffer.writeString('a'.repeat(1000))
        buffer.writeString('b'.repeat(2500))
        buffer.writeString('c'.repeat(5000))
        buffer.writeString('d'.repeat(10000))
        buffer.writeString('e'.repeat(25000))
        buffer.writeString('f'.repeat(50000))

        assertEquals('a'.repeat(999), buffer.readString(999)) // a...a
        assertEquals("a" + 'b'.repeat(2500) + "c", buffer.readString(2502)) // ab...bc
        assertEquals('c'.repeat(4998), buffer.readString(4998)) // c...c
        assertEquals("c" + 'd'.repeat(10000) + "e", buffer.readString(10002)) // cd...de
        assertEquals('e'.repeat(24998), buffer.readString(24998)) // e...e
        assertEquals("e" + 'f'.repeat(50000), buffer.readString(50001)) // ef...f
        assertEquals(0, buffer.size)
    }

    @Test
    fun fillAndDrainPool() {
        val buffer = Buffer()

        // Take 2 * MAX_SIZE segments. This will drain the pool, even if other tests filled it.
        buffer.write(ByteArray(SegmentPool.MAX_SIZE))
        buffer.write(ByteArray(SegmentPool.MAX_SIZE))
        assertEquals(0, SegmentPool.byteCount)

        // Recycle MAX_SIZE segments. They're all in the pool.
        buffer.skip(SegmentPool.MAX_SIZE.toLong())
        assertEquals(SegmentPool.MAX_SIZE, SegmentPool.byteCount)

        // Recycle MAX_SIZE more segments. The pool is full so they get garbage collected.
        buffer.skip(SegmentPool.MAX_SIZE.toLong())
        assertEquals(SegmentPool.MAX_SIZE, SegmentPool.byteCount)

        // Take MAX_SIZE segments to drain the pool.
        buffer.write(ByteArray(SegmentPool.MAX_SIZE))
        assertEquals(0, SegmentPool.byteCount)

        // Take MAX_SIZE more segments. The pool is drained so these will need to be allocated.
        buffer.write(ByteArray(SegmentPool.MAX_SIZE))
        assertEquals(0, SegmentPool.byteCount)
    }

    @Test
    fun moveBytesBetweenBuffersShareSegment() {
        val size = Segment.SIZE / 2 - 1
        val segmentSizes = moveBytesBetweenBuffers('a'.repeat(size), 'b'.repeat(size))
        assertEquals(listOf(size * 2), segmentSizes)
    }

    @Test
    fun moveBytesBetweenBuffersReassignSegment() {
        val size = Segment.SIZE / 2 + 1
        val segmentSizes = moveBytesBetweenBuffers('a'.repeat(size), 'b'.repeat(size))
        assertEquals(listOf(size, size), segmentSizes)
    }

    @Test
    fun moveBytesBetweenBuffersMultipleSegments() {
        val size = 3 * Segment.SIZE + 1
        val segmentSizes = moveBytesBetweenBuffers('a'.repeat(size), 'b'.repeat(size))
        assertEquals(
            listOf(
                Segment.SIZE, Segment.SIZE, Segment.SIZE, 1,
                Segment.SIZE, Segment.SIZE, Segment.SIZE, 1
            ),
            segmentSizes
        )
    }

    private fun moveBytesBetweenBuffers(vararg contents: String): List<Int> {
        val expected = StringBuilder()
        val buffer = Buffer()
        for (s in contents) {
            val source = Buffer()
            source.writeString(s)
            buffer.transferFrom(source)
            expected.append(s)
        }
        val segmentSizes = segmentSizes(buffer)
        assertEquals(expected.toString(), buffer.readString(expected.length.toLong()))
        return segmentSizes
    }

    /** The big part of source's first segment is being moved.  */
    @Test
    fun writeSplitSourceBufferLeft() {
        val writeSize = Segment.SIZE / 2 + 1

        val sink = Buffer()
        sink.writeString('b'.repeat(Segment.SIZE - 10))

        val source = Buffer()
        source.writeString('a'.repeat(Segment.SIZE * 2))
        sink.write(source, writeSize.toLong())

        assertEquals(listOf(Segment.SIZE - 10, writeSize), segmentSizes(sink))
        assertEquals(listOf(Segment.SIZE - writeSize, Segment.SIZE), segmentSizes(source))
    }

    /** The big part of source's first segment is staying put.  */
    @Test
    fun writeSplitSourceBufferRight() {
        val writeSize = Segment.SIZE / 2 - 1

        val sink = Buffer()
        sink.writeString('b'.repeat(Segment.SIZE - 10))

        val source = Buffer()
        source.writeString('a'.repeat(Segment.SIZE * 2))
        sink.write(source, writeSize.toLong())

        assertEquals(listOf(Segment.SIZE - 10, writeSize), segmentSizes(sink))
        assertEquals(listOf(Segment.SIZE - writeSize, Segment.SIZE), segmentSizes(source))
    }

    @Test
    fun writePrefixDoesntSplit() {
        val sink = Buffer()
        sink.writeString('b'.repeat(10))

        val source = Buffer()
        source.writeString('a'.repeat(Segment.SIZE * 2))
        sink.write(source, 20)

        assertEquals(listOf(30), segmentSizes(sink))
        assertEquals(listOf(Segment.SIZE - 20, Segment.SIZE), segmentSizes(source))
        assertEquals(30, sink.size)
        assertEquals((Segment.SIZE * 2 - 20).toLong(), source.size)
    }

    @Test
    fun writePrefixDoesntSplitButRequiresCompact() {
        val sink = Buffer()
        sink.writeString('b'.repeat(Segment.SIZE - 10)) // limit = size - 10
        sink.readString((Segment.SIZE - 20).toLong()) // pos = size = 20

        val source = Buffer()
        source.writeString('a'.repeat(Segment.SIZE * 2))
        sink.write(source, 20)

        assertEquals(listOf(30), segmentSizes(sink))
        assertEquals(listOf(Segment.SIZE - 20, Segment.SIZE), segmentSizes(source))
        assertEquals(30, sink.size)
        assertEquals((Segment.SIZE * 2 - 20).toLong(), source.size)
    }

    @Test
    fun writeSourceWithNegativeNumberOfBytes() {
        val sink = Buffer()
        val source: Source = Buffer()

        assertFailsWith<IllegalArgumentException> { sink.write(source, -1L) }
    }

    @Test
    fun moveAllRequestedBytesWithRead() {
        val sink = Buffer()
        sink.writeString('a'.repeat(10))

        val source = Buffer()
        source.writeString('b'.repeat(15))

        assertEquals(10, source.readAtMostTo(sink, 10))
        assertEquals(20, sink.size)
        assertEquals(5, source.size)
        assertEquals('a'.repeat(10) + 'b'.repeat(10), sink.readString(20))
    }

    @Test
    fun moveFewerThanRequestedBytesWithRead() {
        val sink = Buffer()
        sink.writeString('a'.repeat(10))

        val source = Buffer()
        source.writeString('b'.repeat(20))

        assertEquals(20, source.readAtMostTo(sink, 25))
        assertEquals(30, sink.size)
        assertEquals(0, source.size)
        assertEquals('a'.repeat(10) + 'b'.repeat(20), sink.readString(30))
    }

    @Test
    fun indexOfWithOffset() {
        val buffer = Buffer()
        val halfSegment = Segment.SIZE / 2
        buffer.writeString('a'.repeat(halfSegment))
        buffer.writeString('b'.repeat(halfSegment))
        buffer.writeString('c'.repeat(halfSegment))
        buffer.writeString('d'.repeat(halfSegment))
        assertEquals(0, buffer.indexOf('a'.code.toByte(), 0))
        assertEquals((halfSegment - 1).toLong(), buffer.indexOf('a'.code.toByte(), (halfSegment - 1).toLong()))
        assertEquals(halfSegment.toLong(), buffer.indexOf('b'.code.toByte(), (halfSegment - 1).toLong()))
        assertEquals((halfSegment * 2).toLong(), buffer.indexOf('c'.code.toByte(), (halfSegment - 1).toLong()))
        assertEquals((halfSegment * 3).toLong(), buffer.indexOf('d'.code.toByte(), (halfSegment - 1).toLong()))
        assertEquals((halfSegment * 3).toLong(), buffer.indexOf('d'.code.toByte(), (halfSegment * 2).toLong()))
        assertEquals((halfSegment * 3).toLong(), buffer.indexOf('d'.code.toByte(), (halfSegment * 3).toLong()))
        assertEquals((halfSegment * 4 - 1).toLong(), buffer.indexOf('d'.code.toByte(), (halfSegment * 4 - 1).toLong()))
    }

    @Test
    fun byteAt() {
        val buffer = Buffer()
        buffer.writeString("a")
        buffer.writeString('b'.repeat(Segment.SIZE))
        buffer.writeString("c")
        assertEquals('a'.code.toLong(), buffer[0].toLong())
        assertEquals('a'.code.toLong(), buffer[0].toLong()) // getByte doesn't mutate!
        assertEquals('c'.code.toLong(), buffer[buffer.size - 1].toLong())
        assertEquals('b'.code.toLong(), buffer[buffer.size - 2].toLong())
        assertEquals('b'.code.toLong(), buffer[buffer.size - 3].toLong())
    }

    @Test
    fun getByteOfEmptyBuffer() {
        val buffer = Buffer()
        assertFailsWith<IndexOutOfBoundsException> {
            buffer[0]
        }
    }

    @Test
    fun getByteByInvalidIndex() {
        val buffer = Buffer().also { it.write(ByteArray(10)) }

        assertFailsWith<IndexOutOfBoundsException> { buffer[-1] }
        assertFailsWith<IndexOutOfBoundsException> { buffer[buffer.size] }
    }

    @Test
    fun writePrefixToEmptyBuffer() {
        val sink = Buffer()
        val source = Buffer()
        source.writeString("abcd")
        sink.write(source, 2)
        assertEquals("ab", sink.readString(2))
    }

    // Buffer don't override equals and hashCode
    @Test
    fun equalsAndHashCode() {
        val a = Buffer().also { it.writeString("dog") }
        assertEquals(a, a)

        val b = Buffer().also { it.writeString("hotdog") }
        assertTrue(a != b)

        b.readString(3) // Leaves b containing 'dog'.
        assertTrue(a != b)
    }

    /**
     * When writing data that's already buffered, there's no reason to page the
     * data by segment.
     */
    @Test
    fun readAllWritesAllSegmentsAtOnce() {
        val write1 = Buffer()
        write1.writeString(
            'a'.repeat(Segment.SIZE) +
                    'b'.repeat(Segment.SIZE) +
                    'c'.repeat(Segment.SIZE)
        )

        val source = Buffer()
        source.writeString(
            'a'.repeat(Segment.SIZE) +
                    'b'.repeat(Segment.SIZE) +
                    'c'.repeat(Segment.SIZE)
        )

        val mockSink = MockSink()

        assertEquals((Segment.SIZE * 3).toLong(), source.transferTo(mockSink))
        assertEquals(0, source.size)
        mockSink.assertLog("write($write1, ${write1.size})")
    }

    @Test
    fun writeAllMultipleSegments() {
        val source = Buffer().also { it.writeString('a'.repeat(Segment.SIZE * 3)) }
        val sink = Buffer()

        assertEquals((Segment.SIZE * 3).toLong(), sink.transferFrom(source))
        assertEquals(0, source.size)
        assertEquals('a'.repeat(Segment.SIZE * 3), sink.readString())
    }

    @Test
    fun copyTo() {
        val source = Buffer()
        source.writeString("party")

        val target = Buffer()
        source.copyTo(target, startIndex = 1, endIndex = 4)

        assertEquals("art", target.readString())
        assertEquals("party", source.readString())
    }

    @Test
    fun copyToAll() {
        val source = Buffer()
        source.writeString("hello")

        val target = Buffer()
        source.copyTo(target)

        assertEquals("hello", source.readString())
        assertEquals("hello", target.readString())
    }

    @Test
    fun copyToWithOnlyStartIndex() {
        val source = Buffer()
        source.writeString("hello")

        val target = Buffer()
        source.copyTo(target, startIndex = 1)

        assertEquals("hello", source.readString())
        assertEquals("ello", target.readString())
    }

    @Test
    fun copyToWithOnlyEndIndex() {
        val source = Buffer()
        source.writeString("hello")

        val target = Buffer()
        source.copyTo(target, endIndex = 1)

        assertEquals("hello", source.readString())
        assertEquals("h", target.readString())
    }

    @Test
    fun copyToOnSegmentBoundary() {
        val aStr = 'a'.repeat(Segment.SIZE)
        val bs = 'b'.repeat(Segment.SIZE)
        val cs = 'c'.repeat(Segment.SIZE)
        val ds = 'd'.repeat(Segment.SIZE)

        val source = Buffer()
        source.writeString(aStr)
        source.writeString(bs)
        source.writeString(cs)

        val target = Buffer()
        target.writeString(ds)

        source.copyTo(
            target, startIndex = aStr.length.toLong(),
            endIndex = aStr.length.toLong() + (bs.length + cs.length).toLong()
        )
        assertEquals(ds + bs + cs, target.readString())
    }

    @Test
    fun copyToOffSegmentBoundary() {
        val aStr = 'a'.repeat(Segment.SIZE - 1)
        val bs = 'b'.repeat(Segment.SIZE + 2)
        val cs = 'c'.repeat(Segment.SIZE - 4)
        val ds = 'd'.repeat(Segment.SIZE + 8)

        val source = Buffer()
        source.writeString(aStr)
        source.writeString(bs)
        source.writeString(cs)

        val target = Buffer()
        target.writeString(ds)

        source.copyTo(
            target, startIndex = aStr.length.toLong(),
            endIndex = aStr.length.toLong() + (bs.length + cs.length).toLong()
        )
        assertEquals(ds + bs + cs, target.readString())
    }

    @Test
    fun copyToSourceAndTargetCanBeTheSame() {
        val aStr = 'a'.repeat(Segment.SIZE)
        val bs = 'b'.repeat(Segment.SIZE)

        val source = Buffer()
        source.writeString(aStr)
        source.writeString(bs)

        source.copyTo(source, startIndex = 0, endIndex = source.size)
        assertEquals(aStr + bs + aStr + bs, source.readString())
    }

    @Test
    fun copyToEmptySource() {
        val source = Buffer()
        val target = Buffer().also { it.writeString("aaa") }
        source.copyTo(target, startIndex = 0L, endIndex = 0L)
        assertEquals("", source.readString())
        assertEquals("aaa", target.readString())
    }

    @Test
    fun copyToEmptyTarget() {
        val source = Buffer().also { it.writeString("aaa") }
        val target = Buffer()
        source.copyTo(target, startIndex = 0L, endIndex = 3L)
        assertEquals("aaa", source.readString())
        assertEquals("aaa", target.readString())
    }

    @Test
    fun completeSegmentByteCountOnEmptyBuffer() {
        val buffer = Buffer()
        assertEquals(0, buffer.completeSegmentByteCount())
    }

    @Test
    fun completeSegmentByteCountOnBufferWithFullSegments() {
        val buffer = Buffer()
        buffer.writeString("a".repeat(Segment.SIZE * 4))
        assertEquals((Segment.SIZE * 4).toLong(), buffer.completeSegmentByteCount())
    }

    @Test
    fun completeSegmentByteCountOnBufferWithIncompleteTailSegment() {
        val buffer = Buffer()
        buffer.writeString("a".repeat(Segment.SIZE * 4 - 10))
        assertEquals((Segment.SIZE * 3).toLong(), buffer.completeSegmentByteCount())
    }

    @Test
    fun cloneDoesNotObserveWritesToOriginal() {
        val original = Buffer()
        val clone: Buffer = original.copy()
        original.writeString("abc")
        assertEquals(0, clone.size)
    }

    @Test
    fun cloneDoesNotObserveReadsFromOriginal() {
        val original = Buffer()
        original.writeString("abc")
        val clone: Buffer = original.copy()
        assertEquals("abc", original.readString(3))
        assertEquals(3, clone.size)
        assertEquals("ab", clone.readString(2))
    }

    @Test
    fun originalDoesNotObserveWritesToClone() {
        val original = Buffer()
        val clone: Buffer = original.copy()
        clone.writeString("abc")
        assertEquals(0, original.size)
    }

    @Test
    fun originalDoesNotObserveReadsFromClone() {
        val original = Buffer()
        original.writeString("abc")
        val clone: Buffer = original.copy()
        assertEquals("abc", clone.readString(3))
        assertEquals(3, original.size)
        assertEquals("ab", original.readString(2))
    }

    @Test
    fun cloneMultipleSegments() {
        val original = Buffer()
        original.writeString("a".repeat(SEGMENT_SIZE * 3))
        val clone: Buffer = original.copy()
        original.writeString("b".repeat(SEGMENT_SIZE * 3))
        clone.writeString("c".repeat(SEGMENT_SIZE * 3))

        assertEquals(
            "a".repeat(SEGMENT_SIZE * 3) + "b".repeat(SEGMENT_SIZE * 3),
            original.readString((SEGMENT_SIZE * 6).toLong())
        )
        assertEquals(
            "a".repeat(SEGMENT_SIZE * 3) + "c".repeat(SEGMENT_SIZE * 3),
            clone.readString((SEGMENT_SIZE * 6).toLong())
        )
    }

    @Test
    fun readAndWriteToSelf() {
        val buffer = Buffer().also { it.writeByte(1) }
        val src: Source = buffer
        val dst: Sink = buffer

        assertFailsWith<IllegalArgumentException> { src.transferTo(dst) }
        assertFailsWith<IllegalArgumentException> { dst.transferFrom(src) }
        assertFailsWith<IllegalArgumentException> { src.readAtMostTo(buffer, 1) }
        assertFailsWith<IllegalArgumentException> { src.readTo(dst, 1) }
        assertFailsWith<IllegalArgumentException> { dst.write(buffer, 1) }
        assertFailsWith<IllegalArgumentException> { dst.write(src, 1) }
    }

    @Test
    fun transferCopy() {
        val buffer = Buffer().also { it.writeByte(42) }
        val copy = buffer.copy()
        copy.transferTo(buffer)
        assertArrayEquals(byteArrayOf(42, 42), buffer.readByteArray())
    }

    @Test
    fun snapshot() {
        val buffer = Buffer()
        assertEquals(ByteString(), buffer.snapshot())
        buffer.writeString("hello")
        assertEquals("hello".encodeToByteString(), buffer.snapshot())
        buffer.clear()
        assertEquals(ByteString(), buffer.snapshot())
    }

    @Test
    fun testTransferring() {
        val aaaa = ByteArray(10000) { 'a'.code.toByte() }
        val buffer = Buffer().also { it.write(aaaa) }
        val str = buffer.readString()
        assertEquals("a".repeat(aaaa.size), str)

        buffer.write(aaaa)
        val dstBuffer = Buffer()
        dstBuffer.writeByte('?'.code.toByte())

        dstBuffer.transferFrom(buffer)
        dstBuffer.readByteArray()
        assertTrue(buffer.readByteArray().isEmpty())
        //assertEquals("?" + "a".repeat(aaaa.size), dstBuffer.readString())
    }

    @Test
    fun writeUnbound() {
        assertFailsWith<IllegalArgumentException> { Buffer().writeUnbound(Int.MAX_VALUE) { 0 } }

        assertFailsWith<IllegalStateException> { Buffer().writeUnbound(1) { -1 } }
        assertFailsWith<IllegalStateException> { Buffer().writeUnbound(1) { it.capacity + 1 } }

        Buffer().apply {
            writeUnbound(4) {
                it[0] = 1
                it[1] = 2
                it[2] = 3
                it[3] = 4
                1
            }
            assertEquals(1, size)
            assertEquals(1, readByte())

            writeUnbound(1) {
                assertTrue(it.capacity > 1)
                it[0] = 5
                it[1] = 6
                2
            }
            assertEquals(2, size)
            assertEquals(ByteString(5, 6), readByteString())

            writeUnbound(3) {
                it[0] = 7
                it[1] = 8
                it[2] = 9
                3
            }
            assertEquals(3, size)
            assertEquals(ByteString(7, 8, 9), readByteString())
        }

        assertFailsWith<IllegalArgumentException> {
            Buffer().apply {
                writeUnbound(1) {
                    val cap = it.capacity
                    it[cap] = 0
                    0
                }
            }
        }
        assertFailsWith<IllegalArgumentException> {
            Buffer().apply {
                writeUnbound(1) {
                    it[-1] = 0
                    0
                }
            }
        }
    }

    @Test
    fun segmentRead() {
        val buffer = Buffer().apply {
            writeInt(0xdeadc0de.toInt())
        }
        buffer.head!!.also { head ->
            assertEquals(4, head.size)
            assertEquals(0xde.toByte(), head[0])
            assertEquals(0xad.toByte(), head[1])
            assertEquals(0xc0.toByte(), head[2])
            assertEquals(0xde.toByte(), head[3])

            assertFailsWith<IllegalArgumentException> { head[4] }
            assertFailsWith<IllegalArgumentException> { head[-1] }
        }

        buffer.writeByte(0)
        buffer.skip(2)
        buffer.head!!.also { head ->
            assertEquals(3, head.size)
            assertEquals(0xc0.toByte(), head[0])
            assertEquals(0xde.toByte(), head[1])
            assertEquals(0, head[2])
        }
    }
}
