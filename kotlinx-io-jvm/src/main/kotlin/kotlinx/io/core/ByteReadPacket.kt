package kotlinx.io.core

import kotlinx.io.pool.*
import java.nio.*

actual abstract class ByteReadPacketPlatformBase actual constructor(head: BufferView, pool: ObjectPool<BufferView>) : ByteReadPacketBase(head, pool), Input {

    override fun readFully(dst: ByteBuffer, length: Int) {
        require(length <= remaining) { "Not enough bytes available ($remaining) to read $length bytes" }
        require(length <= dst.remaining()) { "Not enough free space in destination buffer to write $length bytes" }
        var copied = 0

        @Suppress("INVISIBLE_MEMBER")
        takeWhile { buffer: BufferView ->
            val rc = buffer.readAvailable(dst, length - copied)
            if (rc > 0) copied += rc
            copied < length
        }
    }

    override fun readAvailable(dst: ByteBuffer, length: Int): Int {
        val remaining = remaining
        if (remaining == 0) return -1
        val size = minOf(dst.remaining(), length, remaining)
        readFully(dst, size)
        return size
    }
}

actual class ByteReadPacket
    actual constructor(head: BufferView, pool: ObjectPool<BufferView>) : ByteReadPacketPlatformBase(head, pool), Input {

    final override fun fill() = null

    actual companion object {
        actual val Empty = ByteReadPacketBase.Empty
        actual val ReservedSize = ByteReadPacketBase.ReservedSize
    }
}