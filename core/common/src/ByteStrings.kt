/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import kotlinx.io.bytestring.ByteString
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
 *
 * @sample kotlinx.io.samples.ByteStringSamples.writeByteString
 */
@OptIn(DelicateIoApi::class, UnsafeByteStringApi::class)
public fun Sink.write(byteString: ByteString, startIndex: Int = 0, endIndex: Int = byteString.size) {
    checkBounds(byteString.size, startIndex, endIndex)
    if (endIndex == startIndex) {
        return
    }

    writeToInternalBuffer { buffer ->
        var offset = startIndex
        val tail = buffer.tail
        if (tail != null) {
            val bytesToWrite = min(tail.capacity, endIndex - offset)
            UnsafeByteStringOperations.withByteArrayUnsafe(byteString) {
                tail.write(it, offset, offset + bytesToWrite)
            }
            offset += bytesToWrite
            buffer.size += bytesToWrite
        }
        while (offset < endIndex) {
            val segment = buffer.writableSegment(1)
            val bytesToWrite = min(endIndex - offset, segment.capacity)
            UnsafeByteStringOperations.withByteArrayUnsafe(byteString) { data ->
                segment.write(data, offset, offset + bytesToWrite)
            }
            offset += bytesToWrite
            buffer.size += bytesToWrite
        }
    }
}

/**
 * Consumes all bytes from this source and wraps it into a byte string.
 *
 * @throws IllegalStateException if the source is closed.
 *
 * @sample kotlinx.io.samples.ByteStringSamples.readByteString
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
 *
 * @sample kotlinx.io.samples.ByteStringSamples.readByteString
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
 *
 * @sample kotlinx.io.samples.ByteStringSamples.indexOfByteString
 */
@OptIn(InternalIoApi::class, UnsafeByteStringApi::class)
public fun Source.indexOf(byteString: ByteString, startIndex: Long = 0): Long {
    require(startIndex >= 0) { "startIndex: $startIndex" }

    if (byteString.isEmpty()) {
        return 0
    }

    var offset = startIndex
    while (request(offset + byteString.size)) {
        val idx = buffer.indexOf(byteString, offset)
        if (idx < 0) {
            // The buffer does not contain the pattern, let's try fetching at least one extra byte
            // and start a new search attempt so that the pattern would fit in the suffix of
            // the current buffer + 1 extra byte.
            offset = buffer.size - byteString.size + 1
        } else {
            return idx
        }
    }
    return -1
}

@OptIn(UnsafeByteStringApi::class)
public fun Buffer.indexOf(byteString: ByteString, startIndex: Long = 0): Long {
    require(startIndex <= size) {
        "startIndex ($startIndex) should not exceed size ($size)"
    }
    if (byteString.isEmpty()) return 0
    if (startIndex > size - byteString.size) return -1L

    UnsafeByteStringOperations.withByteArrayUnsafe(byteString) { byteStringData ->
        seek(startIndex) { seg, o ->
            if (o == -1L) {
                return -1L
            }
            var segment = seg!!
            var offset = o
            do {
                // If start index within this segment, the diff will be positive and
                // we'll scan the segment starting from the corresponding offset.
                // Otherwise, the diff will be negative and we'll scan the segment from the beginning.
                val startOffset = maxOf((startIndex - offset).toInt(), 0)
                // Try to search the pattern within the current segment.
                val idx = segment.indexOfBytesInbound(byteStringData, startOffset)
                if (idx != -1) {
                    // The offset corresponds to the segment's start, idx - to offset within the segment.
                    return offset + idx.toLong()
                }
                // firstOutboundOffset corresponds to a first byte starting reading the pattern from which
                // will result in running out of the current segment bounds.
                val firstOutboundOffset = maxOf(startOffset, segment.size - byteStringData.size + 1)
                // Try to find a pattern in all suffixes shorter than the pattern. These suffixes start
                // in the current segment, but ends in the following segments; thus we're using outbound function.
                val idx1 = segment.indexOfBytesOutbound(byteStringData, firstOutboundOffset, head)
                if (idx1 != -1) {
                    // Offset corresponds to the segment's start, idx - to offset within the segment.
                    return offset + idx1.toLong()
                }

                // We scanned the whole segment, so let's go to the next one
                offset += segment.size
                segment = segment.next!!
            } while (segment !== head && offset + byteString.size <= size)
            return -1L
        }
    }
    return -1
}
