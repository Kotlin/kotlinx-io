package kotlinx.io.core

import kotlinx.io.pool.*
import org.khronos.webgl.*

actual abstract class ByteReadPacketPlatformBase
    protected actual constructor(head: IoBuffer, remaining: Long, pool: ObjectPool<IoBuffer>) : ByteReadPacketBase(head, remaining, pool), Input {

    override fun readFully(dst: Int8Array, offset: Int, length: Int) {
        if (remaining < length) throw IllegalArgumentException("Not enough bytes available ($remaining) to read $length bytes")
        var copied = 0

        @Suppress("INVISIBLE_MEMBER")
        takeWhile { buffer: IoBuffer ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc > 0) copied += rc
            copied < length
        }
    }

    override fun readFully(dst: ArrayBuffer, offset: Int, length: Int) {
        if (remaining < length) throw IllegalArgumentException("Not enough bytes available ($remaining) to read $length bytes")
        var copied = 0

        @Suppress("INVISIBLE_MEMBER")
        takeWhile { buffer: IoBuffer ->
            val rc = buffer.readAvailable(dst, offset + copied, length - copied)
            if (rc > 0) copied += rc
            copied < length
        }
    }

    override fun readFully(dst: ArrayBufferView, offset: Int, length: Int) {
        if (remaining < length) throw IllegalArgumentException("Not enough bytes available ($remaining) to read $length bytes")
        var copied = 0

        @Suppress("INVISIBLE_MEMBER")
        takeWhile { buffer: IoBuffer ->
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
    internal actual constructor(head: IoBuffer, remaining: Long, pool: ObjectPool<IoBuffer>) : ByteReadPacketPlatformBase(head, remaining, pool), Input {
    actual constructor(head: IoBuffer, pool: ObjectPool<IoBuffer>) : this(head, head.remainingAll(), pool)

    final override fun fill() = null
    override fun closeSource() {
    }

    actual companion object {
        actual val Empty: ByteReadPacket get() = ByteReadPacketBase.Empty
        actual val ReservedSize get() = ByteReadPacketBase.ReservedSize
    }
}

actual fun ByteReadPacket(array: ByteArray, offset: Int, length: Int, block: (ByteArray) -> Unit): ByteReadPacket {
    val content = array.asDynamic() as Int8Array
    val sub = when {
        offset == 0 && length == array.size -> content.buffer
        else -> content.buffer.slice(offset, offset + length)
    }

    val pool = object : SingleInstancePool<IoBuffer>() {
        override fun produceInstance(): IoBuffer {
            return IoBuffer(sub, null)
        }

        override fun disposeInstance(instance: IoBuffer) {
            block(array)
        }
    }

    return ByteReadPacket(pool.borrow().apply { resetForRead() }, pool)
}
