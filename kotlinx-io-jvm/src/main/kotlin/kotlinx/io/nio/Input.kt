package kotlinx.io.nio

import kotlinx.io.core.*
import kotlinx.io.pool.*
import java.nio.*
import java.nio.channels.*

private class ChannelAsInput(private val channel: ReadableByteChannel, pool: ObjectPool<IoBuffer>) : ByteReadPacketPlatformBase(IoBuffer.Empty, 0L, pool), Input {
    init {
        require(channel !is SelectableChannel || !channel.isBlocking) { "Non-blocking channels as not supported" }
    }

    override fun fill(): IoBuffer? {
        val buffer: IoBuffer = pool.borrow()
        try {
            var rc = -1

            buffer.writeDirect(1) { bb: ByteBuffer ->
                rc = channel.read(bb)
            }

            if (rc == -1) {
                buffer.release(pool)
                return null
            }

            return buffer
        } catch (t: Throwable) {
            buffer.release(pool)
            throw t
        }
    }

    override fun closeSource() {
        channel.close()
    }
}

fun ReadableByteChannel.asInput(pool: ObjectPool<IoBuffer> = IoBuffer.Pool): Input = ChannelAsInput(this, pool)

