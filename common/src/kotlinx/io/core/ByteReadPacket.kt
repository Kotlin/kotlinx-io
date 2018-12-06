package kotlinx.io.core

import kotlinx.io.pool.*

expect class ByteReadPacket internal constructor(head: IoBuffer, remaining: Long, pool: ObjectPool<IoBuffer>) :
    ByteReadPacketPlatformBase {
    constructor(head: IoBuffer, pool: ObjectPool<IoBuffer>)

    companion object {
        val Empty: ByteReadPacket
        val ReservedSize: Int
    }
}

expect abstract class ByteReadPacketPlatformBase protected constructor(
    head: IoBuffer,
    remaining: Long,
    pool: ObjectPool<IoBuffer>
) : ByteReadPacketBase {
}

/**
 * The default abstract base class for implementing [Input] interface.
 * @see [ByteReadPacketBase.fill] amd [ByteReadPacketBase.closeSource]
 */
@ExperimentalIoApi
abstract class AbstractInput(
    head: IoBuffer = IoBuffer.Empty,
    remaining: Long = head.remainingAll(),
    pool: ObjectPool<IoBuffer> = IoBuffer.Pool
) : ByteReadPacketPlatformBase(head, remaining, pool) {
    abstract override fun fill(): IoBuffer?
    abstract override fun closeSource()
}

expect fun ByteReadPacket(
    array: ByteArray, offset: Int = 0, length: Int = array.size,
    block: (ByteArray) -> Unit
): ByteReadPacket

@Suppress("NOTHING_TO_INLINE")
inline fun ByteReadPacket(array: ByteArray, offset: Int = 0, length: Int = array.size): ByteReadPacket {
    return ByteReadPacket(array, offset, length) {}
}
