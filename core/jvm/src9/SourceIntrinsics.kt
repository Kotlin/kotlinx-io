/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

internal object SourceIntrinsics {
    fun read(source: Source, target: ShortArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readShort) {
            ArrayHandles.shortHandle.get(this, it) as Short
        }
    }

    fun read(source: Source, target: IntArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readInt) {
            ArrayHandles.intHandle.get(this, it) as Int
        }
    }

    fun read(source: Source, target: LongArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readLong) {
            ArrayHandles.longHandle.get(this, it) as Long
        }
    }

    fun read(source: Source, target: FloatArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readFloat) {
            ArrayHandles.floatHandle.get(this, it) as Float
        }
    }

    fun read(source: Source, target: DoubleArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readDouble) {
            ArrayHandles.doubleHandle.get(this, it) as Double
        }
    }

    fun readLe(source: Source, target: ShortArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readShortLe) {
            ArrayHandles.shortLeHandle.get(this, it) as Short
        }
    }

    fun readLe(source: Source, target: IntArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readIntLe) {
            ArrayHandles.intLeHandle.get(this, it) as Int
        }
    }

    fun readLe(source: Source, target: LongArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readLongLe) {
            ArrayHandles.longLeHandle.get(this, it) as Long
        }
    }

    fun readLe(source: Source, target: FloatArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readFloatLe) {
            ArrayHandles.floatLeHandle.get(this, it) as Float
        }
    }

    fun readLe(source: Source, target: DoubleArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readDoubleLe) {
            ArrayHandles.doubleLeHandle.get(this, it) as Double
        }
    }
}
