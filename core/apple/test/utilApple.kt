package kotlinx.io

import kotlinx.cinterop.*
import platform.Foundation.NSData
import platform.posix.memcpy

@OptIn(UnsafeNumber::class)
fun NSData.toByteArray() = ByteArray(length.toInt()).apply {
    if (isNotEmpty()) {
        memcpy(refTo(0), bytes, length)
    }
}
