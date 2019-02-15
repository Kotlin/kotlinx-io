package kotlinx.io.core

import kotlinx.io.pool.*
import kotlinx.cinterop.*
import kotlinx.io.bits.*
import kotlinx.io.core.internal.*
import kotlin.require

@DangerousInternalIoApi
actual abstract class ByteReadPacketPlatformBase protected actual constructor(
    head: ChunkBuffer,
    remaining: Long,
    pool: ObjectPool<ChunkBuffer>
) : ByteReadPacketBase(head, remaining, pool), Input {

    override fun readFully(dst: CPointer<ByteVar>, offset: Int, length: Int) {
        return readFully(dst, offset.toLong(), length.toLong())
    }

    override fun readFully(dst: CPointer<ByteVar>, offset: Long, length: Long) {
        require(length <= remaining) { "Not enough bytes available ($remaining) to read $length bytes" }
        require(length >= 0L) { "length shouldn't be negative: $length" }

        val lengthInt = length.toIntOrFail("length")
        var copied = 0

        takeWhile { buffer: Buffer ->
            val rc = buffer.readAvailable(dst, copied, lengthInt - copied)
            if (rc > 0) copied += rc
            copied < lengthInt
        }
    }

    override fun readAvailable(dst: CPointer<ByteVar>, offset: Int, length: Int): Int {
        return readAvailable(dst, offset.toLong(), length.toLong()).toInt()
    }

    override fun readAvailable(dst: CPointer<ByteVar>, offset: Long, length: Long): Long {
        val remaining = remaining
        if (remaining == 0L) return -1
        val size = minOf(length.toLong(), remaining)
        readFully(dst, offset, size)
        return size
    }
}

actual class ByteReadPacket
internal actual constructor(head: ChunkBuffer, remaining: Long, pool: ObjectPool<ChunkBuffer>) :
    ByteReadPacketPlatformBase(head, remaining, pool), Input {
    actual constructor(head: ChunkBuffer, pool: ObjectPool<ChunkBuffer>) : this(head, head.remainingAll(), pool)

    init {
        markNoMoreChunksAvailable()
    }

    final override fun fill() = null

    override fun fill(destination: Buffer): Boolean {
        return true
    }

    override fun closeSource() {
    }

    actual companion object {
        actual val Empty: ByteReadPacket
            get() = ByteReadPacket(ChunkBuffer.Empty, object : NoPoolImpl<ChunkBuffer>() {
                override fun borrow() = ChunkBuffer.Empty
            })

        actual inline val ReservedSize get() = Buffer.ReservedSize
    }
}

actual fun ByteReadPacket(array: ByteArray, offset: Int, length: Int, block: (ByteArray) -> Unit): ByteReadPacket {
    if (length == 0) {
        block(array)
        return ByteReadPacket(ChunkBuffer.Empty, ChunkBuffer.NoPool)
    }

    val pool = object : SingleInstancePool<ChunkBuffer>() {
        override fun produceInstance(): ChunkBuffer {
            val content = array.pin()
            val base = content.addressOf(offset)

            return ChunkBuffer(Memory.of(base, length), null)
        }

        override fun disposeInstance(instance: ChunkBuffer) {
            block(array)
        }
    }

    return ByteReadPacket(pool.borrow().apply { resetForRead() }, pool)
}
