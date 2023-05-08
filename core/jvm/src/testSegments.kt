/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

class ByteArraySegment2 : Segment {
    val data: ByteArray

    constructor(pool: SegmentPool) : super(pool) {
        data = ByteArray(SIZE)
    }

    constructor(data: ByteArray, pos: Int, limit: Int, shared: Boolean, owner: Boolean, pool: SegmentPool)
            : super(pos, limit, shared, owner, pool) {
        this.data = data
    }

    override fun get(idx: Int): Byte = data[idx]

    override fun set(idx: Int, byte: Byte) {
        data[idx] = byte
    }

    override fun sharedCopy(): Segment {
        shared = true
        return ByteArraySegment2(data, pos, limit, true, false, pool)
    }

    override fun unsharedCopy(): Segment = ByteArraySegment2(data.copyOf(), pos, limit, false, true, pool)
    override fun readLong(): Long {
        var p = pos
        return (data[p++] and 0xffL shl 56).
        or (data[p++] and 0xffL shl 48).
        or (data[p++] and 0xffL shl 40).
        or (data[p++] and 0xffL shl 32).
        or (data[p++] and 0xffL shl 24).
        or (data[p++] and 0xffL shl 16).
        or (data[p++] and 0xffL shl 8).
        or (data[p] and 0xffL)
    }

    override fun writeLong(v: Long) {
        var p = limit
        data[p++] = (v ushr 56 and 0xffL).toByte()
        data[p++] = (v ushr 48 and 0xffL).toByte()
        data[p++] = (v ushr 40 and 0xffL).toByte()
        data[p++] = (v ushr 32 and 0xffL).toByte()
        data[p++] = (v ushr 24 and 0xffL).toByte()
        data[p++] = (v ushr 16 and 0xffL).toByte()
        data[p++] = (v ushr 8 and 0xffL).toByte()
        data[p] = (v and 0xffL).toByte()
    }
}

class ByteArraySegment3 : Segment {
    val data: ByteArray

    constructor(pool: SegmentPool) : super(pool) {
        data = ByteArray(SIZE)
    }

    constructor(data: ByteArray, pos: Int, limit: Int, shared: Boolean, owner: Boolean, pool: SegmentPool)
            : super(pos, limit, shared, owner, pool) {
        this.data = data
    }

    override fun get(idx: Int): Byte = data[idx]

    override fun set(idx: Int, byte: Byte) {
        data[idx] = byte
    }

    override fun sharedCopy(): Segment {
        shared = true
        return ByteArraySegment3(data, pos, limit, true, false, pool)
    }

    override fun unsharedCopy(): Segment = ByteArraySegment3(data.copyOf(), pos, limit, false, true, pool)

    override fun readLong(): Long {
        var p = pos
        return (data[p++] and 0xffL shl 56).
        or (data[p++] and 0xffL shl 48).
        or (data[p++] and 0xffL shl 40).
        or (data[p++] and 0xffL shl 32).
        or (data[p++] and 0xffL shl 24).
        or (data[p++] and 0xffL shl 16).
        or (data[p++] and 0xffL shl 8).
        or (data[p] and 0xffL)
    }

    override fun writeLong(v: Long) {
        var p = limit
        data[p++] = (v ushr 56 and 0xffL).toByte()
        data[p++] = (v ushr 48 and 0xffL).toByte()
        data[p++] = (v ushr 40 and 0xffL).toByte()
        data[p++] = (v ushr 32 and 0xffL).toByte()
        data[p++] = (v ushr 24 and 0xffL).toByte()
        data[p++] = (v ushr 16 and 0xffL).toByte()
        data[p++] = (v ushr 8 and 0xffL).toByte()
        data[p] = (v and 0xffL).toByte()
    }
}

class ByteArraySegment4 : Segment {
    val data: ByteArray

    constructor(pool: SegmentPool) : super(pool) {
        data = ByteArray(SIZE)
    }

    constructor(data: ByteArray, pos: Int, limit: Int, shared: Boolean, owner: Boolean, pool: SegmentPool)
            : super(pos, limit, shared, owner, pool) {
        this.data = data
    }

    override fun get(idx: Int): Byte = data[idx]

    override fun set(idx: Int, byte: Byte) {
        data[idx] = byte
    }

    override fun sharedCopy(): Segment {
        shared = true
        return ByteArraySegment4(data, pos, limit, true, false, pool)
    }

    override fun unsharedCopy(): Segment = ByteArraySegment4(data.copyOf(), pos, limit, false, true, pool)

    override fun readLong(): Long {
        var p = pos
        return (data[p++] and 0xffL shl 56).
        or (data[p++] and 0xffL shl 48).
        or (data[p++] and 0xffL shl 40).
        or (data[p++] and 0xffL shl 32).
        or (data[p++] and 0xffL shl 24).
        or (data[p++] and 0xffL shl 16).
        or (data[p++] and 0xffL shl 8).
        or (data[p] and 0xffL)
    }

    override fun writeLong(v: Long) {
        var p = limit
        data[p++] = (v ushr 56 and 0xffL).toByte()
        data[p++] = (v ushr 48 and 0xffL).toByte()
        data[p++] = (v ushr 40 and 0xffL).toByte()
        data[p++] = (v ushr 32 and 0xffL).toByte()
        data[p++] = (v ushr 24 and 0xffL).toByte()
        data[p++] = (v ushr 16 and 0xffL).toByte()
        data[p++] = (v ushr 8 and 0xffL).toByte()
        data[p] = (v and 0xffL).toByte()
    }
}