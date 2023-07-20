/*
 * Copyright 2017-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

@file:OptIn(UnsafeNumber::class)

package kotlinx.io

import kotlinx.cinterop.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.data
import platform.posix.memcpy

internal fun ByteArray.toNSData() = if (isNotEmpty()) {
    usePinned {
        NSData.create(bytes = it.addressOf(0), length = size.convert())
    }
} else {
    NSData.data()
}

fun NSData.toByteArray() = ByteArray(length.toInt()).apply {
    if (isNotEmpty()) {
        memcpy(refTo(0), bytes, length)
    }
}
