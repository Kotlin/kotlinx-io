package kotlinx.io.core

import kotlinx.io.pool.*

expect class ByteReadPacket internal constructor(head: BufferView, remaining: Long, pool: ObjectPool<BufferView>): ByteReadPacketPlatformBase {
    constructor(head: BufferView, pool: ObjectPool<BufferView>)

    companion object {
        val Empty: ByteReadPacket
        val ReservedSize: Int
    }
}

expect abstract class ByteReadPacketPlatformBase protected constructor(head: BufferView, remaining: Long, pool: ObjectPool<BufferView>) : ByteReadPacketBase {
}
