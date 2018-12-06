package kotlinx.io.streams

import kotlinx.io.core.*
import kotlinx.io.pool.*
import java.io.*

private class OutputStreamAdapter(pool: ObjectPool<IoBuffer>, private val stream: OutputStream) : AbstractOutput(pool) {
    override fun release() {
        flush()
        stream.close()
    }

    override fun last(buffer: IoBuffer) {
        val current = currentTail
        currentTail = buffer

        if (current === IoBuffer.Empty) return

        val array = ByteArrayPool.borrow()
        try {
            while (current.canRead()) {
                val rc = current.readAvailable(array)
                if (rc > 0) {
                    stream.write(array, 0, rc)
                }
            }
        } finally {
            ByteArrayPool.recycle(array)
            current.release(pool)
        }
    }

    override fun flush() {
        last(IoBuffer.Empty)
    }

    override fun close() {
        flush()
        stream.close()
    }
}

fun OutputStream.asOutput(): Output = OutputStreamAdapter(IoBuffer.Pool, this)
