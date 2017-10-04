package kotlinx.io.pool

interface ObjectPool<T : Any> {
    val capacity: Int
    fun borrow(): T
    fun recycle(instance: T) // can only recycle what was borrowed before
    fun dispose()
}

abstract class NoPoolImpl<T : Any> : ObjectPool<T> {
    override val capacity: Int
        get() = 0

    override fun recycle(instance: T) {
    }

    override fun dispose() {
    }
}

expect abstract class DefaultPool<T : Any> : ObjectPool<T> {
    final override val capacity: Int

    protected abstract fun produceInstance(): T
    protected abstract fun disposeInstance(instance: T)

    protected open fun clearInstance(instance: T): T
    protected open fun validateInstance(instance: T)
}
