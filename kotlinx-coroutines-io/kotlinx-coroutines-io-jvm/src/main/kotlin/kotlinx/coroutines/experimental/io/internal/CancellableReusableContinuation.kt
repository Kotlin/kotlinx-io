package kotlinx.coroutines.io.internal

import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

/**
 * Semi-cancellable reusable continuation. Unlike regular continuation this implementation has limitations:
 * - could be resumed only once per [completeSuspendBlock], undefined behaviour otherwise
 * - [T] should be neither [Throwable] nor [Continuation]
 * - value shouldn't be null
 */
internal class CancellableReusableContinuation<T : Any> : Continuation<T> {
    private val state = atomic<Any?>(null)
    private val jobCancellationHandler = atomic<JobRelation?>(null)

    /**
     * Remember [actual] continuation or return resumed value
     * @return `COROUTINE_SUSPENDED` when remembered or return value if already resumed
     */
    fun completeSuspendBlock(actual: Continuation<T>): Any {
        loop@while (true) {
            val before = state.value

            when (before) {
                null -> {
                    if (!state.compareAndSet(null, actual)) continue@loop
                    parent(actual.context)
                    return COROUTINE_SUSPENDED
                }
                else -> {
                    if (!state.compareAndSet(before, null)) continue@loop
                    if (before is Throwable) throw before
                    @Suppress("UNCHECKED_CAST")
                    return before as T
                }
            }
        }
    }

    private fun parent(context: CoroutineContext) {
        val job = context[Job]
        if (jobCancellationHandler.value?.job === job) return

        if (job == null) {
            jobCancellationHandler.getAndSet(null)?.dispose()
        } else {
            val handler = JobRelation(job)
            val old = this.jobCancellationHandler.getAndUpdate { j ->
                when {
                    j == null -> handler
                    j.job === job -> return
                    else -> handler
                }
            }
            old?.dispose()
        }
    }

    override val context: CoroutineContext
        get() = (state.value as? Continuation<*>)?.context ?: EmptyCoroutineContext

    override fun resumeWith(result: Result<T>) {
        val before = state.getAndUpdate { before ->
            when (before) {
                null -> result.exceptionOrNull() ?: result.getOrThrow()
                is Continuation<*> -> null
                else -> return
            }
        }

        if (before is Continuation<*>) {
            @Suppress("UNCHECKED_CAST")
            val cont = before as Continuation<T>
            cont.resumeWith(result)
        }
    }

    private fun resumeWithExceptionContinuationOnly(job: Job, exception: Throwable) {
        @Suppress("UNCHECKED_CAST")
        val c = state.getAndUpdate {
            if (it !is Continuation<*>) return
            if (it.context[Job] !== job) return
            null
        } as Continuation<T>

        c.resumeWith(Result.failure(exception))
    }

    private inner class JobRelation(val job: Job) : CompletionHandler {
        private var handler: DisposableHandle? = null

        init {
            @UseExperimental(InternalCoroutinesApi::class)
            val h = job.invokeOnCompletion(onCancelling = true, handler = this)

            if (job.isActive) {
                handler = h
            }
        }

        override fun invoke(cause: Throwable?) {
            this@CancellableReusableContinuation.jobCancellationHandler.compareAndSet(this, null)

            handler?.let {
                this.handler = null
                it.dispose()
            }

            if (cause != null) {
                resumeWithExceptionContinuationOnly(job, cause)
            }
        }

        fun dispose() {
            invoke(null)
        }
    }
}
