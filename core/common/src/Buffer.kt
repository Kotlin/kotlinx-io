/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2019 Square, Inc.
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

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlinx.io.unsafe.UnsafeBufferOperations
import kotlin.jvm.JvmSynthetic

/**
 * A collection of bytes in memory.
 *
 * The buffer can be viewed as an unbound queue whose size grows with the data being written
 * and shrinks with data being consumed. Internally, the buffer consists of data segments,
 * and the buffer's capacity grows and shrinks in units of data segments instead of individual bytes.
 *
 * The buffer was designed to reduce memory allocations when possible. Instead of copying bytes
 * from one place in memory to another, this class just changes ownership of the underlying data segments.
 *
 * To reduce allocations and speed up the buffer's extension, it may use data segments pooling.
 *
 * [Buffer] implements both [Source] and [Sink] and could be used as a source or a sink,
 * but unlike regular sinks and sources its [close], [flush], [emit], [hintEmit]
 * does not affect buffer's state and [exhausted] only indicates that a buffer is empty.
 *
 * ### Thread-safety guarantees
 *
 * [Buffer] does not provide any thread-safety guarantees.
 * If a [Buffer] needs to be accessed from multiple threads, an additional synchronization is required.
 * Failure to do so will result in possible data corruption, loss, and runtime errors.
 */
public class Buffer : Source, Sink {
    @PublishedApi
    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var head: Segment? = null

    @PublishedApi
    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var tail: Segment? = null

    /**
     * The number of bytes accessible for read from this buffer.
     */
    public val size: Long
        get() = sizeMut

    @PublishedApi
    @get:JvmSynthetic
    @set:JvmSynthetic
    internal var sizeMut: Long = 0L

    /**
     * Returns the buffer itself.
     */
    @InternalIoApi
    override val buffer: Buffer get() = this

    override fun exhausted(): Boolean = size == 0L

    override fun require(byteCount: Long) {
        require(byteCount >= 0) { "byteCount: $byteCount" }
        if (size < byteCount) {
            throw EOFException("Buffer doesn't contain required number of bytes (size: $size, required: $byteCount)")
        }
    }

    override fun request(byteCount: Long): Boolean {
        require(byteCount >= 0) { "byteCount: $byteCount < 0" }
        return size >= byteCount
    }

    override fun readByte(): Byte {
        val segment = head ?: throwEof(1)
        val segmentSize = segment.size
        if (segmentSize == 0) {
            recycleHead()
            return readByte()
        }
        val v = segment.readByte()
        sizeMut -= 1L
        if (segmentSize == 1) {
            recycleHead()
        }
        return v
    }

    override fun readShort(): Short {
        val segment = head ?: throwEof(2)
        val segmentSize = segment.size
        if (segmentSize < 2) {
            // If the short is split across multiple segments, delegate to readByte().
            require(2)
            if (segmentSize == 0) {
                recycleHead()
                return readShort()
            }
            return (readByte() and 0xff shl 8 or (readByte() and 0xff)).toShort()
        }
        val v = segment.readShort()
        sizeMut -= 2L
        if (segmentSize == 2) {
            recycleHead()
        }
        return v
    }

    override fun readInt(): Int {
        val segment = head ?: throwEof(4)
        val segmentSize = segment.size
        if (segmentSize < 4) {
            // If the short is split across multiple segments, delegate to readShort().
            require(4)
            if (segmentSize == 0) {
                recycleHead()
                return readInt()
            }
            return (readShort().toInt() shl 16 or (readShort().toInt() and 0xffff))
        }
        val v = segment.readInt()
        sizeMut -= 4L
        if (segmentSize == 4) {
            recycleHead()
        }
        return v
    }

    override fun readLong(): Long {
        val segment = head ?: throwEof(8)
        val segmentSize = segment.size
        if (segmentSize < 8) {
            // If the short is split across multiple segments, delegate to readInt().
            require(8)
            if (segmentSize == 0) {
                recycleHead()
                return readLong()
            }
            return (readInt().toLong() shl 32 or (readInt().toLong() and 0xffffffffL))
        }
        val v = segment.readLong()
        sizeMut -= 8L
        if (segmentSize == 8) {
            recycleHead()
        }
        return v
    }

    private fun throwEof(byteCount: Long): Nothing {
        throw EOFException("Buffer doesn't contain required number of bytes (size: $size, required: $byteCount)")
    }

    /**
     * This method does not affect the buffer's content as there is no upstream to write data to.
     */
    @InternalIoApi
    override fun hintEmit(): Unit = Unit

    /**
     * This method does not affect the buffer's content as there is no upstream to write data to.
     */
    override fun emit(): Unit = Unit

    /**
     * This method does not affect the buffer's content as there is no upstream to write data to.
     */
    override fun flush(): Unit = Unit

    /**
     * Copies bytes from this buffer's subrange starting at [startIndex] and ending at [endIndex], to [out] buffer.
     * This method does not consume data from the buffer.
     *
     * @param out the destination buffer to copy data into.
     * @param startIndex the index (inclusive) of the first byte of data in this buffer to copy,
     * 0 by default.
     * @param endIndex the index (exclusive) of the last byte of data in this buffer to copy, `buffer.size` by default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of this buffer bounds
     * (`[0..buffer.size)`).
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.bufferCopy
     */
    public fun copyTo(
        out: Buffer,
        startIndex: Long = 0L,
        endIndex: Long = size
    ) {
        checkBounds(size, startIndex, endIndex)
        if (startIndex == endIndex) return

        var currentOffset = startIndex
        var remainingByteCount = endIndex - startIndex

        out.sizeMut += remainingByteCount

        // Skip segments that we aren't copying from.
        var s = head
        while (currentOffset >= s!!.limit - s.pos) {
            currentOffset -= (s.limit - s.pos).toLong()
            s = s.next
        }

        // Copy one segment at a time.
        while (remainingByteCount > 0L) {
            val copy = s!!.sharedCopy()
            copy.pos += currentOffset.toInt()
            copy.limit = minOf(copy.pos + remainingByteCount.toInt(), copy.limit)
            out.pushSegment(copy)
            remainingByteCount -= (copy.limit - copy.pos).toLong()
            currentOffset = 0L
            s = s.next
        }
    }

    /**
     * Returns the number of bytes in segments that are fully filled and are no longer writable.
     *
     * This is the number of bytes that can be flushed immediately to an underlying sink without harming throughput.
     */
    internal fun completeSegmentByteCount(): Long {
        var result = size
        if (result == 0L) return 0L

        // Omit the tail if it's still writable.
        val tail = tail!!
        if (tail.limit < Segment.SIZE && tail.owner) {
            result -= (tail.limit - tail.pos).toLong()
        }

        return result
    }

    /**
     * Returns the byte at [position].
     *
     * Use of this method may expose significant performance penalties and it's not recommended to use it
     * for sequential access to a range of bytes within the buffer.
     *
     * @throws IndexOutOfBoundsException when [position] is negative or greater or equal to [Buffer.size].
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.bufferGetByte
     */
    public operator fun get(position: Long): Byte {
        if (position < 0 || position >= size) {
            throw IndexOutOfBoundsException("position ($position) is not within the range [0..size($size))")
        }
        if (position == 0L) {
            return head!!.getUnchecked(0)
        }
        seek(position) { s, offset ->
            return s!!.getUnchecked((position - offset).toInt())
        }
    }

    /**
     * Discards all bytes in this buffer.
     *
     * Call to this method is equivalent to [skip] with `byteCount = size`.
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.bufferClear
     */
    public fun clear(): Unit = skip(size)

    /**
     * Discards [byteCount] bytes from the head of this buffer.
     *
     * @throws IllegalArgumentException when [byteCount] is negative.
     */
    override fun skip(byteCount: Long) {
        checkByteCount(byteCount)
        var remainingByteCount = byteCount
        while (remainingByteCount > 0) {
            val head = head ?: throw EOFException("Buffer exhausted before skipping $byteCount bytes.")

            val toSkip = minOf(remainingByteCount, head.limit - head.pos).toInt()
            sizeMut -= toSkip.toLong()
            remainingByteCount -= toSkip.toLong()
            head.pos += toSkip

            if (head.pos == head.limit) {
                recycleHead()
            }
        }
    }

    override fun readAtMostTo(sink: ByteArray, startIndex: Int, endIndex: Int): Int {
        checkBounds(sink.size, startIndex, endIndex)

        val s = this.head ?: return -1
        val toCopy = minOf(endIndex - startIndex, s.size)
        s.readTo(sink, startIndex, startIndex + toCopy)
        sizeMut -= toCopy.toLong()

        if (s.isEmpty()) {
            recycleHead()
        }

        return toCopy
    }

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        checkByteCount(byteCount)
        if (size == 0L) return -1L
        val bytesWritten = if (byteCount > size) size else byteCount
        sink.write(this, bytesWritten)
        return bytesWritten
    }

    override fun readTo(sink: RawSink, byteCount: Long) {
        checkByteCount(byteCount)
        if (size < byteCount) {
            sink.write(this, size) // Exhaust ourselves.
            throw EOFException("Buffer exhausted before writing $byteCount bytes. Only $size bytes were written.")
        }
        sink.write(this, byteCount)
    }

    override fun transferTo(sink: RawSink): Long {
        val byteCount = size
        if (byteCount > 0L) {
            sink.write(this, byteCount)
        }
        return byteCount
    }

    override fun peek(): Source = PeekSource(this).buffered()

    /**
     * Returns a tail segment that we can write at least `minimumCapacity`
     * bytes to, creating it if necessary.
     */
    @PublishedApi
    @JvmSynthetic
    internal fun writableSegment(minimumCapacity: Int): Segment {
        require(minimumCapacity >= 1 && minimumCapacity <= Segment.SIZE) {
            "unexpected capacity ($minimumCapacity), should be in range [1, ${Segment.SIZE}]"
        }

        if (tail == null) {
            val result = SegmentPool.take() // Acquire a first segment.
            head = result
            tail = result
            return result
        }

        val t = tail!!
        if (t.limit + minimumCapacity > Segment.SIZE || !t.owner) {
            val newTail = t.push(SegmentPool.take()) // Append a new empty segment to fill up.
            tail = newTail
            return newTail
        }
        return t
    }

    override fun write(source: ByteArray, startIndex: Int, endIndex: Int) {
        checkBounds(source.size, startIndex, endIndex)
        var currentOffset = startIndex
        while (currentOffset < endIndex) {
            val tail = writableSegment(1)
            val toCopy = minOf(endIndex - currentOffset, tail.remainingCapacity)
            tail.write(source, currentOffset, currentOffset + toCopy)
            currentOffset += toCopy
        }
        sizeMut  += endIndex - startIndex
    }

    override fun write(source: RawSource, byteCount: Long) {
        checkByteCount(byteCount)
        var remainingByteCount = byteCount
        while (remainingByteCount > 0L) {
            val read = source.readAtMostTo(this, remainingByteCount)
            if (read == -1L) {
                throw EOFException(
                    "Source exhausted before reading $byteCount bytes. " +
                            "Only ${byteCount - remainingByteCount} were read."
                )
            }
            remainingByteCount -= read
        }
    }

    override fun write(source: Buffer, byteCount: Long) {
        // Move bytes from the head of the source buffer to the tail of this buffer
        // while balancing two conflicting goals: don't waste CPU and don't waste
        // memory.
        //
        //
        // Don't waste CPU (ie. don't copy data around).
        //
        // Copying large amounts of data is expensive. Instead, we prefer to
        // reassign entire segments from one buffer to the other.
        //
        //
        // Don't waste memory.
        //
        // As an invariant, adjacent pairs of segments in a buffer should be at
        // least 50% full, except for the head segment and the tail segment.
        //
        // The head segment cannot maintain the invariant because the application is
        // consuming bytes from this segment, decreasing its level.
        //
        // The tail segment cannot maintain the invariant because the application is
        // producing bytes, which may require new nearly-empty tail segments to be
        // appended.
        //
        //
        // Moving segments between buffers
        //
        // When writing one buffer to another, we prefer to reassign entire segments
        // over copying bytes into their most compact form. Suppose we have a buffer
        // with these segment levels [91%, 61%]. If we append a buffer with a
        // single [72%] segment, that yields [91%, 61%, 72%]. No bytes are copied.
        //
        // Or suppose we have a buffer with these segment levels: [100%, 2%], and we
        // want to append it to a buffer with these segment levels [99%, 3%]. This
        // operation will yield the following segments: [100%, 2%, 99%, 3%]. That
        // is, we do not spend time copying bytes around to achieve more efficient
        // memory use like [100%, 100%, 4%].
        //
        // When combining buffers, we will compact adjacent buffers when their
        // combined level doesn't exceed 100%. For example, when we start with
        // [100%, 40%] and append [30%, 80%], the result is [100%, 70%, 80%].
        //
        //
        // Splitting segments
        //
        // Occasionally we write only part of a source buffer to a sink buffer. For
        // example, given a sink [51%, 91%], we may want to write the first 30% of
        // a source [92%, 82%] to it. To simplify, we first transform the source to
        // an equivalent buffer [30%, 62%, 82%] and then move the head segment,
        // yielding sink [51%, 91%, 30%] and source [62%, 82%].

        require(source !== this) { "source == this" }
        checkOffsetAndCount(source.sizeMut, 0, byteCount)

        var remainingByteCount = byteCount

        while (remainingByteCount > 0L) {
            // Is a prefix of the source's head segment all that we need to move?
            if (remainingByteCount < source.head!!.size) {
                val tail = tail
                if (tail != null && tail.owner &&
                    remainingByteCount + tail.limit - (if (tail.shared) 0 else tail.pos) <= Segment.SIZE
                ) {
                    // Our existing segments are sufficient. Move bytes from source's head to our tail.
                    source.head!!.writeTo(tail, remainingByteCount.toInt())
                    source.sizeMut -= remainingByteCount
                    sizeMut += remainingByteCount
                    return
                } else {
                    // We're going to need another segment. Split the source's head
                    // segment in two, then move the first of those two to this buffer.
                    source.head = source.head!!.split(remainingByteCount.toInt())
                }
            }

            // Remove the source's head segment and append it to our tail.
            val segmentToMove = source.head!!
            val movedByteCount = segmentToMove.size.toLong()
            source.head = segmentToMove.pop()
            if (source.head == null) {
                source.tail = null
            }
            pushSegment(segmentToMove, true)
            source.sizeMut -= movedByteCount
            sizeMut += movedByteCount
            remainingByteCount -= movedByteCount
        }
    }

    override fun transferFrom(source: RawSource): Long {
        var totalBytesRead = 0L
        while (true) {
            val readCount = source.readAtMostTo(this, Segment.SIZE.toLong())
            if (readCount == -1L) break
            totalBytesRead += readCount
        }
        return totalBytesRead
    }

    override fun writeByte(byte: Byte) {
        writableSegment(1).writeByte(byte)
        sizeMut += 1L
    }

    override fun writeShort(short: Short) {
        writableSegment(2).writeShort(short)
        sizeMut += 2L
    }

    override fun writeInt(int: Int) {
        writableSegment(4).writeInt(int)
        sizeMut += 4L
    }

    override fun writeLong(long: Long) {
        writableSegment(8).writeLong(long)
        sizeMut += 8L
    }

    /**
     * Returns a deep copy of this buffer.
     */
    public fun copy(): Buffer {
        val result = Buffer()
        if (size == 0L) return result

        val head = this.head!!
        val headCopy = head.sharedCopy()

        result.head = headCopy
        result.tail = headCopy

        var s = head.next
        while (s != null) {
            result.tail = result.tail!!.push(s.sharedCopy())
            s = s.next
        }

        result.sizeMut = size
        return result
    }

    /**
     * This method does not affect the buffer.
     */
    override fun close(): Unit = Unit

    /**
     * Returns a human-readable string that describes the contents of this buffer. For buffers containing
     * few bytes, this is a string like `Buffer(size=4 hex=0000ffff)`. However, if the buffer is too large,
     * a string will contain its size and only a prefix of data, like `Buffer(size=1024 hex=01234…)`.
     * Thus, the string could not be used to compare buffers or verify buffer's content.
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.bufferToString
     */
    @OptIn(UnsafeIoApi::class)
    override fun toString(): String {
        if (size == 0L) return "Buffer(size=0)"

        val maxPrintableBytes = 64
        val len = minOf(maxPrintableBytes, size).toInt()

        val builder = StringBuilder(len * 2 + if (size > maxPrintableBytes) 1 else 0)
        var bytesWritten = 0
        UnsafeBufferOperations.forEachSegment(this) { ctx, segment ->
            var idx = 0
            while (bytesWritten < len && idx < segment.size) {
                val b = ctx.getUnchecked(segment, idx++)
                bytesWritten++
                builder
                    .append(HEX_DIGIT_CHARS[(b shr 4) and 0xf])
                    .append(HEX_DIGIT_CHARS[b and 0xf])
            }
        }

        if (size > maxPrintableBytes) {
            builder.append('…')
        }

        return "Buffer(size=$size hex=$builder)"
    }

    /**
     * Unlinks and recycles this buffer's head.
     *
     * If head had a successor, it'll become a new head.
     * Otherwise, both [head] and [tail] will be set to null.
     *
     * It's up to a caller to ensure that the head exists.
     */
    internal fun recycleHead() {
        val oldHead = head!!
        val nextHead = oldHead.next
        head = nextHead
        if (nextHead == null) {
            tail = null
        } else {
            nextHead.prev = null
        }
        oldHead.next = null
        SegmentPool.recycle(oldHead)
    }

    /**
     * Unlinks and recycles this buffer's tail segment.
     *
     * If tail had a predecessor, it'll become a new tail.
     * Otherwise, both [head] and [tail] will be set to null.
     *
     * It's up to a caller to ensure that the tail exists.
     */
    @PublishedApi
    @JvmSynthetic
    internal fun recycleTail() {
        val oldTail = tail!!
        val newTail = oldTail.prev
        tail = newTail
        if (newTail == null) {
            head = null
        } else {
            newTail.next = null
        }
        oldTail.prev = null
        SegmentPool.recycle(oldTail)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun pushSegment(newTail: Segment, tryCompact: Boolean = false) {
        if (head == null) {
            head = newTail
            tail = newTail
        } else if (tryCompact) {
            tail = tail!!.push(newTail).compact()
            if (tail!!.prev == null) {
                head = tail
            }
        } else {
            tail = tail!!.push(newTail)
        }
    }
}

/**
 * Invoke `lambda` with the segment and offset at `fromIndex`. Searches from the front or the back
 * depending on what's closer to `fromIndex`.
 */
@PublishedApi
@JvmSynthetic
@OptIn(ExperimentalContracts::class)
internal inline fun <T> Buffer.seek(
    fromIndex: Long,
    lambda: (Segment?, Long) -> T
): T {
    contract {
        callsInPlace(lambda, EXACTLY_ONCE)
    }

    if (this.head == null) {
        return lambda(null, -1L)
    }

    if (size - fromIndex < fromIndex) {
        var s = tail
        // We're scanning in the back half of this buffer. Find the segment starting at the back.
        var offset = size
        while (s != null && offset > fromIndex) {
            offset -= (s.limit - s.pos).toLong()
            if (offset <= fromIndex) break
            s = s.prev
        }
        return lambda(s, offset)
    } else {
        var s = this.head
        // We're scanning in the front half of this buffer. Find the segment starting at the front.
        var offset = 0L
        while (s != null) {
            val nextOffset = offset + (s.limit - s.pos)
            if (nextOffset > fromIndex) break
            s = s.next
            offset = nextOffset
        }
        return lambda(s, offset)
    }
}
