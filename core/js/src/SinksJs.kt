/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreShortAt(idx: Int, value: Short) =
    uncheckedStoreShortAtCommon(idx, value)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreShortLeAt(idx: Int, value: Short) =
    uncheckedStoreShortLeAtCommon(idx, value)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreIntAt(idx: Int, value: Int) =
    uncheckedStoreIntAtCommon(idx, value)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreIntLeAt(idx: Int, value: Int) =
    uncheckedStoreIntLeAtCommon(idx, value)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreLongAt(idx: Int, value: Long) =
    uncheckedStoreLongAtCommon(idx, value)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreLongLeAt(idx: Int, value: Long) =
    uncheckedStoreLongLeAtCommon(idx, value)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreFloatAt(idx: Int, value: Float) =
    uncheckedStoreFloatAtCommon(idx, value)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreFloatLeAt(idx: Int, value: Float) =
    uncheckedStoreFloatLeAtCommon(idx, value)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreDoubleAt(idx: Int, value: Double) =
    uncheckedStoreDoubleAtCommon(idx, value)

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreDoubleLeAt(idx: Int, value: Double) =
    uncheckedStoreDoubleLeAtCommon(idx, value)
