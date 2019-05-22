package kotlinx.io.pool

/**
 * Default object pool implementation.
 */
expect abstract class DefaultPool<T : Any>(capacity: Int) : ObjectPool<T> {
    /**
     * Pool capacity.
     */
    final override val capacity: Int

    /**
     * Creates a new instance of [T]
     */
    protected abstract fun produceInstance(): T

    /**
     * Dispose [instance] and release it's resources
     */
    protected open fun disposeInstance(instance: T)

    /**
     * Clear [instance]'s state before reuse: reset pointers, counters and so on
     */
    protected open fun clearInstance(instance: T): T

    /**
     * Validate [instance] of [T]. Could verify that the object has been borrowed from this pool
     */
    protected open fun validateInstance(instance: T)

    final override fun borrow(): T

    final override fun recycle(instance: T)

    final override fun close()
}