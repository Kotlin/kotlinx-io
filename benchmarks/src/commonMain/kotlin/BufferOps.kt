/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package kotlinx.io.benchmark

import kotlinx.benchmark.*
import kotlinx.io.Buffer

@State(Scope.Benchmark)
open class PrimitiveOps {
    private val buffer = Buffer()

    @Benchmark
    fun readWriteByte() = buffer.writeByte(42).readByte()

    @Benchmark
    fun writeByte() {
        buffer.writeByte(42)
        buffer.clear()
    }

    @Benchmark
    fun readWriteShort() = buffer.writeShort(42).readShort()

    @Benchmark
    fun writeShort() {
        buffer.writeShort(42)
        buffer.clear()
    }

    @Benchmark
    fun writeShortReadShortLe() = buffer.writeShort(1234).readShortLe()

    @Benchmark
    fun writeShortLe() {
        buffer.writeShortLe(1234)
        buffer.clear()
    }

    @Benchmark
    fun readWriteInt() = buffer.writeInt(42).readInt()

    @Benchmark
    fun writeInt() {
        buffer.writeInt(42)
        buffer.clear()
    }

    @Benchmark
    fun writeIntReadIntLe() = buffer.writeInt(1234).readIntLe()

    @Benchmark
    fun writeIntLe() {
        buffer.writeIntLe(1234)
        buffer.clear()
    }

    @Benchmark
    fun readWriteLong() = buffer.writeLong(1234).readLong()

    @Benchmark
    fun writeLong() {
        buffer.writeLong(42)
        buffer.clear()
    }

    @Benchmark
    fun writeLongReadLongLe() = buffer.writeLong(1234).readLongLe()

    @Benchmark
    fun writeLongLe() {
        buffer.writeLongLe(1234)
        buffer.clear()
    }

    @Benchmark
    fun readWriteDecimalLong() = buffer.writeDecimalLong(0xdeadc0deL).readDecimalLong()

    @Benchmark
    fun writeDecimalLong() {
        buffer.writeDecimalLong(0xdeadc0de)
        buffer.clear()
    }

    @Benchmark
    fun readWriteHexadecimalLong() =
        buffer.writeHexadecimalUnsignedLong(0xbeefc0de15c00L).readHexadecimalUnsignedLong()

    @Benchmark
    fun writeHexadecimalLong() {
        buffer.writeHexadecimalUnsignedLong(0xbeefc0de15c00L)
        buffer.clear()
    }
}

@State(Scope.Benchmark)
open class CopyBuffer {
    private val buffer = Buffer()

    @Param("0", "1", "8192", "10000", "40000")
    var size: Int = 0

    @Setup
    fun fillBuffer() {
        buffer.write(ByteArray(size))
    }

    @Benchmark
    fun copy() = buffer.copy()
}

@State(Scope.Benchmark)
open class ByteArrayOps {
    private val buffer = Buffer()
    private var array = ByteArray(0)

    @Param("0", "1", "8192", "10000", "40000")
    var size: Int = 0

    @Setup
    fun createByteArray() {
        array = ByteArray(size)
    }

    @Benchmark
    fun readWrite(): ByteArray {
        buffer.write(array)
        buffer.readFully(array)
        return array
    }
}

@State(Scope.Benchmark)
open class Utf8CodePointOps {
    private val buffer = Buffer()

    @Benchmark
    fun ascii() = buffer.writeUtf8CodePoint('a'.code).readUtf8CodePoint()

    @Benchmark
    fun utf8() = buffer.writeUtf8CodePoint('й'.code).readUtf8CodePoint()

    @Benchmark
    fun utf8long() = buffer.writeUtf8CodePoint('å'.code).readUtf8CodePoint()
}

@State(Scope.Benchmark)
open class ReadWholeBuffer {
    private var source = Buffer()
    private var destination = Buffer()

    @Param("0", "1", "8196", "10000", "40000")
    var size: Int = 0

    @Setup
    fun setupSource() {
        source = Buffer().write(ByteArray(size))
    }

    @Benchmark
    fun readAll() {
        destination.readAll(source)
        swapBuffers()
    }

    @Benchmark
    fun writeAll() {
        source.writeAll(destination)
        swapBuffers()
    }

    private fun swapBuffers() {
        val tmp = source
        source = destination
        destination = tmp
    }
}