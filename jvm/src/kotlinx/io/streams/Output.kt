package kotlinx.io.streams

import kotlinx.io.core.*
import kotlinx.io.core.internal.*
import kotlinx.io.pool.*
import java.io.*

private class OutputStreamAdapter(pool: ObjectPool<ChunkBuffer>, private val stream: OutputStream) :
    AbstractOutput(pool) {
    override fun flush(buffer: Buffer) {
        val nioBuffer = buffer.memory.buffer
        if (nioBuffer.hasArray() && !nioBuffer.isReadOnly) {
            stream.write(nioBuffer.array(), nioBuffer.arrayOffset() + buffer.readPosition, buffer.readRemaining)
            buffer.discard(buffer.readRemaining)
            return
        }

        val array = ByteArrayPool.borrow()
        try {
            while (buffer.canRead()) {
                val rc = buffer.readAvailable(array)
                if (rc > 0) {
                    stream.write(array, 0, rc)
                }
            }
        } finally {
            ByteArrayPool.recycle(array)
        }
    }

    override fun closeDestination() {
        stream.close()
    }
}

fun OutputStream.asOutput(): Output = OutputStreamAdapter(ChunkBuffer.Pool, this)
