package kotlinx.io.core

import kotlinx.io.charsets.*
import kotlinx.cinterop.*

actual fun String(bytes: ByteArray, offset: Int, length: Int, charset: Charset): String {
    if (length == 0 && offset <= bytes.size) return ""

    return bytes.usePinned { pinned ->
        val ptr = pinned.addressOf(offset)
        val view = IoBuffer(ptr, length, null)
        view.resetForRead()
        val packet = ByteReadPacket(view, IoBuffer.NoPoolForManaged)
        check(packet.remaining == length.toLong())
        charset.newDecoder().decode(packet, Int.MAX_VALUE)
    }
}

internal actual fun String.getCharsInternal(dst: CharArray, dstOffset: Int) {
    val length = length
    require(dstOffset + length <= dst.size)

    var dstIndex = dstOffset
    for (srcIndex in 0 until length) {
        dst[dstIndex++] = this[srcIndex]
    }
}

