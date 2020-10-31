package kotlinx.io.pool

import kotlinx.io.Closeable
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public interface ObjectPool<T : Any> : Closeable {
    /**
     * Pool capacity
     */
    public val capacity: Int

    /**
     * borrow an instance. Pool can recycle an old instance or create a new one
     */
    public fun borrow(): T

    /**
     * Recycle an instance. Should be recycled what was borrowed before otherwise could fail
     */
    public fun recycle(instance: T)
}

/**
 * Borrows and instance of [T] from the pool, invokes [block] with it and finally recycles it
 */
public inline fun <T : Any, R> ObjectPool<T>.useInstance(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val instance = borrow()

    try {
        return block(instance)
    } finally {
        recycle(instance)
    }
}
