/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Sink.writeArrayImpl(source: ShortArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        writeArrayImpl(source, startIndex, endIndex, ByteArray::writeShortAt_)
    } else {
        writeArrayImpl(source, startIndex, endIndex, ByteArray::writeShortLeAt_)
    }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Sink.writeArrayImpl(source: IntArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        writeArrayImpl(source, startIndex, endIndex, ByteArray::writeIntAt_)
    } else {
        writeArrayImpl(source, startIndex, endIndex, ByteArray::writeIntLeAt_)
    }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Sink.writeArrayImpl(source: LongArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        writeArrayImpl(source, startIndex, endIndex, ByteArray::writeLongAt_)
    } else {
        writeArrayImpl(source, startIndex, endIndex, ByteArray::writeLongLeAt_)
    }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Sink.writeArrayImpl(source: FloatArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        writeArrayImpl(source, startIndex, endIndex) { idx, value -> writeIntAt_(idx, value.toBits()) }
    } else {
        writeArrayImpl(source, startIndex, endIndex) { idx, value -> writeIntLeAt_(idx, value.toBits()) }
    }
}

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Sink.writeArrayImpl(source: DoubleArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        writeArrayImpl(source, startIndex, endIndex) { idx, value -> writeLongAt_(idx, value.toBits()) }
    } else {
        writeArrayImpl(source, startIndex, endIndex) { idx, value -> writeLongLeAt_(idx, value.toBits()) }
    }
}
