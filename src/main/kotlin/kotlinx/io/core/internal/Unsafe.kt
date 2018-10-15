package kotlinx.io.core.internal

import kotlinx.io.core.*

/**
 * API marked with this annotation is internal and extremely fragile and not intended to be used by library users.
 * Such API could be changed without notice including rename, removal and behaviour change.
 * Also using API marked with this annotation could cause data loss or any other damage.
 */
@Experimental(level = Experimental.Level.ERROR)
annotation class DangerousInternalIoApi

@DangerousInternalIoApi
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
