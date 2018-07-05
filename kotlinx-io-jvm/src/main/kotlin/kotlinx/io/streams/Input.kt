package kotlinx.io.streams

import kotlinx.io.core.*
import kotlinx.io.pool.*
import java.io.*

internal class InputStreamAsInput(private val stream: InputStream, pool: ObjectPool<IoBuffer>)
    : ByteReadPacketPlatformBase(IoBuffer.Empty, 0L, pool), Input {
    override fun fill(): IoBuffer? {
        val buffer = ByteArrayPool.borrow()
        try {
            val rc = stream.read(buffer)
            val result = when {
                rc >= 0 -> pool.borrow().also { it.writeFully(buffer, 0, rc) }
                else -> null
            }

            ByteArrayPool.recycle(buffer)
            return result
        } catch (t: Throwable) {
            ByteArrayPool.recycle(buffer)
            throw t
        }
    }

    override fun closeSource() {
        stream.close()
    }
}

fun InputStream.asInput(pool: ObjectPool<IoBuffer> = IoBuffer.Pool): Input = InputStreamAsInput(this, pool)