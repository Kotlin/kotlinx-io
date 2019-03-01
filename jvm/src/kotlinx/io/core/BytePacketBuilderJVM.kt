package kotlinx.io.core

import kotlinx.io.core.internal.*
import kotlinx.io.pool.*
import java.nio.*

@DangerousInternalIoApi
@Deprecated("Will be removed in future releases.", level = DeprecationLevel.ERROR)
@Suppress("DEPRECATION_ERROR")
actual abstract class BytePacketBuilderPlatformBase
internal actual constructor(pool: ObjectPool<IoBuffer>) : BytePacketBuilderBase(pool) {
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
