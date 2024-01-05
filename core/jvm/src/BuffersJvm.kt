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

import kotlinx.io.unsafe.UnsafeBufferAccessors
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.channels.WritableByteChannel

/**
 * Read and exhaust bytes from [input] into this buffer. Stops reading data on [input] exhaustion.
 *
 * @param input the stream to read data from.
 *
 * @sample kotlinx.io.samples.KotlinxIoSamplesJvm.bufferTransferToStream
 */
public fun Buffer.transferFrom(input: InputStream): Buffer {
    write(input, Long.MAX_VALUE, true)
    return this
}

/**
 * Read [byteCount] bytes from [input] into this buffer. Throws an exception when [input] is
 * exhausted before reading [byteCount] bytes.
 *
 * @param input the stream to read data from.
 * @param byteCount the number of bytes read from [input].
 *
 * @throws IOException when [input] exhausted before reading [byteCount] bytes from it.
 * @throws IllegalArgumentException when [byteCount] is negative.
 *
 * @sample kotlinx.io.samples.KotlinxIoSamplesJvm.writeInputStreamToBuffer
 */
public fun Buffer.write(input: InputStream, byteCount: Long): Buffer {
    checkByteCount(byteCount)
    write(input, byteCount, false)
    return this
}

@OptIn(UnsafeIoApi::class)
private fun Buffer.write(input: InputStream, byteCount: Long, forever: Boolean) {
    var remainingByteCount = byteCount
    var exchaused = false
    while (!exchaused && (remainingByteCount > 0L || forever)) {
        UnsafeBufferAccessors.writeUnbound(this, 1) {
            val maxToCopy = minOf(remainingByteCount, it.remainingCapacity).toInt()
            it.withContainedData { data, _, limit ->
                when (data) {
                    is ByteArray -> {
                        val bytesRead = input.read(data, limit, maxToCopy)
                        if (bytesRead == -1) {
                            if (!forever) {
                                throw  EOFException("Stream exhausted before $byteCount bytes were read.")
                            }
                            exchaused = true
                            0
                        } else {
                            remainingByteCount -= bytesRead
                            bytesRead
                        }
                    }
                    else -> {
                        /*
                        var bytesRead = 0
                        while (bytesRead < maxToCopy) {
                            val b = input.read()
                            if (b == -1) {
                                if (!forever) {
                                    throw EOFException("Stream exhausted before $byteCount bytes were read.")
                                }
                                exchaused = true
                                break
                            }
                            it[bytesRead++] = b.toByte()
                        }
                        bytesRead
                         */
                        TODO()
                    }
                }
            }
        }

    }
}

/**
 * Consumes [byteCount] bytes from this buffer and writes it to [out].
 *
 * @param out the [OutputStream] to write to.
 * @param byteCount the number of bytes to be written, [Buffer.size] by default.
 *
 * @throws IllegalArgumentException when [byteCount] is negative or exceeds the buffer size.
 *
 * @sample kotlinx.io.samples.KotlinxIoSamplesJvm.bufferTransferToStream
 */
@OptIn(UnsafeIoApi::class)
public fun Buffer.readTo(out: OutputStream, byteCount: Long = size) {
    checkOffsetAndCount(size, 0, byteCount)
    var remainingByteCount = byteCount

    while (remainingByteCount > 0L) {
        val s = this.head
        val toCopy = minOf(remainingByteCount, s!!.size).toInt()

        s.withContainedData { data, pos, _ ->
            when (data) {
                is ByteArray -> {
                    out.write(data, pos, toCopy)
                }
                else -> {
                    TODO()
                    /*
                    for (idx in 0 until toCopy) {
                        out.write(s[idx].toInt())
                    }
                     */
                }
            }
        }
        skip(toCopy.toLong())

        remainingByteCount -= toCopy.toLong()
    }
}

/**
 * Copy bytes from this buffer's subrange, starting at [startIndex] and ending at [endIndex], to [out]. This method
 * does not consume data from the buffer.
 *
 * @param out the destination to copy data into.
 * @param startIndex the index (inclusive) of the first byte to copy, `0` by default.
 * @param endIndex the index (exclusive) of the last byte to copy, `buffer.size` by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of this buffer bounds (`[0..buffer.size)`).
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 *
 * @sample kotlinx.io.samples.KotlinxIoSamplesJvm.copyBufferToOutputStream
 */
@OptIn(UnsafeIoApi::class)
public fun Buffer.copyTo(
    out: OutputStream,
    startIndex: Long = 0L,
    endIndex: Long = size
) {
    checkBounds(size, startIndex, endIndex)
    if (startIndex == endIndex) return

    var currentOffset = startIndex
    var remainingByteCount = endIndex - startIndex

    // Skip segments that we aren't copying from.
    var s = this.head ?: throw IllegalStateException()
    while (currentOffset >= s.limit - s.pos) {
        currentOffset -= (s.limit - s.pos).toLong()
        s = s.next ?: break
    }

    // Copy from one segment at a time.
    while (remainingByteCount > 0L) {
        val pos = (s.pos + currentOffset).toInt()
        val toCopy = minOf(s.limit - pos, remainingByteCount).toInt()
        s.withContainedData { data, _, _ ->
            when (data) {
                is ByteArray -> {
                    out.write(data, pos, toCopy)
                }
                else -> {
                    TODO()
                    /*
                    for (idx in currentOffset until toCopy) {
                        out.write(s[idx.toInt()].toInt())
                    }
                     */
                }
            }
        }
        remainingByteCount -= toCopy.toLong()
        currentOffset = 0L
        s = s.next ?: break
    }
}

/**
 * Writes up to [ByteBuffer.remaining] bytes from this buffer to the sink.
 * Return the number of bytes written.
 *
 * @param sink the sink to write data to.
 *
 * @sample kotlinx.io.samples.KotlinxIoSamplesJvm.readWriteByteBuffer
 */
@OptIn(UnsafeIoApi::class)
public fun Buffer.readAtMostTo(sink: ByteBuffer): Int {
    val s = this.head ?: return -1

    val toCopy = minOf(sink.remaining(), s.size)
    s.withContainedData { data, pos, _ ->
        when (data) {
            is ByteArray -> {
                sink.put(data, pos, toCopy)
            }
            else -> {
                TODO()
                /*
                for (idx in 0 until toCopy) {
                    sink.put(s[idx])
                }
                 */
            }
        }
    }

    skip(toCopy.toLong())
    return toCopy
}

/**
 * Reads all data from [source] into this buffer.
 *
 * @sample kotlinx.io.samples.KotlinxIoSamplesJvm.transferBufferFromByteBuffer
 */
@OptIn(UnsafeIoApi::class)
public fun Buffer.transferFrom(source: ByteBuffer): Buffer {
    val byteCount = source.remaining()
    var remaining = byteCount
    while (remaining > 0) {
        UnsafeBufferAccessors.writeUnbound(this, 1) {
            val toCopy = minOf(remaining, it.remainingCapacity)

            it.withContainedData { data, _, limit ->
                when (data) {
                    is ByteArray -> {
                        source.get(data, limit, toCopy)
                    }
                    else -> {
                        TODO()
                        /*
                        for (idx in 0 until toCopy) {
                            it[idx] = source.get()
                        }
                         */
                    }
                }
            }
            remaining -= toCopy
            toCopy
        }
    }

    return this
}

/**
 * Returns a new [ByteChannel] instance representing this buffer.
 */
public fun Buffer.asByteChannel(): ByteChannel = object : ByteChannel {
    override fun read(sink: ByteBuffer): Int = readAtMostTo(sink)

    override fun write(source: ByteBuffer): Int {
        val sizeBefore = size
        transferFrom(source)
        return (size - sizeBefore).toInt()
    }

    override fun close() {}

    override fun isOpen(): Boolean = true
}

public fun WritableByteChannel.write(buffer: Buffer) {
    var segment = buffer.head
    while (segment != null) {
        val bb = ByteBuffer.wrap(segment.data, segment.pos, segment.limit - segment.pos)
        this.write(bb)
        segment = segment.next
    }
    buffer.clear()
}
