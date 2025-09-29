package kotlinx.io.files

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.windows.FORMAT_MESSAGE_ALLOCATE_BUFFER
import platform.windows.FORMAT_MESSAGE_FROM_SYSTEM
import platform.windows.FORMAT_MESSAGE_IGNORE_INSERTS
import platform.windows.FormatMessageW
import platform.windows.GetLastError
import platform.windows.LPWSTRVar
import platform.windows.LocalFree

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
