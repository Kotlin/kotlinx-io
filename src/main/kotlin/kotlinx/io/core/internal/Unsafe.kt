package kotlinx.io.core.internal

import kotlinx.io.core.*

@Deprecated("Dangerous. For internal use only.", level = DeprecationLevel.ERROR)
fun ByteReadPacket.`$unsafeAppend$`(builder: BytePacketBuilder) {
    val builderSize = builder.size
    val builderHead = builder.head

    if (builderSize <= PACKET_MAX_COPY_SIZE && builderHead.next == null && tryWriteAppend(builderHead)) {
        builder.afterBytesStolen()
        return
    }

    builder.stealAll()?.let { chain ->
        append(chain)
    }
}
