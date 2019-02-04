package kotlinx.io.core

import kotlinx.io.pool.*
import kotlinx.cinterop.*
import kotlinx.io.core.internal.*

@DangerousInternalIoApi
@Deprecated("Will be removed in future releases.")
@Suppress("DEPRECATION")
actual abstract class BytePacketBuilderPlatformBase
internal actual constructor(pool: ObjectPool<IoBuffer>) : BytePacketBuilderBase(pool) {
    final override fun writeFully(src: CPointer<ByteVar>, offset: Int, length: Int) {
        writeFully(src, offset.toLong(), length.toLong())
    }

    final override fun writeFully(src: CPointer<ByteVar>, offset: Long, length: Long) {
        require(length >= 0L)
        require(offset >= 0L)

        var position = offset
        var rem = length

        writeWhile { chunk ->
            val size = minOf(chunk.writeRemaining.toLong(), rem)
            chunk.writeFully(src, position, size)
            position += size
            rem -= size
            rem > 0
        }
    }
}
