/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

internal actual fun Short.reverseBytes(): Short = java.lang.Short.reverseBytes(this)
internal actual fun Int.reverseBytes(): Int = java.lang.Integer.reverseBytes(this)
internal actual fun Long.reverseBytes(): Long = java.lang.Long.reverseBytes(this)
