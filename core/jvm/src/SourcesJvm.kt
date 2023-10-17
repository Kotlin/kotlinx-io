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

import java.io.EOFException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.charset.Charset

private fun Buffer.readStringImpl(byteCount: Long, charset: Charset): String {
    require(byteCount >= 0 && byteCount <= Int.MAX_VALUE) {
        "byteCount ($byteCount) is not within the range [0..${Int.MAX_VALUE})"
    }
    if (size < byteCount) {
        throw EOFException("Buffer contains less bytes then required (byteCount: $byteCount, size: $size)")
    }
    if (byteCount == 0L) return ""

    val s = head!!
    if (s.pos + byteCount > s.limit) {
        // If the string spans multiple segments, delegate to readBytes().
        return String(readByteArray(byteCount.toInt()), charset)
    }

    val result = s.withContainedData { data, pos, _ ->
        when (data) {
            is ByteArray -> {
                String(data, pos, byteCount.toInt(), charset)
            }
            else -> {
                TODO()
                /*
                val ba = ByteArray(byteCount.toInt())
                for (idx in 0 until byteCount.toInt()) {
                    ba[idx] = s[idx]
                }
                String(ba, pos, byteCount.toInt(), charset)
                 */
            }
        }
    }
    s.pos += byteCount.toInt()
    sizeField -= byteCount

    if (s.pos == s.limit) {
        // TODO: update tail too
        headField = s.pop()
        SegmentPool.recycle(s)
    }

    return result
}

/**
 * Decodes whole content of this stream into a string using [charset]. Returns empty string if the source is exhausted.
 *
 * @param charset the [Charset] to use for string decoding.
 *
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoSamplesJvm.readWriteStrings
 */
@OptIn(InternalIoApi::class)
public fun Source.readString(charset: Charset): String {
    var req = 1L
    while (request(req)) {
        req *= 2
    }
    return buffer.readStringImpl(buffer.size, charset)
}

/**
 * Decodes [byteCount] bytes of this stream into a string using [charset].
 *
 * @param byteCount the number of bytes to read from the source for decoding.
 * @param charset the [Charset] to use for string decoding.
 *
 * @throws EOFException when the source exhausted before [byteCount] bytes could be read from it.
 * @throws IllegalStateException when the source is closed.
 * @throws IllegalArgumentException if [byteCount] is negative or its value is greater than [Int.MAX_VALUE].
 *
 * @sample kotlinx.io.samples.KotlinxIoSamplesJvm.readStringBounded
 */
@OptIn(InternalIoApi::class)
public fun Source.readString(byteCount: Long, charset: Charset): String {
    require(byteCount)
    return buffer.readStringImpl(byteCount, charset)
}

/**
 * Returns an input stream that reads from this source. Closing the stream will also close this source.
 *
 * @sample kotlinx.io.samples.KotlinxIoSamplesJvm.asStream
 */
@OptIn(InternalIoApi::class)
public fun Source.asInputStream(): InputStream {
    val isClosed: () -> Boolean = when (this) {
        is RealSource -> this::closed
        is Buffer -> {
            { false }
        }
    }

    return object : InputStream() {
        override fun read(): Int {
            if (isClosed()) throw IOException("Underlying source is closed.")
            if (exhausted()) {
                return -1
            }
            return readByte() and 0xff
        }

        override fun read(data: ByteArray, offset: Int, byteCount: Int): Int {
            if (isClosed()) throw IOException("Underlying source is closed.")
            checkOffsetAndCount(data.size.toLong(), offset.toLong(), byteCount.toLong())

            return this@asInputStream.readAtMostTo(data, offset, offset + byteCount)
        }

        override fun available(): Int {
            if (isClosed()) throw IOException("Underlying source is closed.")
            return minOf(buffer.size, Integer.MAX_VALUE).toInt()
        }

        override fun close() = this@asInputStream.close()

        override fun toString() = "${this@asInputStream}.asInputStream()"
    }
}

/**
 * Reads at most [ByteBuffer.remaining] bytes from this source into [sink] and returns the number of bytes read.
 *
 * @param sink the sink to write the data to.
 *
 * @throws IllegalStateException when the source is closed.
 *
 * @sample kotlinx.io.samples.KotlinxIoSamplesJvm.readWriteByteBuffer
 */
@OptIn(InternalIoApi::class)
public fun Source.readAtMostTo(sink: ByteBuffer): Int {
    if (buffer.size == 0L) {
        request(Segment.SIZE.toLong())
        if (buffer.size == 0L) return -1
    }

    return buffer.readAtMostTo(sink)
}

/**
 * Returns [ReadableByteChannel] backed by this source. Closing the source will close the source.
 */
public fun Source.asByteChannel(): ReadableByteChannel {
    val isClosed: () -> Boolean = when (this) {
        is RealSource -> this::closed
        is Buffer -> {
            { false }
        }
    }

    return object : ReadableByteChannel {
        override fun close() {
            this@asByteChannel.close()
        }

        override fun isOpen(): Boolean = !isClosed()

        override fun read(sink: ByteBuffer): Int = this@asByteChannel.readAtMostTo(sink)
    }
}
