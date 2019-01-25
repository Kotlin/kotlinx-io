package kotlinx.io.nio

import kotlinx.io.core.*
import kotlinx.io.pool.*
import java.nio.channels.*

private class ChannelAsOutput(pool: ObjectPool<IoBuffer>,
                              val channel: WritableByteChannel
) : AbstractOutput(pool) {
    override fun flush(buffer: IoBuffer) {
        buffer.readDirect { bb ->
            while (bb.hasRemaining()) {
                channel.write(bb)
            }
        }
    }

    override fun closeDestination() {
        channel.close()
    }
}

fun WritableByteChannel.asOutput(pool: ObjectPool<IoBuffer> = IoBuffer.Pool): Output
        = ChannelAsOutput(pool, this)
