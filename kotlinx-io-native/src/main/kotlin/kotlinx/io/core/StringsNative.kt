package kotlinx.io.core

import kotlinx.io.charsets.*
import kotlinx.cinterop.*

actual fun String(bytes: ByteArray, offset: Int, length: Int, charset: Charset): String {
    return bytes.usePinned { pinned ->
        val ptr = pinned.addressOf(offset)
        val view = BufferView(ptr, length, null)
        view.resetForRead()
        val packet = ByteReadPacket(view, BufferView.NoPoolForManaged)
        check(packet.remaining == length.toLong())
        charset.newDecoder().decode(packet, Int.MAX_VALUE)
    }
}


