package kotlinx.io.pool

interface ObjectPool<T : Any> {
    /**
     * Pool capacity
     */
    val capacity: Int

    /**
     * borrow an instance. Pool can recycle an old instance or create a new one
     */
    fun borrow(): T

    /**
     * Recycle an instance. Should be recycled what was borrowed before otherwise could fail
     */
    fun recycle(instance: T)

    /**
     * Dispose the whole pool. None of borrowed objects could be used after the pool gets disposed
     * otherwise it can result in undefined behaviour
     */
    fun dispose()
}

/**
 * A pool implementation of zero capacity that always creates new instances
 */
abstract class NoPoolImpl<T : Any> : ObjectPool<T> {
    override val capacity: Int
        get() = 0

    override fun recycle(instance: T) {
    }

    override fun dispose() {
    }
}

/**
 * Default object pool implementation
 */
expect abstract class DefaultPool<T : Any> : ObjectPool<T> {
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
}
