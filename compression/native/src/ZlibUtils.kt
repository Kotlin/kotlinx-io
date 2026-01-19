/*
 * Copyright 2017-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENCE file.
 */

package kotlinx.io.compression

import platform.zlib.*

internal fun zlibErrorMessage(code: Int): String {
    return when (code) {
        Z_ERRNO -> "Z_ERRNO"
        Z_STREAM_ERROR -> "Z_STREAM_ERROR"
        Z_DATA_ERROR -> "Z_DATA_ERROR"
        Z_MEM_ERROR -> "Z_MEM_ERROR"
        Z_BUF_ERROR -> "Z_BUF_ERROR"
        Z_VERSION_ERROR -> "Z_VERSION_ERROR"
        else -> "Unknown error: $code"
    }
}
