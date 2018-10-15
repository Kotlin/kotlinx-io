package kotlinx.io.js

import kotlinx.io.core.*
import org.w3c.xhr.*

inline fun XMLHttpRequest.sendPacket(block: BytePacketBuilder.() -> Unit) {
    sendPacket(buildPacket(block = block))
}

fun XMLHttpRequest.sendPacket(packet: ByteReadPacket) {
    send(packet.readArrayBuffer())
}

@Suppress("UnsafeCastFromDynamic")
fun XMLHttpRequest.responsePacket(): ByteReadPacket = when (responseType) {
    XMLHttpRequestResponseType.ARRAYBUFFER -> ByteReadPacket(IoBuffer(response.asDynamic(), null), IoBuffer.NoPool)
    XMLHttpRequestResponseType.EMPTY -> ByteReadPacket.Empty
    else -> throw IllegalStateException("Incompatible type ${responseType}: only ARRAYBUFFER and EMPTY are supported")
}


