package kotlinx.io.streams

import kotlinx.io.core.*
import kotlinx.io.core.internal.*
import kotlinx.io.pool.*
import java.io.*

internal class InputStreamAsInput(
    private val stream: InputStream,
    pool: ObjectPool<ChunkBuffer>
) : AbstractInput(pool = pool) {

    override fun fill(destination: Buffer): Boolean {
        val buffer = ByteArrayPool.borrow()
        try {
            val rc = stream.read(buffer, 0, minOf(buffer.size, destination.writeRemaining))
            if (rc == -1) return true
            destination.writeFully(buffer, 0, rc)
            return false
        } finally {
            ByteArrayPool.recycle(buffer)
        }
    }

    override fun closeSource() {
        stream.close()
    }
}

fun InputStream.asInput(pool: ObjectPool<ChunkBuffer> = ChunkBuffer.Pool): Input = InputStreamAsInput(this, pool)
