/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

/*
 * Copyright (C) 2019 Square, Inc.
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

/**
 * A sink that facilitates typed data writes and keeps a buffer internally so that caller can write some data without
 * sending it directly to an upstream.
 *
 * [Sink] is the main `kotlinx-io` interface to write data in client's code,
 * any [RawSink] could be turned into [Sink] using [RawSink.buffered].
 *
 * Depending on the kind of upstream and the number of bytes written, buffering may improve the performance
 * by hiding the latency of small writes.
 *
 * Data stored inside the internal buffer could be sent to an upstream using [flush], [emit], or [hintEmit]:
 * - [flush] writes the whole buffer to an upstream and then flushes the upstream.
 * - [emit] writes all data from the buffer into the upstream without flushing it.
 * - [hintEmit] hints the source that current write operation is now finished and a part of data from the buffer
 * may be partially emitted into the upstream.
 * The latter is aimed to reduce memory footprint by keeping the buffer as small as possible without excessive writes
 * to the upstream.
 * All write operations implicitly calls [hintEmit].
 *
 * ### Write methods' behavior and naming conventions
 *
 * Methods writing a value of some type are usually named `write<Type>`, like [writeByte] or [writeInt], except methods
 * writing data from a some collection of bytes, like `write(ByteArray, Int, Int)` or
 * `write(source: RawSource, byteCount: Long)`.
 * In the latter case, if a collection is consumable (i.e., once data was read from it will no longer be available for
 * reading again), write method will consume as many bytes as it was requested to write.
 *
 * Methods fully consuming its argument are named `transferFrom`, like [transferFrom].
 *
 * It is recommended to follow the same naming convention for Sink extensions.
 *
 * ### Thread-safety guarantees
 *
 * Until stated otherwise, [Sink] implementations are not thread safe.
 * If a [Sink] needs to be accessed from multiple threads, an additional synchronization is required.
 */
public sealed interface Sink : RawSink {
    /**
     * This sink's internal buffer. It contains data written to the sink, but not yet flushed to the upstream.
     *
     * Incorrect use of the buffer may cause data loss or unexpected data being sent to the upstream.
     * Consider using alternative APIs to write data into the sink, if possible:
     * - write data into separate [Buffer] instance and write that buffer into the sink and then flush the sink to
     *   ensure that the upstream will receive complete data;
     * - implement [RawSink] and wrap an upstream sink into it to intercept data being written.
     *
     * If there is an actual need to write data directly into the buffer, consider using [Sink.writeToInternalBuffer] instead.
     */
    @InternalIoApi
    public val buffer: Buffer

    /**
     * Writes bytes from [source] array or its subrange to this sink.
     *
     * @param source the array from which bytes will be written into this sink.
     * @param startIndex the start index (inclusive) of the [source] subrange to be written, 0 by default.
     * @param endIndex the endIndex (exclusive) of the [source] subrange to be written, size of the [source] by default.
     *
     * @throws IndexOutOfBoundsException when [startIndex] or [endIndex] is out of range of [source] array indices.
     * @throws IllegalArgumentException when `startIndex > endIndex`.
     * @throws IllegalStateException when the sink is closed.
     * @throws IOException when some I/O error occurs.
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeByteArrayToSink
     */
    public fun write(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size)

    /**
     * Removes all bytes from [source] and write them to this sink.
     * Returns the number of bytes read which will be 0 if [source] is exhausted.
     *
     * @param source the source to consume data from.
     *
     * @throws IllegalStateException when the sink or [source] is closed.
     * @throws IOException when some I/O error occurs.
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.transferFrom
     */
    public fun transferFrom(source: RawSource): Long

    /**
     * Removes [byteCount] bytes from [source] and write them to this sink.
     *
     * If [source] will be exhausted before reading [byteCount] from it then an exception throws on
     * an attempt to read remaining bytes will be propagated to a caller of this method.
     *
     * @param source the source to consume data from.
     * @param byteCount the number of bytes to read from [source] and to write into this sink.
     *
     * @throws IllegalArgumentException when [byteCount] is negative.
     * @throws IllegalStateException when the sink or [source] is closed.
     * @throws IOException when some I/O error occurs.
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeSourceToSink
     */
    public fun write(source: RawSource, byteCount: Long)

    /**
     * Writes a byte to this sink.
     *
     * @param byte the byte to be written.
     *
     * @throws IllegalStateException when the sink is closed.
     * @throws IOException when some I/O error occurs.
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeByte
     */
    public fun writeByte(byte: Byte)

    /**
     * Writes two bytes containing [short], in the big-endian order, to this sink.
     *
     * @param short the short integer to be written.
     *
     * @throws IllegalStateException when the sink is closed.
     * @throws IOException when some I/O error occurs.
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeShort
     */
    public fun writeShort(short: Short)

    /**
     * Writes four bytes containing [int], in the big-endian order, to this sink.
     *
     * @param int the integer to be written.
     *
     * @throws IllegalStateException when the sink is closed.
     * @throws IOException when some I/O error occurs.
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeInt
     */
    public fun writeInt(int: Int)

    /**
     * Writes eight bytes containing [long], in the big-endian order, to this sink.
     *
     * @param long the long integer to be written.
     *
     * @throws IllegalStateException when the sink is closed.
     * @throws IOException when some I/O error occurs.
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.writeLong
     */
    public fun writeLong(long: Long)

    /**
     * Writes all buffered data to the underlying sink, if one exists.
     * Then the underlying sink is explicitly flushed.
     *
     * @throws IllegalStateException when the sink is closed.
     * @throws IOException when some I/O error occurs.
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.flush
     */
    override fun flush()

    /**
     * Writes all buffered data to the underlying sink if one exists.
     * The underlying sink will not be explicitly flushed.
     *
     * This method behaves like [flush], but has weaker guarantees.
     * Call this method before a buffered sink goes out of scope so that its data can reach its destination.
     *
     * @throws IllegalStateException when the sink is closed.
     * @throws IOException when some I/O error occurs.
     *
     * @sample kotlinx.io.samples.KotlinxIoCoreCommonSamples.emit
     */
    public fun emit()

    /**
     * Hints that the buffer may be *partially* emitted (see [emit]) to the underlying sink.
     * The underlying sink will not be explicitly flushed.
     * There are no guarantees that this call will cause emit of buffered data as well as
     * there are no guarantees how many bytes will be emitted.
     *
     * Typically, application code will not need to call this: it is only necessary when
     * application code writes directly to this [buffered].
     * Use this to limit the memory held in the buffer.
     *
     * Consider using [Sink.writeToInternalBuffer] for writes into [buffered] followed by [hintEmit] call.
     *
     * @throws IllegalStateException when the sink is closed.
     * @throws IOException when some I/O error occurs.
     */
    @InternalIoApi
    public fun hintEmit()
}
