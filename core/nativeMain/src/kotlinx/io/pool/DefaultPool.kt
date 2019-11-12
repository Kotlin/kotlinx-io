package kotlinx.io.pool

import kotlin.native.concurrent.*

actual abstract class DefaultPool<T : Any> actual constructor(actual override final val capacity: Int) : ObjectPool<T> {
    private val instances = arrayOfNulls<Any?>(capacity)
    private var size = 0

    protected actual abstract fun produceInstance(): T
    protected actual open fun disposeInstance(instance: T) {}

    protected actual open fun clearInstance(instance: T): T = instance
    protected actual open fun validateInstance(instance: T) {}

    init {
        ensureNeverFrozen()
    }

    actual final override fun borrow(): T {
        if (size == 0) return produceInstance()
        val idx = --size

        @Suppress("UNCHECKED_CAST")
        val instance = instances[idx] as T
        instances[idx] = null

        return clearInstance(instance)
    }

    actual final override fun recycle(instance: T) {
        validateInstance(instance)
        if (size == capacity) {
            disposeInstance(instance)
        } else {
            instances[size++] = instance
        }
    }

    actual final override fun close() {
        for (i in 0 until size) {
            @Suppress("UNCHECKED_CAST")
            val instance = instances[i] as T
            instances[i] = null
            disposeInstance(instance)
        }
        size = 0
    }
}
