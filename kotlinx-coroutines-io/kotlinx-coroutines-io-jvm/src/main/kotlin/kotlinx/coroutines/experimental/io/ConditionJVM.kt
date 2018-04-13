package kotlinx.coroutines.experimental.io

import java.util.concurrent.atomic.*
import kotlin.coroutines.experimental.*
import kotlin.jvm.*

internal actual class Condition actual constructor(val predicate: () -> Boolean) {
    @Volatile
    private var cond: Continuation<Unit>? = null

    actual fun signal() {
        val cond = cond
        if (cond != null && predicate()) {
            if (updater.compareAndSet(this, cond, null)) {
                cond.resume(Unit)
            }
        }
    }


    actual suspend fun await(block: () -> Unit) {
        if (predicate()) return

        return suspendCoroutine { c ->
            if (!updater.compareAndSet(this, null, c)) throw IllegalStateException()
            if (predicate() && updater.compareAndSet(this, c, null)) c.resume(Unit)
            block()
        }
    }

    actual suspend fun await() {
        if (predicate()) return

        return suspendCoroutine { c ->
            if (!updater.compareAndSet(this, null, c)) throw IllegalStateException()
            if (predicate() && updater.compareAndSet(this, c, null)) c.resume(Unit)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        private val updater = AtomicReferenceFieldUpdater.newUpdater<Condition, Continuation<*>>(Condition::class.java,
            Continuation::class.java, "cond") as AtomicReferenceFieldUpdater<Condition, Continuation<Unit>?>
    }
}
