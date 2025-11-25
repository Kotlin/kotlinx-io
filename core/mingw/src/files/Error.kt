/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package kotlinx.io.files

import kotlinx.cinterop.*
import platform.windows.*

@OptIn(ExperimentalForeignApi::class)
internal fun formatWin32ErrorMessage(code: UInt = GetLastError()): String {
    memScoped {
        val r = alloc<LPWSTRVar>()
        val n = FormatMessageW(
            dwFlags = (FORMAT_MESSAGE_ALLOCATE_BUFFER or FORMAT_MESSAGE_IGNORE_INSERTS or FORMAT_MESSAGE_FROM_SYSTEM).toUInt(),
            lpSource = null,
            dwMessageId = code,
            dwLanguageId = 0u,
            lpBuffer = r.ptr.reinterpret(),
            nSize = 0u,
            Arguments = null,
        )
        if (n == 0u) {
            return "unknown error (${code.toHexString()})"
        }
        val s = r.value!!.toKString().trimEnd()
        LocalFree(r.value)
        return "$s (${code.toHexString()})"
    }

}
