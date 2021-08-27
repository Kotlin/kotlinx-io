package kotlinx.io.buffer

import kotlin.native.concurrent.ThreadLocal
import kotlinx.io.pool.ObjectPool

internal class UnmanagedBufferPool(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) : ObjectPool<Buffer> {
    override val capacity: Int = Int.MAX_VALUE

    override fun borrow(): Buffer = bufferOf(ByteArray(bufferSize))

    override fun recycle(instance: Buffer) {}

    override fun close(): Unit = Unit
}

@ThreadLocal
internal val unmanagedBufferPool: UnmanagedBufferPool = UnmanagedBufferPool()
