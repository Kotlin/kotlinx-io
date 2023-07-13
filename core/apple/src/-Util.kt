@file:OptIn(UnsafeNumber::class)

package kotlinx.io

import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.*

internal fun Exception.toNSError() = NSError(
    domain = "Kotlin",
    code = 0,
    userInfo = mapOf(
        NSLocalizedDescriptionKey to message,
        NSUnderlyingErrorKey to this
    )
)

internal fun ByteArray.toNSData() = if (isNotEmpty()) {
    usePinned {
        NSData.create(bytes = it.addressOf(0), length = size.convert())
    }
} else {
    NSData.data()
}
