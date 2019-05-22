package kotlinx.io.pool

import kotlinx.atomicfu.*
import kotlin.jvm.*

/**
 * A pool that produces at most one instance
 */
abstract class SingleInstancePool<T : Any> : ObjectPool<T> {
    private val borrowed = atomic(0)
    private val disposed = atomic(false)

    @Volatile
    private var instance: T? = null

    /**
     * Creates a new instance of [T]
     */
    protected abstract fun produceInstance(): T

    /**
     * Dispose [instance] and release it's resources
     */
    protected abstract fun disposeInstance(instance: T)

    final override val capacity: Int get() = 1

    final override fun borrow(): T {
        borrowed.update {
            if (it != 0) throw IllegalStateException("Instance is already consumed")
            1
        }

        val instance = produceInstance()
        this.instance = instance

        return instance
    }

    final override fun recycle(instance: T) {
        if (this.instance !== instance) {
            if (this.instance == null && borrowed.value != 0) {
                throw IllegalStateException("Already recycled or an irrelevant instance tried to be recycled")
            }

            throw IllegalStateException("Unable to recycle irrelevant instance")
        }

        this.instance = null

        if (!disposed.compareAndSet(false, true)) {
            throw IllegalStateException("An instance is already disposed")
        }

        disposeInstance(instance)
    }

    final override fun close() {
        if (disposed.compareAndSet(false, true)) {
            val instance = this.instance ?: return
            this.instance = null

            disposeInstance(instance)
        }
    }
}