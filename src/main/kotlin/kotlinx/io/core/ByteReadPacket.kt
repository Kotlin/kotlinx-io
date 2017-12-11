package kotlinx.io.core

import kotlinx.io.pool.*

expect class ByteReadPacket(head: BufferView, pool: ObjectPool<BufferView>): ByteReadPacketPlatformBase {
    companion object {
        val Empty: ByteReadPacket
        val ReservedSize: Int
    }
}

expect abstract class ByteReadPacketPlatformBase(head: BufferView, pool: ObjectPool<BufferView>) : ByteReadPacketBase {
}
