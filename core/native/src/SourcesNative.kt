/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
internal actual fun Source.readArrayImpl(sink: ShortArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readShort) { getShortAt(it).reverseBytes() }
    } else {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readShortLe, ByteArray::getShortAt)
    }
}

@OptIn(ExperimentalNativeApi::class)
internal actual fun Source.readArrayImpl(sink: IntArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readInt) { getIntAt(it).reverseBytes() }
    } else {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readIntLe, ByteArray::getIntAt)
    }
}

@OptIn(ExperimentalNativeApi::class)
internal actual fun Source.readArrayImpl(sink: LongArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readLong) { getLongAt(it).reverseBytes() }
    } else {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readLongLe, ByteArray::getLongAt)
    }
}

@OptIn(ExperimentalNativeApi::class)
internal actual fun Source.readArrayImpl(sink: FloatArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readFloat) { Float.fromBits(getIntAt(it).reverseBytes()) }
    } else {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readFloatLe) { Float.fromBits(getIntAt(it)) }
    }
}

@OptIn(ExperimentalNativeApi::class)
internal actual fun Source.readArrayImpl(sink: DoubleArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readDouble) { Double.fromBits(getLongAt(it).reverseBytes()) }
    } else {
        readArrayImpl(sink, startIndex, endIndex, Buffer::readDoubleLe) { Double.fromBits(getLongAt(it)) }
    }
}
