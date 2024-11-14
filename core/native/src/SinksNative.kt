/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Sink.writeArrayImpl(source: ShortArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        writeArrayImpl(source, startIndex, endIndex) { idx, value -> setShortAt(idx, value.reverseBytes()) }
    } else {
        writeArrayImpl(source, startIndex, endIndex, ByteArray::setShortAt)
    }
}

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Sink.writeArrayImpl(source: IntArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        writeArrayImpl(source, startIndex, endIndex) { idx, value -> setIntAt(idx, value.reverseBytes()) }
    } else {
        writeArrayImpl(source, startIndex, endIndex, ByteArray::setIntAt)
    }
}

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Sink.writeArrayImpl(source: LongArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        writeArrayImpl(source, startIndex, endIndex) { idx, value -> setLongAt(idx, value.reverseBytes()) }
    } else {
        writeArrayImpl(source, startIndex, endIndex, ByteArray::setLongAt)
    }
}

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Sink.writeArrayImpl(source: FloatArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            setIntAt(idx, value.toBits().reverseBytes())
        }
    } else {
        writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            setIntAt(idx, value.toBits())
        }
    }
}

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Sink.writeArrayImpl(source: DoubleArray, startIndex: Int, endIndex: Int, bigEndian: Boolean) {
    if (bigEndian) {
        writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            setLongAt(idx, value.toBits().reverseBytes())
        }
    } else {
        writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            setLongAt(idx, value.toBits())
        }
    }
}
