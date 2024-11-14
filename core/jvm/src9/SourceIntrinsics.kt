/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

internal object SourceIntrinsics {
    fun uncheckedLoadShortAt(array: ByteArray, offset: Int): Short =
        ArrayHandles.shortHandle.get(array, offset) as Short
    fun uncheckedLoadShortLeAt(array: ByteArray, offset: Int): Short =
        ArrayHandles.shortLeHandle.get(array, offset) as Short

    fun uncheckedLoadIntAt(array: ByteArray, offset: Int): Int =
        ArrayHandles.intHandle.get(array, offset) as Int
    fun uncheckedLoadIntLeAt(array: ByteArray, offset: Int): Int =
        ArrayHandles.intLeHandle.get(array, offset) as Int

    fun uncheckedLoadLongAt(array: ByteArray, offset: Int): Long =
        ArrayHandles.longHandle.get(array, offset) as Long
    fun uncheckedLoadLongLeAt(array: ByteArray, offset: Int): Long =
        ArrayHandles.longLeHandle.get(array, offset) as Long

    fun uncheckedLoadFloatAt(array: ByteArray, offset: Int): Float =
        ArrayHandles.floatHandle.get(array, offset) as Float
    fun uncheckedLoadFloatLeAt(array: ByteArray, offset: Int): Float =
        ArrayHandles.floatLeHandle.get(array, offset) as Float

    fun uncheckedLoadDoubleAt(array: ByteArray, offset: Int): Double =
        ArrayHandles.doubleHandle.get(array, offset) as Double
    fun uncheckedLoadDoubleLeAt(array: ByteArray, offset: Int): Double =
        ArrayHandles.doubleLeHandle.get(array, offset) as Double
}
