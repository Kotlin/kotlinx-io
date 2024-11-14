/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

internal object SinkIntrinsics {
    fun uncheckedStoreShortAt(array: ByteArray, idx: Int, value: Short) =
        array.uncheckedStoreShortAtCommon(idx, value)

    fun uncheckedStoreShortLeAt(array: ByteArray, idx: Int, value: Short) =
        array.uncheckedStoreShortLeAtCommon(idx, value)

    fun uncheckedStoreIntAt(array: ByteArray, idx: Int, value: Int) =
        array.uncheckedStoreIntAtCommon(idx, value)

    fun uncheckedStoreIntLeAt(array: ByteArray, idx: Int, value: Int) =
        array.uncheckedStoreIntLeAtCommon(idx, value)

    fun uncheckedStoreLongAt(array: ByteArray, idx: Int, value: Long) =
        array.uncheckedStoreLongAtCommon(idx, value)

    fun uncheckedStoreLongLeAt(array: ByteArray, idx: Int, value: Long) =
        array.uncheckedStoreLongLeAtCommon(idx, value)

    fun uncheckedStoreFloatAt(array: ByteArray, idx: Int, value: Float) =
        array.uncheckedStoreFloatAtCommon(idx, value)

    fun uncheckedStoreFloatLeAt(array: ByteArray, idx: Int, value: Float) =
        array.uncheckedStoreFloatLeAtCommon(idx, value)

    fun uncheckedStoreDoubleAt(array: ByteArray, idx: Int, value: Double) =
        array.uncheckedStoreDoubleAtCommon(idx, value)

    fun uncheckedStoreDoubleLeAt(array: ByteArray, idx: Int, value: Double) =
        array.uncheckedStoreDoubleLeAtCommon(idx, value)
}
