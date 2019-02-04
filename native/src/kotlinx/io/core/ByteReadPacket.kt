package kotlinx.io.core

import kotlinx.io.pool.*
import kotlinx.cinterop.*
import kotlinx.io.core.internal.*
import kotlin.require

@DangerousInternalIoApi
actual abstract class ByteReadPacketPlatformBase protected actual constructor(head: IoBuffer, remaining: Long, pool: ObjectPool<IoBuffer>) : ByteReadPacketBase(head, remaining, pool), Input {

    override fun readFully(dst: CPointer<ByteVar>, offset: Int, length: Int) {
        return readFully(dst, offset.toLong(), length.toLong())
    }

    override fun readFully(dst: CPointer<ByteVar>, offset: Long, length: Long) {
        require(length <= remaining) { "Not enough bytes available ($remaining) to read $length bytes" }
        require(length >= 0L) { "length shouldn't be negative: $length" }

        var copied = 0L

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
    actual constructor(head: IoBuffer, pool: ObjectPool<IoBuffer>) : this(head, head.remainingAll(), pool)

    init {
        markNoMoreChunksAvailable()
    }

    final override fun fill() = null
    override fun closeSource() {
    }

    actual companion object {
        actual val Empty: ByteReadPacket
            get() = ByteReadPacket(IoBuffer.Empty, object : NoPoolImpl<IoBuffer>() {
                override fun borrow() = IoBuffer.Empty
            })

        actual inline val ReservedSize get() = IoBuffer.ReservedSize
    }
}

actual fun ByteReadPacket(array: ByteArray, offset: Int, length: Int, block: (ByteArray) -> Unit): ByteReadPacket {
    if (length == 0) {
        block(array)
        return ByteReadPacket(IoBuffer.Empty, IoBuffer.NoPool)
    }

    val pool = object : SingleInstancePool<IoBuffer>() {
        override fun produceInstance(): IoBuffer {
            val content = array.pin()
            val base = content.addressOf(offset)

            return IoBuffer(base, length, null)
        }

        override fun disposeInstance(instance: IoBuffer) {
            block(array)
        }
    }

    return ByteReadPacket(pool.borrow().apply { resetForRead() }, pool)
}
