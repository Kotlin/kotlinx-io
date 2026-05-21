/*
 * Copyright 2010-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io

import platform.builtin.builtin_bswap16
import platform.builtin.builtin_bswap32
import platform.builtin.builtin_bswap64

internal actual fun Short.reverseBytes(): Short = builtin_bswap16(this)
internal actual fun Int.reverseBytes(): Int = builtin_bswap32(this)
internal actual fun Long.reverseBytes(): Long = builtin_bswap64(this)