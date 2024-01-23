/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import java.nio.ByteBuffer

@UnsafeIoApi
public inline fun <T> Segment.withContainedData(block: (Any, Int, Int) -> T) : T {
    return block(rawData, pos, limit)
}

public class ByteBufferSegment : Segment {
    internal val data: ByteBuffer

    @PublishedApi
    internal override val rawData: Any
        get() = data

    internal constructor() : super(0, 0, false, true) {
        this.data = ByteBuffer.allocateDirect(SIZE)
    }

    internal constructor(data: ByteBuffer, pos: Int, limit: Int, shared: Boolean, owner: Boolean) : super(pos, limit, shared, owner) {
        this.data = data
    }

    /**
     * Returns a new segment that shares the underlying byte array with this. Adjusting pos and limit
     * are safe but writes are forbidden. This also marks the current segment as shared, which
     * prevents it from being pooled.
     */
    internal override fun sharedCopy(): ByteBufferSegment {
        shared = true
        return ByteBufferSegment(data, pos, limit, true, false)
    }

    /**
     * Splits this head of a list into two segments. The first segment contains the
     * data in `[pos..pos+byteCount)`. The second segment contains the data in
     * `[pos+byteCount..limit)`. This can be useful when moving partial segments from one buffer to
     * another.
     *
     * Returns the new head of the list.
     */
    internal override fun split(byteCount: Int): Segment {
        require(byteCount > 0 && byteCount <= limit - pos) { "byteCount out of range" }
        val prefix: Segment

        // We have two competing performance goals:
        //  - Avoid copying data. We accomplish this by sharing segments.
        //  - Avoid short shared segments. These are bad for performance because they are readonly and
        //    may lead to long chains of short segments.
        // To balance these goals we only share segments when the copy will be large.
        if (byteCount >= SHARE_MINIMUM) {
            prefix = sharedCopy()
        } else {
            prefix = SegmentPool.take() as ByteBufferSegment
            data.position(pos)
            data.limit(pos + byteCount)
            prefix.data.put(data)
            data.clear()
            prefix.data.clear()
            //data.copyInto(prefix.data, startIndex = pos, endIndex = pos + byteCount)
        }

        prefix.limit = prefix.pos + byteCount
        pos += byteCount
        if (this.prev != null) {
            this.prev!!.push(prefix)
        } else {
            prefix.next = this
            this.prev = prefix
        }
        return prefix
    }

    /** Moves `byteCount` bytes from this segment to `sink`.  */
    internal override fun writeTo(sink: Segment, byteCount: Int) {
        sink as ByteBufferSegment
        check(sink.owner) { "only owner can write" }
        if (sink.limit + byteCount > SIZE) {
            // We can't fit byteCount bytes at the sink's current position. Shift sink first.
            if (sink.shared) throw IllegalArgumentException()
            if (sink.limit + byteCount - sink.pos > SIZE) throw IllegalArgumentException()
            sink.data.position(sink.pos)
            sink.data.limit(sink.limit)
            sink.data.compact()
            sink.data.clear()
            sink.limit -= sink.pos
            sink.pos = 0
        }

        if (sink.data !== data) {
            sink.data.position(sink.limit)
            data.position(pos)
            data.limit(pos + byteCount)
            sink.data.put(data)
            data.clear()
            sink.data.clear()
        } else {
            val dataCopy = data.duplicate()
            sink.data.position(sink.limit)
            dataCopy.position(pos)
            dataCopy.limit(pos + byteCount)
            sink.data.put(dataCopy)
            sink.data.clear()
        }
/*
        data.copyInto(
            sink.data, destinationOffset = sink.limit, startIndex = pos,
            endIndex = pos + byteCount
        )

 */
        sink.limit += byteCount
        pos += byteCount
    }

    /**
     * Number of bytes that could be written in the segment.
     */
    public override val remainingCapacity: Int
        get() = data.capacity() - limit

    internal override fun writeByte(byte: Byte) {
        data.put(limit++, byte)
    }

    internal override fun writeShort(short: Short) {
        val data = data
        val limit = limit
        data.putShort(limit, short)
        this.limit = limit + 2
    }

    internal override fun writeInt(int: Int) {
        val data = data
        val limit = limit
        data.putInt(limit, int)
        this.limit = limit + 4
    }

    internal override fun writeLong(long: Long) {
        val data = data
        val limit = limit
        data.putLong(limit, long)
        this.limit = limit + 8
    }

    internal override fun readByte(): Byte {
        return data[pos++]
    }

    internal override fun readShort(): Short {
        val data = data
        val pos = pos
        this.pos = pos + 2
        return data.getShort(pos)
    }

    internal override fun readInt(): Int {
        val data = data
        val pos = pos
        this.pos = pos + 4
        return data.getInt(pos)
    }

    internal override fun readLong(): Long {
        val data = data
        val pos = pos
        this.pos = pos + 8
        return data.getLong(pos)
    }

    internal override fun readTo(dst: ByteArray, dstStartOffset: Int, dstEndOffset: Int) {
        val len = dstEndOffset - dstStartOffset
        //require(len in 0 .. size)
        //data.copyInto(dst, dstStartOffset, pos, pos + len)
        data.position(pos)
        data.limit(pos + len)
        data.get(dst, dstStartOffset, len)
        data.clear()
        pos += len
    }

    internal override fun write(src: ByteArray, srcStartOffset: Int, srcEndOffset: Int) {
        //require(srcEndOffset - srcStartOffset in 0 .. remainingCapacity)
        //src.copyInto(data, limit, srcStartOffset, srcEndOffset)
        data.position(limit)
        data.put(src, srcStartOffset, srcEndOffset - srcStartOffset)
        data.clear()
        limit += srcEndOffset - srcStartOffset
    }

    /**
     * Returns value at [index]-position within this segment.
     * [index] value must be in range `[0, Segment.size)`.
     *
     * Unlike [Segment.readByte], this method does not affect [Segment.size], i.e., it does not consume value from
     * the segment.
     *
     * @param index the index of byte to read from the segment.
     *
     * @throws IllegalArgumentException if [index] is negative or greater or equal to [Segment.size].
     */
    public override fun getChecked(index: Int): Byte {
        require(index in 0 until size)
        return data[pos + index]
    }

    internal override fun getUnchecked(index: Int): Byte {
        return data[pos + index]
    }

    internal override fun setChecked(index: Int, value: Byte) {
        require(index in 0 until remainingCapacity)
        data.put(limit + index, value)
    }

    @PublishedApi
    internal override fun setUnchecked(index: Int, value: Byte) {
        data.put(limit + index, value)
    }

    @PublishedApi
    internal override fun setUnchecked(index: Int, b0: Byte, b1: Byte) {
        val d = data
        val l = limit
        d.put(l + index, b0)
        d.put(l + index + 1, b1)
    }

    @PublishedApi
    internal override fun setUnchecked(index: Int, b0: Byte, b1: Byte, b2: Byte) {
        val d = data
        val l = limit
        d.put(l + index, b0)
        d.put(l + index + 1, b1)
        d.put(l + index + 2, b2)
    }

    @PublishedApi
    internal override fun setUnchecked(index: Int, b0: Byte, b1: Byte, b2: Byte, b3: Byte) {
        val d = data
        val l = limit
        d.put(l + index, b0)
        d.put(l + index + 1, b1)
        d.put(l + index + 2, b2)
        d.put(l + index + 3, b3)
    }
}


public class ByteArraySegment : Segment {
    internal val data: ByteArray

    @PublishedApi
    internal override val rawData: Any
        get() = data
    internal constructor() : super(0, 0, false, true) {
        this.data = ByteArray(SIZE)
    }

    internal constructor(data: ByteArray, pos: Int, limit: Int, shared: Boolean, owner: Boolean)
    : super(pos, limit, shared, owner) {
        this.data = data
    }

    /**
     * Returns a new segment that shares the underlying byte array with this. Adjusting pos and limit
     * are safe but writes are forbidden. This also marks the current segment as shared, which
     * prevents it from being pooled.
     */
    internal override fun sharedCopy(): ByteArraySegment {
        shared = true
        return ByteArraySegment(data, pos, limit, true, false)
    }

    /** Returns a new segment that its own private copy of the underlying byte array.  */
    internal fun unsharedCopy() = ByteArraySegment(data.copyOf(), pos, limit, false, true)



    /**
     * Splits this head of a list into two segments. The first segment contains the
     * data in `[pos..pos+byteCount)`. The second segment contains the data in
     * `[pos+byteCount..limit)`. This can be useful when moving partial segments from one buffer to
     * another.
     *
     * Returns the new head of the list.
     */
    internal override fun split(byteCount: Int): Segment {
        require(byteCount > 0 && byteCount <= limit - pos) { "byteCount out of range" }
        val prefix: ByteArraySegment

        // We have two competing performance goals:
        //  - Avoid copying data. We accomplish this by sharing segments.
        //  - Avoid short shared segments. These are bad for performance because they are readonly and
        //    may lead to long chains of short segments.
        // To balance these goals we only share segments when the copy will be large.
        if (byteCount >= SHARE_MINIMUM) {
            prefix = sharedCopy()
        } else {
            prefix = SegmentPool.take() as ByteArraySegment
            data.copyInto(prefix.data, startIndex = pos, endIndex = pos + byteCount)
        }

        prefix.limit = prefix.pos + byteCount
        pos += byteCount
        if (this.prev != null) {
            this.prev!!.push(prefix)
        } else {
            prefix.next = this
            this.prev = prefix
        }
        return prefix
    }

    /** Moves `byteCount` bytes from this segment to `sink`.  */
    internal override fun writeTo(sink: Segment, byteCount: Int) {
        sink as ByteArraySegment
        check(sink.owner) { "only owner can write" }
        if (sink.limit + byteCount > SIZE) {
            // We can't fit byteCount bytes at the sink's current position. Shift sink first.
            if (sink.shared) throw IllegalArgumentException()
            if (sink.limit + byteCount - sink.pos > SIZE) throw IllegalArgumentException()
            sink.data.copyInto(sink.data, startIndex = sink.pos, endIndex = sink.limit)
            sink.limit -= sink.pos
            sink.pos = 0
        }

        data.copyInto(
            sink.data, destinationOffset = sink.limit, startIndex = pos,
            endIndex = pos + byteCount
        )
        sink.limit += byteCount
        pos += byteCount
    }

    /**
     * Number of bytes that could be written in the segment.
     */
    public override val remainingCapacity: Int
        get() = data.size - limit

    internal override fun writeByte(byte: Byte) {
        data[limit++] = byte
    }

    internal override fun writeShort(short: Short) {
        val data = data
        var limit = limit
        data[limit++] = (short.toInt() ushr 8 and 0xff).toByte()
        data[limit++] = (short.toInt() and 0xff).toByte()
        this.limit = limit
    }

    internal override fun writeInt(int: Int) {
        val data = data
        var limit = limit
        data[limit++] = (int ushr 24 and 0xff).toByte()
        data[limit++] = (int ushr 16 and 0xff).toByte()
        data[limit++] = (int ushr 8 and 0xff).toByte()
        data[limit++] = (int and 0xff).toByte()
        this.limit = limit
    }

    internal override fun writeLong(long: Long) {
        val data = data
        var limit = limit
        data[limit++] = (long ushr 56 and 0xffL).toByte()
        data[limit++] = (long ushr 48 and 0xffL).toByte()
        data[limit++] = (long ushr 40 and 0xffL).toByte()
        data[limit++] = (long ushr 32 and 0xffL).toByte()
        data[limit++] = (long ushr 24 and 0xffL).toByte()
        data[limit++] = (long ushr 16 and 0xffL).toByte()
        data[limit++] = (long ushr 8 and 0xffL).toByte()
        data[limit++] = (long and 0xffL).toByte()
        this.limit = limit
    }

    internal override fun readByte(): Byte {
        return data[pos++]
    }

    internal override fun readShort(): Short {
        val data = data
        var pos = pos
        val s = (data[pos++] and 0xff shl 8 or (data[pos++] and 0xff)).toShort()
        this.pos = pos
        return s
    }

    internal override fun readInt(): Int {
        val data = data
        var pos = pos
        val i = (
                data[pos++] and 0xff shl 24
                        or (data[pos++] and 0xff shl 16)
                        or (data[pos++] and 0xff shl 8)
                        or (data[pos++] and 0xff)
                )
        this.pos = pos
        return i
    }

    internal override fun readLong(): Long {
        val data = data
        var pos = pos
        val v = (
                data[pos++] and 0xffL shl 56
                        or (data[pos++] and 0xffL shl 48)
                        or (data[pos++] and 0xffL shl 40)
                        or (data[pos++] and 0xffL shl 32)
                        or (data[pos++] and 0xffL shl 24)
                        or (data[pos++] and 0xffL shl 16)
                        or (data[pos++] and 0xffL shl 8)
                        or (data[pos++] and 0xffL)
                )
        this.pos = pos
        return v
    }

    internal override fun readTo(dst: ByteArray, dstStartOffset: Int, dstEndOffset: Int) {
        val len = dstEndOffset - dstStartOffset
        require(len in 0 .. size)
        data.copyInto(dst, dstStartOffset, pos, pos + len)
        pos += len
    }

    internal override fun write(src: ByteArray, srcStartOffset: Int, srcEndOffset: Int) {
        require(srcEndOffset - srcStartOffset in 0 .. remainingCapacity)
        src.copyInto(data, limit, srcStartOffset, srcEndOffset)
        limit += srcEndOffset - srcStartOffset
    }

    /**
     * Returns value at [index]-position within this segment.
     * [index] value must be in range `[0, Segment.size)`.
     *
     * Unlike [Segment.readByte], this method does not affect [Segment.size], i.e., it does not consume value from
     * the segment.
     *
     * @param index the index of byte to read from the segment.
     *
     * @throws IllegalArgumentException if [index] is negative or greater or equal to [Segment.size].
     */
    public override fun getChecked(index: Int): Byte {
        require(index in 0 until size)
        return data[pos + index]
    }

    internal override fun getUnchecked(index: Int): Byte {
        return data[pos + index]
    }

    internal override fun setChecked(index: Int, value: Byte) {
        require(index in 0 until remainingCapacity)
        data[limit + index] = value
    }

    @PublishedApi
    internal override fun setUnchecked(index: Int, value: Byte) {
        data[limit + index] = value
    }

    @PublishedApi
    internal override fun setUnchecked(index: Int, b0: Byte, b1: Byte) {
        val d = data
        val l = limit
        d[l + index] = b0
        d[l + index + 1] = b1
    }

    @PublishedApi
    internal override fun setUnchecked(index: Int, b0: Byte, b1: Byte, b2: Byte) {
        val d = data
        val l = limit
        d[l + index] = b0
        d[l + index + 1] = b1
        d[l + index + 2] = b2
    }

    @PublishedApi
    internal override fun setUnchecked(index: Int, b0: Byte, b1: Byte, b2: Byte, b3: Byte) {
        val d = data
        val l = limit
        d[l + index] = b0
        d[l + index + 1] = b1
        d[l + index + 2] = b2
        d[l + index + 3] = b3
    }
}
