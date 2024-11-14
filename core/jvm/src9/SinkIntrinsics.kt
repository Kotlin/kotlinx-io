/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

internal object SinkIntrinsics {
    fun uncheckedStoreShortAt(array: ByteArray, idx: Int, value: Short) =
        ArrayHandles.shortHandle.set(array, idx, value)

    fun uncheckedStoreShortLeAt(array: ByteArray, idx: Int, value: Short) =
        ArrayHandles.shortLeHandle.set(array, idx, value)

    fun uncheckedStoreIntAt(array: ByteArray, idx: Int, value: Int) =
        ArrayHandles.intHandle.set(array, idx, value)

    fun uncheckedStoreIntLeAt(array: ByteArray, idx: Int, value: Int) =
        ArrayHandles.intLeHandle.set(array, idx, value)

    fun uncheckedStoreLongAt(array: ByteArray, idx: Int, value: Long) =
        ArrayHandles.longHandle.set(array, idx, value)

    fun uncheckedStoreLongLeAt(array: ByteArray, idx: Int, value: Long) =
        ArrayHandles.longLeHandle.set(array, idx, value)

    fun uncheckedStoreFloatAt(array: ByteArray, idx: Int, value: Float) =
        ArrayHandles.floatHandle.set(array, idx, value)

    fun uncheckedStoreFloatLeAt(array: ByteArray, idx: Int, value: Float) =
        ArrayHandles.floatLeHandle.set(array, idx, value)

    fun uncheckedStoreDoubleAt(array: ByteArray, idx: Int, value: Double) =
        ArrayHandles.doubleHandle.set(array, idx, value)

    fun uncheckedStoreDoubleLeAt(array: ByteArray, idx: Int, value: Double) =
        ArrayHandles.doubleLeHandle.set(array, idx, value)
}
