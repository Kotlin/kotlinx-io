/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadShortAt(offset: Int): Short = uncheckedLoadShortAtCommon(offset)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadShortLeAt(offset: Int): Short = uncheckedLoadShortLeAtCommon(offset)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadIntAt(offset: Int): Int = uncheckedLoadIntAtCommon(offset)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadIntLeAt(offset: Int): Int = uncheckedLoadIntLeAtCommon(offset)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadLongAt(offset: Int): Long = uncheckedLoadLongAtCommon(offset)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadLongLeAt(offset: Int): Long = uncheckedLoadLongLeAtCommon(offset)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadFloatAt(offset: Int): Float = uncheckedLoadFloatAtCommon(offset)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadFloatLeAt(offset: Int): Float = uncheckedLoadFloatLeAtCommon(offset)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadDoubleAt(offset: Int): Double = uncheckedLoadDoubleAtCommon(offset)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadDoubleLeAt(offset: Int): Double =
    uncheckedLoadDoubleLeAtCommon(offset)
