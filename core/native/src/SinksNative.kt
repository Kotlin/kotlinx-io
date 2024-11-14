/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreShortAt(idx: Int, value: Short) =
    setShortAt(idx, value.reverseBytes())

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreShortLeAt(idx: Int, value: Short) =
    setShortAt(idx, value)

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreIntAt(idx: Int, value: Int) =
    setIntAt(idx, value.reverseBytes())

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreIntLeAt(idx: Int, value: Int) =
    setIntAt(idx, value)

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreLongAt(idx: Int, value: Long) =
    setLongAt(idx, value.reverseBytes())

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreLongLeAt(idx: Int, value: Long) =
    setLongAt(idx, value)

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreFloatAt(idx: Int, value: Float) =
    setIntAt(idx, value.toBits().reverseBytes())

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreFloatLeAt(idx: Int, value: Float) =
   setFloatAt(idx, value)

@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalNativeApi::class)
internal actual inline fun ByteArray.uncheckedStoreDoubleAt(idx: Int, value: Double) =
    setLongAt(idx, value.toBits().reverseBytes())

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedStoreDoubleLeAt(idx: Int, value: Double) =
    setDoubleAt(idx, value)
