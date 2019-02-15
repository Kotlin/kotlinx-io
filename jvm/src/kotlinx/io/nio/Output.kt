package kotlinx.io.nio

import kotlinx.io.core.*
import kotlinx.io.core.internal.*
import kotlinx.io.pool.*
import java.nio.channels.*

private class ChannelAsOutput(
    pool: ObjectPool<ChunkBuffer>,
    val channel: WritableByteChannel
) : AbstractOutput(pool) {
    override fun flush(buffer: Buffer) {
        channel.write(buffer)
    }

    override fun closeDestination() {
        channel.close()
    }
}

fun WritableByteChannel.asOutput(
    pool: ObjectPool<ChunkBuffer> = ChunkBuffer.Pool
): Output = ChannelAsOutput(pool, this)
