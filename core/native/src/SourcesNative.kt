/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadShortAt(offset: Int): Short = getShortAt(offset).reverseBytes()

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadShortLeAt(offset: Int): Short = getShortAt(offset)

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadIntAt(offset: Int): Int = getIntAt(offset).reverseBytes()

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadIntLeAt(offset: Int): Int = getIntAt(offset)

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadLongAt(offset: Int): Long = getLongAt(offset).reverseBytes()

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadLongLeAt(offset: Int): Long = getLongAt(offset)

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadFloatAt(offset: Int): Float =
    Float.fromBits(getIntAt(offset).reverseBytes())

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadFloatLeAt(offset: Int): Float = getFloatAt(offset)

@OptIn(ExperimentalNativeApi::class)
@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadDoubleAt(offset: Int): Double =
    Double.fromBits(getLongAt(offset).reverseBytes())

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun ByteArray.uncheckedLoadDoubleLeAt(offset: Int): Double =
    uncheckedLoadDoubleLeAtCommon(offset)
