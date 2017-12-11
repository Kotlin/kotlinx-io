package kotlinx.io.core

import kotlinx.io.pool.*

expect class ByteReadPacket(head: BufferView, pool: ObjectPool<BufferView>): ByteReadPacketBase {
    companion object {
        val Empty: ByteReadPacket
        val ReservedSize: Int
    }
}