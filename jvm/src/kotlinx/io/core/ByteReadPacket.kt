package kotlinx.io.core

import kotlinx.io.core.internal.*
import kotlinx.io.pool.*
import java.nio.*
import kotlin.require

@DangerousInternalIoApi
actual abstract class ByteReadPacketPlatformBase protected actual constructor(
    head: IoBuffer,
    remaining: Long,
    pool: ObjectPool<IoBuffer>
) : ByteReadPacketBase(head, remaining, pool), Input {

    override fun readFully(dst: ByteBuffer, length: Int) {
        require(length <= remaining) { "Not enough bytes available ($remaining) to read $length bytes" }
        require(length <= dst.remaining()) { "Not enough free space in destination buffer to write $length bytes" }
        var copied = 0

        takeWhile { buffer: IoBuffer ->
            val rc = buffer.readAvailable(dst, length - copied)
            if (rc > 0) copied += rc
            copied < length
        }
    }

    override fun readAvailable(dst: ByteBuffer, length: Int): Int {
        val remaining = remaining
        if (remaining == 0L) return -1
        val size = minOf(dst.remaining().toLong(), length.toLong(), remaining).toInt()
        readFully(dst, size)
        return size
    }
}

actual class ByteReadPacket
internal actual constructor(head: IoBuffer, remaining: Long, pool: ObjectPool<IoBuffer>) :
    ByteReadPacketPlatformBase(head, remaining, pool), Input {
    actual constructor(head: IoBuffer, pool: ObjectPool<IoBuffer>) : this(
        head,
        head.remainingAll(),
        pool
    )

    init {
        markNoMoreChunksAvailable()
    }

    final override fun fill() = null

    override fun closeSource() {
    }

    actual companion object {
        actual val Empty get() = ByteReadPacketBase.Empty
        actual val ReservedSize get() = ByteReadPacketBase.ReservedSize
    }
}
