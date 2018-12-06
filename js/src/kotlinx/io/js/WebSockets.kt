package kotlinx.io.js

import kotlinx.io.core.*
import org.khronos.webgl.*
import org.w3c.dom.*

fun WebSocket.sendPacket(packet: ByteReadPacket) {
    send(packet.readArrayBuffer())
}

inline fun WebSocket.sendPacket(block: BytePacketBuilder.() -> Unit) {
    sendPacket(buildPacket(block = block))
}

inline fun MessageEvent.packet(): ByteReadPacket {
    @Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE", "UnsafeCastFromDynamic")
    return ByteReadPacket(IoBuffer(data.asDynamic(), null), IoBuffer.NoPool)
}




