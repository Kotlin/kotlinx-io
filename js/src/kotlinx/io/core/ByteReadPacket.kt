package kotlinx.io.core

import kotlinx.io.bits.*
import kotlinx.io.core.internal.*
import kotlinx.io.pool.*
import org.khronos.webgl.*

@DangerousInternalIoApi
actual abstract class ByteReadPacketPlatformBase
protected actual constructor(head: ChunkBuffer, remaining: Long, pool: ObjectPool<ChunkBuffer>) :
    ByteReadPacketBase(head, remaining, pool), Input {

    override fun readFully(dst: Int8Array, offset: Int, length: Int) {
        if (remaining < length) throw IllegalArgumentException("Not enough bytes available ($remaining) to read $length bytes")
        var copied = 0

        takeWhile { buffer: Buffer ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc > 0) copied += rc
            copied < length
        }
    }

    override fun readFully(dst: ArrayBuffer, offset: Int, length: Int) {
        if (remaining < length) throw IllegalArgumentException("Not enough bytes available ($remaining) to read $length bytes")
        var copied = 0

        takeWhile { buffer: Buffer ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc > 0) copied += rc
            copied < length
        }
    }

    override fun readFully(dst: ArrayBufferView, offset: Int, length: Int) {
        if (remaining < length) throw IllegalArgumentException("Not enough bytes available ($remaining) to read $length bytes")
        var copied = 0

        takeWhile { buffer: Buffer ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc > 0) copied += rc
            copied < length
        }
    }

    override fun readAvailable(dst: Int8Array, offset: Int, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) return -1
        val size = minOf(remaining, length.toLong()).toInt()
        readFully(dst, offset, size)
        return size
    }

    override fun readAvailable(dst: ArrayBuffer, offset: Int, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) return -1
        val size = minOf(remaining, length.toLong()).toInt()
        readFully(dst, offset, size)
        return size
    }

    override fun readAvailable(dst: ArrayBufferView, offset: Int, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) return -1
        val size = minOf(remaining, length.toLong()).toInt()
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

    override fun closeSource() {
    }

    actual companion object {
        actual val Empty: ByteReadPacket
            get() = ByteReadPacket(ChunkBuffer.Empty, object : NoPoolImpl<ChunkBuffer>() {
                override fun borrow() = ChunkBuffer.Empty
            })
        actual inline val ReservedSize get() = IoBuffer.ReservedSize
    }
}

actual fun ByteReadPacket(array: ByteArray, offset: Int, length: Int, block: (ByteArray) -> Unit): ByteReadPacket {
    val content = array.asDynamic() as Int8Array
    val sub = when {
        offset == 0 && length == array.size -> content.buffer
        else -> content.buffer.slice(offset, offset + length)
    }

    val pool = object : SingleInstancePool<ChunkBuffer>() {
        override fun produceInstance(): ChunkBuffer {
            return ChunkBuffer(Memory.of(sub), null)
        }

        override fun disposeInstance(instance: ChunkBuffer) {
            block(array)
        }
    }

    return ByteReadPacket(pool.borrow().apply { resetForRead() }, pool)
}
