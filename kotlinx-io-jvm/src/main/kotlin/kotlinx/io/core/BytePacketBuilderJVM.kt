package kotlinx.io.core

import kotlinx.io.pool.*
import java.nio.*

actual abstract class BytePacketBuilderPlatformBase
internal actual constructor(pool: ObjectPool<BufferView>) : BytePacketBuilderBase(pool) {
    override fun writeFully(bb: ByteBuffer) {
        val l = bb.limit()

        writeWhile { chunk ->
            val size = minOf(bb.remaining(), chunk.writeRemaining)
            bb.limit(bb.position() + size)
            chunk.writeFully(bb)
            bb.limit(l)

            bb.hasRemaining()
        }
    }
}
