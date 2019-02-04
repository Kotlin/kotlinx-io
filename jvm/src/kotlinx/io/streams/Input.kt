package kotlinx.io.streams

import kotlinx.io.core.*
import kotlinx.io.core.IoBuffer.*
import kotlinx.io.pool.*
import java.io.*

internal class InputStreamAsInput(private val stream: InputStream, pool: ObjectPool<IoBuffer>) :
    AbstractInput(pool = pool), Input {
    override fun fill(): IoBuffer? {
        val buffer = ByteArrayPool.borrow()
        try {
            val rc = stream.read(buffer, 0, ByteArrayPoolBufferSize - IoBuffer.ReservedSize)
            val result = when {
                rc >= 0 -> pool.borrow().also {
                    it.reserveEndGap(IoBuffer.ReservedSize); it.writeFully(
                    buffer,
                    0,
                    rc
                )
                }
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
