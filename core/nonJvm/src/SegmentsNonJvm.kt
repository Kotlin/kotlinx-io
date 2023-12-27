/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io


public actual class Segment {
    internal val data: ByteArray

    @PublishedApi
    internal actual val rawData: Any
        get() = data

    /** The next byte of application data byte to read in this segment. */
    @PublishedApi
    internal actual var pos: Int = 0

    /**
     * The first byte of available data ready to be written to.
     *
     * If the segment is free and linked in the segment pool, the field contains total
     * byte count of this and next segments.
     */
    @PublishedApi
    internal actual var limit: Int = 0

    /** True if other segments or byte strings use the same byte array. */
    internal actual var shared: Boolean = false

    /** True if this segment owns the byte array and can append to it, extending `limit`. */
    internal actual var owner: Boolean = false

    /**
     * Next segment or `null` if this segment is the tail of a list.
     */
    internal actual var next: Segment? = null

    /**
     * Previous segment or `null` if this segment is the head of a list.
     */
    internal actual var prev: Segment? = null

    internal actual constructor() {
        this.data = ByteArray(SIZE)
        this.owner = true
        this.shared = false
    }

    internal constructor(data: ByteArray, pos: Int, limit: Int, shared: Boolean, owner: Boolean) {
        this.data = data
        this.pos = pos
        this.limit = limit
        this.shared = shared
        this.owner = owner
    }

    /**
     * Returns a new segment that shares the underlying byte array with this. Adjusting pos and limit
     * are safe but writes are forbidden. This also marks the current segment as shared, which
     * prevents it from being pooled.
     */
    internal actual fun sharedCopy(): Segment {
        shared = true
        return Segment(data, pos, limit, true, false)
    }

    /** Returns a new segment that its own private copy of the underlying byte array.  */
    internal fun unsharedCopy() = Segment(data.copyOf(), pos, limit, false, true)

    /**
     * Removes this segment of a segments list and returns its successor.
     * Returns null if the list is now empty.
     */
    @PublishedApi
    internal actual fun pop(): Segment? {
        val result = this.next
        if (this.prev != null) {
            this.prev!!.next = this.next
        }
        if (this.next != null) {
            this.next!!.prev = this.prev
        }
        this.next = null
        this.prev = null
        return result
    }

    /**
     * Appends `segment` after this segment in the list. Returns the pushed segment.
     */
    internal actual fun push(segment: Segment): Segment {
        segment.prev = this
        segment.next = this.next
        if (this.next != null) {
            this.next!!.prev = segment
        }
        this.next = segment
        return segment
    }

    /**
     * Splits this head of a list into two segments. The first segment contains the
     * data in `[pos..pos+byteCount)`. The second segment contains the data in
     * `[pos+byteCount..limit)`. This can be useful when moving partial segments from one buffer to
     * another.
     *
     * Returns the new head of the list.
     */
    internal actual fun split(byteCount: Int): Segment {
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
            prefix = SegmentPool.take()
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

    /**
     * Call this when the tail and its predecessor may both be less than half full. This will copy
     * data so that segments can be recycled.
     */
    internal actual fun compact(): Segment {
        check(this.prev !== null) { "cannot compact" }
        if (!this.prev!!.owner) return this // Cannot compact: prev isn't writable.
        val byteCount = limit - pos
        val availableByteCount = SIZE - this.prev!!.limit + if (this.prev!!.shared) 0 else this.prev!!.pos
        if (byteCount > availableByteCount) return this // Cannot compact: not enough writable space.
        val predecessor = this.prev
        writeTo(predecessor!!, byteCount)
        val successor = pop()
        check(successor === null)
        SegmentPool.recycle(this)
        return predecessor
    }

    /** Moves `byteCount` bytes from this segment to `sink`.  */
    internal actual fun writeTo(sink: Segment, byteCount: Int) {
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
     * Number of readable bytes within the segment.
     */
    public actual val size: Int
        get() = limit - pos

    /**
     * Number of bytes that could be written in the segment.
     */
    public actual val remainingCapacity: Int
        get() = data.size - limit

    internal actual fun writeByte(byte: Byte) {
        data[limit++] = byte
    }

    internal actual fun writeShort(short: Short) {
        val data = data
        var limit = limit
        data[limit++] = (short.toInt() ushr 8 and 0xff).toByte()
        data[limit++] = (short.toInt() and 0xff).toByte()
        this.limit = limit
    }

    internal actual fun writeInt(int: Int) {
        val data = data
        var limit = limit
        data[limit++] = (int ushr 24 and 0xff).toByte()
        data[limit++] = (int ushr 16 and 0xff).toByte()
        data[limit++] = (int ushr 8 and 0xff).toByte()
        data[limit++] = (int and 0xff).toByte()
        this.limit = limit
    }

    internal actual fun writeLong(long: Long) {
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

    internal actual fun readByte(): Byte {
        return data[pos++]
    }

    internal actual fun readShort(): Short {
        val data = data
        var pos = pos
        val s = (data[pos++] and 0xff shl 8 or (data[pos++] and 0xff)).toShort()
        this.pos = pos
        return s
    }

    internal actual fun readInt(): Int {
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

    internal actual fun readLong(): Long {
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

    internal actual fun readTo(dst: ByteArray, dstStartOffset: Int, dstEndOffset: Int) {
        val len = dstEndOffset - dstStartOffset
        require(len in 0 .. size)
        data.copyInto(dst, dstStartOffset, pos, pos + len)
        pos += len
    }

    internal actual fun write(src: ByteArray, srcStartOffset: Int, srcEndOffset: Int) {
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
    public actual fun getChecked(index: Int): Byte {
        require(index in 0 until size)
        return data[pos + index]
    }

    internal actual fun getUnchecked(index: Int): Byte {
        return data[pos + index]
    }

    internal actual fun setChecked(index: Int, value: Byte) {
        require(index in 0 until remainingCapacity)
        data[limit + index] = value
    }

    @PublishedApi
    internal actual fun setUnchecked(index: Int, value: Byte) {
        data[limit + index] = value
    }

    @PublishedApi
    internal actual fun setUnchecked(index: Int, b0: Byte, b1: Byte) {
        val d = data
        val l = limit
        d[l + index] = b0
        d[l + index + 1] = b1
    }

    @PublishedApi
    internal actual fun setUnchecked(index: Int, b0: Byte, b1: Byte, b2: Byte) {
        val d = data
        val l = limit
        d[l + index] = b0
        d[l + index + 1] = b1
        d[l + index + 2] = b2
    }

    @PublishedApi
    internal actual fun setUnchecked(index: Int, b0: Byte, b1: Byte, b2: Byte, b3: Byte) {
        val d = data
        val l = limit
        d[l + index] = b0
        d[l + index + 1] = b1
        d[l + index + 2] = b2
        d[l + index + 3] = b3
    }

    internal actual companion object {
        /** The size of all segments in bytes.  */
        internal actual const val SIZE = 8192

        /** Segments will be shared when doing so avoids `arraycopy()` of this many bytes.  */
        internal actual const val SHARE_MINIMUM = 1024
    }
}
