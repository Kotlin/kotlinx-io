package kotlinx.io

import kotlinx.cinterop.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.data
import platform.posix.memcpy

fun ByteArray.toNSData() = if (isNotEmpty()) {
    usePinned {
        @OptIn(UnsafeNumber::class)
        NSData.create(bytes = it.addressOf(0), length = size.convert())
    }
} else {
    NSData.data()
}

@OptIn(UnsafeNumber::class)
fun NSData.toByteArray() = ByteArray(length.toInt()).apply {
    if (isNotEmpty()) {
        memcpy(refTo(0), bytes, length)
    }
}
