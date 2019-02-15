package kotlinx.io.core

import kotlinx.io.core.internal.*
import kotlinx.io.pool.*

expect class ByteReadPacket internal constructor(head: ChunkBuffer, remaining: Long, pool: ObjectPool<ChunkBuffer>) :
    ByteReadPacketPlatformBase {
    constructor(head: ChunkBuffer, pool: ObjectPool<ChunkBuffer>)

    companion object {
        val Empty: ByteReadPacket
        val ReservedSize: Int
    }
}

@DangerousInternalIoApi
expect abstract class ByteReadPacketPlatformBase protected constructor(
    head: ChunkBuffer,
    remaining: Long,
    pool: ObjectPool<ChunkBuffer>
) : ByteReadPacketBase

/**
 * The default abstract base class for implementing [Input] interface.
 * @see [AbstractInput.fill] amd [AbstractInput.closeSource]
 */
@ExperimentalIoApi
abstract class AbstractInput(
    head: ChunkBuffer = ChunkBuffer.Empty,
    remaining: Long = head.remainingAll(),
    pool: ObjectPool<ChunkBuffer> = ChunkBuffer.Pool
) : ByteReadPacketPlatformBase(head, remaining, pool) {
    /**
     * Read the next bytes into the [destination]
     * @return `true` if EOF encountered
     */
    abstract override fun fill(destination: Buffer): Boolean

    /**
     * Should close the underlying bytes source. Could do nothing or throw exceptions.
     */
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
