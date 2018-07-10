package kotlinx.io.core

import kotlinx.io.pool.*

expect class ByteReadPacket internal constructor(head: IoBuffer, remaining: Long, pool: ObjectPool<IoBuffer>): ByteReadPacketPlatformBase {
    constructor(head: IoBuffer, pool: ObjectPool<IoBuffer>)

    companion object {
        val Empty: ByteReadPacket
        val ReservedSize: Int
    }
}

expect abstract class ByteReadPacketPlatformBase protected constructor(head: IoBuffer, remaining: Long, pool: ObjectPool<IoBuffer>) : ByteReadPacketBase {
}
