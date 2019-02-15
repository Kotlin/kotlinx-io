package kotlinx.io.nio

import kotlinx.io.core.*
import kotlinx.io.core.Buffer
import kotlinx.io.core.internal.*
import kotlinx.io.pool.*
import java.nio.channels.*
import kotlin.require

private class ChannelAsInput(private val channel: ReadableByteChannel, pool: ObjectPool<ChunkBuffer>) :
    AbstractInput(pool = pool), Input {
    init {
        require(channel !is SelectableChannel || !channel.isBlocking) { "Non-blocking channels are not supported" }
    }

    override fun fill(destination: Buffer): Boolean {
        return channel.read(destination) == -1
    }

    override fun closeSource() {
        channel.close()
    }
}

fun ReadableByteChannel.asInput(
    pool: ObjectPool<ChunkBuffer> = ChunkBuffer.Pool
): Input = ChannelAsInput(this, pool)

