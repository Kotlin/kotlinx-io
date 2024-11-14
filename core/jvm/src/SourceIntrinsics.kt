/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io


internal object SourceIntrinsics {
    fun read(source: Source, target: ShortArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readShort, ByteArray::readShortAt_)
    }

    fun read(source: Source, target: IntArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readInt, ByteArray::readIntAt_)
    }

    fun read(source: Source, target: LongArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readLong, ByteArray::readLongAt_)
    }

    fun read(source: Source, target: FloatArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readFloat) {
            Float.fromBits(readIntAt_(it))
        }
    }

    fun read(source: Source, target: DoubleArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readDouble) {
            Double.fromBits(readLongAt_(it))
        }
    }

    fun readLe(source: Source, target: ShortArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readShortLe, ByteArray::readShortLeAt_)
    }

    fun readLe(source: Source, target: IntArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readIntLe, ByteArray::readIntLeAt_)
    }

    fun readLe(source: Source, target: LongArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readLongLe, ByteArray::readLongLeAt_)
    }

    fun readLe(source: Source, target: FloatArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readFloatLe) {
            Float.fromBits(readIntLeAt_(it))
        }
    }

    fun readLe(source: Source, target: DoubleArray, startIndex: Int, endIndex: Int) {
        source.readArrayImpl(target, startIndex, endIndex, Buffer::readDoubleLe) {
            Double.fromBits(readLongLeAt_(it))
        }
    }
}
