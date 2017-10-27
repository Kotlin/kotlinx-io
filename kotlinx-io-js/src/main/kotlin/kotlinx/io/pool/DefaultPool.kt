package kotlinx.io.pool

actual abstract class DefaultPool<T : Any>(actual override final val capacity: Int) : ObjectPool<T> {
    private val instances = arrayOfNulls<Any?>(capacity)
    private var size = 0

    actual protected abstract fun produceInstance(): T
    actual protected open fun disposeInstance(instance: T) {}

    actual protected open fun clearInstance(instance: T): T = instance
    actual protected open fun validateInstance(instance: T) {}

    override final fun borrow(): T {
        if (size == 0) return produceInstance()
        val idx = --size

        @Suppress("UNCHECKED_CAST")
        val instance = instances[idx] as T
        instances[idx] = null

        return instance
    }

    override final fun recycle(instance: T) {
        if (size == capacity) disposeInstance(instance)
        instances[size++] = instance
    }

    override final fun dispose() {
        for (i in 0 until size) {
            @Suppress("UNCHECKED_CAST")
            val instance = instances[i] as T
            instances[i] = null
            disposeInstance(instance)
        }
        size = 0
    }
}