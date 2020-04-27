package kotlinx.io.buffer

import kotlinx.io.pool.*
import kotlin.native.concurrent.*

internal class UnmanagedBufferPool(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) : ObjectPool<Buffer> {
    override val capacity: Int = Int.MAX_VALUE

    override fun borrow(): Buffer = bufferOf(ByteArray(bufferSize))

    override fun recycle(instance: Buffer) {}

    override fun close() {}

    @ThreadLocal
    companion object {
        @ThreadLocal
        val Instance = UnmanagedBufferPool()
    }
}
