/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

internal object SinkIntrinsics {
    fun write(target: Sink, source: ShortArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex, ByteArray::writeShortAt_)
    }

    fun write(target: Sink, source: IntArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex, ByteArray::writeIntAt_)
    }

    fun write(target: Sink, source: LongArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex, ByteArray::writeLongAt_)
    }

    fun write(target: Sink, source: FloatArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            writeIntAt_(idx, value.toBits())
        }
    }

    fun write(target: Sink, source: DoubleArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            writeLongAt_(idx, value.toBits())
        }
    }

    fun writeLe(target: Sink, source: ShortArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex, ByteArray::writeShortLeAt_)
    }

    fun writeLe(target: Sink, source: IntArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex, ByteArray::writeIntLeAt_)
    }

    fun writeLe(target: Sink, source: LongArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex, ByteArray::writeLongLeAt_)
    }

    fun writeLe(target: Sink, source: FloatArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            writeIntLeAt_(idx, value.toBits())
        }
    }

    fun writeLe(target: Sink, source: DoubleArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            writeLongLeAt_(idx, value.toBits())
        }
    }
}
