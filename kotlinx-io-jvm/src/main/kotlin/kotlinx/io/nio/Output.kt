package kotlinx.io.nio

import kotlinx.io.core.*
import kotlinx.io.pool.*
import java.nio.channels.*

private class ChannelAsOutput(pool: ObjectPool<IoBuffer>,
                              val channel: WritableByteChannel) : BytePacketBuilderPlatformBase(pool) {
    override fun release() {
        flush()
        channel.close()
    }

    override fun last(buffer: IoBuffer) {
        val current = tail
        tail = buffer

        if (current === IoBuffer.Empty) return

        current.readDirect { bb ->
            while (bb.hasRemaining()) {
                channel.write(bb)
            }
        }
    }

    override fun flush() {
        last(IoBuffer.Empty)
    }

    override fun close() {
        flush()
        channel.close()
    }
}

fun WritableByteChannel.asOutput(pool: ObjectPool<IoBuffer> = IoBuffer.Pool): Output
        = ChannelAsOutput(pool, this)