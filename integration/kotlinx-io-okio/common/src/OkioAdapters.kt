/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.okio

import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.UnsafeIoApi
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.isEmpty
import kotlinx.io.bytestring.unsafe.UnsafeByteStringApi
import kotlinx.io.bytestring.unsafe.UnsafeByteStringOperations
import kotlinx.io.unsafe.UnsafeBufferOperations
import okio.ByteString.Companion.toByteString
import kotlin.math.min

/**
 * Returns a [okio.Source] backed by this [kotlinx.io.RawSource].
 *
 * Closing one of these sources will also close another one.
 */
public fun RawSource.asOkioSource(): okio.Source = object : okio.Source {
    private val buffer = Buffer()

    override fun close() {
        try {
            this@asOkioSource.close()
        } catch (e: kotlinx.io.IOException) {
            throw okio.IOException(e.message, e)
        }
    }

    override fun read(sink: okio.Buffer, byteCount: Long): Long {
        try {
            val read = this@asOkioSource.readAtMostTo(buffer, byteCount)
            if (read <= 0) return read

            while (!buffer.exhausted()) {
                @OptIn(UnsafeIoApi::class)
                UnsafeBufferOperations.readFromHead(buffer) { data, from, to ->
                    val len = to - from
                    sink.write(data, from, len)
                    len
                }
            }
            return read
        } catch (eofe: kotlinx.io.EOFException) {
            throw okio.EOFException(eofe.message)
        } catch (ioe: kotlinx.io.IOException) {
            throw okio.IOException(ioe.message, ioe)
        }
    }

    override fun timeout(): okio.Timeout = okio.Timeout.NONE
}

/**
 * Returns a [okio.Sink] backed by this [kotlinx.io.RawSink].
 *
 * Closing one of these sinks will also close another one.
 */
public fun RawSink.asOkioSink(): okio.Sink = object : okio.Sink {
    private val buffer = Buffer()

    override fun close() {
        try {
            this@asOkioSink.close()
        } catch (e: kotlinx.io.IOException) {
            throw okio.IOException(e.message, e)
        }
    }

    override fun flush() {
        try {
            this@asOkioSink.flush()
        } catch (e: kotlinx.io.IOException) {
            throw okio.IOException(e.message, e)
        }
    }

    override fun timeout(): okio.Timeout = okio.Timeout.NONE

    override fun write(source: okio.Buffer, byteCount: Long) {
        require(source.size >= byteCount) {
            "Buffer does not contain enough bytes to write. Requested $byteCount, actual size is ${source.size}"
        }
        try {
            var remaining = byteCount
            while (remaining > 0) {
                @OptIn(UnsafeIoApi::class)
                UnsafeBufferOperations.writeToTail(buffer, 1) { data, from, to ->
                    val toWrite = min((to - from).toLong(), remaining).toInt()

                    val read = source.read(data, from, toWrite)
                    check(read > 0) { "Can't read $toWrite bytes from okio.Buffer" }
                    remaining -= read
                    read
                }
            }
            this@asOkioSink.write(buffer, buffer.size)
        } catch (e: kotlinx.io.IOException) {
            throw okio.IOException(e.message, e)
        }
    }
}

/**
 * Returns a [kotlinx.io.RawSink] backed by this [okio.Sink].
 *
 * Closing one of these sinks will also close another one.
 */
public fun okio.Sink.asKotlinxIoRawSink(): RawSink = object : RawSink {
    private val buffer = okio.Buffer()

    override fun write(source: Buffer, byteCount: Long) {
        require(source.size >= byteCount) {
            "Buffer does not contain enough bytes to write. Requested $byteCount, actual size is ${source.size}"
        }
        try {
            var remaining = byteCount
            while (remaining > 0) {
                @OptIn(UnsafeIoApi::class)
                UnsafeBufferOperations.readFromHead(source) { data, from, to ->
                    val toRead = min((to - from).toLong(), remaining).toInt()
                    remaining -= toRead
                    buffer.write(data, from, toRead)
                    toRead
                }
                this@asKotlinxIoRawSink.write(buffer, byteCount)
            }
        } catch (e: okio.IOException) {
            throw kotlinx.io.IOException(e.message, e)
        }
    }

    override fun flush() {
        try {
            this@asKotlinxIoRawSink.flush()
        } catch (e: okio.IOException) {
            throw kotlinx.io.IOException(e.message, e)
        }
    }

    override fun close() {
        try {
            this@asKotlinxIoRawSink.close()
        } catch (e: okio.IOException) {
            throw kotlinx.io.IOException(e.message, e)
        }
    }
}

/**
 * Returns a [kotlinx.io.RawSource] backed by this [okio.Source].
 *
 * Closing one of these sources will also close another one.
 */
public fun okio.Source.asKotlinxIoRawSource(): RawSource = object : RawSource {
    private val buffer = okio.Buffer()

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        try {
            val readBytes = this@asKotlinxIoRawSource.read(buffer, byteCount)

            if (readBytes <= 0L) return readBytes

            var remaining = readBytes
            while (remaining > 0) {
                @OptIn(UnsafeIoApi::class)
                UnsafeBufferOperations.writeToTail(sink, 1) { data, from, to ->
                    val toRead = min((to - from).toLong(), remaining).toInt()
                    val read = buffer.read(data, from, toRead)
                    check(read == toRead) { "Can't read $toRead bytes from okio.Buffer" }
                    remaining -= toRead
                    toRead
                }
            }

            return readBytes
        } catch (eofe: okio.EOFException) {
            throw kotlinx.io.EOFException(eofe.message)
        } catch (e: okio.IOException) {
            throw kotlinx.io.IOException(e.message, e)
        }
    }

    override fun close() {
        try {
            this@asKotlinxIoRawSource.close()
        } catch (e: okio.IOException) {
            throw kotlinx.io.IOException(e.message, e)
        }
    }
}

/**
 * Returns a new [kotlinx.io.bytestring.ByteString] instance with
 * exactly the same content as this [okio.ByteString].
 */
public fun okio.ByteString.toKotlinxIoByteString(): ByteString {
    if (size == 0) return ByteString()
    @OptIn(UnsafeByteStringApi::class)
    return UnsafeByteStringOperations.wrapUnsafe(toByteArray())
}

/**
 * Returns a new [okio.ByteString] instance with
 * exactly the same content as this [kotlinx.io.bytestring.ByteString].
 */
public fun ByteString.toOkioByteString(): okio.ByteString {
    if (isEmpty()) return okio.ByteString.EMPTY
    val resultingByteString: okio.ByteString
    @OptIn(UnsafeByteStringApi::class)
    UnsafeByteStringOperations.withByteArrayUnsafe(this) { array ->
        resultingByteString = array.toByteString()
    }
    return resultingByteString
}
