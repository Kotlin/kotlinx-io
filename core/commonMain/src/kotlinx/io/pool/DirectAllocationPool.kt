package kotlinx.io.pool

/**
 * A pool implementation of zero capacity that always creates new instances
 */
abstract class DirectAllocationPool<T : Any> : ObjectPool<T> {
    override val capacity: Int get() = 0

    override fun recycle(instance: T) {}

    override fun close() {}
}