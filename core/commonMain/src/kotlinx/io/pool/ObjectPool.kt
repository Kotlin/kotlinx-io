package kotlinx.io.pool

import kotlinx.io.*

interface ObjectPool<T : Any> : Closeable {
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
}
