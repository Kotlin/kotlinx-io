package kotlinx.io.core

import kotlinx.io.pool.*

actual class ByteReadPacket
    actual constructor(head: BufferView, pool: ObjectPool<BufferView>) : ByteReadPacketBase(head, pool) {

    actual companion object {
        actual val Empty = ByteReadPacketBase.Empty
        actual val ReservedSize = ByteReadPacketBase.ReservedSize
    }
}
