/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

internal actual fun Short.reverseBytes(): Short = reverseBytesCommon()
internal actual fun Int.reverseBytes(): Int = reverseBytesCommon()
internal actual fun Long.reverseBytes(): Long = reverseBytesCommon()
