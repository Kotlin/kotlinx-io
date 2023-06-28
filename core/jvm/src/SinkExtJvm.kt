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

import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel
import java.nio.charset.Charset

/**
 * Encodes substring of [string] starting at [startIndex] and ending at [endIndex] using [charset]
 * and writes into this sink.
 *
 * @param string the string to encode into this sink.
 * @param charset the [Charset] to use for encoding.
 * @param startIndex the index of the first character to encode, inclusive, 0 by default.
 * @param endIndex the index of the last character to encode, exclusive, `string.length` by default.
 *
 * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [string] indices.
 * @throws IllegalArgumentException when `startIndex > endIndex`.
 * @throws IllegalStateException when the sink is closed.
 */
public fun Sink.writeString(string: String, charset: Charset, startIndex: Int = 0, endIndex: Int = string.length) {
    checkBounds(string.length, startIndex, endIndex)
    if (charset == Charsets.UTF_8) return writeString(string, startIndex, endIndex)
    val data = string.substring(startIndex, endIndex).toByteArray(charset)
    write(data, 0, data.size)
}

/**
 * Returns an output stream that writes to this sink. Closing the stream will also close this sink.
 */
@OptIn(DelicateIoApi::class)
public fun Sink.asOutputStream(): OutputStream {
    val isClosed: () -> Boolean = when (this) {
        is RealSink -> this::closed
        is Buffer -> {
            { false }
        }
    }

    return object : OutputStream() {
        override fun write(byte: Int) {
            if (isClosed()) throw IOException("Underlying sink is closed")
            writeToInternalBuffer { it.writeByte(byte.toByte()) }
        }

        override fun write(data: ByteArray, offset: Int, byteCount: Int) {
            if (isClosed()) throw IOException("Underlying sink is closed")
            writeToInternalBuffer { it.write(data, offset, offset + byteCount) }
        }

        override fun flush() {
            // For backwards compatibility, a flush() on a closed stream is a no-op.
            if (!isClosed()) {
                this@asOutputStream.flush()
            }
        }

        override fun close() = this@asOutputStream.close()

        override fun toString() = "${this@asOutputStream}.outputStream()"
    }
}

/**
 * Writes data from the [source] into this sink and returns the number of bytes written.
 *
 * @param source the source to read from.
 *
 * @throws IllegalStateException when the sink is closed.
 */
@OptIn(InternalIoApi::class)
public fun Sink.write(source: ByteBuffer): Int {
    val sizeBefore = buffer.size
    buffer.transferFrom(source)
    val bytesRead = buffer.size - sizeBefore
    hintEmit()
    return bytesRead.toInt()
}

/**
 * Returns [WritableByteChannel] backed by this sink. Closing the channel will also close the sink.
 */
public fun Sink.asByteChannel(): WritableByteChannel {
    val isClosed: () -> Boolean = when (this) {
        is RealSink -> this::closed
        is Buffer -> {
            { false }
        }
    }

    return object : WritableByteChannel {
        override fun close() {
            this@asByteChannel.close()
        }

        override fun isOpen(): Boolean = !isClosed()

        override fun write(source: ByteBuffer): Int {
            check(!isClosed()) { "Underlying sink is closed." }
            return this@asByteChannel.write(source)
        }
    }
}
