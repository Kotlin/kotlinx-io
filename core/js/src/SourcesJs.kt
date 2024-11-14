/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

internal actual fun Source.readArrayImpl(sink: ShortArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readShort, ByteArray::readShortAt_)
    } else {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readShortLe, ByteArray::readShortLeAt_)
    }
}

internal actual fun Source.readArrayImpl(sink: IntArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readInt, ByteArray::readIntAt_)
    } else {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readIntLe, ByteArray::readIntLeAt_)
    }
}

internal actual fun Source.readArrayImpl(sink: LongArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readLong, ByteArray::readLongAt_)
    } else {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readLongLe, ByteArray::readLongLeAt_)
    }
}

internal actual fun Source.readArrayImpl(sink: FloatArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readFloat) { Float.fromBits(readIntAt_(it)) }
    } else {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readFloatLe) { Float.fromBits(readIntLeAt_(it)) }
    }
}

internal actual fun Source.readArrayImpl(sink: DoubleArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readDouble) { Double.fromBits(readLongAt_(it)) }
    } else {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readDoubleLe) { Double.fromBits(readLongLeAt_(it)) }
    }
}
