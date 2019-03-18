package kotlinx.io.streams

import kotlinx.io.bits.Memory
import kotlinx.io.bits.sliceSafe
import kotlinx.io.core.*
import kotlinx.io.core.internal.*
import kotlinx.io.pool.*
import java.io.*

private class OutputStreamAdapter(pool: ObjectPool<ChunkBuffer>, private val stream: OutputStream) :
    AbstractOutput(pool) {
    override fun flush(source: Memory, offset: Int, length: Int) {
        val nioBuffer = source.buffer
        if (nioBuffer.hasArray() && !nioBuffer.isReadOnly) {
            stream.write(nioBuffer.array(), nioBuffer.arrayOffset() + offset, length)
            return
        }

        val array = ByteArrayPool.borrow()
        val slice = nioBuffer.sliceSafe(offset, length)
        try {
            do {
                val partSize = minOf(slice.remaining(), array.size)
                if (partSize == 0) break

                slice.get(array, 0, partSize)
                stream.write(array, 0, partSize)
            } while (true)
        } finally {
            ByteArrayPool.recycle(array)
        }
    }

    override fun closeDestination() {
        stream.close()
    }
}

fun OutputStream.asOutput(): Output = OutputStreamAdapter(ChunkBuffer.Pool, this)
