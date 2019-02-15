package kotlinx.io.js

import kotlinx.io.bits.*
import kotlinx.io.core.*
import kotlinx.io.core.internal.*
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
    return ByteReadPacket(ChunkBuffer(Memory.of(data.asDynamic() as DataView), null), ChunkBuffer.NoPool)
}




