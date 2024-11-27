/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.okio

import kotlinx.io.Buffer
import kotlinx.io.InternalIoApi
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.UnsafeIoApi
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.isEmpty
import kotlinx.io.bytestring.unsafe.UnsafeByteStringApi
import kotlinx.io.bytestring.unsafe.UnsafeByteStringOperations
import kotlinx.io.unsafe.UnsafeBufferOperations
import okio.ByteString.Companion.toByteString
import okio.Timeout
import kotlin.math.min

private inline fun <T> withOkio2KxIOExceptionMapping(block: () -> T): T {
    try {
        return block()
    } catch (eofe: okio.EOFException) {
        throw kotlinx.io.EOFException(eofe.message)
    } catch (ioe: okio.IOException) {
        throw kotlinx.io.IOException(ioe.message, ioe)
    }
}

private inline fun <T> withKxIO2OkioExceptionMapping(block: () -> T): T {
    try {
        return block()
    } catch (eofe: kotlinx.io.EOFException) {
        throw okio.EOFException(eofe.message)
    } catch (ioe: kotlinx.io.IOException) {
        throw okio.IOException(ioe.message, ioe)
    }
}

private abstract class RawSource2OkioSourceAdapterBase<T : RawSource>(
    val source: T,
    val buffer: Buffer
) : okio.Source {
    override fun close() = withKxIO2OkioExceptionMapping(source::close)
    override fun timeout(): Timeout = Timeout.NONE

    override fun read(sink: okio.Buffer, byteCount: Long): Long = withKxIO2OkioExceptionMapping {
        val fetched = fetchData(byteCount)
        if (fetched <= 0) return fetched

        var remaining = fetched

        while (remaining > 0) {
            @OptIn(UnsafeIoApi::class)
            UnsafeBufferOperations.readFromHead(buffer) { data, from, to ->
                val len = min((to - from).toLong(), remaining).toInt()
                sink.write(data, from, len)
                remaining -= len
                len
            }
        }
        return fetched
    }

    abstract fun fetchData(fetchAtMost: Long): Long
}

private abstract class RawSink2OkioSinkAdapterBase<T : RawSink>(
    val sink: T,
    val buffer: Buffer
) : okio.Sink {
    override fun close() = withKxIO2OkioExceptionMapping(sink::close)
    override fun flush() = withKxIO2OkioExceptionMapping(sink::flush)
    override fun timeout(): Timeout = Timeout.NONE

    override fun write(source: okio.Buffer, byteCount: Long) = withKxIO2OkioExceptionMapping {
        require(source.size >= byteCount) {
            "Buffer does not contain enough bytes to write. Requested $byteCount, actual size is ${source.size}"
        }
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
        flushBuffer()
    }

    abstract fun flushBuffer()
}

private class RawSource2OkioSourceAdapter(source: RawSource) :
    RawSource2OkioSourceAdapterBase<RawSource>(source, Buffer()) {
    override fun fetchData(fetchAtMost: Long): Long = source.readAtMostTo(buffer, fetchAtMost)
}

@OptIn(InternalIoApi::class)
private class Source2OkioSourceAdapter(source: Source) :
    RawSource2OkioSourceAdapterBase<Source>(source, source.buffer) {
    override fun fetchData(fetchAtMost: Long): Long {
        // if there's no cached data and source is exhausted, we're done
        if (buffer.exhausted() && source.exhausted()) {
            return -1L
        }
        // the buffer is empty, but the source is not exhausted yet
        if (buffer.size == 0L) {
            return 0L
        }

        return min(fetchAtMost, buffer.size)
    }
}

private class RawSink2OkioSinkAdapter(sink: RawSink) : RawSink2OkioSinkAdapterBase<RawSink>(sink, Buffer()) {
    override fun flushBuffer() = sink.write(buffer, buffer.size)
}

@OptIn(InternalIoApi::class)
private class Sink2OkioSinkAdapter(sink: Sink) : RawSink2OkioSinkAdapterBase<Sink>(sink, sink.buffer) {
    override fun flushBuffer() = sink.hintEmit()
}

private abstract class OkioSource2RawSourceAdapterBase<T : okio.Source>(
    val source: T,
    val buffer: okio.Buffer
) : RawSource {
    override fun close() = withOkio2KxIOExceptionMapping(source::close)

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        val readBytes = fetchData(byteCount)
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
    }

    abstract fun fetchData(byteCount: Long): Long
}

private abstract class OkioSink2RawSinkAdapterBase<T : okio.Sink>(
    val sink: T,
    val buffer: okio.Buffer
) : RawSink {
    override fun flush() = withOkio2KxIOExceptionMapping(sink::flush)
    override fun close() = withOkio2KxIOExceptionMapping(sink::close)

    override fun write(source: Buffer, byteCount: Long) {
        require(source.size >= byteCount) {
            "Buffer does not contain enough bytes to write. Requested $byteCount, actual size is ${source.size}"
        }
        var remaining = byteCount
        while (remaining > 0) {
            @OptIn(UnsafeIoApi::class)
            UnsafeBufferOperations.readFromHead(source) { data, from, to ->
                val toRead = min((to - from).toLong(), remaining).toInt()
                remaining -= toRead
                buffer.write(data, from, toRead)
                toRead
            }
        }

        flushBuffer()
    }

    abstract fun flushBuffer()
}

private class OkioSink2RawSinkAdapter(sink: okio.Sink) : OkioSink2RawSinkAdapterBase<okio.Sink>(sink, okio.Buffer()) {
    override fun flushBuffer() = sink.write(buffer, buffer.size)
}

private class OkioBufferedSink2RawSinkAdapter(sink: okio.BufferedSink) :
    OkioSink2RawSinkAdapterBase<okio.BufferedSink>(sink, sink.buffer) {
    override fun flushBuffer() {
        sink.emitCompleteSegments()
    }
}

private class OkioSource2RawSourceAdapter(source: okio.Source) :
    OkioSource2RawSourceAdapterBase<okio.Source>(source, okio.Buffer()) {
    override fun fetchData(byteCount: Long): Long = source.read(buffer, byteCount)
}

private class OkioBufferedSource2RawSourceAdapter(source: okio.BufferedSource) :
    OkioSource2RawSourceAdapterBase<okio.BufferedSource>(source, source.buffer) {
    override fun fetchData(byteCount: Long): Long {
        if (buffer.exhausted() && source.exhausted()) {
            return -1L
        }
        if (buffer.size == 0L) {
            return 0L
        }
        return min(buffer.size, byteCount)
    }
}

/**
 * Returns a [okio.Source] backed by this [kotlinx.io.RawSource].
 *
 * Closing one of these sources will also close another one.
 */
public fun RawSource.asOkioSource(): okio.Source = when (this) {
    is Source -> Source2OkioSourceAdapter(this)
    else -> RawSource2OkioSourceAdapter(this)
}

/**
 * Returns a [okio.Sink] backed by this [kotlinx.io.RawSink].
 *
 * Closing one of these sinks will also close another one.
 */
public fun RawSink.asOkioSink(): okio.Sink = when (this) {
    is Sink -> Sink2OkioSinkAdapter(this)
    else -> RawSink2OkioSinkAdapter(this)
}

/**
 * Returns a [kotlinx.io.RawSink] backed by this [okio.Sink].
 *
 * Closing one of these sinks will also close another one.
 */
public fun okio.Sink.asKotlinxIoRawSink(): RawSink = when (this) {
    is okio.BufferedSink -> OkioBufferedSink2RawSinkAdapter(this)
    else -> OkioSink2RawSinkAdapter(this)
}

/**
 * Returns a [kotlinx.io.RawSource] backed by this [okio.Source].
 *
 * Closing one of these sources will also close another one.
 */
public fun okio.Source.asKotlinxIoRawSource(): RawSource = when (this) {
    is okio.BufferedSource -> OkioBufferedSource2RawSourceAdapter(this)
    else -> OkioSource2RawSourceAdapter(this)
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
