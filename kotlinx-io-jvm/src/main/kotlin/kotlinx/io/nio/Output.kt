package kotlinx.io.nio

import kotlinx.io.core.*
import kotlinx.io.pool.*
import java.nio.channels.*

private class ChannelAsOutput(pool: ObjectPool<BufferView>,
                              val channel: WritableByteChannel) : BytePacketBuilderBase(pool) {
    override fun release() {
        flush()
        channel.close()
    }

    override fun last(buffer: BufferView) {
        val current = tail
        tail = buffer

        if (current === BufferView.Empty) return

        current.readDirect { bb ->
            while (bb.hasRemaining()) {
                channel.write(bb)
            }
        }
    }

    override fun flush() {
        last(BufferView.Empty)
    }

    override fun close() {
        flush()
        channel.close()
    }
}

fun WritableByteChannel.asOutput(pool: ObjectPool<BufferView> = BufferView.Pool): Output
        = ChannelAsOutput(pool, this)