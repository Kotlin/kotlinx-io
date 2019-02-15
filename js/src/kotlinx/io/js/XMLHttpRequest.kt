package kotlinx.io.js

import kotlinx.io.bits.*
import kotlinx.io.core.*
import kotlinx.io.core.internal.*
import org.khronos.webgl.*
import org.w3c.xhr.*

inline fun XMLHttpRequest.sendPacket(block: BytePacketBuilder.() -> Unit) {
    sendPacket(buildPacket(block = block))
}

fun XMLHttpRequest.sendPacket(packet: ByteReadPacket) {
    send(packet.readArrayBuffer())
}

@Suppress("UnsafeCastFromDynamic")
fun XMLHttpRequest.responsePacket(): ByteReadPacket = when (responseType) {
    XMLHttpRequestResponseType.ARRAYBUFFER -> ByteReadPacket(
        ChunkBuffer(
            Memory.of(response.asDynamic() as DataView),
            null
        ), ChunkBuffer.NoPool
    )
    XMLHttpRequestResponseType.EMPTY -> ByteReadPacket.Empty
    else -> throw IllegalStateException("Incompatible type $responseType: only ARRAYBUFFER and EMPTY are supported")
}


