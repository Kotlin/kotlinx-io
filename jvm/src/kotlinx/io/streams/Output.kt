package kotlinx.io.streams

import kotlinx.io.core.*
import kotlinx.io.pool.*
import java.io.*

private class OutputStreamAdapter(pool: ObjectPool<IoBuffer>, private val stream: OutputStream) : AbstractOutput(pool) {
    override fun flush(buffer: IoBuffer) {
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

fun OutputStream.asOutput(): Output = OutputStreamAdapter(IoBuffer.Pool, this)
