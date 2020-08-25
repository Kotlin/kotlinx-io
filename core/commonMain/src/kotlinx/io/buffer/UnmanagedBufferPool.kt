package kotlinx.io.buffer

import kotlinx.io.pool.ObjectPool
import kotlin.native.concurrent.ThreadLocal

internal class UnmanagedBufferPool(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) : ObjectPool<Buffer> {
    override val capacity: Int = Int.MAX_VALUE

    override fun borrow(): Buffer = bufferOf(ByteArray(bufferSize))

    override fun recycle(instance: Buffer) {}

    override fun close() {}

    @ThreadLocal
    companion object {
        val Instance = UnmanagedBufferPool()
    }
}
