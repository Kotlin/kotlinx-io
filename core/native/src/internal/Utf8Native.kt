/*
 * Copyright 2017-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.internal

internal actual fun decodeUtf8(source: ByteArray, beginIndex: Int, endIndex: Int): String =
    source.commonToUtf8String(beginIndex, endIndex)
