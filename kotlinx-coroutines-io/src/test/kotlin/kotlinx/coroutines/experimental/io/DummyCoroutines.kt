package kotlinx.coroutines.experimental.io

import kotlin.coroutines.experimental.*

class DummyCoroutines {
    private var failure: Throwable? = null
    private val queue = ArrayList<Task<*>>()
    private var liveCoroutines = 0

    private inner class Completion : Continuation<Unit> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resume(value: Unit) {
            liveCoroutines--
            process()
        }

        override fun resumeWithException(exception: Throwable) {
            liveCoroutines--
            failure = exception
        }
    }

    private val completion = Completion()

    fun schedule(c: Continuation<Unit>) {
        ensureNotFailed()
        liveCoroutines++
        queue += Task.Resume(c, Unit)
    }

    fun schedule(block: suspend () -> Unit) {
        schedule(block.createCoroutine(completion))
    }

    fun run() {
        if (liveCoroutines == 0) throw IllegalStateException("No coroutines has been scheduled")
        ensureNotFailed()

        process()
        if (liveCoroutines > 0) throw IllegalStateException("There are suspended coroutines remaining")
    }

    private fun process() {
        ensureNotFailed()

        while (queue.isNotEmpty()) {
            queue.removeAt(0).run()
            ensureNotFailed()
        }
    }

    private fun ensureNotFailed() {
        failure?.let { throw it }
    }

    sealed class Task<T>(val c: Continuation<T>) {
        abstract fun run()

        class Resume<T>(c: Continuation<T>, val value: T) : Task<T>(c) {
            override fun run() {
                c.resume(value)
            }
        }

        class ResumeExceptionally(c: Continuation<*>, val t: Throwable) : Task<Nothing>(c) {
            override fun run() {
                c.resumeWithException(t)
            }
        }
    }
}
