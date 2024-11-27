/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.okio

import kotlinx.io.bytestring.isEmpty
import kotlinx.io.readByteArray
import okio.Buffer
import okio.Timeout
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OkioAdaptersTest {
    @Test
    fun convertByteStrings() {
        assertEquals(0, kotlinx.io.bytestring.ByteString().toOkioByteString().size)
        assertTrue(okio.ByteString.EMPTY.toKotlinxIoByteString().isEmpty())

        val data = ByteArray(42) { it.toByte() }
        assertContentEquals(data, kotlinx.io.bytestring.ByteString(data).toOkioByteString().toByteArray())
        assertContentEquals(data, okio.ByteString.of(*data).toKotlinxIoByteString().toByteArray())
    }

    @Test
    fun okioSinkAsKxIoSink() {
        val arrayLength = 42
        val prefixLength = 30

        val data = ByteArray(arrayLength) { it.toByte() }
        val kxBuffer = kotlinx.io.Buffer().also { it.write(data) }

        val okioBuffer = okio.Buffer()
        val okioSink = okioBuffer as okio.Sink

        val wrapper = okioSink.asKotlinxIoRawSink()

        wrapper.write(kxBuffer, prefixLength.toLong())

        assertEquals(okioBuffer.size, prefixLength.toLong())
        assertEquals(kxBuffer.size, arrayLength - prefixLength.toLong())

        wrapper.write(kxBuffer, kxBuffer.size.toLong())
        assertContentEquals(data, okioBuffer.readByteArray())
        assertTrue(kxBuffer.exhausted())

        assertFailsWith<IllegalArgumentException> {
            wrapper.write(kxBuffer, 9001L)
        }
    }

    @Test
    fun okioSinkViewDelegation() {
        var closed = false
        var flushed = false
        val sink = object : okio.Sink {
            override fun close() {
                closed = true
            }

            override fun flush() {
                flushed = true
            }

            override fun timeout(): okio.Timeout = TODO()

            override fun write(source: okio.Buffer, byteCount: Long) = TODO()
        }

        val view = sink.asKotlinxIoRawSink()
        view.flush()
        assertTrue(flushed)

        view.close()
        assertTrue(closed)
    }

    @Test
    fun okioSinkAdapterExceptionTranslation() {
        val adapter = object : okio.Sink {
            override fun close() = throw okio.IOException()
            override fun flush() = throw okio.IOException()
            override fun timeout(): Timeout = TODO()
            override fun write(source: Buffer, byteCount: Long) = throw okio.IOException()
        }.asKotlinxIoRawSink()

        assertFailsWith<kotlinx.io.IOException> {
            adapter.write(kotlinx.io.Buffer().also { it.writeByte(1) }, 1)
        }

        assertFailsWith<kotlinx.io.IOException> {
            adapter.flush()
        }

        assertFailsWith<kotlinx.io.IOException> {
            adapter.close()
        }
    }

    @Test
    fun okioSourceAsKxSource() {
        val arrayLength = 42
        val prefixLength = 3
        val data = ByteArray(arrayLength) { it.toByte() }

        val okioBuffer = okio.Buffer().also { it.write(data) }
        val okioSource = okioBuffer as okio.Source

        val kxBuffer = kotlinx.io.Buffer()

        val wrapper = okioSource.asKotlinxIoRawSource()

        val read = wrapper.readAtMostTo(kxBuffer, prefixLength.toLong())
        assertEquals(prefixLength.toLong(), read)
        assertEquals(prefixLength.toLong(), kxBuffer.size)
        assertEquals(arrayLength - prefixLength.toLong(), okioBuffer.size)

        val remaining = wrapper.readAtMostTo(kxBuffer, Long.MAX_VALUE)
        assertEquals(arrayLength.toLong() - prefixLength, remaining)
        assertContentEquals(data, kxBuffer.readByteArray())
        assertTrue(okioBuffer.exhausted())

        assertEquals(-1L, wrapper.readAtMostTo(kxBuffer, 1))
    }

    @Test
    fun okioSourceViewDelegation() {
        var closed = false
        val view = object : okio.Source {
            override fun close() {
                closed = true
            }

            override fun read(sink: okio.Buffer, byteCount: Long): Long = TODO()

            override fun timeout(): okio.Timeout = TODO()
        }

        view.asKotlinxIoRawSource().close()
        assertTrue(closed)
    }

    @Test
    fun okioSourceAdapterExceptionTranslation() {
        var throwEOF = false
        val adapter = object : okio.Source {
            override fun close() = throw okio.IOException()

            override fun read(sink: Buffer, byteCount: Long): Long = if (throwEOF) {
                throw okio.EOFException()
            } else {
                throw okio.IOException()
            }

            override fun timeout(): Timeout = TODO()
        }.asKotlinxIoRawSource()

        assertFailsWith<kotlinx.io.IOException> {
            adapter.readAtMostTo(kotlinx.io.Buffer(), 1)
        }
        throwEOF = true
        assertFailsWith<kotlinx.io.EOFException> {
            adapter.readAtMostTo(kotlinx.io.Buffer(), 1)
        }

        assertFailsWith<kotlinx.io.IOException> {
            adapter.close()
        }
    }

    @Test
    fun kotlinxIoSourceAsOkioSource() {
        val arrayLength = 42
        val prefixLength = 30

        val data = ByteArray(arrayLength) { it.toByte() }
        val kxBuffer = kotlinx.io.Buffer().also { it.write(data) }
        val kxSource = kxBuffer as kotlinx.io.RawSource

        val okioBuffer = okio.Buffer()
        val wrapper = kxSource.asOkioSource()

        val read = wrapper.read(okioBuffer, prefixLength.toLong())
        assertEquals(prefixLength.toLong(), read)
        assertEquals(prefixLength.toLong(), okioBuffer.size)
        assertEquals(arrayLength - prefixLength.toLong(), kxBuffer.size)

        val remaining = wrapper.read(okioBuffer, Long.MAX_VALUE)
        assertEquals(arrayLength.toLong() - prefixLength.toLong(), remaining)
        assertContentEquals(data, okioBuffer.readByteArray())
        assertTrue(kxBuffer.exhausted())

        assertEquals(-1L, wrapper.read(okioBuffer, 1))
    }

    @Test
    fun kotlinxIoSourceViewDelegation() {
        var closed = false
        val view = object : kotlinx.io.RawSource {
            override fun readAtMostTo(sink: kotlinx.io.Buffer, byteCount: Long): Long = TODO()

            override fun close() {
                closed = true
            }
        }

        view.asOkioSource().close()
        assertTrue(closed)
    }

    @Test
    fun kotlinxIoSourceAdapterExceptionTranslation() {
        var throwEOF = false
        val adapter = object : kotlinx.io.RawSource {
            override fun readAtMostTo(sink: kotlinx.io.Buffer, byteCount: Long): Long = if (throwEOF) {
                throw kotlinx.io.EOFException()
            } else {
                throw kotlinx.io.IOException()
            }

            override fun close() = throw kotlinx.io.IOException()
        }.asOkioSource()

        assertFailsWith<okio.IOException> {
            adapter.read(okio.Buffer(), 1)
        }
        throwEOF = true
        assertFailsWith<okio.EOFException> {
            adapter.read(okio.Buffer(), 1)
        }

        assertFailsWith<okio.IOException> {
            adapter.close()
        }
    }

    @Test
    fun kotlinxIoSinkAsOkioSink() {
        val arrayLength = 42
        val prefixLength = 3

        val data = ByteArray(arrayLength) { it.toByte() }
        val kxBuffer = kotlinx.io.Buffer()
        val kxSink = kxBuffer as kotlinx.io.RawSink

        val okioBuffer = okio.Buffer().also { it.write(data) }
        val wrapper = kxSink.asOkioSink()

        wrapper.write(okioBuffer, prefixLength.toLong())
        assertEquals(prefixLength.toLong(), kxBuffer.size)
        assertEquals(arrayLength.toLong() - prefixLength, okioBuffer.size)

        wrapper.write(okioBuffer, okioBuffer.size.toLong())
        assertContentEquals(data, kxBuffer.readByteArray())
        assertTrue(okioBuffer.exhausted())

        assertFailsWith<IllegalArgumentException> {
            wrapper.write(okioBuffer, 9001L)
        }
    }

    @Test
    fun kotlinxIoSinkViewDelegation() {
        var closed = false
        var flushed = false
        val view = object : kotlinx.io.RawSink {
            override fun write(source: kotlinx.io.Buffer, byteCount: Long) = TODO()

            override fun flush() {
                flushed = true
            }

            override fun close() {
                closed = true
            }
        }

        view.asOkioSink().flush()
        assertTrue(flushed)

        view.asOkioSink().close()
        assertTrue(closed)
    }

    @Test
    fun kotlinxIoSinkAdapterExceptionTranslation() {
        val adapter = object : kotlinx.io.RawSink {
            override fun write(source: kotlinx.io.Buffer, byteCount: Long) = throw kotlinx.io.IOException()
            override fun flush() = throw kotlinx.io.IOException()
            override fun close() = throw kotlinx.io.IOException()
        }.asOkioSink()

        assertFailsWith<okio.IOException> {
            adapter.write(okio.Buffer().also { it.writeByte(1) }, 1)
        }

        assertFailsWith<okio.IOException> {
            adapter.flush()
        }

        assertFailsWith<okio.IOException> {
            adapter.close()
        }
    }
}
