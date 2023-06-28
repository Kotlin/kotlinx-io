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
        buffer.writeUtf8("ab")
        assertEquals(2, buffer.size)
        buffer.writeUtf8("cdef")
        assertEquals(6, buffer.size)
        assertEquals("abcd", buffer.readUtf8(4))
        assertEquals(2, buffer.size)
        assertEquals("ef", buffer.readUtf8(2))
        assertEquals(0, buffer.size)
        assertFailsWith<EOFException> {
            buffer.readUtf8(1)
        }
    }

    @Test
    fun bufferToString() {
        assertEquals("Buffer(size=0)", Buffer().toString())

        assertEquals(
            "Buffer(size=10 hex=610d0a620a630d645c65)",
            Buffer().also { it.writeUtf8("a\r\nb\nc\rd\\e") }.toString()
        )

        assertEquals(
            "Buffer(size=11 hex=547972616e6e6f73617572)",
            Buffer().also { it.writeUtf8("Tyrannosaur") }.toString()
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
        buffer.writeUtf8('a'.repeat(1000))
        buffer.writeUtf8('b'.repeat(2500))
        buffer.writeUtf8('c'.repeat(5000))
        buffer.writeUtf8('d'.repeat(10000))
        buffer.writeUtf8('e'.repeat(25000))
        buffer.writeUtf8('f'.repeat(50000))

        assertEquals('a'.repeat(999), buffer.readUtf8(999)) // a...a
        assertEquals("a" + 'b'.repeat(2500) + "c", buffer.readUtf8(2502)) // ab...bc
        assertEquals('c'.repeat(4998), buffer.readUtf8(4998)) // c...c
        assertEquals("c" + 'd'.repeat(10000) + "e", buffer.readUtf8(10002)) // cd...de
        assertEquals('e'.repeat(24998), buffer.readUtf8(24998)) // e...e
        assertEquals("e" + 'f'.repeat(50000), buffer.readUtf8(50001)) // ef...f
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
            source.writeUtf8(s)
            buffer.transferFrom(source)
            expected.append(s)
        }
        val segmentSizes = segmentSizes(buffer)
        assertEquals(expected.toString(), buffer.readUtf8(expected.length.toLong()))
        return segmentSizes
    }

    /** The big part of source's first segment is being moved.  */
    @Test
    fun writeSplitSourceBufferLeft() {
        val writeSize = Segment.SIZE / 2 + 1

        val sink = Buffer()
        sink.writeUtf8('b'.repeat(Segment.SIZE - 10))

        val source = Buffer()
        source.writeUtf8('a'.repeat(Segment.SIZE * 2))
        sink.write(source, writeSize.toLong())

        assertEquals(listOf(Segment.SIZE - 10, writeSize), segmentSizes(sink))
        assertEquals(listOf(Segment.SIZE - writeSize, Segment.SIZE), segmentSizes(source))
    }

    /** The big part of source's first segment is staying put.  */
    @Test
    fun writeSplitSourceBufferRight() {
        val writeSize = Segment.SIZE / 2 - 1

        val sink = Buffer()
        sink.writeUtf8('b'.repeat(Segment.SIZE - 10))

        val source = Buffer()
        source.writeUtf8('a'.repeat(Segment.SIZE * 2))
        sink.write(source, writeSize.toLong())

        assertEquals(listOf(Segment.SIZE - 10, writeSize), segmentSizes(sink))
        assertEquals(listOf(Segment.SIZE - writeSize, Segment.SIZE), segmentSizes(source))
    }

    @Test
    fun writePrefixDoesntSplit() {
        val sink = Buffer()
        sink.writeUtf8('b'.repeat(10))

        val source = Buffer()
        source.writeUtf8('a'.repeat(Segment.SIZE * 2))
        sink.write(source, 20)

        assertEquals(listOf(30), segmentSizes(sink))
        assertEquals(listOf(Segment.SIZE - 20, Segment.SIZE), segmentSizes(source))
        assertEquals(30, sink.size)
        assertEquals((Segment.SIZE * 2 - 20).toLong(), source.size)
    }

    @Test
    fun writePrefixDoesntSplitButRequiresCompact() {
        val sink = Buffer()
        sink.writeUtf8('b'.repeat(Segment.SIZE - 10)) // limit = size - 10
        sink.readUtf8((Segment.SIZE - 20).toLong()) // pos = size = 20

        val source = Buffer()
        source.writeUtf8('a'.repeat(Segment.SIZE * 2))
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
        sink.writeUtf8('a'.repeat(10))

        val source = Buffer()
        source.writeUtf8('b'.repeat(15))

        assertEquals(10, source.readAtMostTo(sink, 10))
        assertEquals(20, sink.size)
        assertEquals(5, source.size)
        assertEquals('a'.repeat(10) + 'b'.repeat(10), sink.readUtf8(20))
    }

    @Test
    fun moveFewerThanRequestedBytesWithRead() {
        val sink = Buffer()
        sink.writeUtf8('a'.repeat(10))

        val source = Buffer()
        source.writeUtf8('b'.repeat(20))

        assertEquals(20, source.readAtMostTo(sink, 25))
        assertEquals(30, sink.size)
        assertEquals(0, source.size)
        assertEquals('a'.repeat(10) + 'b'.repeat(20), sink.readUtf8(30))
    }

    @Test
    fun indexOfWithOffset() {
        val buffer = Buffer()
        val halfSegment = Segment.SIZE / 2
        buffer.writeUtf8('a'.repeat(halfSegment))
        buffer.writeUtf8('b'.repeat(halfSegment))
        buffer.writeUtf8('c'.repeat(halfSegment))
        buffer.writeUtf8('d'.repeat(halfSegment))
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
        buffer.writeUtf8("a")
        buffer.writeUtf8('b'.repeat(Segment.SIZE))
        buffer.writeUtf8("c")
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
        source.writeUtf8("abcd")
        sink.write(source, 2)
        assertEquals("ab", sink.readUtf8(2))
    }

    // Buffer don't override equals and hashCode
    @Test
    fun equalsAndHashCode() {
        val a = Buffer().also { it.writeUtf8("dog") }
        assertEquals(a, a)

        val b = Buffer().also { it.writeUtf8("hotdog") }
        assertTrue(a != b)

        b.readUtf8(3) // Leaves b containing 'dog'.
        assertTrue(a != b)
    }

    /**
     * When writing data that's already buffered, there's no reason to page the
     * data by segment.
     */
    @Test
    fun readAllWritesAllSegmentsAtOnce() {
        val write1 = Buffer()
        write1.writeUtf8(
            'a'.repeat(Segment.SIZE) +
                    'b'.repeat(Segment.SIZE) +
                    'c'.repeat(Segment.SIZE)
        )

        val source = Buffer()
        source.writeUtf8(
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
        val source = Buffer().also { it.writeUtf8('a'.repeat(Segment.SIZE * 3)) }
        val sink = Buffer()

        assertEquals((Segment.SIZE * 3).toLong(), sink.transferFrom(source))
        assertEquals(0, source.size)
        assertEquals('a'.repeat(Segment.SIZE * 3), sink.readUtf8())
    }

    @Test
    fun copyTo() {
        val source = Buffer()
        source.writeUtf8("party")

        val target = Buffer()
        source.copyTo(target, startIndex = 1, endIndex = 4)

        assertEquals("art", target.readUtf8())
        assertEquals("party", source.readUtf8())
    }

    @Test
    fun copyToAll() {
        val source = Buffer()
        source.writeUtf8("hello")

        val target = Buffer()
        source.copyTo(target)

        assertEquals("hello", source.readUtf8())
        assertEquals("hello", target.readUtf8())
    }

    @Test
    fun copyToWithOnlyStartIndex() {
        val source = Buffer()
        source.writeUtf8("hello")

        val target = Buffer()
        source.copyTo(target, startIndex = 1)

        assertEquals("hello", source.readUtf8())
        assertEquals("ello", target.readUtf8())
    }

    @Test
    fun copyToWithOnlyEndIndex() {
        val source = Buffer()
        source.writeUtf8("hello")

        val target = Buffer()
        source.copyTo(target, endIndex = 1)

        assertEquals("hello", source.readUtf8())
        assertEquals("h", target.readUtf8())
    }

    @Test
    fun copyToOnSegmentBoundary() {
        val aStr = 'a'.repeat(Segment.SIZE)
        val bs = 'b'.repeat(Segment.SIZE)
        val cs = 'c'.repeat(Segment.SIZE)
        val ds = 'd'.repeat(Segment.SIZE)

        val source = Buffer()
        source.writeUtf8(aStr)
        source.writeUtf8(bs)
        source.writeUtf8(cs)

        val target = Buffer()
        target.writeUtf8(ds)

        source.copyTo(
            target, startIndex = aStr.length.toLong(),
            endIndex = aStr.length.toLong() + (bs.length + cs.length).toLong()
        )
        assertEquals(ds + bs + cs, target.readUtf8())
    }

    @Test
    fun copyToOffSegmentBoundary() {
        val aStr = 'a'.repeat(Segment.SIZE - 1)
        val bs = 'b'.repeat(Segment.SIZE + 2)
        val cs = 'c'.repeat(Segment.SIZE - 4)
        val ds = 'd'.repeat(Segment.SIZE + 8)

        val source = Buffer()
        source.writeUtf8(aStr)
        source.writeUtf8(bs)
        source.writeUtf8(cs)

        val target = Buffer()
        target.writeUtf8(ds)

        source.copyTo(
            target, startIndex = aStr.length.toLong(),
            endIndex = aStr.length.toLong() + (bs.length + cs.length).toLong()
        )
        assertEquals(ds + bs + cs, target.readUtf8())
    }

    @Test
    fun copyToSourceAndTargetCanBeTheSame() {
        val aStr = 'a'.repeat(Segment.SIZE)
        val bs = 'b'.repeat(Segment.SIZE)

        val source = Buffer()
        source.writeUtf8(aStr)
        source.writeUtf8(bs)

        source.copyTo(source, startIndex = 0, endIndex = source.size)
        assertEquals(aStr + bs + aStr + bs, source.readUtf8())
    }

    @Test
    fun copyToEmptySource() {
        val source = Buffer()
        val target = Buffer().also { it.writeUtf8("aaa") }
        source.copyTo(target, startIndex = 0L, endIndex = 0L)
        assertEquals("", source.readUtf8())
        assertEquals("aaa", target.readUtf8())
    }

    @Test
    fun copyToEmptyTarget() {
        val source = Buffer().also { it.writeUtf8("aaa") }
        val target = Buffer()
        source.copyTo(target, startIndex = 0L, endIndex = 3L)
        assertEquals("aaa", source.readUtf8())
        assertEquals("aaa", target.readUtf8())
    }

    @Test
    fun completeSegmentByteCountOnEmptyBuffer() {
        val buffer = Buffer()
        assertEquals(0, buffer.completeSegmentByteCount())
    }

    @Test
    fun completeSegmentByteCountOnBufferWithFullSegments() {
        val buffer = Buffer()
        buffer.writeUtf8("a".repeat(Segment.SIZE * 4))
        assertEquals((Segment.SIZE * 4).toLong(), buffer.completeSegmentByteCount())
    }

    @Test
    fun completeSegmentByteCountOnBufferWithIncompleteTailSegment() {
        val buffer = Buffer()
        buffer.writeUtf8("a".repeat(Segment.SIZE * 4 - 10))
        assertEquals((Segment.SIZE * 3).toLong(), buffer.completeSegmentByteCount())
    }

    @Test
    fun cloneDoesNotObserveWritesToOriginal() {
        val original = Buffer()
        val clone: Buffer = original.copy()
        original.writeUtf8("abc")
        assertEquals(0, clone.size)
    }

    @Test
    fun cloneDoesNotObserveReadsFromOriginal() {
        val original = Buffer()
        original.writeUtf8("abc")
        val clone: Buffer = original.copy()
        assertEquals("abc", original.readUtf8(3))
        assertEquals(3, clone.size)
        assertEquals("ab", clone.readUtf8(2))
    }

    @Test
    fun originalDoesNotObserveWritesToClone() {
        val original = Buffer()
        val clone: Buffer = original.copy()
        clone.writeUtf8("abc")
        assertEquals(0, original.size)
    }

    @Test
    fun originalDoesNotObserveReadsFromClone() {
        val original = Buffer()
        original.writeUtf8("abc")
        val clone: Buffer = original.copy()
        assertEquals("abc", clone.readUtf8(3))
        assertEquals(3, original.size)
        assertEquals("ab", original.readUtf8(2))
    }

    @Test
    fun cloneMultipleSegments() {
        val original = Buffer()
        original.writeUtf8("a".repeat(SEGMENT_SIZE * 3))
        val clone: Buffer = original.copy()
        original.writeUtf8("b".repeat(SEGMENT_SIZE * 3))
        clone.writeUtf8("c".repeat(SEGMENT_SIZE * 3))

        assertEquals(
            "a".repeat(SEGMENT_SIZE * 3) + "b".repeat(SEGMENT_SIZE * 3),
            original.readUtf8((SEGMENT_SIZE * 6).toLong())
        )
        assertEquals(
            "a".repeat(SEGMENT_SIZE * 3) + "c".repeat(SEGMENT_SIZE * 3),
            clone.readUtf8((SEGMENT_SIZE * 6).toLong())
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
        buffer.writeUtf8("hello")
        assertEquals("hello".encodeToByteString(), buffer.snapshot())
        buffer.clear()
        assertEquals(ByteString(), buffer.snapshot())
    }
}
