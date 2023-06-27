/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.indices
import kotlinx.io.bytestring.isEmpty
import kotlinx.io.bytestring.unsafe.UnsafeByteStringApi
import kotlinx.io.bytestring.unsafe.UnsafeByteStringOperations
import kotlin.math.min

/**
 * Writes subsequence of data from [byteString] starting at [startIndex] and ending at [endIndex] into a sink.
 *
 * @param byteString the byte string whose subsequence should be written to a sink.
 * @param startIndex the first index (inclusive) to copy data from the [byteString].
 * @param endIndex the last index (exclusive) to copy data from the [byteString]
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [byteString] indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalStateException if the sink is closed.
 */
@OptIn(DelicateIoApi::class)
public fun Sink.write(byteString: ByteString, startIndex: Int = 0, endIndex: Int = byteString.size) {
    checkBounds(byteString.size, startIndex, endIndex)

    writeToInternalBuffer { buffer ->
        var offset = startIndex
        while (offset < endIndex) {
            val bytesToWrite = min(endIndex - offset, Segment.SIZE)
            val seg = buffer.writableSegment(bytesToWrite)
            byteString.copyInto(seg.data, seg.limit, offset, offset + bytesToWrite)
            seg.limit += bytesToWrite
            buffer.size += bytesToWrite
            offset += bytesToWrite
        }
    }
}

/**
 * Consumes all bytes from this source and wraps it into a byte string.
 *
 * @throws IllegalStateException if the source is closed.
 */
@OptIn(UnsafeByteStringApi::class)
public fun Source.readByteString(): ByteString {
    return UnsafeByteStringOperations.wrapUnsafe(readByteArray())
}

/**
 * Consumes exactly [byteCount] bytes from this source and wraps it into a byte string.
 *
 * @param byteCount the number of bytes to read from the source.
 *
 * @throws EOFException when the source is exhausted before reading [byteCount] bytes from it.
 * @throws IllegalArgumentException when [byteCount] is negative.
 * @throws IllegalStateException if the source is closed.
 */
@OptIn(UnsafeByteStringApi::class)
public fun Source.readByteString(byteCount: Int): ByteString {
    return UnsafeByteStringOperations.wrapUnsafe(readByteArray(byteCount))
}

/**
 * Returns the index of the first match for [byteString] in the source at or after [startIndex]. This
 * expands the source's buffer as necessary until [byteString] is found. This reads an unbounded number of
 * bytes into the buffer. Returns `-1` if the stream is exhausted before the requested bytes are found.
 *
 * @param byteString the sequence of bytes to find within the source.
 * @param startIndex the index into the source to start searching from.
 *
 * @throws IllegalArgumentException if [startIndex] is negative.
 * @throws IllegalStateException if the source is closed.
 */
@OptIn(InternalIoApi::class)
public fun Source.indexOf(byteString: ByteString, startIndex: Long = 0): Long {
    require(startIndex >= 0) { "startIndex: $startIndex" }

    if (byteString.isEmpty()) {
        return 0
    }

    var offset = startIndex
    val peek = peek()
    if (!request(startIndex)) {
        return -1L
    }
    peek.skip(offset)
    while (!peek.exhausted()) {
        val index = peek.indexOf(byteString[0])
        if (index == -1L) {
            return -1L
        }
        offset += index
        peek.skip(index)
        if (!peek.request(byteString.size.toLong())) {
            return -1L
        }

        var matches = true
        for (idx in byteString.indices) {
            if (byteString[idx] != peek.buffer[idx.toLong()]) {
                matches = false
                offset++
                peek.skip(1)
                break
            }
        }
        if (matches) {
            return offset
        }
    }
    return -1L
}
