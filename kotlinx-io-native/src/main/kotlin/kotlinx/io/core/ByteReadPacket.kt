package kotlinx.io.core

import kotlinx.io.pool.*
import kotlinx.cinterop.*

actual abstract class ByteReadPacketPlatformBase protected actual constructor(head: BufferView, remaining: Long, pool: ObjectPool<BufferView>) : ByteReadPacketBase(head, remaining, pool), Input {

    override fun readFully(dst: CPointer<ByteVar>, offset: Int, length: Int) {
        require(length <= remaining) { "Not enough bytes available ($remaining) to read $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }

        var copied = 0

        @Suppress("INVISIBLE_MEMBER")
        takeWhile { buffer: BufferView ->
            val rc = buffer.readAvailable(dst, copied, length - copied)
            if (rc > 0) copied += rc
            copied < length
        }
    }

    override fun readAvailable(dst: CPointer<ByteVar>, offset: Int, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) return -1
        val size = minOf(length.toLong(), remaining).toInt()
        readFully(dst, offset, size)
        return size
    }
}

actual class ByteReadPacket
internal actual constructor(head: BufferView, remaining: Long, pool: ObjectPool<BufferView>) : ByteReadPacketPlatformBase(head, remaining, pool), Input {
    actual constructor(head: BufferView, pool: ObjectPool<BufferView>) : this(head, @Suppress("INVISIBLE_MEMBER") head.remainingAll(), pool)

    final override fun fill() = null
    override fun closeSource() {
    }

    actual companion object {
        actual val Empty: ByteReadPacket = ByteReadPacketBase.Empty
        actual val ReservedSize = ByteReadPacketBase.ReservedSize
    }
}