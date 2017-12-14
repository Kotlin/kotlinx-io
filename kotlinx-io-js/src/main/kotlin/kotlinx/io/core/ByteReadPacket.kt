package kotlinx.io.core

import kotlinx.io.pool.*
import org.khronos.webgl.*

actual abstract class ByteReadPacketPlatformBase
    protected actual constructor(head: BufferView, remaining: Long, pool: ObjectPool<BufferView>) : ByteReadPacketBase(head, remaining, pool), Input {

    override fun readFully(dst: Int8Array, offset: Int, length: Int) {
        if (remaining < length) throw IllegalArgumentException("Not enough bytes available ($remaining) to read $length bytes")
        var copied = 0

        @Suppress("INVISIBLE_MEMBER")
        takeWhile { buffer: BufferView ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc > 0) copied += rc
            copied < length
        }
    }

    override fun readFully(dst: ArrayBuffer, offset: Int, length: Int) {
        if (remaining < length) throw IllegalArgumentException("Not enough bytes available ($remaining) to read $length bytes")
        var copied = 0

        @Suppress("INVISIBLE_MEMBER")
        takeWhile { buffer: BufferView ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc > 0) copied += rc
            copied < length
        }
    }

    override fun readFully(dst: ArrayBufferView, offset: Int, length: Int) {
        if (remaining < length) throw IllegalArgumentException("Not enough bytes available ($remaining) to read $length bytes")
        var copied = 0

        @Suppress("INVISIBLE_MEMBER")
        takeWhile { buffer: BufferView ->
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
    internal actual constructor(head: BufferView, remaining: Long, pool: ObjectPool<BufferView>) : ByteReadPacketPlatformBase(head, remaining, pool), Input {
    actual constructor(head: BufferView, pool: ObjectPool<BufferView>) : this(head, @Suppress("INVISIBLE_MEMBER") head.remainingAll(), pool)

    final override fun fill() = null

    actual companion object {
        actual val Empty = ByteReadPacketBase.Empty
        actual val ReservedSize = ByteReadPacketBase.ReservedSize
    }
}
