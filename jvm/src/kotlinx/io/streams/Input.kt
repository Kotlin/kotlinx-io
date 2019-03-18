package kotlinx.io.streams

import kotlinx.io.bits.Memory
import kotlinx.io.bits.storeByteArray
import kotlinx.io.core.*
import kotlinx.io.core.internal.*
import kotlinx.io.pool.*
import java.io.*

internal class InputStreamAsInput(
    private val stream: InputStream,
    pool: ObjectPool<ChunkBuffer>
) : AbstractInput(pool = pool) {

    override fun fill(destination: Memory, offset: Int, length: Int): Int {
        if (destination.buffer.hasArray() && !destination.buffer.isReadOnly) {
            return stream.read(destination.buffer.array(), destination.buffer.arrayOffset() + offset, length)
        }

        val buffer = ByteArrayPool.borrow()
        try {
            val rc = stream.read(buffer, 0, minOf(buffer.size, length))
            if (rc == -1) return 0
            destination.storeByteArray(offset, buffer, 0, rc)
            return rc
        } finally {
            ByteArrayPool.recycle(buffer)
        }
    }

    override fun closeSource() {
        stream.close()
    }
}

fun InputStream.asInput(pool: ObjectPool<ChunkBuffer> = ChunkBuffer.Pool): Input = InputStreamAsInput(this, pool)
