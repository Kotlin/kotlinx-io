package kotlinx.io.core

import kotlinx.io.pool.*
import kotlinx.cinterop.*

actual abstract class ByteReadPacketPlatformBase protected actual constructor(head: IoBuffer, remaining: Long, pool: ObjectPool<IoBuffer>) : ByteReadPacketBase(head, remaining, pool), Input {

    override fun readFully(dst: CPointer<ByteVar>, offset: Int, length: Int) {
        return readFully(dst, offset.toLong(), length.toLong())
    }

    override fun readFully(dst: CPointer<ByteVar>, offset: Long, length: Long) {
        require(length <= remaining) { "Not enough bytes available ($remaining) to read $length bytes" }
        require(length >= 0L) { "length shouldn't be negative: $length" }

        var copied = 0L

        @Suppress("INVISIBLE_MEMBER")
        takeWhile { buffer: IoBuffer ->
            val rc = buffer.readAvailable(dst, copied, length - copied)
            if (rc > 0) copied += rc
            copied < length
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
internal actual constructor(head: IoBuffer, remaining: Long, pool: ObjectPool<IoBuffer>) : ByteReadPacketPlatformBase(head, remaining, pool), Input {
    actual constructor(head: IoBuffer, pool: ObjectPool<IoBuffer>) : this(head, @Suppress("INVISIBLE_MEMBER") head.remainingAll(), pool)

    init {
        if (remaining == 0L) {
            doFill()
        }
    }

    final override fun fill() = null
    override fun closeSource() {
    }

    actual companion object {
        actual val Empty: ByteReadPacket get() = ByteReadPacketBase.Empty
        actual val ReservedSize get() = ByteReadPacketBase.ReservedSize
    }
}