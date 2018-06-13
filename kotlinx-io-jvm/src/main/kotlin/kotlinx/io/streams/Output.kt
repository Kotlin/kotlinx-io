package kotlinx.io.streams

import kotlinx.io.core.*
import kotlinx.io.pool.*
import java.io.*

private class OutputStreamAdapter(pool: ObjectPool<BufferView>, private val stream: OutputStream): BytePacketBuilderPlatformBase(pool) {
    override fun release() {
        flush()
        stream.close()
    }

    override fun last(buffer: BufferView) {
        val current = tail
        tail = buffer

        if (current === BufferView.Empty) return

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
        last(BufferView.Empty)
    }

    override fun close() {
        flush()
        stream.close()
    }
}

fun OutputStream.asOutput(): Output = OutputStreamAdapter(BufferView.Pool, this)
