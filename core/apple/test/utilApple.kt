package kotlinx.io

import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Foundation.data

fun ByteArray.toNSData() = if (isNotEmpty()) {
    usePinned {
        @OptIn(UnsafeNumber::class)
        NSData.create(bytes = it.addressOf(0), length = size.convert())
    }
} else {
    NSData.data()
}
