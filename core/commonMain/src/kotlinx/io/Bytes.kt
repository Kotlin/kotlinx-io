package kotlinx.io

import kotlinx.io.buffer.Buffer
import kotlinx.io.pool.ObjectPool
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal typealias BytesPointer = Int

/**
 * Read-only bytes container.
 *
 * Use [input] to create readable [Input].
 * All inputs from a [Bytes] instance share the same buffers.
 *
 * ```
 * buildBytes {
 * }
 * ```
 * 1. create
 * 2. close
 * 3. example
 */
public class Bytes internal constructor(internal val bufferPool: ObjectPool<Buffer>) : Closeable {
    @PublishedApi
    internal var buffers: Array<Buffer?> = arrayOfNulls(initialPreviewSize)

    @PublishedApi
    internal var limits: IntArray = IntArray(initialPreviewSize)

    @PublishedApi
    internal var head: Int = 0

    internal var tail: Int = 0

    /**
     * Calculate the size of [Bytes].
     */
    public fun size(): Int = size(StartPointer)

    /**
     * Create [Input] view on content.
     */
    public fun input(): Input = object : Input(this@Bytes, bufferPool) {
        override fun fill(buffer: Buffer, startIndex: Int, endIndex: Int): Int = 0
        override fun closeSource() {}
    }

    override fun toString(): String = "Bytes($head..$tail)"

    /**
     * Release all data, brokes all input produced by [input()].
     */
    override fun close() {
        (head until tail).forEach {
            bufferPool.recycle(buffers[it]!!)
            buffers[it] = null
        }
        head = 0
        tail = 0
    }

    internal fun append(buffer: Buffer, limit: Int) {
        if (head > 0) {
            // if we are appending buffers after [discardFirst()],
            // compact arrays so we can store more without allocations
            buffers.copyInto(buffers, 0, head, tail)
            limits.copyInto(limits, 0, head, tail)
            tail -= head
            head = 0
        }

        if (tail == buffers.size) {
            buffers = buffers.copyInto(arrayOfNulls(buffers.size * 2))
            limits = limits.copyInto(IntArray(buffers.size * 2))
        }

        buffers[tail] = buffer
        limits[tail] = limit
        tail++
    }

    internal fun discardFirst() {
        if (head == tail)
            throw NoSuchElementException("There is no buffer to discard in this instance")
        buffers[head] = null
        limits[head] = -1
        head++
    }

    internal fun limit(pointer: BytesPointer): Int = limits[pointer + head]

    internal fun isEmpty() = tail == head

    internal fun isAfterLast(index: BytesPointer) = head + index >= tail

    internal fun size(pointer: BytesPointer): Int {
        var sum = 0
        for (index in (pointer + head) until tail) {
            sum += limits[index]
        }
        return sum
    }

    public companion object {
        internal const val InvalidPointer = Int.MIN_VALUE
        internal const val StartPointer = 0

        private const val initialPreviewSize = 1
    }
}

/**
 * Get [Buffer] and limit according to [pointer] offset in [Bytes.buffers].
 */
@PublishedApi
internal inline fun Bytes.pointed(pointer: Int, consumer: (buffer: Buffer, limit: Int) -> Unit) {
    contract {
        callsInPlace(consumer, InvocationKind.EXACTLY_ONCE)
    }

    val index = pointer + head
    val buffer = buffers[index] ?: throw NoSuchElementException("There is no buffer at pointer $pointer.")
    val limit = limits[index]
    consumer(buffer, limit)
}
