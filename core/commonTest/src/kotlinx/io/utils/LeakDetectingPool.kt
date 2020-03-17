@file:Suppress("FORBIDDEN_IDENTITY_EQUALS")

package kotlinx.io.utils

import kotlinx.io.buffer.*
import kotlinx.io.pool.*

/**
 * The pool that tracks every borrowed object and check if it belongs to this pool.
 *
 * It also verifies if this pool is empty at close.
 */
internal class LeakDetectingPool(private val size: Int = DEFAULT_BUFFER_SIZE) : ObjectPool<Buffer> {
    override val capacity: Int = Int.MAX_VALUE
    private val allocated = mutableListOf<Buffer>()
    private val removed = mutableListOf<Buffer>()

    override fun borrow(): Buffer {
        return bufferOf(ByteArray(size)).also {
            allocated += it
        }
    }

    override fun recycle(instance: Buffer) {
        if (allocated.find { instance.sameAs(it) } == null) {
            error("Buffer was allocated in different pool instance.")
        }
        val find = removed.find { instance.sameAs(it) }
        if (find != null) {
            error("Buffer has been already recycled.")
        }

        removed.add(instance)
    }

    override fun close() {
        val liveBuffers = allocated.size - removed.size
        check(liveBuffers == 0) {
            "Memory leak: $liveBuffers buffers still in use. Total allocated: ${allocated.size}"
        }
    }
}
