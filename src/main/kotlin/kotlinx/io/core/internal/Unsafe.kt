package kotlinx.io.core.internal

import kotlinx.io.core.BytePacketBuilder
import kotlinx.io.core.ByteReadPacket

@Deprecated("Dangerous. For internal use only.", level = DeprecationLevel.ERROR)
fun ByteReadPacket.`$unsafeAppend$`(builder: BytePacketBuilder) {
    builder.stealAll()?.let { chain ->
        append(chain)
    }
}
