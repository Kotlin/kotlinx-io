package kotlinx.io.nio

import kotlinx.io.bits.Memory
import kotlinx.io.bits.sliceSafe
import kotlinx.io.core.*
import kotlinx.io.core.internal.*
import kotlinx.io.pool.*
import java.nio.channels.*

private class ChannelAsOutput(
    pool: ObjectPool<ChunkBuffer>,
    val channel: WritableByteChannel
) : AbstractOutput(pool) {
    override fun flush(source: Memory, offset: Int, length: Int) {
        val slice = source.buffer.sliceSafe(offset, length)
        while (slice.hasRemaining()) {
            channel.write(slice)
        }
    }

    override fun closeDestination() {
        channel.close()
    }
}

fun WritableByteChannel.asOutput(
    pool: ObjectPool<ChunkBuffer> = ChunkBuffer.Pool
): Output = ChannelAsOutput(pool, this)
