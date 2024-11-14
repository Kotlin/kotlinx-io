/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

internal object SinkIntrinsics {
    fun write(target: Sink, source: ShortArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            ArrayHandles.shortHandle.set(this, idx, value)
        }
    }

    fun write(target: Sink, source: IntArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            ArrayHandles.intHandle.set(this, idx, value)
        }
    }

    fun write(target: Sink, source: LongArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            ArrayHandles.longHandle.set(this, idx, value)
        }
    }

    fun write(target: Sink, source: FloatArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            ArrayHandles.floatHandle.set(this, idx, value)
        }
    }

    fun write(target: Sink, source: DoubleArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            ArrayHandles.doubleHandle.set(this, idx, value)
        }
    }

    fun writeLe(target: Sink, source: ShortArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            ArrayHandles.shortLeHandle.set(this, idx, value)
        }
    }

    fun writeLe(target: Sink, source: IntArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            ArrayHandles.intLeHandle.set(this, idx, value)
        }
    }

    fun writeLe(target: Sink, source: LongArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            ArrayHandles.longLeHandle.set(this, idx, value)
        }
    }

    fun writeLe(target: Sink, source: FloatArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            ArrayHandles.floatLeHandle.set(this, idx, value)
        }
    }

    fun writeLe(target: Sink, source: DoubleArray, startIndex: Int, endIndex: Int) {
        target.writeArrayImpl(source, startIndex, endIndex) { idx, value ->
            ArrayHandles.doubleLeHandle.set(this, idx, value)
        }
    }
}
