/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package kotlinx.io

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset

private val EMPTY_ARRAY = ByteArray(0)

internal class BufferedPeekSource(private val upstream: Source): Source {
    private var pos: Int = 0
    private val upstreamBuffer = upstream.buffer
    private var head = upstreamBuffer.head
    private var data = head?.data ?: EMPTY_ARRAY
    private var expectedPos = head?.pos ?: -1
    private var limit: Int = head?.limit ?: -1

    override fun read(sink: Buffer, byteCount: Long): Long {
        TODO("Not yet implemented")
    }

    override fun read(dst: ByteBuffer?): Int {
        TODO("Not yet implemented")
    }

    override fun cancel() {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun isOpen(): Boolean {
        TODO("Not yet implemented")
    }

    override fun buffer(): Buffer {
        TODO("Not yet implemented")
    }

    override val buffer: Buffer
        get() = TODO("Not yet implemented")

    override fun exhausted(): Boolean = !request( 1L)

    override fun require(byteCount: Long) {
        if (!request(byteCount)) throw EOFException()
    }

    override fun request(byteCount: Long): Boolean = upstream.request(byteCount + pos)

    override fun readByte(): Byte {
        if (expectedPos == limit) {
            require(1)
            if (head == null) {
                val h = upstream.buffer.head!!
                expectedPos = h.pos
                limit = h.limit
                data = h.data
                head = h
            } else if (limit < head!!.limit) {
                limit = head!!.limit
            } else {
                val h = head!!.next!!
                expectedPos = h.pos
                limit = h.limit
                data = h.data
                head = h
            }
        }
        pos++
        return data[expectedPos++]
    }

    override fun readShort(): Short {
        TODO("Not yet implemented")
    }

    override fun readShortLe(): Short {
        TODO("Not yet implemented")
    }

    override fun readInt(): Int {
        TODO("Not yet implemented")
    }

    override fun readIntLe(): Int {
        TODO("Not yet implemented")
    }

    override fun readLong(): Long {
        TODO("Not yet implemented")
    }

    override fun readLongLe(): Long {
        TODO("Not yet implemented")
    }

    override fun readDecimalLong(): Long {
        TODO("Not yet implemented")
    }

    override fun readHexadecimalUnsignedLong(): Long {
        TODO("Not yet implemented")
    }

    override fun skip(byteCount: Long) {
        TODO("Not yet implemented")
    }

    override fun select(options: Options): Int {
        TODO("Not yet implemented")
    }

    override fun selectUsingPeekSource(options: Options): Int {
        TODO("Not yet implemented")
    }

    override fun selectUsingBufferedPeekSource(options: Options): Int {
        TODO("Not yet implemented")
    }

    override fun readByteArray(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun readByteArray(byteCount: Long): ByteArray {
        TODO("Not yet implemented")
    }

    override fun read(sink: ByteArray): Int {
        TODO("Not yet implemented")
    }

    override fun readFully(sink: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun read(sink: ByteArray, offset: Int, byteCount: Int): Int {
        TODO("Not yet implemented")
    }

    override fun readFully(sink: Buffer, byteCount: Long) {
        TODO("Not yet implemented")
    }

    override fun readAll(sink: RawSink): Long {
        TODO("Not yet implemented")
    }

    override fun readUtf8(): String {
        TODO("Not yet implemented")
    }

    override fun readUtf8(byteCount: Long): String {
        TODO("Not yet implemented")
    }

    override fun readUtf8Line(): String? {
        TODO("Not yet implemented")
    }

    override fun readUtf8LineStrict(): String {
        TODO("Not yet implemented")
    }

    override fun readUtf8LineStrict(limit: Long): String {
        TODO("Not yet implemented")
    }

    override fun readUtf8CodePoint(): Int {
        TODO("Not yet implemented")
    }

    override fun readString(charset: Charset): String {
        TODO("Not yet implemented")
    }

    override fun readString(byteCount: Long, charset: Charset): String {
        TODO("Not yet implemented")
    }

    override fun indexOf(b: Byte): Long {
        TODO("Not yet implemented")
    }

    override fun indexOf(b: Byte, fromIndex: Long): Long {
        TODO("Not yet implemented")
    }

    override fun indexOf(b: Byte, fromIndex: Long, toIndex: Long): Long {
        TODO("Not yet implemented")
    }

    override fun peek(): Source {
        TODO("Not yet implemented")
    }

    override fun inputStream(): InputStream {
        TODO("Not yet implemented")
    }

    override fun peekBuffered(): Source {
        TODO("Not yet implemented")
    }
}