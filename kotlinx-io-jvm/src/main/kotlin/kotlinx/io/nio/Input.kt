package kotlinx.io.nio

import kotlinx.io.core.*
import kotlinx.io.pool.*
import java.nio.*
import java.nio.channels.*

private class ChannelAsInput(private val channel: ReadableByteChannel, pool: ObjectPool<BufferView>) : ByteReadPacketPlatformBase(BufferView.Empty, pool), Input {
    init {
        require(channel !is SelectableChannel || !channel.isBlocking) { "Non-blocking channels as not supported" }
    }

    override fun fill(): BufferView? {
        val buffer: BufferView = pool.borrow()
        try {
            var rc = -1

            buffer.writeDirect(1) { bb: ByteBuffer ->
                rc = channel.read(bb)
            }

            if (rc == -1) {
                pool.recycle(buffer)
                return null
            }

            return buffer
        } catch (t: Throwable) {
            pool.recycle(buffer)
            throw t
        }
    }
}

fun ReadableByteChannel.asInput(pool: ObjectPool<BufferView> = BufferView.Pool): Input = ChannelAsInput(this, pool)

