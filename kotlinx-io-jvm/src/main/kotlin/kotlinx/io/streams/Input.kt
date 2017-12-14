package kotlinx.io.streams

import kotlinx.io.core.*
import kotlinx.io.pool.*
import java.io.*

internal class InputStreamAsInput(private val stream: InputStream, pool: ObjectPool<BufferView>) : ByteReadPacketPlatformBase(BufferView.Empty, 0L, pool), Input {
    override fun fill(): BufferView? {
        val buffer = ByteArrayPool.borrow()
        try {
            val rc = stream.read(buffer)
            if (rc == -1) {
                ByteArrayPool.recycle(buffer)
                return null
            }

            return pool.borrow().also { it: BufferView -> it.write(buffer, 0, rc) }
        } catch (t: Throwable) {
            ByteArrayPool.recycle(buffer)
            throw t
        }
    }
}

fun InputStream.asInput(pool: ObjectPool<BufferView>): Input = InputStreamAsInput(this, pool)