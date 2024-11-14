/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io


internal object SourceIntrinsics {
    fun uncheckedLoadShortAt(array: ByteArray, offset: Int): Short = array.uncheckedLoadShortAtCommon(offset)
    fun uncheckedLoadShortLeAt(array: ByteArray, offset: Int): Short = array.uncheckedLoadShortLeAtCommon(offset)

    fun uncheckedLoadIntAt(array: ByteArray, offset: Int): Int = array.uncheckedLoadIntAtCommon(offset)
    fun uncheckedLoadIntLeAt(array: ByteArray, offset: Int): Int = array.uncheckedLoadIntLeAtCommon(offset)

    fun uncheckedLoadLongAt(array: ByteArray, offset: Int): Long = array.uncheckedLoadLongAtCommon(offset)
    fun uncheckedLoadLongLeAt(array: ByteArray, offset: Int): Long = array.uncheckedLoadLongLeAtCommon(offset)

    fun uncheckedLoadFloatAt(array: ByteArray, offset: Int): Float = array.uncheckedLoadFloatAtCommon(offset)
    fun uncheckedLoadFloatLeAt(array: ByteArray, offset: Int): Float = array.uncheckedLoadFloatLeAtCommon(offset)

    fun uncheckedLoadDoubleAt(array: ByteArray, offset: Int): Double = array.uncheckedLoadDoubleAtCommon(offset)
    fun uncheckedLoadDoubleLeAt(array: ByteArray, offset: Int): Double = array.uncheckedLoadDoubleLeAtCommon(offset)
}
